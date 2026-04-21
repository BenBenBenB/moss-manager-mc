#!/usr/bin/env bash
# Decompiles Minecraft 1.21.11 (via Loom genSources + Vineflower) and extracts
# Fabric API source JARs into .mc-sources/ for local grepping.
#
# First run downloads Vineflower (~20MB) and decompiles MC (~2-5 min).
# Subsequent runs are instant (sentinel file guards re-extraction).
# To regenerate: rm .mc-sources/.generated

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$SCRIPT_DIR/.."
OUTPUT="$ROOT/.mc-sources"
SENTINEL="$OUTPUT/.generated"

LOOM_MC="$ROOT/.gradle/loom-cache/minecraftMaven/net/minecraft"
FABRIC_SOURCES_DIR="$ROOT/.gradle/loom-cache/remapped_mods/remapped/net/fabricmc/fabric-api"

# ── Idempotency ─────────────────────────────────────────────────────────────
if [[ -f "$SENTINEL" ]]; then
    echo "Already generated. Delete $SENTINEL to regenerate."
    exit 0
fi

# ── Step 1: Run genSources if MC sources JARs are missing ───────────────────
MC_SOURCES_COUNT=$(find "$LOOM_MC" -name "*-sources.jar" 2>/dev/null | wc -l)
if [[ "$MC_SOURCES_COUNT" -eq 0 ]]; then
    echo "Running :mossman-core:genSources (first run: downloads Vineflower ~20MB, ~2-5 min)..."
    cd "$ROOT"
    ./gradlew :mossman-core:build --quiet
    ./gradlew :mossman-core:genSources --quiet
    echo "genSources complete."
else
    echo "MC sources JARs already present ($MC_SOURCES_COUNT found), skipping genSources."
fi

# ── Step 2: Verify output exists ─────────────────────────────────────────────
MC_SOURCES=$(find "$LOOM_MC" -name "*-sources.jar" 2>/dev/null)
if [[ -z "$MC_SOURCES" ]]; then
    echo "ERROR: genSources did not produce any *-sources.jar under $LOOM_MC"
    exit 1
fi

# ── Step 3: Extract Minecraft sources ────────────────────────────────────────
mkdir -p "$OUTPUT/minecraft"
echo "Extracting Minecraft sources..."
while IFS= read -r jar; do
    echo "  $(basename "$jar")"
    unzip -q -o "$jar" -d "$OUTPUT/minecraft"
done <<< "$MC_SOURCES"

# ── Step 4: Extract Fabric API sources ───────────────────────────────────────
mkdir -p "$OUTPUT/fabric-api"
FABRIC_COUNT=0
if [[ -d "$FABRIC_SOURCES_DIR" ]]; then
    while IFS= read -r jar; do
        unzip -q -o "$jar" -d "$OUTPUT/fabric-api"
        (( FABRIC_COUNT++ )) || true
    done < <(find "$FABRIC_SOURCES_DIR" -name "*-sources.jar" 2>/dev/null)
fi
echo "Extracted $FABRIC_COUNT Fabric API source JARs."

# ── Done ─────────────────────────────────────────────────────────────────────
touch "$SENTINEL"
MC_FILES=$(find "$OUTPUT/minecraft" -name '*.java' | wc -l)
FA_FILES=$(find "$OUTPUT/fabric-api" -name '*.java' | wc -l)
echo ""
echo "Done. Sources written to: $OUTPUT"
echo "  $MC_FILES Minecraft .java files"
echo "  $FA_FILES Fabric API .java files"
echo ""
echo "Search example:"
echo "  grep -r 'setFocused' $OUTPUT/minecraft/net/minecraft/client/gui/"
