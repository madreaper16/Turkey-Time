# Development and testing

## Toolchain

Minecraft Java 26.3, Java 25, Fabric Loader 0.20.1, Fabric API 0.165.0+26.3,
Fabric Loom 1.17.21, and the Gradle 9.6.0 wrapper.

The default build uses the minimum supported Fabric API. The gameplay suite has
also passed with Fabric API 0.165.2+26.3.

## Commands

From the project directory on Windows:

```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat runGameTest --console=plain
.\gradlew.bat runClient --console=plain
.\gradlew.bat runServer --console=plain
```

On Linux or macOS, use `./gradlew` in place of `.\gradlew.bat`.
The server uses a local development instance under `run/server`; review the
Minecraft EULA before accepting it for that instance. The automated Fabric
GameTest task uses its own headless test server.

To check the newer API without changing the baseline:

```powershell
.\gradlew.bat runGameTest -Pfabric_api_version=0.165.2+26.3 --console=plain
```

## Packaged-JAR checks

```powershell
.\gradlew.bat runPackagedServer --console=plain
.\gradlew.bat runPackagedClient --console=plain
```

These tasks copy the built mod and Fabric API into isolated instances under
`run/release`. Configure the local server and EULA acceptance there before use.
No project class/resource directories are included in the packaged runtime.

## Coverage

The suite contains 40 project tests plus Fabric's built-in test, covering:

- Configuration validation, habitat tables, flock composition, movement, and cooldowns.
- Seed temptation, breeding, parent following, growth, laying, and save/load state.
- Adult/baby loot, Looting, fire handling, and all three cooking blocks.
- Handheld food, placement, eight servings, hunger checks, support loss, and safe drops.
- Stale two-player clicks and final-serving duplication prevention.
- Egg consumption, dispenser behavior, collision, hatching, and player credit.
- Feast crafting with all 13 wooden slab types, mixed woods, and invalid inputs.
- Five advancement actions, campfire cook persistence, seasonal dates, and coat compatibility.

The release checks also exercised a packaged client/server connection, a fresh
singleplayer world, restart persistence, icons, and a short 80-turkey load sample.
See [known limitations](KNOWN_ISSUES.md) for the scope of compatibility testing.

## Release packaging

Build first, then run:

```powershell
python tools/package_release.py
```

The script audits the JAR and creates a ZIP with runtime/sources JARs,
documentation, icons, a file manifest, and SHA-256 checksums under `build/release`.
It excludes game instances, accounts, logs, dependency JARs, and build caches.
