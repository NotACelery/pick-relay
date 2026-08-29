# Pick Relay — Development Guide

This document is the canonical development companion for Pick Relay 1.1.1.

The public-facing behavior belongs in `README.md`. The functional contract belongs in
`docs/Pick-Relay-Especificacion.md`. This file concentrates implementation boundaries,
source conventions, layout rules, invariants and regression-sensitive details so the Java
source can remain compact and largely comment-free.

---

## 1. Current target

- Minecraft 1.21.1
- NeoForge 21.1.235+
- Java 21
- ModDevGradle 2.0.143
- Client-side only
- Current stable release: 1.1.1

The server does not require Pick Relay.

---

## 2. Product invariant

Pick Relay is a scheduler, not an automatic tool selector.

The user chooses:

- which concrete tools participate;
- their exact queue order;
- the work rule for each entry;
- the target budget for that entry;
- whether the tool must be preserved at one durability;
- the mining mode used by the session.

Minecraft remains authoritative for normal mining mechanics, reach, block hardness,
enchantments, drops and durability. Pick Relay coordinates normal client actions around an
explicit user-authored plan.

---

## 3. Source hygiene

### 3.1 Formatting baseline

Java source uses:

- four spaces per indentation level;
- no tabs;
- one logical statement per line;
- braces for control-flow blocks;
- a practical 120-column line limit;
- multiline builders, translations and compound conditions when they exceed that limit;
- blank lines between distinct logical phases of a method;
- final newline in every text file.

`.editorconfig` captures the baseline shared by editors.

### 3.2 Comment policy

Implementation comments are intentionally kept to an absolute minimum.

A comment is justified only when it explains a constraint that cannot be expressed safely
through names, types or control flow. Historical notes, feature explanations, QA reasoning,
layout behavior and architecture belong in Markdown documentation instead of being duplicated
throughout the source tree.

The goal is not "no documentation". The goal is one maintained documentation layer instead of
stale comments attached to old implementations.

### 3.3 Formatting-only changes

A formatting pass must not be mixed with gameplay changes.

For formatting-only work:

1. preserve the Java token stream;
2. change only whitespace and comments;
3. run a syntax parse afterward;
4. keep user-facing changelog entries out unless behavior changed;
5. run the relevant in-game regression checklist before release.

---

## 4. Architectural map

The project is intentionally split by responsibility.

### `PickRelay`

Mod entry point and registration boundary.

### `client/ClientEvents`

Client event wiring. It bridges Minecraft/NeoForge callbacks into the controller, GUI, HUD and
session lifecycle without becoming a second source of gameplay state.

### `client/PickRelayController`

Authoritative runtime coordinator.

It owns the session state machine and coordinates:

- configured queue;
- configured/session mining mode;
- active queue index;
- session start/stop;
- controlled attack lifecycle;
- work-target resolution;
- tool transitions;
- cleanup;
- safety failures;
- session counters exposed to the GUI/HUD.

Other client classes should query or request operations through the controller instead of
creating parallel session state.

### `client/RelayDebug`

Optional development logging. It must remain silent by default and is enabled through
`-Dpickrelay.debug=true`.

### `client/RelayKeyMappings`

Client key registration. The Open Pick Relay binding is a toggle: it opens the screen when
closed and closes the Pick Relay screen when already open.

### `client/gui/PickRelayScreen`

Configuration, inspection and live-session presentation.

The screen owns GUI-local interaction state only:

- selected queue entry;
- inventory drag gesture state;
- queue drag state;
- widgets/editors;
- responsive layout calculations;
- live telemetry rendering;
- tooltip assembly.

It must not become the source of truth for the relay session itself.

### `client/hud/PickRelayHud`

Compact runtime feedback above the hotbar. It mirrors controller/session state and does not own
relay logic.

### `client/inventory/InventoryRelayManager`

Physical inventory/hotbar operations required to make the next concrete queued tool the active
main-hand tool.

### `client/progress/MiningProgressTracker`

Per-entry accounting for successful block destructions and durability actually consumed.

### `client/progress/MiningRateEstimator`

Read-only theoretical mining-rate estimation for the live work block and inspected queue tool.
It must never equip, damage or mutate the inspected stack.

### `client/safety/SafetyMonitor`

Spatial/session safety anchor. It observes player/world conditions and reports stop reasons; it
does not decide queue scheduling.

### `client/tool/ToolEligibility`

Defines which inventory items may be queued through vanilla tool tags.

### `client/tool/ToolFingerprint`

Logical fingerprint used to identify concrete tool stacks without injecting custom UUID data into
the player's items.

### `client/tool/ToolTracker`

Reconciles logical queue entries with their current physical inventory locations.

### `mixin/MultiPlayerGameModeInvoker`

Access bridge for the exact vanilla client mining operations required by Pick Relay.

### `mixin/MultiPlayerGameModeMixin`

Marks successful block destruction belonging to a controlled Pick Relay attack so accounting is
based on confirmed destruction rather than clicks or animation attempts.

### `session/RelayEntry`

One independently configured logical queue entry plus its runtime progress/snapshot state.

### `session/RelayQueue`

Ordered collection of up to 36 entries. Queue order is authoritative and independent of physical
inventory ordering.

### Session enums/results

- `RelayEntryStatus`: runtime state of an entry.
- `RelayMiningMode`: `SINGLE_BLOCK` or `LINE_MINING`.
- `RelayState`: global lifecycle state.
- `RelayValidationResult`: start/config validation result.
- `RelayWorkMode`: `UNTIL_BROKEN`, `DURABILITY`, `BLOCKS`.
- `StopReason`: normalized session termination reason.
- `RelayValidator`: queue and entry validation rules.

---

## 5. Session state machine

The global lifecycle is:

```text
IDLE
  -> CONFIGURING
  -> STARTING
  -> ACTIVE
  -> STOPPING
  -> IDLE
```

Important invariants:

- only the controller transitions runtime state;
- queue editing is disabled during `ACTIVE`, `STARTING` and `STOPPING`;
- opening the GUI during `ACTIVE` is inspection-only for queue structure/configuration;
- stop/cleanup must release controlled attack state;
- terminal or safety stops must not leave stale active-entry or mining-mode state behind;
- the queue is temporary and is intentionally cleared at the lifecycle boundaries described in
  the functional specification.

---

## 6. Queue model

### Capacity

Maximum queue size is 36, matching the player's 27 main inventory slots plus nine hotbar slots.
The GUI presents the plan as a 4x9 grid.

### Logical entry versus physical ItemStack

The queue never owns the real inventory stack.

A `RelayEntry` represents the chosen tool logically while the physical item remains in the
player inventory. Reordering or removing a queue entry therefore must never move, drop or delete
the actual item.

### Identity

Inventory slot is a locator, not an identity.

Tool tracking combines an entry-local identity with an ItemStack fingerprint so the relay can
follow a chosen tool after normal inventory movement. Minecraft cannot expose a truly unique
identity for two perfectly identical unmodified ItemStacks; deterministic one-to-one matching is
the safe fallback in that case.

---

## 7. Work modes

### Until Broken

Continue until the physical tool actually breaks.

### Durability Budget

Stop after the requested amount of durability has actually been consumed.

The tracker must count observed durability loss rather than only comparing initial and final
damage values. This is required so later Mending repairs do not erase already-consumed budget and
so Unbreaking only advances the budget when Minecraft really applies durability damage.

### Blocks Broken

Stop after the requested number of successful Pick Relay block destructions.

The counter is tied to controlled destruction confirmation. It is not a click counter, swing
counter, drop counter or inventory-item counter.

### Preserve at 1

Preservation is a safety constraint and therefore has priority over a production target. Pick
Relay must never intentionally consume the final remaining durability when preservation is active.

---

## 8. Mining modes

### Single Block

At Start, capture the exact coordinate of the block under the normal client raycast.

During the session:

- only that coordinate is a valid work block;
- looking elsewhere pauses controlled attack;
- temporary air at the coordinate pauses controlled attack;
- returning the crosshair to the same coordinate resumes work when a valid block exists again;
- camera movement alone never cancels the session.

This mode exists specifically to protect compact generators from accidentally mining a backing
block.

### Line Mining

There is no fixed coordinate.

Each work cycle resolves the currently targeted valid block under the normal raycast. Camera
movement redirects automining immediately. Temporary air/no target pauses attack without stopping
the session.

### Shared safety anchor

Neither mode uses camera direction as a stop condition. The common spatial safety anchor is the
player's position/dimension.

---

## 9. Controlled attack boundary

Pick Relay must distinguish its own automated destroy calls from ordinary player interaction.

Only destruction confirmed inside the controlled attack boundary may advance `Blocks Broken` or
session destruction counters.

This prevents unrelated world changes from being credited to the relay, including:

- another player breaking the block;
- explosions;
- pistons/world replacement;
- server-side state changes not caused by the local controlled mining cycle.

---

## 10. Inventory relay invariants

The logical queue order always wins over physical slot order.

### Tool already in hotbar

Select its slot and synchronize the selected slot through normal client/vanilla behavior.

### Tool in inventory with free hotbar slot

Move it into the first usable free hotbar slot.

### Full hotbar

Reuse the active hotbar slot as the relay slot, swapping the outgoing physical tool back into the
incoming tool's previous inventory location.

Every physical move must be verified. Ambiguous or unsafe inventory state is a fail-safe condition,
not a reason to guess.

### Missing queued tool

A missing pending tool is skipped when its turn arrives instead of invalidating the entire plan.
An active tool that moved is first reconciled/re-located when safely possible.

---

## 11. GUI architecture

`PickRelayScreen` is intentionally stateful as a GUI but should remain presentation-oriented.

Its methods naturally fall into these groups:

1. widget creation and synchronization;
2. gameplay-input suppression while the screen is open;
3. inventory/queue mouse gestures;
4. selected-entry editing;
5. queue and inventory rendering;
6. selected-tool details;
7. session telemetry/effects/attributes;
8. validation and drag feedback;
9. tooltips;
10. responsive geometry helpers;
11. low-level slot/panel drawing;
12. screen close lifecycle.

The class is large because Minecraft `Screen` code couples rendering, hit-testing and widget
positions tightly. Do not split it during a release hotfix solely to reduce line count. A future
refactor should extract coherent read-only or geometry responsibilities with dedicated regression
tests rather than scattering screen state across several partially-coupled classes.

Good future extraction candidates are documented in section 20.

---

## 12. Responsive layout

The GUI must remain usable across normal and compact window sizes.

Core dimensions in 1.1.1:

```text
columns                  = 9
queue rows               = 4
inventory rows           = 4
slot size                = 18 px
grid width               = 162 px
queue height             = 72 px
side-by-side grid gap    = 24 px
session panel width      = 108 px
session panel height     = queue height + 10 px
session panel gap        = 6 px
session panel padding    = 5 px
session row height       = 10 px
layout bottom margin     = 30 px
```

### Normal-height layout

- Queue remains the primary upper grid.
- Session panel appears to the right when the window is wide enough.
- Player Inventory is below the queue/details area.

### Compact-height layout

When vertical space is constrained:

- Queue and Player Inventory become side by side.
- When sufficient horizontal space exists, Session panel sits between them.
- Selected Tool/details remain below the upper grid area rather than being clipped off-screen.

### Ultra-narrow fallback

If the window cannot fit Queue + Session panel + Inventory comfortably, the Session panel is
hidden before sacrificing queue/inventory usability. The 1.0.1 two-grid fallback remains valid.

### Layout invariant

Rendering and hit-testing must use the same geometry helpers. Never add a visual offset directly in
`render...` without ensuring click/drag/tooltip calculations resolve through the corresponding
layout method.

---

## 13. GUI input suppression

The Pick Relay screen is not allowed to behave like a transparent gameplay overlay.

While open, normal background gameplay input is suppressed:

- Use is released;
- Attack key state is released;
- W/A/S/D are released;
- Jump is released;
- Sneak is released;
- Sprint is released;
- ongoing item use is cancelled/released;
- manual block destruction is stopped when no Pick Relay session owns the attack cycle.

During an `ACTIVE` relay session, controlled Pick Relay automining is the only gameplay action that
continues intentionally behind the screen.

This distinction is a 1.1.1 release-critical invariant.

---

## 14. Queue gestures

### Inventory click

Left-clicking an eligible tool toggles its queue membership.

### Inventory paint drag

The first touched slot selects the gesture mode:

- not queued -> ADD;
- already queued -> REMOVE.

Each inventory slot may be processed only once per drag gesture.

### Queue click

A short left click selects the entry for inspection/editing.

### Queue right click

Removes the logical entry.

### Queue drag

Movement beyond the drag threshold becomes reorder mode.

Drop behavior distinguishes:

- center target -> swap;
- insertion side/edge -> insert before/after;
- outside valid queue grid -> remove logical queue entry.

No queue gesture may physically throw or delete the player's ItemStack.

---

## 15. Selected Tool panel

The selected queue entry is GUI-local state and is independent of the runtime ACTIVE entry.

The panel exposes:

- queue position;
- stack name/icon;
- current durability;
- runtime/config progress;
- current work mode;
- work target editor where relevant;
- Preserve at 1;
- theoretical mining-rate preview against the current target block.

Visual states must remain distinguishable:

- ACTIVE entry outer highlight;
- selected-for-inspection inner highlight;
- drag target highlight.

---

## 16. Mining-rate estimator

The estimator is observational.

For the inspected tool it may account for:

- stack mining speed for the target `BlockState`;
- correct-tool/tier requirement;
- main-hand attribute modifiers supplied by the stack;
- Efficiency and normal vanilla/modded attribute contributions;
- Haste;
- Conduit Power;
- Mining Fatigue;
- `BLOCK_BREAK_SPEED`;
- `MINING_EFFICIENCY`;
- `SUBMERGED_MINING_SPEED`;
- underwater/ground penalties where Minecraft applies them.

The result is expressed as theoretical blocks per second and seconds per block after quantizing
through Minecraft's tick-based destruction progress.

The estimator must never:

- equip the inspected tool;
- mutate inventory;
- damage a stack;
- start a destroy action;
- alter controller state.

Generator respawn delays, network latency, TPS and external mod logic are deliberately outside the
estimate.

---

## 17. Session panel

The live panel is read-only telemetry.

It can show:

- elapsed relay time;
- successful relay block destructions;
- active queue position;
- live theoretical BPS/current target;
- mining-related status effects first;
- other active effects;
- positive mining attribute modifiers.

Mining-related effects receive priority so Haste, Conduit Power and Mining Fatigue do not disappear
behind less relevant effects when vertical space is limited.

Attribute modifiers are deduplicated by modifier identity before rendering.

The panel must gracefully degrade when no valid target, player, effect or attribute data is
available.

---

## 18. Safety model

Pick Relay follows a fail-safe policy.

When the engine cannot prove that continuing is safe, stopping is preferable to mining the wrong
block or corrupting the user's inventory plan.

Safety-sensitive events include:

- displacement beyond tolerance;
- knockback/falling displacement;
- death/respawn;
- disconnect/world unload;
- dimension change;
- unresolved active tool;
- unsafe inventory synchronization;
- exhausted queue;
- internal inconsistent state.

Camera rotation alone is never a safety stop.

---

## 19. Documentation ownership

Keep each document focused:

- `README.md`: user-facing feature overview and build entry point.
- `docs/Pick-Relay-Especificacion.md`: behavioral contract.
- `docs/DEVELOPMENT.md`: implementation invariants and developer guidance.
- `docs/TESTING-1.1.1.md`: current release regression checklist.
- `docs/PUBLISHING.md`: current release/publishing checklist.
- `BUILD-STATUS.md`: current release/build validation status.
- `CHANGELOG.md`: user-visible version changes only.
- `SOURCE-MANIFEST.json`: exact expected source-tree hashes for recoverability.

Do not duplicate the same historical explanation in Java comments and several Markdown files.

---

## 20. Future refactor boundaries

The current 1.1.1 `PickRelayScreen` is intentionally not split during the release formatting pass.
With 1.1.1 gameplay validation complete, these are safe candidates to evaluate independently in a future development cycle.

### 20.1 Layout calculator

Potential extraction:

```text
PickRelayLayout
```

It could own pure geometry calculations for:

- queue grid position;
- inventory grid position;
- session panel visibility/position;
- Selected Tool region;
- mode/action row positions;
- hit-test grid coordinates.

Requirement: geometry must be pure/read-only and receive screen width/height as inputs.

### 20.2 Session telemetry formatter

Potential extraction:

```text
SessionTelemetry
```

It could build immutable display rows from controller/player state:

- time formatting;
- effect prioritization;
- attribute modifier formatting;
- modifier source humanization.

Requirement: no rendering API and no mutation of session/player state.

### 20.3 Tooltip builder

Potential extraction:

```text
RelayTooltipBuilder
```

It could append Pick Relay metadata to the vanilla/modded ItemStack tooltip while leaving rendering
inside the screen.

### 20.4 Do not extract interaction state casually

Inventory paint-drag and queue-drag state are tightly coupled to Minecraft mouse callbacks. Moving
them out only makes sense if the extracted object owns a complete gesture state machine and has
dedicated regression coverage.

---

## 21. Release regression minimum

Before publishing any release that touches the controller, screen, inventory relay or progress
tracking, validate at minimum:

1. add/remove/reorder a mixed queue;
2. all three work modes;
3. Preserve at 1;
4. Single Block pause/resume without backing-block damage;
5. Line Mining camera redirection;
6. full-hotbar relay;
7. moved queued tool reconciliation;
8. missing queued tool skip;
9. block accounting with hopper-fed drops;
10. Unbreaking and Mending accounting;
11. movement/knockback/death/disconnect/dimension stops;
12. GUI open/close during active mining;
13. 1.1.1 background-input suppression;
14. responsive normal/compact/ultra-narrow layouts;
15. session panel/effects/attribute display;
16. selected-tool BPS preview;
17. HUD progress/waiting state.

The release-specific checklist remains authoritative for the exact hotfix being shipped.

---

## 22. Formatting validation

A clean-code-only pass should produce all of the following:

- no Java line above 120 columns unless an unavoidable literal requires it;
- no multiple executable statements packed onto one line;
- no explanatory source comments that duplicate documentation;
- Java source parses with Java 21;
- normalized Java token stream is unchanged from the pre-format source across all 25 Java files;
- Markdown/JSON/properties remain valid text with final newlines;
- no behavior change is added to `CHANGELOG.md` when none occurred.

---

## 23. Source snapshots and `SOURCE-MANIFEST.json`

The 1.1.1 cleanup baseline is a complete repository snapshot containing the current validated
source tree, configuration, resources and release documentation.

`SOURCE-MANIFEST.json` is the integrity index for the release-relevant tree:

- regenerate its hashes after an intentional source/configuration/documentation change;
- include `.editorconfig` and `docs/DEVELOPMENT.md` because they define the maintained source baseline;
- do not treat `.git`, build outputs, IDE metadata or temporary tooling as release content;
- verify the manifest before producing a release archive or handing the project to another working
  environment.

A differential recovery ZIP may still be useful for isolated patches, but a complete source
snapshot is the canonical baseline for release preparation.

---

## 24. Build and QA

The repository's build helpers select Java 21 and obtain the pinned Gradle toolchain when needed.

### 24.1 Java 21 discovery

The helper scripts must prefer an explicitly configured project JDK, then `JAVA_HOME`, then PATH,
while still checking common launcher/vendor JDK locations when PATH points to an incompatible Java
version. Java detection should rely primarily on the normal `java -version` banner and only fall
back to alternate JVM metadata when necessary.

This keeps builds deterministic on development machines that have several JDK versions installed.

Expected release artifact:

```text
build/libs/pickrelay-1.21.1-1.1.1.jar
```

The maintainer accepted the 1.1.1 gameplay build after in-game QA. Any later Java/resource change must produce a new JAR and repeat the affected checks from `docs/TESTING-1.1.1.md` before publication or replacement of the binary.
