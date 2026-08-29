# Build status

Release: **1.1.1**  
Status: **development complete; accepted after in-game QA on 2026-08-29**

## Target

- Minecraft 1.21.1
- NeoForge 21.1.235+
- Java 21
- ModDevGradle 2.0.143
- Local build helper: Gradle 8.10.2

## Stable baseline

Pick Relay 1.1.1 is the current stable source baseline. The 1.0.1 release previously validated the relay engine and responsive queue/inventory layout; 1.1.1 keeps that behavior and adds the session/technical tooling developed in the 1.1.x cycle.

The maintainer accepted the final 1.1.1 gameplay build after in-game QA. Session telemetry, theoretical BPS display and the responsive Queue / Session / Player Inventory layout were explicitly validated in gameplay, including compact-window behavior. The release-specific regression matrix remains in `docs/TESTING-1.1.1.md` for future rebuilds.

## 1.1.x release scope

- Pick Relay keybind toggles the GUI closed as well as open.
- The normal inventory key closes the Pick Relay GUI without stopping an active session.
- Live Session panel tracks elapsed time, successful Pick Relay block destructions, active queue position and theoretical BPS against the targeted block.
- Active status effects are shown in the Pick Relay GUI with Haste, Conduit Power and Mining Fatigue prioritized/highlighted.
- Positive block-break/mining-efficiency/submerged-mining-speed attribute modifiers are surfaced generically, including compatible accessory-mod bonuses without a hard dependency.
- Selected queue tools can be compared against the targeted block with a theoretical BPS/seconds-per-block estimator that simulates that stack as the main-hand tool.
- Selected queue entries use a distinct highlight and queue drag insertion/swap targets use a separate visual cue.
- Responsive layout keeps the Session panel between Queue and Player Inventory when enough horizontal space exists; ultra-narrow widths retain the two-grid fallback instead of sacrificing layout usability.
- 1.1.1 makes the Pick Relay screen an exclusive manual-input state: ongoing item use is released and manual use/attack/movement input is suppressed while active Pick Relay automining remains allowed in the background.

## Source-maintenance baseline

- All 25 Java source files use the shared project formatting baseline.
- Java implementation comments remain intentionally minimal; architecture and non-obvious constraints live in `docs/DEVELOPMENT.md`.
- `.editorconfig` defines the formatting baseline for future edits.
- `SOURCE-MANIFEST.json` indexes the exact release-relevant source tree for recoverability.
- The current repository snapshot is the canonical 1.1.1 baseline for future development.

No Java behavior is changed by the final Git/documentation pass performed after QA.

## Build validation note

The preparation runtime used for some source-maintenance passes could resolve Java 21 but could not resolve `downloads.gradle.org` or `services.gradle.org`, so it could not perform its own Gradle download. The maintainer-built 1.1.1 artifact was nevertheless run and accepted in Minecraft during external gameplay QA.

## Release artifact

Expected artifact:

```text
build/libs/pickrelay-1.21.1-1.1.1.jar
```

If Java source or resources change after this baseline, rebuild the JAR and repeat the affected items from `docs/TESTING-1.1.1.md`. Documentation-only edits do not change the published binary.

## Visual identity

- The official Pick Relay icon is bundled as `src/main/resources/pick-relay-icon.png`.
- NeoForge metadata references it through `logoFile="pick-relay-icon.png"` with `logoBlur=false` so the pixel art stays crisp in mod-list UIs.
- The same image is the canonical project icon to upload to Modrinth and CurseForge.
