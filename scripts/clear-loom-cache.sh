#!/usr/bin/env bash
# Clears stale Loom remapped JARs for mossman-core.
#
# Loom keys its remapped-JAR cache by mappings version (not JAR content), so
# changing core code does NOT automatically invalidate the cache. Run this
# script after any mossman-core change before running :mossman-tui:runClient.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$SCRIPT_DIR/.."

GLOBAL_CACHE="$ROOT/.gradle/loom-cache/remapped_mods/remapped/com/mossman"
TUI_WORKING="$ROOT/mossman-tui/build/loom-cache/remapped_working"
GUI_WORKING="$ROOT/mossman-gui/build/loom-cache/remapped_working"

echo "Clearing stale mossman-core Loom caches..."

for dir in \
    "$GLOBAL_CACHE/mossman-core-"* \
    "$TUI_WORKING/remapped.com.mossman-mossman-core-"* \
    "$GUI_WORKING/remapped.com.mossman-mossman-core-"*
do
    if [ -e "$dir" ]; then
        echo "  rm $dir"
        rm -rf "$dir"
    fi
done

echo "Done. Run ./gradlew :mossman-tui:runClient to pick up the fresh build."
