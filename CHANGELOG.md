# Changelog

## 1.1.1

Hotfix for background player actions while the Pick Relay screen is open.

### Fixed

- Opening Pick Relay now immediately releases an ongoing item-use action, such as eating, drinking, shielding or charging a usable item.
- Manual attack/use and movement key states are suppressed while the Pick Relay screen remains open.
- Manual block breaking is aborted when Pick Relay is opened outside an active relay session.
- Active Pick Relay automining remains allowed in the background and continues normally with the GUI open.

## 1.1.0

### Session visibility

- Added a live session panel with elapsed time, total blocks broken and the currently active queue tool.
- Added live theoretical blocks-per-second (BPS) for the block currently under the crosshair while a session is running.
- Active status effects are visible directly from the Pick Relay screen, with mining-related effects prioritized.
- Positive mining-speed attribute bonuses from compatible mods are surfaced without adding hard dependencies.
- The session panel sits between the queue and inventory in the responsive layout and to the right of the queue in the normal layout.

### Controls

- Pressing the Pick Relay keybind again now closes the Pick Relay screen.
- The normal Minecraft inventory key also closes Pick Relay, matching vanilla inventory-screen muscle memory.
- Closing the GUI during an active relay session keeps the mining session running.

### Mining rate estimator

- Selecting a queued tool now estimates how quickly that exact tool would break the block currently under the crosshair.
- The estimate accounts for the tool's block mining speed/material, correct-tool rules, enchantment and main-hand attribute modifiers, Haste/Conduit Power, Mining Fatigue, mining-speed attributes, underwater penalties and airborne penalties.
- Selected-tool details show both estimated BPS and seconds per block, making it easier to compare queued tools without starting the session.

### Queue UX

- The queue entry selected for inspection now uses a distinct gold highlight.
- Drag insertion and swap targets now use a separate cyan highlight so they are not confused with the active or selected tool.

## 1.0.1

Hotfix for the Pick Relay configuration screen.

### Fixed

- The player inventory now moves beside the relay queue when vertical space is limited and enough horizontal space is available.
- Selected Tool details and session controls remain below the queue/inventory pair instead of being pushed off-screen.
- Compact-window validation and controls remain readable without changing queue, relay or mining behaviour.

## 1.0.0

Initial public-ready release of Pick Relay.

### Tool scheduling

- Added an explicit ordered queue of up to 36 concrete tools.
- Added per-tool **Until Broken**, **Durability Budget** and **Blocks Broken** work modes.
- Added per-tool **Preserve at 1 durability** protection.
- Added independent configuration for every queued entry.
- Added queue add/remove painting, right-click removal, swap/insert reordering and live inspection.

### Mining modes

- Added **Single Block** mode for fixed-coordinate generator safety.
- Added **Line Mining** mode for camera-directed continuous mining.
- Camera movement does not cancel active mining; player displacement does.

### Inventory relay

- Added automatic hotbar selection and inventory-to-hotbar relay.
- Added full-hotbar relay-slot swaps.
- Added concrete tool tracking across inventory slot changes.
- Missing queued tools are skipped rather than terminating the entire session.
- Added inventory consistency checks around automatic swaps.

### Progress and durability

- Added successful block-destruction counting independent of item drops.
- Added real durability-consumption accounting with Unbreaking awareness.
- Added Mending-safe accumulated durability budgets.
- Added guarded Preserve-at-1 transitions near the break threshold.

### Safety and UX

- Added the official hand-made pixel-art Pick Relay icon and wired it into NeoForge mod metadata.
- Added position/dimension anchoring and automatic stop on displacement, death, disconnect or unsafe relay state.
- Added non-pausing Pick Relay GUI with read-only inspection during active sessions.
- Added event-style HUD above the hotbar with tool/progress/waiting status.
- Added configurable keybind, Clear Queue and Close controls.
- Added English, Chilean Spanish and Spanish localization.
