# Pick Relay 0.1.0-alpha.3 — Focused gameplay validation

This checklist is intentionally ordered to test the dangerous inventory/session core before long AFK runs.

## 1. Boot and GUI

- Launch Minecraft 1.21.1 NeoForge with Pick Relay client-side only.
- Confirm the mod loads without mixin errors.
- Open Pick Relay with the configured bind.
- Confirm the screen does not pause singleplayer.
- Confirm queue add/remove/drag/reorder from alpha.2 still works.
- Configure all three work modes and Preserve at 1.
- Confirm invalid targets disable Start and highlight the affected entry.

## 2. Minimal relay — hotbar only

Queue two disposable pickaxes already in the hotbar:

1. Pickaxe A — Until Broken.
2. Pickaxe B — Until Broken.

Expected:

- A becomes selected and mines normally.
- After A breaks, B becomes selected.
- after B breaks, Attack is released;
- queue exhausts and clears;
- HUD disappears.

## 3. Main inventory -> empty hotbar

Place a queued tool only in the main inventory with at least one empty hotbar slot.

Expected:

- tool moves to the first empty hotbar slot from left to right;
- Pick Relay selects that slot;
- unrelated items are unchanged.

## 4. Initial full-hotbar fallback

Fill all nine hotbar slots with unrelated items. Put the first queued tool in the main inventory.

Expected:

- first queued tool swaps into hotbar slot 1;
- the displaced slot-1 item moves to the queued tool's former main-inventory slot;
- no item disappears or duplicates.

## 5. Full hotbar + preserved relay

Use:

1. Tool A in hotbar — Blocks 2, Preserve at 1.
2. Tool B in main inventory — Blocks 2, Preserve at 1.

Keep the hotbar full.

Expected when A finishes:

- A swaps directly into B's former inventory slot;
- B occupies A's active hotbar slot;
- no extra empty slot is required;
- both stacks remain unique and intact.

## 6. Queue order beats physical order

Place tools physically in arbitrary positions and queue them in a different order.

Expected: execution follows queue order only.

## 7. Block Budget

Set a tool to `Blocks Broken = 5` and mine a generator feeding drops directly into a hopper if available.

Expected:

- HUD increments exactly once per successful player block destruction;
- item pickup is irrelevant;
- partial breaking/clicking does not increment;
- after 5, the entry relays before another Pick Relay attack is initiated.

## 8. Durability Budget + Unbreaking

Use an Unbreaking tool with a small durability budget.

Expected:

- budget advances only when the ItemStack's damage actually increases;
- blocks where Unbreaking prevents damage do not consume the durability budget.

## 9. Preserve at 1

Use a low-durability tool with Preserve at 1.

Expected:

- tool reaches 1 remaining durability and is never used again;
- entry becomes preserved and relays/stops.

Also queue a tool that already has exactly 1 durability remaining with Preserve enabled.

Expected: it is skipped without one attempted use.

## 10. Mending observation

If practical, use a Mending tool and cause it to consume durability, then repair afterward with XP while the same session remains active.

Expected:

- durability already consumed remains in the budget counter after repair;
- repair never subtracts prior consumption.

## 11. Movement safety

Start mining and test separately:

- WASD;
- jump;
- mob/player push;
- water movement;
- knockback if convenient.

Expected: any real position change stops immediately and releases Attack.

Camera-only rotation must NOT stop.

## 12. Physical click emergency stop

While ACTIVE and no Pick Relay GUI is open:

- physically left-click;
- on another run, physically right-click.

Expected:

- input is consumed;
- session stops immediately;
- the emergency click does not also perform the world action.

## 13. GUI while ACTIVE

Open Pick Relay with its bind during an active session.

Expected:

- mining continues;
- queue is read-only;
- hover/tooltips/live progress work;
- normal clicks inside Pick Relay do not trigger emergency stop;
- Stop button ends and clears the session.

## 14. Fail-safe identity/inventory tests

During ACTIVE, if another mod or controlled test can move/replace the expected queued tool unexpectedly, do so.

Expected: Pick Relay stops rather than selecting another similar tool.

## 15. Lifecycle cleanup

Test death, dimension change, disconnect/world exit and queue completion.

Expected in every case:

- Attack released;
- HUD removed;
- session state cleared;
- temporary queue cleared;
- no continued mining after returning/changing world.
