# Known limitations

- Minecraft Java 26.3, Fabric Loader 0.20.1, Fabric API 0.165.0+26.3 and
  0.165.2+26.3, and Java 25 have been tested on Windows. Other Minecraft versions,
  Forge/NeoForge, Bedrock, other operating systems, and arbitrary modpack
  combinations have not been validated.
- Campfire advancement credit requires the player who placed the raw turkey to
  be online when cooking finishes. Food placed before cook attribution was added
  has no recorded cook. Furnaces and smokers award credit when output is collected.
- Disabling the seasonal event stops new autumn coats; existing coats remain.
- Handheld cooked turkey restores 8 hunger. A placed roast or feast provides
  eight 2-hunger servings. Partially eaten meals cannot be recovered by breaking them.
- Sounds are synthesized turkey-like calls rather than field recordings.
- Two-player serving safety is covered by server-side mock-player tests. The
  short multi-flock performance check is not a public-server soak or memory-leak study.

For development launches, retain Minecraft's JVM/native settings. The packaged
launch tasks use the same native-access and stack-margin settings as Loom.
Offline development accounts may log unrelated Realms authentication failures;
some Windows installations also report OSHI performance-counter warnings.
