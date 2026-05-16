package com.mossman.persistence;

import com.mossman.core.model.Project;
import com.mossman.core.repository.ProjectRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * File-backed {@link ProjectRepository} that serves reads from an in-memory
 * cache and performs all disk IO on a dedicated single-threaded executor.
 *
 * <p>Layout: one JSON file per project at {@code <root>/<projectId>.json}.
 * Writes go through a temp file + atomic rename so a crashed write never
 * leaves a half-written {@code .json} on disk.
 *
 * <p>{@link #save} and {@link #delete} update the cache synchronously and
 * return immediately; the corresponding file IO happens later on the IO
 * thread. Callers that need durability (server shutdown, tests) must call
 * {@link #awaitIdle} or {@link #close}.
 */
public final class JsonProjectRepository implements ProjectRepository, AutoCloseable {

    private static final System.Logger LOG = System.getLogger(JsonProjectRepository.class.getName());
    private static final String SUFFIX = ".json";
    private static final String TMP_SUFFIX = ".json.tmp";

    // Conservative: alphanumerics, dash, underscore. Keeps us safe across
    // case-insensitive filesystems and reserved Windows names.
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final Path root;
    private final ProjectJson json;
    private final ConcurrentMap<String, Project> cache = new ConcurrentHashMap<>();
    private final ExecutorService io;

    public JsonProjectRepository(Path root) {
        this(root, new ProjectJson());
    }

    public JsonProjectRepository(Path root, ProjectJson json) {
        this.root = Objects.requireNonNull(root, "root");
        this.json = Objects.requireNonNull(json, "json");
        this.io = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "moss-manager-json-io");
            t.setDaemon(true);
            return t;
        });
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("creating repo directory " + root, e);
        }
    }

    /**
     * Eagerly populates the in-memory cache from every {@code .json} file in
     * {@link #root}. Files that fail to parse are logged and skipped so a
     * single corrupted project doesn't take down the whole load.
     */
    public void loadAll() {
        cache.clear();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, "*" + SUFFIX)) {
            for (Path file : stream) {
                try {
                    String text = Files.readString(file, StandardCharsets.UTF_8);
                    Project p = json.fromJson(text);
                    if (p == null) {
                        LOG.log(System.Logger.Level.WARNING, "empty project file: {0}", file);
                        continue;
                    }
                    cache.put(p.id(), p);
                } catch (Exception e) {
                    LOG.log(System.Logger.Level.ERROR, "failed to load project file " + file, e);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("listing repo directory " + root, e);
        }
    }

    @Override
    public Optional<Project> find(String projectId) {
        return Optional.ofNullable(cache.get(projectId));
    }

    @Override
    public List<Project> list() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public void save(Project project) {
        Objects.requireNonNull(project, "project");
        validateId(project.id());
        cache.put(project.id(), project);
        String body = json.toJson(project);
        Path target = root.resolve(project.id() + SUFFIX);
        Path tmp = root.resolve(project.id() + TMP_SUFFIX);
        submit(() -> writeAtomically(tmp, target, body));
    }

    @Override
    public boolean delete(String projectId) {
        validateId(projectId);
        boolean removed = cache.remove(projectId) != null;
        if (removed) {
            Path target = root.resolve(projectId + SUFFIX);
            submit(() -> {
                try {
                    Files.deleteIfExists(target);
                } catch (IOException e) {
                    LOG.log(System.Logger.Level.ERROR, "failed to delete " + target, e);
                }
            });
        }
        return removed;
    }

    /** Blocks until the IO queue is drained. */
    public void awaitIdle() {
        Future<?> fence = io.submit(() -> { });
        try {
            fence.get();
        } catch (Exception e) {
            throw new RuntimeException("interrupted waiting for IO queue", e);
        }
    }

    /** Drains queued IO, then shuts the executor down. Idempotent. */
    @Override
    public void close() {
        if (io.isShutdown()) return;
        io.shutdown();
        try {
            if (!io.awaitTermination(30, TimeUnit.SECONDS)) {
                LOG.log(System.Logger.Level.WARNING,
                        "IO executor did not drain within 30s; forcing shutdown");
                io.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            io.shutdownNow();
        }
    }

    private void submit(Runnable task) {
        if (io.isShutdown()) {
            // Run inline so close()-after-save doesn't silently drop writes.
            task.run();
            return;
        }
        io.execute(task);
    }

    private void writeAtomically(Path tmp, Path target, String body) {
        try {
            Files.writeString(tmp, body, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.log(System.Logger.Level.ERROR, "failed to write " + target, e);
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
            }
        }
    }

    private static void validateId(String id) {
        if (id == null || !SAFE_ID.matcher(id).matches()) {
            throw new IllegalArgumentException(
                    "unsafe project id for filesystem use: " + id);
        }
    }
}
