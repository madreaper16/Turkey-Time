# Turkey Time configuration

The game creates `config/turkeytime.json` on first startup. On a dedicated server,
edit the server's copy and restart the server. In singleplayer, restart the game.
These settings are startup-only; there is no hot-reload command. The
server owns gameplay decisions. No configuration library or menu is required.

Missing fields retain defaults. A malformed root, explicit null, wrong value
type or non-integer/overflowing numeric value causes safe default configuration
to be used with a warning; the original file is preserved. Valid integer values
outside supported ranges are clamped. Unknown fields are ignored.

| Setting | Default | Meaning / accepted bounds |
|---|---|---|
| `naturalSpawningEnabled` | true | Stops new natural spawn registration when false; existing mobs and registries remain |
| `spawnWeight` | 8 | Relative passive-animal weight, 1–100; no mob cap increase |
| `minGroupSize` | 2 | 2–8 |
| `maxGroupSize` | 5 | At least the configured minimum, at most 8 |
| `breedingEnabled` | true | Enables love mode/mating; does not remove existing babies |
| `eggLayingEnabled` | true | Adult hens lay eggs; timer pauses while disabled |
| `eggHatchingEnabled` | true | Thrown/dispenser eggs have a 1-in-8 chance to hatch one poult; false keeps throwing and particles but prevents hatching |
| `eggLayIntervalMinTicks` | 6000 | 20–1,728,000 active entity ticks |
| `eggLayIntervalMaxTicks` | 12000 | At least the configured minimum, at most 1,728,000 |
| `fleeFromSprintingPlayers` | true | Flee nearby sprinting survival/adventure players, including in Peaceful |
| `fleeFromHostiles` | true | Flee nearby living hostile mobs |
| `escapeFlutterEnabled` | true | Allows a small panic hop; does not grant flight |
| `flockingEnabled` | true | Adult local flock cohesion |
| `foragingEnabled` | true | Natural cosmetic pecking; never edits terrain |
| `gobbleResponseEnabled` | true | At most two delayed answers; answers cannot trigger further answers |
| `maleStruttingEnabled` | true | Natural adult male strutting near adult hens |
| `enableSeasonalEvent` | true | Enables new seasonal coat rolls; no gameplay gates |
| `seasonalEventStartMonthDay` | `11-01` | Inclusive start date, strict `MM-DD` |
| `seasonalEventEndMonthDay` | `11-30` | Inclusive end date; ranges can cross New Year |
| `seasonalCosmeticChance` | 0.05 | Fraction of newly created turkeys with an autumn coat during the event; clamped to 0–1 |
| `seasonalEventTimezone` | `system` | Server OS timezone; alternatively a Java timezone such as `America/Los_Angeles` or `UTC` |

At 20 ticks per second, default laying intervals are approximately 5–10 minutes.
Unloaded entities do not advance timers. Egg timers and flutter/gobble cooldowns
are persisted; animations reset to idle after loading. Gameplay timers use
active game ticks rather than the real-world calendar.

## Seasonal appearance and advancements

The server checks its calendar on first use/startup and every 1,200 server ticks
(about a minute at 20 TPS). Clients use the synchronized coat flag, never their
own calendar. Date checks do not fetch anything from the internet. Missing new
fields use defaults. Invalid dates/timezones, nulls, wrong types or non-finite
chance values cause safe default configuration with the existing warning and
file-preservation policy. An equal start/end date means that single calendar day.

Tom, hen and poult autumn coats are purely cosmetic. New natural, bred, hatched
and spawn-egg turkeys can receive a coat. The flag persists through growth and
save/reload. Old saves without the flag stay ordinary. Existing autumn turkeys
retain their coats after November or after the event is disabled; the toggle
prevents new seasonal coats. Spawning, breeding, laying, hatching, all recipes,
loot and all five advancements stay available year-round.

The five advancements are in the Husbandry tree, with no item/XP
rewards. Tree parents organize the display but do not gate earning. Adult-only
kills and successful breeding use vanilla criteria. Feast placement requires
the actual placed block. Hatching credits only a successful egg's player owner;
failed, collision-blocked and dispenser eggs do not earn it.

Cooking uses completed recipe criteria, not simply acquiring cooked turkey.
For furnaces and smokers, collect the output normally. For campfires, the
player who placed raw turkey receives credit when the cooked result drops;
that player must be online at completion. Campfire ownership is saved per slot
so cooking can survive a chunk unload/reload. Old food already cooking before
this version has no recorded owner and earns no campfire credit. Automated
cooking without an attributable player grants no advancement.

Fear searches run at staggered intervals, within six blocks, with at most eight
matching candidates. Flock queries inspect up to sixteen neighbors within ten
blocks. Seeds tempt turkeys within eight blocks, using the vanilla 2.5-block
stopping distance. Natural displays use an approximately 20–40 second cooldown plus a chance
check. Gobbles have a 600-tick cooldown; alarm calls 120 ticks; physical flutter
200 ticks. Hops use 0.24 vertical velocity and capped horizontal impulse; normal
gravity, collision and fall damage remain in effect.

## Datapack overrides

- Habitat tag: `data/turkeytime/tags/worldgen/biome/turkey_spawns.json`.
  Defaults: forest, birch forest, old growth birch forest, plains, sunflower plains,
  dappled forest. Spawn placement additionally requires the Overworld and standard
  passive-animal surface/light rules, even if a datapack adds a different biome.
- Breeding/temptation tag: `data/turkeytime/tags/item/turkey_food.json`.
  Defaults: wheat, pumpkin, melon and beetroot seeds.
- Loot: `data/turkeytime/loot_table/entities/turkey.json`.
- Recipes: `data/turkeytime/recipe/cooked_turkey*.json` and `thanksgiving_feast.json`.

Use a tag with `"replace": true` to replace the default list. Operators can override
recipes and loot through normal datapacks. Natural spawning settings require a
server restart; do not assume a datapack `/reload` reruns Fabric biome injection.

## Food balance

Adults drop 1–2 raw turkey and 0–2 vanilla feathers before Looting. Poults drop no
meat or feathers. Burning adult meat is cooked by the loot table. Raw turkey has
2 hunger and 1.2 saturation. Cooked turkey has **8 hunger (4 drumsticks)** and 9.6
saturation. These are fixed item properties rather than configuration fields.

Cooked turkey can also be placed on a sturdy top surface. A placed turkey has
eight servings restoring 2 hunger each, for **16 hunger total when placed**, while
normal handheld eating still restores 8. Each serving provides up to 2.4
saturation, subject to the usual hunger/saturation caps. Use the item on a block
to place it; hold use while aiming into the air to eat it normally.

An untouched roast drops one cooked turkey when broken. A partially eaten roast
drops nothing, including when its support is removed. The last bite removes the
block. Full survival players cannot take servings. Comparator strength decreases
from 15 to 1 in steps of two as servings are eaten. Servings are fixed at eight in
this build so the block states and models stay consistent.

Recipes yield one cooked turkey per raw turkey. Furnace and smoker recipes use
200 base cooking ticks and 0.35 experience, matching the target release's meat
recipe conventions; the smoker's native speed makes it faster in use. Campfire
cooking uses 600 ticks. Acquiring raw turkey unlocks all three recipes.

The Thanksgiving Feast uses a crafting table: bread, cooked turkey and carrot
across the top row, three wooden slabs across the middle row, bottom row empty.
Any item in `minecraft:wooden_slabs` works, including mixed wood types. No bowl
is needed. The feast is place-only and offers the same eight
2-hunger servings as a placed cooked turkey. Each bite removes a portion and
garnish. Intact feasts drop one feast; partially eaten feasts drop nothing.
The final bite removes the tray without returning a bowl. Comparator output
and sturdy-support requirements match the plain roast. This recipe is available
year-round and unlocks when cooked turkey is acquired.

Turkey eggs stack to 16. Throwing consumes one in survival, while creative use
retains it. Dispensers consume one and fire the custom projectile. Each impact
has a 1-in-8 chance to hatch exactly one baby turkey, never chickens or a bonus
brood. Solid blocks can prevent a hatch; the egg still breaks. Hatch decisions
and baby spawning happen only on the server. Eggs cannot claim a second hatch
from a repeated hit. Like other manually triggered animal creation, hatching
does not depend on natural-spawn habitats or passive mob caps; use the separate
hatching toggle to stop it. Existing config files can omit the new field.

Any two adults can breed with the tagged foods, following familiar vanilla
animal rules; sex only affects appearance, egg laying and adult male displays.
