> **HISTORICAL — SUPERSEDED BY alpha.6:** alpha.5 camera-following behaviour is retained as **Line Mining**. alpha.6 additionally introduces the explicit **Single Block** mode.

# Pick Relay 0.1.0-alpha.5 — Gameplay test matrix

For historical alpha.5, the fixed-target tests were obsolete because camera-following was the only mode. In alpha.6 that camera-following behaviour is Line Mining, while fixed-target safety returns as the explicit Single Block mode.

## 1. Start requirement

- Prepare one valid queued tool.
- Point at air/out of reach: Start must refuse and show the initial block requirement.
- Point at a valid block within normal reach: Start must succeed.

## 2. Camera follows current block — critical

- Start while aiming at Block A.
- Without moving the player, rotate to adjacent Block B while mining.
- Pick Relay must stop working on A and begin working on B normally.
- Rotate to Block C and repeat.
- Session must remain ACTIVE throughout.
- No "waiting for anchored target" HUD/GUI state may exist.

## 3. Temporary air / generator gap — critical

- Use a regenerating cobblestone/stone/OneBlock-like target.
- Let Pick Relay break the current block.
- During the air/regeneration gap, session remains ACTIVE.
- When a valid block appears under the current crosshair again, mining resumes automatically.
- Rotate during the air gap to another valid block: Pick Relay may mine that newly aimed block because camera direction is authoritative.

## 4. Player-position safety — critical

While ACTIVE, test separately:

- W/A/S/D movement;
- jump that changes position;
- mob/player knockback;
- flowing water;
- piston displacement if practical.

Every real displacement beyond tolerance must stop immediately with `PLAYER_MOVED` regardless of camera direction.

Camera rotation alone must never trigger `PLAYER_MOVED`.

## 5. Destroy provenance / Blocks Budget

- Configure Blocks Broken = 5.
- Rotate between multiple blocks while remaining stationary.
- Exactly five successful blocks destroyed by Pick Relay across those targets should complete the entry.
- Partial hits must not count.
- Breaking/changing a block by an unrelated cause must not advance the budget merely because Pick Relay is ACTIVE.

## 6. Entity/no-block crosshair

- During ACTIVE, point at an entity or empty air.
- Pick Relay should stop the current block destroy animation but remain ACTIVE.
- It should not use the block budget while no block is destroyed.
- Aim at a valid block again: mining resumes.

## 7. Queue relay

Repeat existing relay tests:

- queued tool already in hotbar;
- first empty hotbar slot left-to-right;
- initial full hotbar -> slot 1 swap;
- preserved/completed current tool + full hotbar -> active relay-slot swap;
- pending queued tool displaced by a swap keeps correct logical slot;
- deliberately invalidate a tracked tool -> fail-safe stop, no substitution.

## 8. Preserve at 1

- Tool begins above 1 durability and Preserve enabled.
- It must relay before an additional use can break it.
- Tool already at 1 when its turn arrives must be skipped without use.

## 9. Durability Budget / Unbreaking / Mending

- Confirm budget advances only when ItemStack damage actually increases.
- Unbreaking-prevented damage does not consume budget.
- Later Mending repair does not subtract already observed consumption.

## 10. Physical emergency input

- Rotate camera using mouse movement only: session continues.
- Physical gameplay left click: immediate stop.
- Physical gameplay right click: immediate stop.
- Clicks inside PickRelayScreen: no emergency stop.

## 11. GUI during ACTIVE

- Open Pick Relay while mining.
- World remains unpaused and mining continues using the player's current camera direction.
- Queue remains read-only; inspection/Stop works.

## 12. Queue exhaustion

- Run a small disposable queue to completion.
- Attack/destroy animation released.
- Queue/session cleared.
- HUD removed.
- State returns to IDLE.
