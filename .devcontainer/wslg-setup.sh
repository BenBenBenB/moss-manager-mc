#!/usr/bin/env bash
# Verifies WSLg display forwarding is ready after the container starts.
# The actual mounts (/tmp/.X11-unix, /mnt/wslg) are declared in devcontainer.json.

XSOCK=/tmp/.X11-unix/X0

if [ ! -S "$XSOCK" ]; then
  echo "WARNING: WSLg X11 socket not found at $XSOCK"
  echo "  1. Open any GUI app from a WSL2 terminal outside this container to initialize WSLg."
  echo "  2. Then rebuild the devcontainer (Ctrl+Shift+P → 'Rebuild Container')."
  exit 0
fi

echo "WSLg display forwarding active. DISPLAY=$DISPLAY"
