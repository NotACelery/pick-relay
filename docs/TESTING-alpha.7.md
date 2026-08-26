# Pick Relay 0.1.0-alpha.7 — Gameplay certification matrix

Run the alpha.6 mining-mode cases first, then these new high-risk checks.

## A. Build / launch

1. Run `build-alpha.bat` or `./build-alpha.sh`.
2. Launch a NeoForge 1.21.1 client with Pick Relay only, then with the target modpack.
3. Confirm no mixin application errors and the Mouse Button 5 keybind appears under Pick Relay.

## B. Provenance-confirmed tool break

- Queue a disposable damaged pickaxe as `Until Broken`, followed by another tool.
- Let Pick Relay itself break the first pickaxe while mining a block.
- Expected: first entry becomes BROKEN and the next tool relays normally.

Then deliberately remove/drop the active tool without letting Pick Relay break it (use a controlled test method/mod/server correction if the GUI/input safety prevents normal Q interaction).

- Expected: immediate safe Stop with Tool unavailable.
- Forbidden: mark it BROKEN and silently continue.

## C. Pending-tool external reorder

- Queue at least 4 distinct tools.
- Start on tool #1.
- While #1 is active, use a controlled external inventory modification to move/replace tool #4.
- Expected: Pick Relay stops on the next audit cycle; it must not wait until #4's turn.

## D. Exact full-hotbar SWAP conservation

- Fill hotbar slot 1 with a stack such as 64 blocks/food.
- Keep all queued tools in the main inventory and start with a completely full hotbar.
- Expected initial fallback: queued tool swaps into hotbar slot 1 and the exact 64-count stack appears in the tool's former inventory slot.
- Repeat relays with other multi-count displaced stacks.
- No item count may change, duplicate or vanish.

## E. Waiting feedback — Single Block

- Start on a generator output in Single Block.
- Break output and let the ray reach the backing block.
- Expected: session remains ACTIVE, does not mine backing block, HUD/GUI report waiting for the selected block.
- Regenerate the selected coordinate and aim it; waiting feedback clears and mining resumes.

## F. Waiting feedback — Line Mining

- Start in Line Mining.
- Aim at air/an entity/out of reach.
- Expected: session remains ACTIVE and HUD/GUI report waiting for a block.
- Aim at another valid block; mining resumes and waiting feedback clears.

## G. Queue/tooltips/details

- Select several queue entries and confirm the central panel renders their actual icon.
- Hover that icon: vanilla/modded tooltip must render.
- Confirm durability is shown as remaining/max plus percentage.
- Intentionally invalidate a queued tool before Start; queue outline turns invalid and its augmented tooltip states the concrete problem.
- During ACTIVE inspect PENDING/ACTIVE/COMPLETED/BROKEN/PRESERVED statuses in tooltips as available.

## H. Clear Queue

- In configuration with a non-empty queue, press Clear.
- Expected: queue entries disappear, inventory ItemStacks remain untouched, Start becomes unavailable.
- During ACTIVE the queue cannot be cleared.

## I. Full regression

Repeat:

- 36 disposable tools;
- full hotbar initial fallback;
- preserved/completed active relay slot swap;
- pending queued tool already in hotbar;
- Blocks budget exact transition;
- Durability budget + Unbreaking;
- Mending after previously observed consumption;
- Preserve at 1;
- physical left/right gameplay click stop;
- voluntary and involuntary movement stop;
- GUI open while ACTIVE;
- death/disconnect/dimension cleanup;
- Single Block and Line Mining camera behaviour.
