# Build status

Version: `0.1.0-alpha.7`

Target:

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- ModDevGradle 2.0.143
- Local build helper: Gradle 8.10.2

## Validation in this snapshot

Completed in the preparation environment:

- All JSON language resources and `pickrelay.mixins.json` parse successfully.
- Language files expose the same key set.
- NeoForge mixin declaration remains present in the generated `neoforge.mods.toml` template.
- The alpha.4 private `Minecraft.startAttack/continueAttack` invoker was removed.
- Both mining modes use the validated 1.21.1 path: normal-reach `player.pick(blockInteractionRange())` followed by vanilla `MultiPlayerGameMode.continueDestroyBlock(...)`. Line Mining follows the current crosshair; Single Block filters that raycast to the coordinate captured at Start.
- `MultiPlayerGameMode.ensureHasSentCarriedItem()` remains the only private vanilla method reached through a mixin invoker, for explicit hotbar selection synchronization.
- `MultiPlayerGameMode.handleInventoryMouseClick(...)`, `continueDestroyBlock(...)` and `destroyBlock(...)` are the relevant vanilla relay/mining paths.
- Inventory relay logic verifies expected local post-SWAP contents **and exact stack counts** before accepting a move.
- Fixed-coordinate targeting exists only as the explicit opt-in Single Block session mode. Line Mining keeps alpha.5/THE Pick camera-following semantics.
- The safety anchor is the player position/dimension; camera rotation is explicitly not a stop condition.
- Block-progress provenance is scoped to synchronous Pick Relay `continueDestroyBlock(...)` invocations.
- Empty active slots are accepted as normal breakage only after relay-provenance `destroyBlock(...)` confirms the tool vanished during that destruction.
- Runtime queue integrity audits every PENDING/ACTIVE tracked tool each cycle for slot range, uniqueness and concrete fingerprint compatibility.
- Gradle helper uses Gradle 8.10.2 and verifies the distribution checksum before extraction.

## Environment limitation

A full Gradle compile/runClient still cannot be executed in this preparation container because outbound DNS/network access from the runtime cannot resolve the Gradle/Maven hosts.

Therefore alpha.7 is a **recoverable implementation snapshot awaiting gameplay certification**.

The next validation should run:

1. `build-alpha.bat` (Windows) or `./build-alpha.sh`;
2. a NeoForge `runClient`/IDE launch;
3. `docs/TESTING-alpha.7.md` in order.
