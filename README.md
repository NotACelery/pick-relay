# Pick Relay

**Automate the grind. Schedule your tools.**

Pick Relay is a client-side Minecraft 1.21.1 NeoForge mod for scheduling explicit, ordered tool queues for repetitive mining/work sessions.

The functional source of truth is `docs/Pick-Relay-Especificacion-Pre-Desarrollo.md`.

## Current development state

`0.1.0-alpha.7` hardens the end-to-end relay system after the two-mode mining design was completed. The focus is runtime inventory integrity, provenance-confirmed tool breakage, clearer waiting/validation feedback, and the remaining queue/detail UX defined before development.

### Queue and configuration

- temporary 4x9 queue, maximum 36 concrete tools;
- pickaxes, axes, shovels and hoes recognized through vanilla tool tags;
- click and drag-paint ADD/REMOVE from the player inventory;
- queue inspection, right-click removal, swap and insert reordering;
- one-click **Clear Queue** while configuration is editable;
- per-entry modes: Until Broken, Durability Budget and Blocks Broken;
- integer durability slider constrained to currently usable durability;
- numeric Blocks Broken target;
- per-entry Preserve at 1 durability;
- pre-start validation highlights invalid entries and refuses unsafe starts;
- Start requires a valid block within normal interaction reach at activation;
- queue becomes read-only while a session is active.

### Session mining modes

Before Start, Pick Relay exposes two mutually exclusive modes:

#### Single Block

- captures the exact non-air block coordinate under the crosshair when the session starts;
- mines only while the current raycast resolves that same coordinate;
- rotating the camera does not stop the relay, but aiming elsewhere pauses mining rather than attacking a different block;
- temporary air at the captured generator coordinate is safe: Pick Relay stays ACTIVE and resumes when a block exists there and the player aims it again;
- intended for compact cobblestone/stone generators and other farms where breaking the backing structure would be dangerous.

#### Line Mining

- matches THE Pick's validated Auto Mining semantics;
- every cycle raycasts again using normal `blockInteractionRange()`;
- the current non-air block under the crosshair is mined with vanilla `continueDestroyBlock` behaviour;
- rotating the camera immediately redirects mining to the newly aimed block;
- temporary air, entities or no valid block under the crosshair do not cancel the session;
- intended for advanced generators/lines where the player deliberately sweeps the camera across multiple blocks.

In **both** modes, the player's position/dimension is the safety anchor. Any real displacement stops the session. Camera rotation alone never does.

### Relay execution

- exact queue order takes priority over physical inventory order;
- tools already in the hotbar are selected directly;
- inventory tools use the first empty hotbar slot from left to right;
- with a full hotbar, the active hotbar slot is reused as the relay slot;
- initial full-hotbar fallback is hotbar slot 1 (inventory index 0);
- inventory SWAP results are checked locally before Pick Relay accepts the move, including exact stack counts of displaced non-tool items;
- queued tools displaced by a relay swap have their logical tracked slot updated;
- every automatic hotbar selection explicitly invokes vanilla selected-slot synchronization before subsequent mining;
- if the expected concrete tool cannot be safely resolved, the session stops;
- every ACTIVE tick audits every pending/active tracked entry for slot uniqueness and fingerprint integrity, not only the currently equipped tool;
- an empty active slot is treated as a normal broken tool only when Pick Relay observed that disappearance inside its own successful block destruction.

### Mining progress

- block budgets observe successful destruction from the vanilla client destruction path;
- the destruction hook is scoped to synchronous Pick Relay `continueDestroyBlock` calls;
- blocks broken by unrelated actions do not automatically count merely because a session is active;
- block budgets count destroyed blocks, not clicks, drops or collected items;
- durability budgets count observed ItemStack damage increases, so Unbreaking does not consume budget when it prevents durability loss;
- later repairs do not subtract durability consumption already observed by Pick Relay;
- Preserve at 1 takes priority over production targets;
- tools already at 1 durability with Preserve enabled are skipped safely.

### Safety

A running session stops and cleans up on:

- player movement from the captured player anchor position;
- physical left-click or right-click in gameplay;
- death/respawn;
- disconnect/world exit;
- dimension change;
- tool identity mismatch;
- inventory desync;
- unsafe foreign screens/containers;
- queue exhaustion;
- manual Stop.

Camera rotation is explicitly **not** a stop condition. `PickRelayScreen` can be opened while ACTIVE and does not pause the world; its clicks are UI input rather than emergency gameplay clicks.

### HUD and live inspection

- compact HUD below the crosshair with Tool Z/X;
- explicit waiting feedback when Single Block is waiting for its captured coordinate or Line Mining currently has no valid block under the crosshair;
- Blocks and Durability modes show current progress/target;
- Preserve at 1 is indicated while relevant;
- the GUI shows active/completed/preserved entries and live progress;
- the selected-tool panel now renders the actual tool icon, remaining/max durability and percentage;
- invalid queued entries expose their concrete validation problem in the augmented tooltip;
- completed/preserved entries retain their latest observed runtime ItemStack snapshot;
- vanilla/modded item tooltips are retained and augmented with queue position, physical inventory location and explicit durability data.

## Debug logging

Development logging is normally silent. Enable it with:

```text
-Dpickrelay.debug=true
```

This logs session transitions, relay swaps, destroyed blocks and safety-relevant relay decisions.

## Build helper

The included build helpers use Gradle `8.10.2`, with direct/fallback official distribution URLs and SHA-256 verification before extraction:

- Windows: `build-alpha.bat`
- Linux/macOS: `./build-alpha.sh`

Java 21 is required.

## Safety rule

If Pick Relay cannot prove that it can continue with the exact tool selected by the player, it stops rather than improvising.

## Current validation status

The source/resources and the dangerous 1.21.1 API touchpoints have been statically reviewed, but this preparation runtime cannot resolve external Gradle/Maven hosts, so `0.1.0-alpha.7` still requires the first real Gradle build and `runClient` gameplay pass.

Use `docs/TESTING-alpha.7.md` for that validation.
