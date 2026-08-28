# Build status

Release candidate: **1.1.1**

## Target

- Minecraft 1.21.1
- NeoForge 21.1.235+
- Java 21
- ModDevGradle 2.0.143
- Local build helper: Gradle 8.10.2

## Previously validated baseline

The published 1.0.1 build validated the complete relay engine and responsive compact-height layout, including queue ordering/reordering, concrete tool relocation, full-hotbar relay, missing-tool skipping, all work modes, Preserve at 1, durability accounting, both mining modes, movement/dimension safety, GUI lifecycle and the side-by-side queue/inventory layout.

## 1.1.0 feature set + 1.1.1 hotfix awaiting final gameplay regression

- Pick Relay keybind toggles the GUI closed as well as open.
- The normal inventory key closes the Pick Relay GUI without stopping an active session.
- Live session panel tracks elapsed session ticks, successful Pick Relay block destructions, active queue position and theoretical BPS against the targeted block.
- Active status effects are shown in the Pick Relay GUI with Haste, Conduit Power and Mining Fatigue prioritized/highlighted.
- Positive block-break/mining-efficiency/submerged-mining-speed attribute modifiers are surfaced generically, including compatible accessory-mod bonuses without a hard dependency.
- Selected queue tools can be compared against the targeted block with a theoretical BPS/seconds-per-block estimator that simulates that stack as the main-hand tool.
- Selected queue entries use a distinct highlight and queue drag insertion/swap targets use a separate visual cue.
- Responsive layout keeps the session panel between queue and inventory when enough horizontal space exists; ultra-narrow widths retain the 1.0.1 two-grid fallback instead of sacrificing layout usability.
- 1.1.1: opening/keeping the Pick Relay screen open releases ongoing item use and suppresses manual use/attack/movement inputs while preserving active Pick Relay automining.

Static checks in the preparation environment validate Java syntax/tokens, JSON resources, manifest paths and patch structure. The local build helper correctly resolves Java 21 (`/usr/bin/java` in this environment), but a full Gradle compile cannot be completed here because both `downloads.gradle.org` and `services.gradle.org` are not resolvable from the runtime.

Source-maintenance pass for the 1.1.1 candidate:

- All 25 Java source files were reviewed and normalized to the shared project formatting baseline.
- The complete Java token stream is unchanged by the cleanup when comments and whitespace are ignored.
- Java implementation comments are intentionally absent; architecture and non-obvious constraints live in `docs/DEVELOPMENT.md`.
- `.editorconfig` defines the formatting baseline for future edits.
- JSON/resources and build metadata are normalized without changing their effective values.
- The responsive layout regression matrix is included in `docs/TESTING-1.1.1.md`.

## Release artifact

Expected output after the local build:

```text
build/libs/pickrelay-1.21.1-1.1.1.jar
```

Run `docs/TESTING-1.1.1.md` against the exact final JAR before publishing.

## Visual identity

- The official Pick Relay icon is bundled as `src/main/resources/pick-relay-icon.png`.
- NeoForge metadata references it through `logoFile="pick-relay-icon.png"` with `logoBlur=false` so the pixel art stays crisp in mod-list UIs.
- The same image is the canonical project icon to upload to Modrinth and CurseForge.
