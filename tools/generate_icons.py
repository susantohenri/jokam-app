"""
Script to generate all Android launcher icon assets and store assets
from the source image provided by the user.
"""

import os
import math
from PIL import Image, ImageFilter, ImageDraw

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE_IMAGE_PATH = os.path.join(
    r"C:\Users\webhe\.gemini\antigravity\brain\41a7e934-a141-4c4f-80b5-3188cdadf74f\.user_uploaded",
    "media_1790803003304.jpg"
)

RES_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "res")
STORE_ASSETS_DIR = os.path.join(PROJECT_ROOT, "store-assets")


def main():
    print(f"Loading source image from: {SOURCE_IMAGE_PATH}")
    src_img = Image.open(SOURCE_IMAGE_PATH).convert("RGBA")
    w, h = src_img.size
    print(f"Source image size: {w}x{h}")

    # ==========================================
    # 1. GENERATE FULL-BLEED 1024x1024 IMAGE
    # ==========================================
    print("Generating 1024x1024 full-bleed square image...")
    full_bleed = src_img.convert("RGB")
    fb_pixels = full_bleed.load()

    def is_corner_outside(x, y):
        is_corner = (
            (x < 220 and y < 220) or
            (x > w - 220 and y < 220) or
            (x < 220 and y > h - 220) or
            (x > w - 220 and y > h - 220)
        )
        if not is_corner:
            return False
        r, g, b, _ = src_img.getpixel((x, y))
        return (r > 190 and g > 190 and b > 190)

    corners = [
        (range(0, 220), range(0, 220)),
        (range(w - 220, w), range(0, 220)),
        (range(0, 220), range(h - 220, h)),
        (range(w - 220, w), range(h - 220, h)),
    ]

    for xr, yr in corners:
        for y in yr:
            for x in xr:
                if is_corner_outside(x, y):
                    vx = x - 512
                    vy = y - 512
                    dist = math.hypot(vx, vy)
                    if dist > 0:
                        ux = vx / dist
                        uy = vy / dist
                        for step in range(1, 350):
                            test_r = dist - step
                            tx = int(512 + ux * test_r)
                            ty = int(512 + uy * test_r)
                            if 0 <= tx < w and 0 <= ty < h:
                                if not is_corner_outside(tx, ty):
                                    safe_r = test_r - 4
                                    sx = max(0, min(w - 1, int(512 + ux * safe_r)))
                                    sy = max(0, min(h - 1, int(512 + uy * safe_r)))
                                    fb_pixels[x, y] = src_img.getpixel((sx, sy))[:3]
                                    break

    # ==========================================
    # 2. GENERATE SQUIRCLE ICON WITH TRANSPARENT OUTSIDE
    # ==========================================
    print("Generating transparent squircle icon...")
    squircle_img = src_img.copy()
    sq_alpha = Image.new("L", (w, h), 255)

    for y in range(h):
        for x in range(w):
            is_corner = (
                (x < 220 and y < 220) or
                (x > w - 220 and y < 220) or
                (x < 220 and y > h - 220) or
                (x > w - 220 and y > h - 220)
            )
            if is_corner:
                r, g, b, _ = src_img.getpixel((x, y))
                if r > 240 and g > 240 and b > 240:
                    sq_alpha.putpixel((x, y), 0)
                elif r > 200 and g > 200 and b > 200:
                    avg = (r + g + b) / 3.0
                    a = int(255 * (255 - avg) / 55.0)
                    sq_alpha.putpixel((x, y), max(0, min(255, a)))

    squircle_img.putalpha(sq_alpha)

    # ==========================================
    # 3. GENERATE CIRCULAR MASKED ICON
    # ==========================================
    print("Generating circular masked icon...")
    circular_img = full_bleed.convert("RGBA")
    circ_mask = Image.new("L", (w, h), 0)
    circ_draw = ImageDraw.Draw(circ_mask)
    circ_draw.ellipse((4, 4, w - 5, h - 5), fill=255)
    # Smooth edges with 1px blur on mask
    circ_mask = circ_mask.filter(ImageFilter.GaussianBlur(radius=1.0))
    circular_img.putalpha(circ_mask)

    # ==========================================
    # 4. GENERATE ADAPTIVE ICON BACKGROUND
    # ==========================================
    print("Generating adaptive background layer...")
    adaptive_bg = full_bleed.copy()
    bg_pixels = adaptive_bg.load()

    top_rgb = (23, 128, 80)
    bot_rgb = (10, 85, 60)

    # Fill inside ellipse rx=420, ry=435, cy=520 with smooth gradient
    for y in range(h):
        for x in range(w):
            dx = (x - 512) / 425.0
            dy = (y - 520) / 435.0
            dist = math.hypot(dx, dy)
            if dist < 1.05:
                t = y / float(h)
                gr = int(top_rgb[0] * (1 - t) + bot_rgb[0] * t)
                gg = int(top_rgb[1] * (1 - t) + bot_rgb[1] * t)
                gb = int(top_rgb[2] * (1 - t) + bot_rgb[2] * t)

                if dist < 0.90:
                    bg_pixels[x, y] = (gr, gg, gb)
                else:
                    factor = (dist - 0.90) / (1.05 - 0.90)
                    orig_r, orig_g, orig_b = full_bleed.getpixel((x, y))
                    bg_pixels[x, y] = (
                        int(gr * (1 - factor) + orig_r * factor),
                        int(gg * (1 - factor) + orig_g * factor),
                        int(gb * (1 - factor) + orig_b * factor)
                    )

    adaptive_bg = adaptive_bg.filter(ImageFilter.GaussianBlur(radius=1.0)).convert("RGBA")

    # ==========================================
    # 5. GENERATE ADAPTIVE ICON FOREGROUND (EMBLEM)
    # ==========================================
    print("Generating adaptive foreground emblem...")
    emblem_raw = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    cx, cy = 512, 512
    R_inner = 401.0
    R_outer = 404.0

    for y in range(512 - 410, 512 + 410):
        for x in range(512 - 410, 512 + 410):
            dist = math.hypot(x - cx, y - cy)
            if dist <= R_inner:
                r, g, b, _ = src_img.getpixel((x, y))
                emblem_raw.putpixel((x, y), (r, g, b, 255))
            elif dist < R_outer:
                r, g, b, _ = src_img.getpixel((x, y))
                alpha = int(255 * (R_outer - dist) / (R_outer - R_inner))
                emblem_raw.putpixel((x, y), (r, g, b, alpha))

    # Scale emblem to 80% to fit perfectly inside the 66-72dp safe zone of 108dp canvas
    new_size = int(1024 * 0.80)
    emblem_scaled = emblem_raw.resize((new_size, new_size), Image.Resampling.LANCZOS)

    # Add realistic subtle 3D drop shadow
    shadow_canvas = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    emblem_alpha = emblem_scaled.split()[3]
    shadow_tint = Image.new("RGBA", (new_size, new_size), (0, 30, 15, 80))
    shadow_tint.putalpha(emblem_alpha)

    offset = (w - new_size) // 2
    shadow_canvas.paste(shadow_tint, (offset, offset + 6), shadow_tint)
    shadow_canvas = shadow_canvas.filter(ImageFilter.GaussianBlur(radius=6))

    adaptive_fg = shadow_canvas
    adaptive_fg.paste(emblem_scaled, (offset, offset), emblem_scaled)

    # ==========================================
    # 6. GENERATE MONOCHROME THEMED ICON (ANDROID 13+)
    # ==========================================
    print("Generating monochrome themed icon...")
    mono_img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for y in range(h):
        for x in range(w):
            r, g, b, a = adaptive_fg.getpixel((x, y))
            if a > 40:
                lum = (r * 299 + g * 587 + b * 114) // 1000
                if lum > 200:
                    mono_img.putpixel((x, y), (255, 255, 255, a))
                elif lum > 100:
                    mono_img.putpixel((x, y), (255, 255, 255, int(a * 0.7)))

    # ==========================================
    # 7. SAVE STORE ASSETS
    # ==========================================
    print("Saving store assets...")
    os.makedirs(STORE_ASSETS_DIR, exist_ok=True)

    # 512x512 full bleed app icon for Google Play
    app_icon_512 = full_bleed.resize((512, 512), Image.Resampling.LANCZOS)
    app_icon_512_path = os.path.join(STORE_ASSETS_DIR, "app-icon-512.png")
    app_icon_512.save(app_icon_512_path, "PNG")
    print(f"Saved: {app_icon_512_path}")

    # Feature Graphic 1024x500
    feat_img = Image.new("RGB", (1024, 500), (27, 94, 32))
    # Fill with linear gradient matching the app
    f_draw = ImageDraw.Draw(feat_img)
    for x in range(1024):
        # gradient from deep green #0D4018 to rich forest #1B5E20
        t = x / 1024.0
        r = int(13 * (1 - t) + 27 * t)
        g = int(64 * (1 - t) + 94 * t)
        b = int(24 * (1 - t) + 32 * t)
        f_draw.line([(x, 0), (x, 500)], fill=(r, g, b))

    # Badge on the left
    badge_size = 300
    badge = squircle_img.resize((badge_size, badge_size), Image.Resampling.LANCZOS)
    feat_img.paste(badge, (75, (500 - badge_size) // 2), badge)

    # Text rendered onto feature graphic
    from PIL import ImageFont
    try:
        font_large = ImageFont.truetype("arialbd.ttf", 46)
        font_sub = ImageFont.truetype("arial.ttf", 19)
    except Exception:
        try:
            font_large = ImageFont.truetype("arial.ttf", 46)
            font_sub = ImageFont.truetype("arial.ttf", 19)
        except Exception:
            font_large = ImageFont.load_default()
            font_sub = ImageFont.load_default()

    f_draw = ImageDraw.Draw(feat_img)
    f_draw.text((415, 205), "Tempat Sambung", fill=(255, 255, 255), font=font_large)
    f_draw.text((417, 275), "Temukan tempat sambung terdekat, di mana pun kamu berada", fill=(200, 230, 201), font=font_sub)

    feat_path = os.path.join(STORE_ASSETS_DIR, "feature-graphic-1024x500.png")
    feat_img.save(feat_path, "PNG")
    print(f"Saved: {feat_path}")

    # ==========================================
    # 8. SAVE MIPMAP LEGACY ICONS
    # ==========================================
    mipmap_densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }

    for folder, size in mipmap_densities.items():
        folder_path = os.path.join(RES_DIR, folder)
        os.makedirs(folder_path, exist_ok=True)

        # Standard squircle launcher icon
        icon_squircle = squircle_img.resize((size, size), Image.Resampling.LANCZOS)
        out_sq = os.path.join(folder_path, "ic_launcher.png")
        icon_squircle.save(out_sq, "PNG")

        # Round launcher icon
        icon_round = circular_img.resize((size, size), Image.Resampling.LANCZOS)
        out_rd = os.path.join(folder_path, "ic_launcher_round.png")
        icon_round.save(out_rd, "PNG")

        print(f"Saved {folder}: ic_launcher.png, ic_launcher_round.png ({size}x{size})")

    # ==========================================
    # 9. SAVE ADAPTIVE DRAWABLE LAYERS
    # ==========================================
    # 108dp canvas sizes across densities:
    # mdpi: 108, hdpi: 162, xhdpi: 216, xxhdpi: 324, xxxhdpi: 432
    drawable_densities = {
        "drawable-mdpi": 108,
        "drawable-hdpi": 162,
        "drawable-xhdpi": 216,
        "drawable-xxhdpi": 324,
        "drawable-xxxhdpi": 432,
    }

    for folder, size in drawable_densities.items():
        folder_path = os.path.join(RES_DIR, folder)
        os.makedirs(folder_path, exist_ok=True)

        bg_resized = adaptive_bg.resize((size, size), Image.Resampling.LANCZOS)
        bg_out = os.path.join(folder_path, "ic_launcher_background.png")
        bg_resized.save(bg_out, "PNG")

        fg_resized = adaptive_fg.resize((size, size), Image.Resampling.LANCZOS)
        fg_out = os.path.join(folder_path, "ic_launcher_foreground.png")
        fg_resized.save(fg_out, "PNG")

        mono_resized = mono_img.resize((size, size), Image.Resampling.LANCZOS)
        mono_out = os.path.join(folder_path, "ic_launcher_monochrome.png")
        mono_resized.save(mono_out, "PNG")

        print(f"Saved {folder}: background, foreground, monochrome ({size}x{size})")

    # Default drawable fallback (432x432 xxxhdpi quality)
    drawable_default = os.path.join(RES_DIR, "drawable")
    adaptive_bg.resize((432, 432), Image.Resampling.LANCZOS).save(os.path.join(drawable_default, "ic_launcher_background.png"), "PNG")
    adaptive_fg.resize((432, 432), Image.Resampling.LANCZOS).save(os.path.join(drawable_default, "ic_launcher_foreground.png"), "PNG")
    mono_img.resize((432, 432), Image.Resampling.LANCZOS).save(os.path.join(drawable_default, "ic_launcher_monochrome.png"), "PNG")

    # ==========================================
    # 10. REMOVE OLD VECTOR XMLS IN DRAWABLE
    # ==========================================
    old_xmls = [
        os.path.join(drawable_default, "ic_launcher_background.xml"),
        os.path.join(drawable_default, "ic_launcher_foreground.xml"),
        os.path.join(drawable_default, "ic_launcher_monochrome.xml"),
    ]
    for old_file in old_xmls:
        if os.path.exists(old_file):
            os.remove(old_file)
            print(f"Removed legacy vector file: {old_file}")

    print("\nAll icon assets generated successfully!")


if __name__ == "__main__":
    main()
