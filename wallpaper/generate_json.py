#!/usr/bin/env python3
"""
Generate wallpaper thumbnails (480px width) and wallpapers.json.
Scans: wallpaper/result/*.webp
Generates:
  - wallpaper/thumbs/*.webp
  - wallpaper/wallpapers.json
"""
import json
from pathlib import Path
from PIL import Image

REPO_BASE_URL = "https://raw.githubusercontent.com/susantohenri/jokam-app/refs/heads/main"
THUMB_WIDTH = 480
THUMB_QUALITY = 80


def main():
    wallpaper_dir = Path(__file__).resolve().parent
    result_dir = wallpaper_dir / "result"
    thumbs_dir = wallpaper_dir / "thumbs"
    json_out = wallpaper_dir / "wallpapers.json"

    if not result_dir.exists():
        print(f"Error: {result_dir} not found.")
        return

    thumbs_dir.mkdir(parents=True, exist_ok=True)
    images = sorted(result_dir.glob("*.webp"))
    print(f"Ditemukan {len(images)} wallpaper di {result_dir}")

    items = []
    for idx, img_path in enumerate(images, start=1):
        thumb_path = thumbs_dir / img_path.name
        
        # Buat thumbnail jika belum ada
        if not thumb_path.exists():
            with Image.open(img_path) as im:
                # Resize proporsional dengan lebar 480px
                aspect_ratio = im.height / im.width
                new_height = int(THUMB_WIDTH * aspect_ratio)
                thumb = im.resize((THUMB_WIDTH, new_height), Image.Resampling.LANCZOS)
                thumb.save(thumb_path, "WEBP", quality=THUMB_QUALITY)

        item = {
            "id": img_path.stem,
            "thumb_url": f"{REPO_BASE_URL}/wallpaper/thumbs/{img_path.name}",
            "full_url": f"{REPO_BASE_URL}/wallpaper/result/{img_path.name}"
        }
        items.append(item)

    with open(json_out, "w", encoding="utf-8") as f:
        json.dump(items, f, ensure_ascii=False, indent=2)

    print(f"Berhasil membuat thumbnail di {thumbs_dir}")
    print(f"Berhasil menyimpan {len(items)} item wallpaper ke {json_out}")


if __name__ == "__main__":
    main()
