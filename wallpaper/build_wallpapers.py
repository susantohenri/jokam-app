#!/usr/bin/env python3
"""
folder/*.jpg|png|webp -> out/<hash>.webp (1440x3200, q82)
Install: pip install pillow
Pakai:   python build_wallpapers.py ./folder ./out
"""
import argparse
import hashlib
from pathlib import Path
from PIL import Image, ImageOps

W, H = 1440, 3200
QUALITY = 82
EXTS = {".jpg", ".jpeg", ".png", ".webp"}


def short_hash(path: Path) -> str:
    return hashlib.sha1(path.read_bytes()).hexdigest()[:8]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("input", type=Path)
    ap.add_argument("output", type=Path)
    args = ap.parse_args()

    args.output.mkdir(parents=True, exist_ok=True)
    files = sorted(p for p in args.input.iterdir() if p.suffix.lower() in EXTS)

    seen = set()
    for i, src in enumerate(files, 1):
        name = short_hash(src)
        if name in seen:
            print(f"[{i}/{len(files)}] {src.name} duplikat, skip")
            continue
        seen.add(name)

        dst = args.output / f"{name}.webp"
        with Image.open(src) as im:
            im = ImageOps.exif_transpose(im).convert("RGB")
            if im.width < 1080 or im.height < 2400:
                print(f"  ! {src.name} kecil ({im.width}x{im.height}), hasil bisa blur")
            im = ImageOps.fit(im, (W, H), Image.LANCZOS, centering=(0.5, 0.35))
            im.save(dst, "WEBP", quality=QUALITY, method=6)
        print(f"[{i}/{len(files)}] {src.name} -> {dst.name} ({dst.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()