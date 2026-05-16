package com.mossman.persistence;

import com.mossman.core.model.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonProjectRepositoryTest {

    @Test
    void saveWritesFileThatLoadAllReadsBack(@TempDir Path dir) {
        Project p = Project.create("alpha", "Alpha", UUID.randomUUID());

        try (JsonProjectRepository repo = new JsonProjectRepository(dir)) {
            repo.save(p);
            repo.awaitIdle();
            assertTrue(Files.exists(dir.resolve("alpha.json")));
        }

        JsonProjectRepository reopened = new JsonProjectRepository(dir);
        try {
            reopened.loadAll();
            assertEquals(p, reopened.find("alpha").orElse(null));
            assertEquals(1, reopened.list().size());
        } finally {
            reopened.close();
        }
    }

    @Test
    void cacheIsAuthoritativeForReadsBeforeIoCompletes(@TempDir Path dir) {
        Project p = Project.create("beta", "Beta", UUID.randomUUID());
        try (JsonProjectRepository repo = new JsonProjectRepository(dir)) {
            repo.save(p);
            // Don't awaitIdle — reads should still see the project from cache.
            assertEquals(p, repo.find("beta").orElse(null));
        }
    }

    @Test
    void deleteRemovesFromCacheAndDisk(@TempDir Path dir) throws IOException {
        Project p = Project.create("gamma", "Gamma", UUID.randomUUID());
        try (JsonProjectRepository repo = new JsonProjectRepository(dir)) {
            repo.save(p);
            repo.awaitIdle();
            assertTrue(Files.exists(dir.resolve("gamma.json")));

            assertTrue(repo.delete("gamma"));
            repo.awaitIdle();

            assertTrue(repo.find("gamma").isEmpty());
            assertFalse(Files.exists(dir.resolve("gamma.json")));
            assertFalse(repo.delete("gamma"));
        }
    }

    @Test
    void loadAllSkipsCorruptedFiles(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("good.json"), validJsonFor("good"), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("broken.json"), "{not valid json", StandardCharsets.UTF_8);

        try (JsonProjectRepository repo = new JsonProjectRepository(dir)) {
            repo.loadAll();
            assertNotNull(repo.find("good").orElse(null));
            assertTrue(repo.find("broken").isEmpty());
        }
    }

    @Test
    void concurrentSavesPreserveLastWrite(@TempDir Path dir) throws Exception {
        try (JsonProjectRepository repo = new JsonProjectRepository(dir)) {
            Project base = Project.create("delta", "Delta", UUID.randomUUID());
            int writers = 8;
            int writesPerThread = 50;
            CountDownLatch start = new CountDownLatch(1);
            List<Thread> threads = new java.util.ArrayList<>();
            for (int i = 0; i < writers; i++) {
                final int idx = i;
                Thread t = new Thread(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException ignored) {
                        return;
                    }
                    for (int j = 0; j < writesPerThread; j++) {
                        repo.save(base.withName("name-" + idx + "-" + j));
                    }
                });
                t.start();
                threads.add(t);
            }
            start.countDown();
            for (Thread t : threads) t.join();
            repo.awaitIdle();

            // File on disk should parse cleanly and match the last cached value.
            String text = Files.readString(dir.resolve("delta.json"), StandardCharsets.UTF_8);
            Project onDisk = new ProjectJson().fromJson(text);
            assertEquals(repo.find("delta").orElseThrow(), onDisk);
        }
    }

    @Test
    void rejectsUnsafeProjectIds(@TempDir Path dir) {
        try (JsonProjectRepository repo = new JsonProjectRepository(dir)) {
            Project bad = Project.create("../escape", "Escape", UUID.randomUUID());
            assertThrows(IllegalArgumentException.class, () -> repo.save(bad));
            assertThrows(IllegalArgumentException.class, () -> repo.delete("../escape"));
        }
    }

    @Test
    void closeIsIdempotent(@TempDir Path dir) {
        JsonProjectRepository repo = new JsonProjectRepository(dir);
        repo.close();
        repo.close();
    }

    private static String validJsonFor(String id) {
        Project p = Project.create(id, "Loaded", UUID.randomUUID());
        return new ProjectJson().toJson(p);
    }
}
