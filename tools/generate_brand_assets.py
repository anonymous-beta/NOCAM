#!/usr/bin/env python3

from pathlib import Path
from PIL import Image, ImageOps
import sys

ROOT = Path(__file__).resolve().parent.parent

SOURCE_LOGO = ROOT / "Assets" / "file_0000000095e88211ab7ea2a3d22aecc5.png"

RES = ROOT / "app" / "src" / "main" / "res"

# Android launcher icon sizes
ICON_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

def main():
    if not SOURCE_LOGO.exists():
        print(f"ERROR: Original logo not found: {SOURCE_LOGO}")
        sys.exit(1)

    try:
        logo = Image.open(SOURCE_LOGO).convert("RGBA")
    except Exception as error:
        print(f"ERROR: Cannot open logo: {error}")
        sys.exit(1)

    # Main application logo
    logo_dir = RES / "drawable-nodpi"
    logo_dir.mkdir(parents=True, exist_ok=True)

    logo.save(logo_dir / "nocam_logo.png", optimize=True)

    # Launcher icons
    for folder, size in ICON_SIZES.items():
        output_dir = RES / folder
        output_dir.mkdir(parents=True, exist_ok=True)

        icon = ImageOps.fit(
            logo,
            (size, size),
            method=Image.Resampling.LANCZOS,
            centering=(0.5, 0.5)
        )

        icon.save(output_dir / "ic_launcher.png", optimize=True)

    # Adaptive icon foreground
    foreground_dir = RES / "drawable"
    foreground_dir.mkdir(parents=True, exist_ok=True)

    adaptive = ImageOps.contain(
        logo,
        (432, 432),
        method=Image.Resampling.LANCZOS
    )

    canvas = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
    x = (432 - adaptive.width) // 2
    y = (432 - adaptive.height) // 2
    canvas.alpha_composite(adaptive, (x, y))

    canvas.save(
        foreground_dir / "ic_launcher_foreground.png",
        optimize=True
    )

    print("NOCAM branding assets generated successfully.")

if __name__ == "__main__":
    main()