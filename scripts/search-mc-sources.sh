#!/usr/bin/env bash
# Search decompiled Minecraft/Fabric sources.
# Usage: scripts/search-mc-sources.sh <pattern> [subdir]
#   subdir defaults to "minecraft" — use "fabric-api" for Fabric API
#
# Examples:
#   scripts/search-mc-sources.sh 'class EntryListWidget'
#   scripts/search-mc-sources.sh 'setSelected' minecraft/net/minecraft/client/gui/widget
#   scripts/search-mc-sources.sh 'CommandRegistrationCallback' fabric-api

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCES="$SCRIPT_DIR/../.mc-sources"

if [[ ! -d "$SOURCES" ]]; then
    echo "Sources not yet generated. Run: scripts/gen-sources.sh"
    exit 1
fi

PATTERN="${1:?Usage: search-mc-sources.sh <pattern> [subdir]}"
SUBDIR="${2:-minecraft}"

grep -r --include='*.java' -n "$PATTERN" "$SOURCES/$SUBDIR"
