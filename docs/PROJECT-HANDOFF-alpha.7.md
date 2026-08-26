# Pick Relay — Technical handoff 0.1.0-alpha.7

## Current baseline

Use this snapshot as the continuation point after alpha.6. The mining-mode decision remains unchanged:

- **Single Block** pins the coordinate selected at Start but never pins player camera direction. Looking elsewhere/temporary air pauses mining without cancelling.
- **Line Mining** follows the current valid crosshair block every cycle, matching THE Pick.
- Both stop on actual player displacement; camera rotation never stops the session by itself.

## New safety invariants in alpha.7

### Provenance-confirmed breakage

An empty active slot is **not** enough to infer that the tool broke. `MultiPlayerGameModeMixin` already scopes successful destroys to Pick Relay. When a damageable tool disappears during that confirmed destroy, the entry is marked `BROKEN`. On the following tick an empty slot advances only if that mark exists. Otherwise the session stops as `TOOL_INVALID`.

This distinction protects against Q/drop actions, server corrections, external inventory mods and unexplained tool removal being silently treated as a legitimate relay transition.

### Whole-queue runtime audit

Every active cycle checks every `PENDING` and `ACTIVE` entry:

1. tracked slot is 0..35;
2. no two live tracked entries claim the same slot;
3. the concrete stack still matches the expected fingerprint;
4. exactly one entry is ACTIVE;
5. the player remains on the normal inventory menu.

Any ambiguity stops rather than trying to recover automatically.

### Exact SWAP conservation

The local post-`ClickType.SWAP` verification now compares stack count plus item/components for both sides. This matters because a relay may displace an unqueued hotbar stack containing multiple items.

## UX completed in this pass

- selected-tool icon in the central details panel;
- selected durability remaining/max + percentage;
- vanilla/modded hover tooltip on that icon;
- entry status in augmented queue tooltips;
- exact pre-start validation reason in invalid-entry tooltips;
- waiting feedback in HUD/GUI for both mining modes;
- Clear Queue in editable spacious layouts.

## Next step

Do not add THE Pick compatibility yet. First compile/run `alpha.7` and execute `docs/TESTING-alpha.7.md`, particularly the deliberate tool disappearance and external queued-tool move cases.
