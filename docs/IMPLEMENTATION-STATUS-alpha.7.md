# Pick Relay 0.1.0-alpha.7 — Implementation status

This file distinguishes **implemented in source** from **certified in a running Minecraft client**.

The alpha.7 preparation environment cannot perform the external Gradle/Maven download, so all entries below are currently `IMPLEMENTED / AWAITING GAMEPLAY CERTIFICATION` unless a future test snapshot updates them.

| # | 1.0 acceptance criterion | Source status |
|---|---|---|
| 1 | Configurable keybind opens GUI | Implemented |
| 2 | Add concrete tools | Implemented |
| 3 | Drag-select ADD | Implemented |
| 4 | Drag-select REMOVE | Implemented |
| 5 | Reorder queue | Implemented |
| 6 | Right-click remove | Implemented |
| 7 | 36 entries | Implemented |
| 8 | Until Broken | Implemented |
| 9 | Block Budget | Implemented |
| 10 | Durability Budget | Implemented |
| 11 | Preserve at 1 | Implemented |
| 12 | Start automining | Implemented |
| 13 | Continuous vanilla mining loop | Implemented |
| 14 | Automatic tool rotation | Implemented |
| 15 | Inventory -> hotbar relay | Implemented |
| 16 | Initial full-hotbar fallback | Implemented |
| 17 | Queue order overrides physical order | Implemented |
| 18 | HUD Z/X + progress | Implemented |
| 19 | Successful block counts once | Implemented with destroy provenance |
| 20 | Drops do not drive Block Budget | Implemented |
| 21 | Player movement stops | Implemented |
| 22 | Physical gameplay L/R click stops | Implemented |
| 23 | Bind opens GUI during ACTIVE without stopping | Implemented |
| 24 | Manual Stop | Implemented |
| 25 | ACTIVE queue is read-only | Implemented |
| 26 | Queue exhaustion stops | Implemented |
| 27 | Death/disconnect/dimension cleanup | Implemented |
| 28 | Never select an unqueued substitute | Implemented fail-safe |
| 29 | Preserve tool is never intentionally used below 1 remaining | Implemented safety-first |
| 30 | Relay swaps must not lose/duplicate stacks | Implemented with exact two-sided post-SWAP verification |

## Additional decisions already implemented

- Session mining mode selector: **Single Block** / **Line Mining**.
- Single Block coordinate safety against backing blocks.
- Line Mining THE Pick-style camera-following raycast.
- Whole-queue runtime integrity audit.
- Provenance-confirmed distinction between a legitimately broken active tool and unexplained disappearance.
- Unbreaking-aware durability use accounting.
- Previously observed durability consumption is not erased by later Mending repair.
- Selected-tool details icon/tooltip/durability percentage.
- Waiting feedback in GUI/HUD.
- Clear Queue in editable spacious GUI layouts.
- Temporary queue; session Stop/complete/emergency clears it.

## Intentionally deferred beyond gameplay certification

- Optional Eruruu's Patch / THE Pick compatibility layer.
- Any persistent ItemStack-specific queue across game/world restarts.
- Support claims for exotic modded tools that do not follow normal item tags/durability/mining semantics.
- Additional shortcuts such as Select All, bulk presets, pathfinding, target searching or automatic best-tool selection.

## Gate before new feature work

Run `docs/TESTING-alpha.7.md` and record actual failures. Fix those before expanding scope.
