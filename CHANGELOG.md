# Changelog

## 0.1.0-alpha.7

### Safety / correctness
- Active-tool disappearance is no longer automatically interpreted as normal breakage. A broken tool is accepted only when Pick Relay observed the slot become empty during its own successful `destroyBlock` call; unexplained disappearance stops with `TOOL_INVALID`.
- Added a full runtime audit of every PENDING/ACTIVE queue entry: tracked slots must remain unique, in range and fingerprint-compatible every active cycle. External inventory reorders are therefore detected before a later relay transition.
- Relay SWAP verification now checks exact stack counts as well as item/components, protecting displaced full-hotbar stacks such as food or blocks from silent count inconsistencies.
- Runtime audit also requires exactly one ACTIVE entry and the normal player inventory menu while the relay is running.

### UX
- Added live waiting feedback to HUD and selected-entry progress: Single Block reports that it is waiting for the selected coordinate, while Line Mining reports that it is waiting for any valid block under the crosshair.
- Added the selected tool icon to the details panel with vanilla/modded hover tooltip.
- Selected details now show remaining/max durability plus percentage.
- Augmented queue tooltips now show runtime entry status and the concrete pre-start validation problem when an entry is invalid.
- Added **Clear Queue** while configuration is editable on the normal/spacious layout.

## 0.1.0-alpha.6

### Added
- Added an explicit session mining-mode selector before Start: **Single Block** or **Line Mining**.
- Single Block captures the exact block coordinate under the crosshair at session start and refuses to mine any other coordinate.
- In Single Block mode, looking away or temporary air pauses mining without cancelling the relay; aiming the captured coordinate again resumes automatically.
- Line Mining preserves THE Pick-style behaviour: every cycle mines the current valid block under the crosshair and camera rotation redirects mining immediately.
- Both modes keep the same stationary-player safety anchor; any real player displacement still stops the entire session.

### Changed
- The alpha.5 camera-following behaviour is now specifically the **Line Mining** mode rather than the only mining behaviour.
- The alpha.4 fixed-coordinate idea returns only as an explicit opt-in **Single Block** mode; it is no longer forced on every session.
- The GUI exposes the two mining modes as dedicated buttons and locks them read-only while ACTIVE.

## 0.1.0-alpha.5

### Fixed
- Removed the incorrect fixed-coordinate work target introduced in alpha.4.
- Auto Mining now follows the current block under the crosshair, matching THE Pick's validated behaviour.
- Rotating the camera no longer pauses mining or requires returning to an anchored block coordinate.
- Temporary air/no block under the crosshair keeps the session ACTIVE and resumes automatically on the next valid aimed block.

### Changed
- Mining now raycasts every cycle with the player's normal `blockInteractionRange()` and uses vanilla `MultiPlayerGameMode.continueDestroyBlock(...)`.
- Removed the private `Minecraft.startAttack/continueAttack` mixin invoker; Pick Relay no longer needs that private attack pipeline.
- Destroy provenance remains scoped to Pick Relay's own synchronous mining call, preserving Blocks Broken/Durability accounting without pinning a coordinate.
- HUD/GUI no longer expose an anchored-target waiting state.
- Start validation text now describes only the initial in-reach block requirement.

### Safety
- The safety anchor is exclusively the player's world position/dimension, not the block being mined.
- Any voluntary or involuntary player displacement still stops the session. Camera rotation explicitly does not.

## 0.1.0-alpha.4

### Added
- Anchored work-position guard: Start captures the exact block position under the crosshair.
- Mining pauses safely when the crosshair no longer points at the anchored work position and resumes when it returns.
- HUD/GUI waiting state while the work target is temporarily unavailable or the camera is pointed elsewhere.
- Provenance guard around vanilla attack calls so Blocks Broken only advances for destroys initiated synchronously by Pick Relay.
- Explicit selected-hotbar synchronization through vanilla `ensureHasSentCarriedItem()` after every relay selection.
- Integer Durability Budget slider backed by NeoForge `ExtendedSlider`; Blocks Broken keeps the unbounded numeric field.
- Runtime last-known ItemStack snapshots so completed/preserved entries retain their final visible durability instead of reverting to their pre-session snapshot.
- Stronger inventory SWAP verification for both the incoming queued tool and any tracked displaced tool.
- Optional debug logging with `-Dpickrelay.debug=true`.
- Respawn/clone lifecycle stop as an additional death cleanup path.
- Expanded tooltips with real inventory location and explicit remaining/max durability percentage.
- Focused alpha.4 test matrix covering target anchoring, protocol slot sync and destroy provenance.

### Changed
- Build helper now uses Gradle 8.10.2 instead of Gradle 9.x, matching the Gradle 8 generation targeted by ModDevGradle 2.x.
- Gradle helper downloads from the direct official distribution host first, falls back to `services.gradle.org`, and verifies the distribution SHA-256 before extraction.
- Tool history rendering now uses the latest observed runtime snapshot.

### Safety
- Pick Relay will never attack an entity or a different block simply because the original generator block disappeared for a tick.
- Looking away does not cancel the session, but it also does not redirect mining to the new crosshair target.
- A tracked queued tool occupying a relay destination is validated before Pick Relay swaps through that slot.
- The first mining action after a tool rotation cannot rely solely on next-tick vanilla hotbar synchronization.

## 0.1.0-alpha.3

### Added
- End-to-end Relay Session controller connected to the configured queue.
- Editable per-entry Until Broken, Durability Budget and Blocks Broken modes.
- Numeric work targets and per-entry Preserve at 1 durability.
- Full pre-start queue/tool validation and invalid-entry highlighting.
- Concrete tool fingerprinting without modifying ItemStack NBT/components.
- Runtime logical slot tracking for every queued tool.
- Inventory relay manager with hotbar selection, first-empty-slot movement and full-hotbar swap fallback.
- Active hotbar slot reuse as the relay slot for preserved/completed tools.
- Centralized progression tracker for block and durability budgets.
- Successful block destruction tracking through the vanilla `MultiPlayerGameMode.destroyBlock` path.
- Durability sampling around successful block destruction, plus tick fallback for other durability changes.
- Mixin invoker for the vanilla attack pipeline so AFK mining can continue while Pick Relay's non-pausing GUI is open.
- Movement anchor safety monitor.
- Physical gameplay left/right click emergency stops using the pre-input event.
- Death, disconnect, dimension, inventory/tool inconsistency and unsafe-screen stops.
- Live HUD for Tool Z/X and mode progress.
- Live queue state/progress rendering and augmented tooltips.
- Responsive Start/Stop placement for short GUI heights.
- Client mixin configuration.
- Focused alpha.3 gameplay test matrix.

### Changed
- Start AFK Mining is now enabled only when the complete queue validates successfully.
- Queue editing is locked while ACTIVE, while inspection and Stop remain available.
- Session completion and all emergency/manual stop paths use centralized cleanup and clear the temporary queue.

### Safety
- Pick Relay never substitutes an unselected "similar" tool when tracking becomes ambiguous.
- Preserve at 1 is evaluated before production targets.
- Physical emergency clicks are consumed before vanilla gameplay handles them.

## 0.1.0-alpha.2

### Added
- Real temporary `RelayQueue` model with a hard 36-entry limit.
- Concrete `RelayEntry` identity and per-entry work/safety configuration groundwork.
- Work modes: Until Broken, Durability Budget, and Blocks Broken.
- Queue entry lifecycle status model.
- Tag-based support detection for pickaxes, axes, shovels, and hoes.
- Player inventory and hotbar rendering inside the Pick Relay screen.
- Left-click add/remove and drag-paint ADD/REMOVE selection.
- Queue inspection and right-click removal.
- Queue drag reordering with insert-before, swap, and insert-after zones.
- Drop-outside removal semantics for queue drags.
- Vanilla/modded ItemStack tooltips in both queue and inventory views.

### Safety
- Start remained disabled until the relay execution engine could use only validated explicit queue entries.

## 0.1.0-alpha.1

### Added
- Initial NeoForge 1.21.1 client-only project skeleton.
- Configurable Pick Relay keybind.
- Non-pausing GUI and 4x9 queue visual skeleton.
- Controller state machine and centralized stop reasons.
- Controlled Attack ownership groundwork.
- HUD rendering path.
