#!/usr/bin/env python3

from pathlib import Path
from PIL import Image, ImageOps
import sys

ROOT = Path(__file__).resolve().parent.parent

# Original NOCAM logo
SOURCE_LOGO = (
    ROOT
    / "Assets"
    / "file_0000000095e88211ab7ea2a3d22aecc5.png"
)

# Android resources directory
RES = ROOT / "app" / "src" / "main" / "res"

# Launcher icon dimensions for Android devices
ICON_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def write_text(path, content):
    """Create a resource file and its parent directories."""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def main():

    # --------------------------------------------------
    # 1. Verify and load original logo
    # --------------------------------------------------

    if not SOURCE_LOGO.exists():
        print(f"ERROR: Original NOCAM logo not found:")
        print(SOURCE_LOGO)
        sys.exit(1)

    try:
        logo = Image.open(SOURCE_LOGO).convert("RGBA")
    except Exception as error:
        print(f"ERROR: Could not load NOCAM logo: {error}")
        sys.exit(1)

    if logo.width == 0 or logo.height == 0:
        print("ERROR: NOCAM logo has invalid dimensions.")
        sys.exit(1)

    print(f"Loaded NOCAM logo: {logo.width}x{logo.height}")

    # --------------------------------------------------
    # 2. Generate the main in-app logo
    # --------------------------------------------------

    logo_dir = RES / "drawable-nodpi"
    logo_dir.mkdir(parents=True, exist_ok=True)

    logo.save(
        logo_dir / "nocam_logo.png",
        optimize=True
    )

    print("Generated in-app NOCAM logo.")

    # --------------------------------------------------
    # 3. Generate legacy launcher icons
    # --------------------------------------------------

    for folder, size in ICON_SIZES.items():

        output_dir = RES / folder
        output_dir.mkdir(parents=True, exist_ok=True)

        icon = ImageOps.fit(
            logo,
            (size, size),
            method=Image.Resampling.LANCZOS,
            centering=(0.5, 0.5)
        )

        # Standard launcher icon
        icon.save(
            output_dir / "ic_launcher.png",
            optimize=True
        )

        # Round launcher icon
        icon.save(
            output_dir / "ic_launcher_round.png",
            optimize=True
        )

        print(f"Generated {folder}: {size}x{size}")

    # --------------------------------------------------
    # 4. Generate adaptive icon foreground
    # --------------------------------------------------

    # Android adaptive icons use a 432x432 canvas.
    # Keep the actual artwork inside the central safe area
    # so circular, squircle and rounded-square masks do
    # not cut off important parts of the NOCAM logo.

    foreground_dir = RES / "drawable"
    foreground_dir.mkdir(parents=True, exist_ok=True)

    safe_logo = ImageOps.contain(
        logo,
        (280, 280),
        method=Image.Resampling.LANCZOS
    )

    foreground = Image.new(
        "RGBA",
        (432, 432),
        (0, 0, 0, 0)
    )

    x = (432 - safe_logo.width) // 2
    y = (432 - safe_logo.height) // 2

    foreground.alpha_composite(
        safe_logo,
        (x, y)
    )

    foreground.save(
        foreground_dir / "ic_launcher_foreground.png",
        optimize=True
    )

    print("Generated adaptive icon foreground.")

    # --------------------------------------------------
    # 5. Generate adaptive icon XML resources
    # --------------------------------------------------

    adaptive_dir = RES / "mipmap-anydpi-v26"

    adaptive_xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/nocam_icon_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""

    write_text(
        adaptive_dir / "ic_launcher.xml",
        adaptive_xml
    )

    write_text(
        adaptive_dir / "ic_launcher_round.xml",
        adaptive_xml
    )

    print("Generated adaptive launcher XML resources.")

    # --------------------------------------------------
    # 6. Generate adaptive icon background color
    # --------------------------------------------------

    colors_xml = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="nocam_icon_background">#090909</color>
</resources>
"""

    write_text(
        RES / "values" / "nocam_icon_colors.xml",
        colors_xml
    )

    print("Generated NOCAM adaptive background color.")

    # --------------------------------------------------
    # 7. Verify generated resources
    # --------------------------------------------------

    required_files = [
        RES / "drawable-nodpi" / "nocam_logo.png",
        RES / "drawable" / "ic_launcher_foreground.png",
        RES / "mipmap-anydpi-v26" / "ic_launcher.xml",
        RES / "mipmap-anydpi-v26" / "ic_launcher_round.xml",
        RES / "values" / "nocam_icon_colors.xml",
    ]

    for folder in ICON_SIZES:
        required_files.append(
            RES / folder / "ic_launcher.png"
        )
        required_files.append(
            RES / folder / "ic_launcher_round.png"
        )

    missing = [
        str(path)
        for path in required_files
        if not path.exists()
    ]

    if missing:
        print("\nERROR: Some branding resources were not generated:")

        for path in missing:
            print(f" - {path}")

        sys.exit(1)

    print("\n========================================")
    print(" NOCAM BRANDING GENERATED SUCCESSFULLY")
    print("========================================")
    print(" Main logo:       OK")
    print(" Density icons:   OK")
    print(" Round icons:     OK")
    print(" Adaptive icons:  OK")
    print(" Background:      OK")
    print(" Resource checks: OK")
    print("========================================")


if __name__ == "__main__":
    main()