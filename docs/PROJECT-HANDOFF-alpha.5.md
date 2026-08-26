> **HISTORICAL — SUPERSEDED BY alpha.6:** alpha.5 camera-following behaviour is retained as **Line Mining**. alpha.6 additionally introduces the explicit **Single Block** mode.

# Pick Relay — Technical handoff 0.1.0-alpha.5

This file supersedes the alpha.4 handoff. The alpha.4 fixed-coordinate target lock was an incorrect interpretation **as the only/global behaviour**. Alpha.6 later restores that concept only as the explicit opt-in Single Block mode.

## Authoritative Auto Mining behaviour

Pick Relay should behave like THE Pick's validated Auto Mining controller:

1. `SafetyMonitor` anchors the **player position** and dimension at Start.
2. Camera rotation is free and never cancels the session.
3. Each active mining cycle calls `player.pick(player.blockInteractionRange(), 1.0F, false)` again.
4. If the raycast resolves to a non-air block, Pick Relay mines that current block through vanilla `continueDestroyBlock(pos, direction)`.
5. If the camera now points at a different block, mining follows that different block immediately.
6. If the raycast hits air/no block/entity, stop the current destroy animation but keep the relay session ACTIVE.
7. When a valid block comes under the crosshair again, mining resumes automatically.
8. Any real player displacement beyond the tiny floating-point tolerance stops the entire session (`PLAYER_MOVED`), whether caused by WASD, knockback, water, mobs, pistons, etc.

For this historical alpha.5 behaviour there was no stored block coordinate. In alpha.6 that remains true for Line Mining, while Single Block deliberately stores one session target coordinate.

## Destroy/progress provenance

`MultiPlayerGameModeMixin` still records successful `destroyBlock` calls only when `PickRelayController.isControlledAttackInvocation()` is true. The controller sets that flag only around its own synchronous `continueDestroyBlock(...)` call.

Therefore:

- rotating to another block is valid and its successful destruction counts;
- unrelated block changes/destruction do not count merely because the relay is ACTIVE;
- Block Budget remains tool/session work progress, not world-state polling.

## Core invariants retained

- Queue is explicit and ordered; never choose an unselected substitute tool.
- Queue max = 36 concrete tools.
- Queue is read-only during ACTIVE.
- Preserve at 1 > production target.
- First empty hotbar slot left-to-right; full-hotbar fallback uses the active relay slot, with initial fallback slot 1.
- Every physical relay swap is verified and tracked tools displaced by the swap have their logical slot updated.
- Selected hotbar slot is explicitly synchronized to the server before mining with the new tool.
- Tool identity ambiguity/desync => stop, never guess.
- Physical gameplay left/right click => emergency stop.
- Death, disconnect and dimension change => stop.
- Queue completion => stop and clear temporary session/queue.

## Current mining implementation

The old `MinecraftAttackInvoker` from alpha.4 has been removed. Mining uses the same high-level path as THE Pick:

```text
raycast current camera
→ valid block?
    no  → stopDestroyBlock, remain ACTIVE
    yes → ClientHooks.onClickInput
        → continueDestroyBlock(current pos/current face)
        → vanilla particles/swing
```

The provenance flag wraps only the `continueDestroyBlock(...)` call.

## Next required validation

Run `docs/TESTING-alpha.5.md` in a real 1.21.1 NeoForge client. Highest-priority tests:

1. rotate continuously across multiple blocks without moving the player;
2. generator air gaps regenerate and resume automatically;
3. move/knock player while camera stays fixed -> immediate stop;
4. verify Blocks Broken follows whichever current block Pick Relay actually destroys;
5. full-hotbar preserved-tool relay;
6. Unbreaking/Mending durability budgets;
7. 36-tool disposable queue.
