#!/usr/bin/env bash
# Sends a sample navigation state to the running emery emulator.
# Usage: tools/send_sample.sh [right|left|straight]   (STATE=0 resets to the waiting screen: tools/send_sample.sh idle)
set -euo pipefail
cd "$(dirname "$0")/.."

KIND="${1:-right}"
if [ "$KIND" = "idle" ]; then
  exec pebble send-app-message --emulator emery --uint 10000=0
fi

ICON="$(mktemp)"
trap 'rm -f "$ICON"' EXIT
python3 tools/make_icon.py sample "$KIND" "$ICON"

# Keys: 10000 STATE, 10001 DISTANCE, 10002 MANEUVER, 10003 REMAINING, 10004 ETA, 10005 DURATION, 10006 ICON
pebble send-app-message --emulator emery \
  --uint 10000=1 \
  --string 10001="150 м" 10002="Поверните направо" 10003="1,61 км" 10004="23:57" 10005="15 мин" \
  --bytes-file 10006="$ICON"
