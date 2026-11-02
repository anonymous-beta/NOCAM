#!/usr/bin/env python3
"""
NOCAM Brand Asset Generator

Uses the existing repository logo to generate:
- Android legacy launcher icons
- Android adaptive launcher icon foreground
- Adaptive icon XML resources
- High-resolution logo and splash artwork
- Feature graphic
- Android icon background resources

Requirements:
    Python 3
    Pillow
"""

from pathlib import Path
import sys

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]

SOURCE = (
    ROOT
    / "Assets"
    / "file_0000000095e88211ab7ea2a3d22aecc5.png"
)

RES = ROOT / "app" / "src" / "main" / "res"

BLACK = "#050607"
RED = "#FF1018"
WHITE = "#FFFFFF"

DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def save_png(image, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.save(destination, "PNG", optimize=True)
    print(f"Generated: {destination.relative_to(ROOT)}")


def contain(image, size, background=None, padding=0):
    """Resize an image without distorting its aspect ratio."""
    width, height = size

    if background is None:
        canvas = Image.new(
            "RGBA", size, (0, 0, 0, 0)
        )
    else:
        canvas = Image.new("RGBA", size, background)

    available_width = max(1, width - padding * 2)
    available_height = max(1, height - padding * 2)

    scale = min(
        available_width / image.width,
        available_height / image.height,
    )

    new_size = (
        max(1, round(image.width * scale)),
        max(1, round(image.height * scale)),
    )

    resized = image.resize(
        new_size,
        Image.Resampling.LANCZOS,
    ).convert("RGBA")

    position = (
        (width - new_size[0]) // 2,
        (height - new_size[1]) // 2,
    )

    canvas.alpha_composite(resized, position)
    return canvas


def write_text(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.strip() + "\n", encoding="utf-8")
    print(f"Generated: {path.relative_to(ROOT)}")


def main():
    if not SOURCE.is_file():
        print(f"ERROR: Source logo not found: {SOURCE}")
        print("Check that the PNG exists in Assets/ and is committed.")
        return 1

    try:
        with Image.open(SOURCE) as opened:
            logo = opened.convert("RGBA")
            logo.load()
    except Exception as exc:
        print(f"ERROR: Could not read source logo: {exc}")
        return 1

    print(f"Source logo: {logo.width}x{logo.height}")

    # Full-resolution master artwork.
    save_png(
        logo,
        RES / "drawable-nodpi" / "nocam_logo.png",
    )

    # Splash artwork asset. This file is generated for use by the app;
    # the script does not modify the app's splash-screen code.
    splash = Image.new("RGBA", (1080, 1920), BLACK)
    splash_logo = contain(logo, (820, 820))
    splash.alpha_composite(
        splash_logo,
        ((1080 - 820) // 2, (1920 - 820) // 2 - 80),
    )

    # Add a restrained red accent at the bottom.
    draw = ImageDraw.Draw(splash)
    draw.rounded_rectangle(
        (390, 1450, 690, 1458),
        radius=4,
        fill=RED,
    )

    save_png(
        splash,
        RES / "drawable-nodpi" / "nocam_splash.png",
    )

    # Android legacy launcher icons.
    # The square artwork preserves the original black background.
    for density, pixels in DENSITIES.items():
        icon = contain(
            logo,
            (pixels, pixels),
            background=BLACK,
        )

        save_png(
            icon,
            RES / f"mipmap-{density}" / "ic_launcher.png",
        )

        save_png(
            icon,
            RES / f"mipmap-{density}" / "ic_launcher_round.png",
        )

    # Adaptive launcher foreground.
    # Keep artwork centered with margins so the Android launcher mask
    # is less likely to crop the important part of the logo.
    foreground = contain(
        logo,
        (432, 432),
        padding=42,
    )

    save_png(
        foreground,
        RES / "drawable-nodpi" / "ic_launcher_foreground.png",
    )

    # Background for adaptive icons.
    write_text(
        RES / "values" / "ic_launcher_background.xml",
        """
        <resources>
            <color name="ic_launcher_background">#050607</color>
        </resources>
        """,
    )

    adaptive_icon = """
    <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
        <background android:drawable="@color/ic_launcher_background" />
        <foreground android:drawable="@drawable/ic_launcher_foreground" />
    </adaptive-icon>
    """

    write_text(
        RES / "mipmap-anydpi-v26" / "ic_launcher.xml",
        adaptive_icon,
    )

    write_text(
        RES / "mipmap-anydpi-v26" / "ic_launcher_round.xml",
        adaptive_icon,
    )

    # A 1024 x 500 feature graphic for documentation or release pages.
    feature = Image.new("RGBA", (1024, 500), BLACK)
    feature_logo = contain(logo, (460, 460), padding=16)
    feature.alpha_composite(
        feature_logo,
        ((1024 - 460) // 2, 20),
    )

    feature_draw = ImageDraw.Draw(feature)
    feature_draw.rounded_rectangle(
        (80, 470, 944, 476),
        radius=3,
        fill=RED,
    )

    save_png(
        feature,
        ROOT / "Assets" / "nocam_feature_graphic.png",
    )

    print()
    print("NOCAM brand asset generation completed.")
    print("Generated Android resources in app/src/main/res/")
    print("Feature graphic: Assets/nocam_feature_graphic.png")
    print("Note: The splash image is an asset, not an enabled splash screen.")

    return 0


if __name__ == "__main__":
    sys.exit(main())