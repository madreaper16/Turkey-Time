# Turkey Time!

<img src="src/main/resources/assets/turkeytime/icon.png" alt="Turkey Time!" width="160" align="right">

Wild turkeys, breedable poults, throwable eggs, whole-turkey meals, and a
shareable Thanksgiving Feast for Minecraft.

**Created and maintained by MadReaper16.**

## Features

- Wild flocks in forests and plains, with distinct toms, hens, and poults.
- Foraging, tail-fan strutting, flock behavior, and short escape flutters.
- Seed-based breeding, baby growth, and eggs laid by adult hens.
- Throwable turkey eggs with a 1-in-8 chance to hatch one poult; dispensers work too.
- Raw turkey and feather drops from adults, with furnace, smoker, and campfire recipes.
- Whole cooked turkey that can be eaten normally or placed and shared.
- A decorated Thanksgiving Feast with eight visible servings.
- Five advancements and optional, configurable November plumage.

Turkeys and all core recipes remain available year-round. Seasonal settings only
change the chance of new autumn-colored coats.

## Requirements

| Component | Version |
|---|---|
| Minecraft Java Edition | 26.3 |
| Java | 25 |
| Fabric Loader | 0.20.1 or newer |
| Fabric API | 0.165.0+26.3 or newer for Minecraft 26.3 |

The gameplay suite passes on Fabric API **0.165.0+26.3** and **0.165.2+26.3**.
Turkey Time does not require an upgrade from 0.165.0 to 0.165.2.

Install the same Turkey Time version on the client and server. Remove older
Turkey Time JARs when updating; the sources JAR is for development.

## Cooking and sharing

Cooked turkey restores **8 hunger points (4 drumsticks)** when eaten normally.
Use it on a supported surface to place it, or hold use while aiming into the air
to eat it from your hand. A placed turkey has **eight servings of 2 hunger points**
each, with a visible portion removed on every bite.

Craft a Thanksgiving Feast in a crafting table:

| Left | Middle | Right |
|---|---|---|
| Bread | Cooked turkey | Carrot |
| Wooden slab | Wooden slab | Wooden slab |
| Empty | Empty | Empty |

Any wooden slabs work, including mixed wood types. The feast has eight servings.
An untouched placed meal returns its item when broken; a partially eaten meal
drops nothing. The last serving removes the meal.

## Advancements

- **Turkey Hunter** — Kill an adult turkey; poults do not count.
- **Smells Good** — Cook turkey.
- **Happy Thanksgiving!** — Place a Thanksgiving Feast.
- **Turkey Hatchery** — Successfully hatch a poult from a thrown turkey egg.
- **Turkey Breeder** — Breed two turkeys.

All five can be earned year-round and in any order.

## Configuration

Settings are stored in `config/turkeytime.json` and take effect after restart.
See [configuration](docs/CONFIG.md), [installation](docs/INSTALL.md), and
[known limitations](docs/KNOWN_ISSUES.md).

## Building

A Java 25 JDK is required. From the project directory:

```powershell
.\gradlew.bat build --console=plain
```

On Linux or macOS, use `./gradlew build`. Built JARs are placed in `build/libs`.
See [testing](docs/TESTING.md) for the gameplay suite and development launch tasks.

To assemble the release ZIP after building:

```powershell
python tools/package_release.py
```

The package contains runtime and sources JARs, documentation, project icons,
and SHA-256 checksums. Art authoring scripts require Pillow and, for sounds,
ffmpeg; these are not runtime dependencies.

## License and credits

Copyright (c) 2026 **MadReaper16**. Released under the [MIT License](LICENSE).
See [credits](docs/CREDITS.md) and [third-party notices](THIRD_PARTY_NOTICES.md).

*Gobble your way through the Overworld.*
