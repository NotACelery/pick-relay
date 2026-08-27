# Changelog

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

- Added position/dimension anchoring and automatic stop on displacement, death, disconnect or unsafe relay state.
- Added non-pausing Pick Relay GUI with read-only inspection during active sessions.
- Added event-style HUD above the hotbar with tool/progress/waiting status.
- Added configurable keybind, Clear Queue and Close controls.
- Added English, Chilean Spanish and Spanish localization.
