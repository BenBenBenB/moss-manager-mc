package com.mossman.adapters.tui;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility to parse an {@link NbtCompound} SNBT payload into a plain {@code Map<String, String>}
 * suitable for passing to patch use cases.
 *
 * Only string and numeric leaf values are extracted; nested compounds and lists are ignored.
 *
 * Usage example (from the game):
 * <pre>
 *   /mossman ticket update MOSS-1 {status:"DONE",priority:"HIGH"}
 *   /mossman project update MOSS {name:"Better Name",description:"A new desc"}
 * </pre>
 */
public final class NbtPatchParser {

    private NbtPatchParser() {}

    /**
     * Converts an {@link NbtCompound} to a mutable {@code Map<String, String>}.
     * Keys are lowercased. Values are the NBT element's string representation
     * for numerics, or the raw string content for string types.
     */
    public static Map<String, String> toMap(NbtCompound nbt) {
        Map<String, String> map = new HashMap<>();
        for (String key : nbt.getKeys()) {
            NbtElement element = nbt.get(key);
            if (element == null) continue;
            byte type = element.getType();
            if (type == NbtElement.STRING_TYPE) {
                // getString returns Optional<String> in 1.21.11
                nbt.getString(key).ifPresent(val -> map.put(key, val));
            } else if (isNumeric(type)) {
                // Numeric types: toString() gives the raw SNBT literal (e.g. "42", "3.14f")
                map.put(key, element.toString());
            }
            // Lists / compounds are silently ignored
        }
        return map;
    }

    private static boolean isNumeric(byte type) {
        return type == NbtElement.BYTE_TYPE
                || type == NbtElement.SHORT_TYPE
                || type == NbtElement.INT_TYPE
                || type == NbtElement.LONG_TYPE
                || type == NbtElement.FLOAT_TYPE
                || type == NbtElement.DOUBLE_TYPE;
    }
}
