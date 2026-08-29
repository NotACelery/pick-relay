# Pick Relay 1.1.1

This release includes everything added since **1.0.1**.

## Session telemetry

- Added a live **Session** panel with elapsed time, total blocks broken and the currently active queue tool.
- Added live theoretical **blocks per second (BPS)** and seconds-per-block information for the block currently under the crosshair.
- Added active status-effect visibility directly inside Pick Relay, with mining-related effects such as Haste, Conduit Power and Mining Fatigue prioritized.
- Added generic visibility for positive mining-speed attribute modifiers supplied by compatible mods, without adding hard dependencies.

## Tool comparison

- Selecting a queued tool now estimates how quickly that exact tool would break the block currently under the crosshair.
- The estimator accounts for tool mining speed/material, correct-tool rules, enchantments/main-hand modifiers, current mining buffs/debuffs, relevant mining attributes, underwater penalties and airborne penalties.
- Selected Tool displays approximate **BPS** and **seconds per block**, making it much easier to compare queued tools before or during a long mining session.

The BPS value is a theoretical Minecraft block-breaking rate. Generator respawn timing, server/network delay, tick lag and custom mod logic can make observed throughput lower.

## GUI and controls

- Added a responsive Session panel: in compact layouts it sits between the Relay Queue and Player Inventory; in the normal layout it sits to the right of the queue.
- Pressing the Pick Relay keybind again now closes the GUI.
- The normal Minecraft inventory key also closes Pick Relay, matching vanilla inventory-screen muscle memory.
- Closing the GUI during an active session continues automining normally.
- The queued tool selected for inspection now has a distinct highlight.
- Drag insertion/swap targets use a separate visual cue so queue editing states are easier to read.

## 1.1.1 hotfix

- Opening Pick Relay now immediately releases ongoing item use such as eating, drinking, shielding or charging a usable item.
- Manual attack/use and movement inputs are suppressed while the Pick Relay screen is open.
- Manual block breaking is aborted when opening Pick Relay outside an active relay session.
- Active Pick Relay automining remains the only intentional gameplay action allowed to continue behind the GUI.

No mining speed, reach, drops or resources are modified by Pick Relay; the mod continues to coordinate normal Minecraft mining around the exact tools and rules configured by the player.
