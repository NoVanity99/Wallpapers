#!/usr/bin/env python3
"""Rebuilds catalog/catalog.json from the folders in catalog/wallpapers/ and
refreshes the wallpapers that are bundled inside the APK.

Run it after adding or editing a wallpaper:   python3 tools/build_catalog.py
"""
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WALLS = ROOT / "catalog" / "wallpapers"
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "wallpapers"

# Shipped inside the APK so the app works on first launch without internet.
BUNDLED = ["aurora", "neon-grid", "lava-lamp"]
CATEGORIES = ["Abstract", "Space", "Nature", "Neon/Cyber", "Minimal", "Retro"]


def main() -> int:
    entries, errors = [], []
    for folder in sorted(p for p in WALLS.iterdir() if p.is_dir()):
        def_file = folder / "wallpaper.json"
        if not def_file.exists():
            errors.append(f"{folder.name}: missing wallpaper.json")
            continue
        d = json.loads(def_file.read_text())
        if d.get("id") != folder.name:
            errors.append(f"{folder.name}: \"id\" must match the folder name")
        if d.get("category") not in CATEGORIES:
            errors.append(f"{folder.name}: category must be one of {CATEGORIES}")
        for key in ("shader", "thumbnail"):
            if not (folder / d.get(key, "")).is_file():
                errors.append(f"{folder.name}: {key} file '{d.get(key)}' not found")
        files = [{"name": f.name, "size": f.stat().st_size}
                 for f in sorted(folder.iterdir()) if f.is_file() and not f.name.startswith(".")]
        entries.append({
            "id": d["id"],
            "name": d["name"],
            "category": d["category"],
            "description": d.get("description", ""),
            "interactive": bool(d.get("interactive", False)),
            "version": int(d.get("version", 1)),
            "format": int(d.get("format", 1)),
            "path": f"wallpapers/{folder.name}",
            "thumbnail": d["thumbnail"],
            "size": sum(f["size"] for f in files),
            "files": files,
        })
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1

    entries.sort(key=lambda e: (CATEGORIES.index(e["category"]), e["name"]))
    out = ROOT / "catalog" / "catalog.json"
    out.write_text(json.dumps({"format": 1, "wallpapers": entries}, indent=2) + "\n")

    if ASSETS.exists():
        shutil.rmtree(ASSETS)
    for wid in BUNDLED:
        shutil.copytree(WALLS / wid, ASSETS / wid)
    (ASSETS / "bundled.json").write_text(
        json.dumps({"format": 1, "wallpapers": [e for e in entries if e["id"] in BUNDLED]}, indent=2) + "\n")

    print(f"catalog.json: {len(entries)} wallpapers, {sum(e['size'] for e in entries) // 1024} KB total; bundled: {', '.join(BUNDLED)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
