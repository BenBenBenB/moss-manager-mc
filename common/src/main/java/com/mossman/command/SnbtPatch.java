package com.mossman.command;

import com.mossman.core.permission.Permission;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Thin reader on top of a {@link CompoundTag} that pulls only the field
 * shapes the command layer cares about. Every {@code optionalX} method
 * returns {@link Optional#empty()} when the key is absent so callers can
 * implement patch semantics ("leave the field alone if not provided").
 *
 * <p>Type mismatches throw {@link Format} (an {@link IllegalArgumentException})
 * with a message naming the offending key, so command handlers can catch
 * once at the top and surface the message via {@code sendFailure}.
 */
public final class SnbtPatch {

    public static final class Format extends IllegalArgumentException {
        Format(String message) { super(message); }
    }

    private final CompoundTag tag;

    public SnbtPatch(CompoundTag tag) {
        this.tag = tag;
    }

    public boolean has(String key) {
        return tag.contains(key);
    }

    public Optional<String> optionalString(String key) {
        if (!tag.contains(key)) return Optional.empty();
        return Optional.of(tag.getString(key)
                .orElseThrow(() -> new Format("expected string for key '" + key + "'")));
    }

    public Optional<Integer> optionalInt(String key) {
        if (!tag.contains(key)) return Optional.empty();
        return Optional.of(tag.getInt(key)
                .orElseThrow(() -> new Format("expected int for key '" + key + "'")));
    }

    public Optional<Boolean> optionalBool(String key) {
        if (!tag.contains(key)) return Optional.empty();
        return Optional.of(tag.getBoolean(key)
                .orElseThrow(() -> new Format("expected boolean for key '" + key + "'")));
    }

    public Optional<UUID> optionalUuid(String key) {
        return optionalString(key).map(s -> {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                throw new Format("invalid UUID for key '" + key + "': " + s);
            }
        });
    }

    public Optional<Set<Permission>> optionalPermissions(String key) {
        if (!tag.contains(key)) return Optional.empty();
        ListTag list = tag.getList(key)
                .orElseThrow(() -> new Format("expected list for key '" + key + "'"));
        EnumSet<Permission> out = EnumSet.noneOf(Permission.class);
        for (int i = 0; i < list.size(); i++) {
            final int idx = i;
            String name = list.getString(i)
                    .orElseThrow(() -> new Format("expected string in '" + key + "' at index " + idx));
            try {
                out.add(Permission.valueOf(name));
            } catch (IllegalArgumentException e) {
                throw new Format("unknown permission '" + name + "' in '" + key + "'");
            }
        }
        return Optional.of(out);
    }
}
