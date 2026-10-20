# Forge launcher icons

`assets/forge-chaos-brawl-icon.png` is the shared Forge and Chaos Brawl master icon. The transparent `assets/forge-chaos-brawl-foreground.png` is its Android adaptive foreground. The generator creates a gold compass variant for Adventure, the Android density/adaptive/monochrome images, the Forge desktop frame icon, and multi-size Windows `.ico` files.

To regenerate platform assets after changing either source image, install the pinned image dependency and run:

```sh
python3 -m pip install -r chaosbrawl/requirements-icons.txt
python3 chaosbrawl/generate_forge_icons.py
```

The Android app label is “Chaos Brawl Forge”. The Windows package uses the shared icon for “Play Chaos Brawl” and the compass variant for “Play Adventure”.
