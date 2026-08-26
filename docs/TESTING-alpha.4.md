> **HISTORICAL — SUPERSEDED BY alpha.6:** alpha.4 applied fixed-coordinate targeting to every session. That is still wrong as a global rule, but the behaviour now exists deliberately as the opt-in **Single Block** mode. **Line Mining** retains THE Pick-style crosshair following.

# Pick Relay 0.1.0-alpha.4 — Focused gameplay validation

Run these in order. The first tests exercise the dangerous target/inventory protocol path before long AFK sessions.

## 1. Build and boot

- Run `build-alpha.bat` or `./build-alpha.sh`.
- Confirm Gradle 8.10.2 is downloaded and checksum verification succeeds.
- Confirm the mod loads with no mixin apply/invoker errors.
- Confirm Mouse Button 5 opens Pick Relay by default.

## 2. Planning GUI regression

- Add/remove tools with short left clicks.
- Drag ADD across several tools and verify first-entry order.
- Drag REMOVE starting from a selected tool and cross unselected tools; only selected entries should be removed.
- Reorder queue entries by swap, insert-before, insert-after, and drop-outside.
- Confirm right click removes a queue entry without moving/dropping its real ItemStack.

## 3. Per-entry controls

- Until Broken: no target control visible.
- Durability Budget: integer slider visible; its maximum equals usable remaining durability (minus 1 when Preserve is enabled).
- Blocks Broken: numeric field visible and accepts large positive values.
- Toggle Preserve at 1 and confirm the durability slider clamps if required.

## 4. Start target requirement

- With a valid queue, look at air/entity and confirm Start is disabled with the work-target validation message.
- Look at a block and confirm Start becomes available.
- Start the session and note the exact block position used as the work point.

## 5. Anchored work-position safety

Use a generator or a replaceable test block with a valuable/structural block directly behind it.

Expected:

- Pick Relay mines the initial position.
- When that position becomes air and the crosshair ray reaches the backing block, Pick Relay pauses instead of mining the backing block.
- When a new block appears at the original position, mining resumes.
- Rotate the camera to another block: session remains ACTIVE, HUD says it is waiting, and the other block is not attacked.
- Rotate back to the original position: mining resumes.
- Put an entity under the crosshair: Pick Relay does not attack it.

## 6. GUI while ACTIVE

- While actively mining the anchored target, open Pick Relay with the bind.
- Confirm the world does not pause.
- Confirm mining keeps working if Minecraft still reports the anchored block under the stored crosshair ray.
- Queue remains read-only.
- GUI clicks do not trigger emergency Stop.
- Close the GUI and verify the session continues.

## 7. Hotbar-only relay + selected-slot protocol

Queue two disposable tools already in different hotbar slots.

Expected:

- Tool #1 becomes selected.
- The selected slot is synchronized before Pick Relay initiates mining.
- After Tool #1 completes/breaks, Tool #2 is selected and synchronized before its first mining action.
- Server behavior/durability corresponds to the visible selected tool on the very first block after each relay.

## 8. Main inventory -> first empty hotbar

- Put the queued tool in main inventory.
- Leave multiple hotbar holes.

Expected: it moves to the first empty hotbar slot from left to right, becomes selected, and no unrelated stack changes.

## 9. Full hotbar initial fallback

- Fill all hotbar slots with unrelated items.
- Put the first queued tool in main inventory.

Expected:

- queued tool swaps into hotbar slot 1;
- old hotbar slot-1 item goes to the tool's former inventory slot;
- local post-SWAP verification passes;
- no item disappears/duplicates.

## 10. Full hotbar preserved relay

Queue:

1. Tool A in hotbar — Blocks 2 + Preserve at 1.
2. Tool B in main inventory — Blocks 2 + Preserve at 1.

With a full hotbar, expected when A completes:

- B swaps into A's active hotbar slot;
- A moves to B's former inventory position;
- A's tracked location updates;
- both concrete stacks remain intact.

## 11. Tracked destination tamper

If practical, use another inventory-management mod or controlled test to replace/move a queued tool that currently occupies a hotbar destination before Pick Relay needs to swap through it.

Expected: Pick Relay refuses the swap and stops with inventory/tool safety rather than displacing an ambiguous stack.

## 12. Blocks Broken provenance

Set Blocks Broken = 5.

Expected:

- only successful destruction from Pick Relay's own attack call increments the counter;
- clicks/partial breaking do not count;
- item pickup/hoppers do not matter;
- another player, piston, explosion, command, or unrelated client-mod destruction should not advance Pick Relay's counter merely because the session is active.

## 13. Durability + Unbreaking

Set a small Durability Budget on an Unbreaking tool.

Expected: only observed increases in ItemStack damage consume budget. Prevented durability loss does not count.

## 14. Mending observation

Consume durability, then repair the active tool with Mending while the same entry remains active if practical.

Expected: already-observed consumption never decreases when damage later goes down. Record any case where server synchronization combines damage+repair so tightly that the client never observes the intermediate damage.

## 15. Preserve at 1

- Test a low-durability tool.
- Test a queued tool already at exactly 1 durability before Start.

Expected: neither receives a Pick Relay use that breaks it; the second is skipped immediately when its turn arrives.

## 16. Movement and physical-input safety

During ACTIVE test separately:

- WASD;
- jump;
- water/push/knockback;
- physical left click;
- physical right click.

Expected: movement or either physical gameplay mouse click stops and releases mining. Camera rotation alone does not stop.

## 17. Lifecycle cleanup

Test:

- death/respawn;
- dimension change;
- disconnect;
- manual Stop;
- queue exhaustion.

Expected in every case:

- attack released;
- HUD removed;
- active state cleared;
- temporary queue cleared;
- no mining continues after lifecycle transition.
