# Pick Relay 0.1.0-alpha.6 — Gameplay test matrix

This pass adds the explicit **Single Block / Line Mining** session selector.

## 1. Mode selector

- Open Pick Relay while IDLE.
- Confirm two separate buttons exist before Start: Single Block and Line Mining.
- Switch repeatedly between them; exactly one should be visibly selected.
- Start a session and reopen the GUI. Both mode buttons must be read-only while ACTIVE.

## 2. Single Block — generator safety

- Aim at a cobblestone generator output and start in Single Block.
- Confirm the exact initial coordinate is mined.
- Let the output become temporary air so the backing block is visible through the ray.
- Confirm Pick Relay does NOT mine the backing block.
- When cobblestone regenerates at the captured coordinate, confirm mining resumes.

## 3. Single Block — camera movement

- While ACTIVE, rotate the camera onto a different reachable block.
- Session must remain ACTIVE but mining must pause.
- Return the crosshair to the captured coordinate and confirm mining resumes.
- Rotate freely without moving the player's feet; this must never trigger PLAYER_MOVED.

## 4. Line Mining — THE Pick semantics

- Start in Line Mining while looking at a valid block.
- Rotate across several blocks within normal reach.
- Confirm mining follows the current crosshair target each cycle.
- Look at air/an entity temporarily; the session remains ACTIVE and resumes on the next valid block.

## 5. Shared movement safety

Run both modes separately:

- WASD movement -> stop.
- jump/displacement -> stop.
- mob/player knockback -> stop.
- piston/water movement -> stop.
- camera rotation without displacement -> no stop.

## 6. Progress provenance

- In both modes, Blocks Broken advances only on successful Pick Relay destruction.
- Single Block waiting periods must not advance Blocks or Durability budgets.
- Line Mining changing targets must not double-count a destruction.

## 7. Relay regression

Repeat the alpha.5 high-risk cases in both modes:

- full hotbar initial swap;
- preserved tool -> next inventory tool relay;
- queued tool already in hotbar;
- 36 disposable tools;
- Unbreaking durability budget;
- Mending after observed durability use;
- inventory desync -> safe Stop.
