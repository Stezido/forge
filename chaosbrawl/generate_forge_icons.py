#!/usr/bin/env python3
"""Generate Android and Windows Forge launcher icons from the reviewed master art."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageOps


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "chaosbrawl" / "assets"
MASTER = ASSETS / "forge-chaos-brawl-icon.png"
FOREGROUND = ASSETS / "forge-chaos-brawl-foreground.png"
ADVENTURE = ASSETS / "forge-adventure-chaos-brawl-icon.png"
ANDROID = ROOT / "forge-gui-android"
BACKGROUND = (23, 25, 23, 255)


def fit(image: Image.Image, size: int) -> Image.Image:
    return ImageOps.fit(
        image.convert("RGBA"),
        (size, size),
        method=Image.Resampling.LANCZOS,
        centering=(0.5, 0.5),
    )


def make_adventure_icon(master: Image.Image) -> Image.Image:
    """Add a small gold compass badge to distinguish the Adventure launcher."""
    icon = master.convert("RGBA").copy()
    width, height = icon.size
    scale = min(width, height) / 1254
    cx, cy, radius = (int(width * 0.795), int(height * 0.79), int(95 * scale))
    draw = ImageDraw.Draw(icon)
    draw.ellipse(
        (cx - radius, cy - radius, cx + radius, cy + radius),
        fill=(20, 19, 17, 245),
        outline=(222, 174, 78, 255),
        width=max(3, int(11 * scale)),
    )
    points = [
        (cx, cy - int(radius * 0.66)),
        (cx + int(radius * 0.19), cy - int(radius * 0.12)),
        (cx + int(radius * 0.66), cy),
        (cx + int(radius * 0.19), cy + int(radius * 0.12)),
        (cx, cy + int(radius * 0.66)),
        (cx - int(radius * 0.19), cy + int(radius * 0.12)),
        (cx - int(radius * 0.66), cy),
        (cx - int(radius * 0.19), cy - int(radius * 0.12)),
    ]
    draw.polygon(points, fill=(237, 185, 80, 255))
    draw.polygon(
        [points[0], points[1], (cx, cy), points[7]],
        fill=(255, 224, 143, 255),
    )
    return icon


def save_adaptive_layer(source: Image.Image, size: int, destination: Path) -> None:
    alpha = source.getchannel("A")
    bounds = alpha.getbbox()
    if bounds is None:
        raise ValueError(f"Icon foreground is fully transparent: {source}")
    cutout = source.crop(bounds)
    safe_size = int(size * 0.66)
    cutout.thumbnail((safe_size, safe_size), Image.Resampling.LANCZOS)
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    layer.alpha_composite(cutout, ((size - cutout.width) // 2, (size - cutout.height) // 2))
    layer.save(destination)


def save_round_icon(master: Image.Image, size: int, destination: Path) -> None:
    canvas = Image.new("RGBA", (size, size), BACKGROUND)
    art_size = max(1, int(size * 0.86))
    art = fit(master, art_size)
    canvas.alpha_composite(art, ((size - art_size) // 2, (size - art_size) // 2))
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    canvas.putalpha(mask)
    canvas.save(destination)


def save_ico(image: Image.Image, destination: Path) -> None:
    image.save(
        destination,
        format="ICO",
        sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)],
    )


def main() -> None:
    master = Image.open(MASTER).convert("RGBA")
    foreground = Image.open(FOREGROUND).convert("RGBA")
    adventure = make_adventure_icon(master)
    adventure.save(ADVENTURE)

    # The desktop frame icon is an 80px cell in Forge's shared icon atlas.
    atlas_path = ROOT / "forge-gui" / "res" / "skins" / "default" / "sprite_icons.png"
    atlas = Image.open(atlas_path).convert("RGBA")
    if atlas.width < 80 or atlas.height < 720:
        raise ValueError(f"Forge icon atlas is too small: {atlas_path}")
    atlas.paste(fit(master, 80), (0, 640))
    atlas.save(atlas_path)

    density_sizes = {
        "ldpi": 36,
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192,
    }
    for density, size in density_sizes.items():
        directory = ANDROID / "res" / f"mipmap-{density}"
        directory.mkdir(parents=True, exist_ok=True)
        fit(master, size).save(directory / "ic_launcher.png")
        if density != "ldpi":
            save_round_icon(master, size, directory / "ic_launcher_round.png")

    drawable_sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144}
    for density, size in drawable_sizes.items():
        directory = ANDROID / "res" / f"drawable-{density}"
        directory.mkdir(parents=True, exist_ok=True)
        fit(master, size).save(directory / "ic_launcher.png")

    fit(master, 512).save(ANDROID / "ic_launcher-web.png")
    for density, size in density_sizes.items():
        if density == "ldpi":
            continue
        layer_size = size * 9 // 4
        directory = ANDROID / "res" / f"mipmap-{density}"
        save_adaptive_layer(foreground, layer_size, directory / "ic_launcher_foreground.png")
        alpha = Image.open(directory / "ic_launcher_foreground.png").getchannel("A")
        monochrome = Image.new("RGBA", (layer_size, layer_size), (255, 255, 255, 0))
        monochrome.putalpha(alpha)
        monochrome.save(directory / "ic_launcher_monochrome.png")

    classic_ico = ASSETS / "forge-chaos-brawl.ico"
    adventure_ico = ASSETS / "forge-adventure-chaos-brawl.ico"
    save_ico(master, classic_ico)
    save_ico(adventure, adventure_ico)

    for destination in (
        ROOT / "forge-gui" / "src" / "main" / "config" / "forge.ico",
        ROOT / "forge-gui-desktop" / "src" / "main" / "config" / "forge.ico",
    ):
        save_ico(master, destination)
    for destination in (
        ROOT / "forge-gui" / "res" / "adventure" / "common" / "forge-adventure.ico",
        ROOT / "forge-gui-mobile-dev" / "src" / "main" / "config" / "forge-adventure.ico",
    ):
        save_ico(adventure, destination)


if __name__ == "__main__":
    main()
