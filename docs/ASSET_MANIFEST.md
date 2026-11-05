# Asset sources

Turkey Time! assets are part of the project maintained by **MadReaper16**.
See [credits](CREDITS.md) for assistance and dependency acknowledgments.

| Asset | Source |
|---|---|
| Tom, hen, and poult geometry | `src/client/java/com/madreaper16/turkeytime/client/TurkeyModel.java` |
| Entity texture atlases and autumn palettes | `tools/generate_assets.py` |
| Spawn-egg icon | `tools/generate_assets.py` |
| Raw and cooked turkey item artwork | `art/whole-turkey/raw.png`, `art/whole-turkey/cooked.png` |
| Turkey egg artwork | `art/turkey-egg/egg.png` |
| Placed roast and feast models/textures | `tools/generate_food_assets.py` |
| Project icon | `art/branding/male-turkey-icon-source.png`; model reference in the same directory |
| Runtime and CurseForge icon exports | `art/branding/turkey-time-mod-icon.png`, `art/branding/turkey-time-curseforge.png` |
| Turkey sounds | `tools/generate_assets.py`; seven mono Ogg Vorbis files |

## Regeneration

The authoring scripts require Python and Pillow. Sound generation also requires
ffmpeg on PATH. These tools are not required to play the mod.

```text
python tools/generate_assets.py
python tools/generate_food_assets.py
```

The food-asset importer preserves the source artwork and alpha while resizing it
for Minecraft. The mod icon is a 512x512 PNG; the CurseForge export is 400x400.

Sounds are `cluck_1`, `cluck_2`, `gobble`, `alarm`, `hurt`, `death`, and `flutter`,
registered as six sound events with English subtitles. They are synthesized,
mono, 22,050 Hz Ogg Vorbis files.

Minecraft model parents and built-in resource references are supplied by the
game at runtime. Minecraft artwork and third-party animal recordings are not
bundled in the project.
