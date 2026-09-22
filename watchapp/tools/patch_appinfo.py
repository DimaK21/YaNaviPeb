#!/usr/bin/env python3
"""Adds the `companionApp` block to build/watchapp.pbw's appinfo.json.

Why this exists: SDK 4.33.1's `pebble build` only recognizes a fixed set of
`pebble.*` keys from package.json and silently drops anything else, including
`companionApp` (confirmed by inspecting the built appinfo.json). But the
current Pebble companion app (coredevices/mobileapp) requires this exact
block to know which Android package may talk to this watchapp over
PebbleKit 2 — without it, `sendDataToPebble` always fails with
FailedDifferentAppOpen/FailedNoPermissions and the app-opened/app-closed
push callbacks are never sent. See PebbleSenderReceiver.kt / PebbleKit2.kt in
that repo.

Run this after every `pebble build`:
  tools/patch_appinfo.py
"""
import json
import sys
import zipfile
from pathlib import Path

PBW_PATH = Path(__file__).resolve().parent.parent / "build" / "watchapp.pbw"

COMPANION_APP = {
    "android": {
        "required": True,
        "apps": [{"package": "ru.kryu.yanavipeb"}],
    },
}


def main():
    if not PBW_PATH.exists():
        sys.exit(f"{PBW_PATH} not found — run `pebble build` first")

    with zipfile.ZipFile(PBW_PATH, "r") as pbw:
        entries = {info.filename: pbw.read(info.filename) for info in pbw.infolist()}

    appinfo = json.loads(entries["appinfo.json"])
    if appinfo.get("companionApp") == COMPANION_APP:
        print("appinfo.json already has companionApp, nothing to do")
        return
    appinfo["companionApp"] = COMPANION_APP
    entries["appinfo.json"] = json.dumps(appinfo, indent=2).encode("utf-8")

    with zipfile.ZipFile(PBW_PATH, "w", zipfile.ZIP_DEFLATED) as pbw:
        for name, data in entries.items():
            pbw.writestr(name, data)

    print(f"Patched companionApp into {PBW_PATH}")


main()
