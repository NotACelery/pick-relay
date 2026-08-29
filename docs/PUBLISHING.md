# Pick Relay 1.1.1 — Publishing checklist

This document describes the current public-release workflow. Pick Relay 1.1.1 is the stable baseline and the first public release after 1.0.1; the 1.1.0 feature work is included in the 1.1.1 binary.

## Release target

| Field | Value |
| --- | --- |
| Version | 1.1.1 |
| Minecraft | 1.21.1 |
| Loader | NeoForge |
| Java | 21 |
| Environment | Client-side |
| Repository | `NotACelery/pick-relay` |
| License policy | All Rights Reserved |
| QA status | Accepted in-game on 2026-08-29 |

## Freeze the artifact

1. Keep the QA-accepted `build/libs/pickrelay-1.21.1-1.1.1.jar` as the publication binary.
2. Do not change Java source or resources after that QA without rebuilding and repeating the affected tests.
3. Documentation-only Git cleanup may be committed without changing the already-tested JAR.
4. Tag the exact published source/documentation commit after the artifact is accepted on the platform.

## Public positioning

Pick Relay is an explicit tool scheduler, not an automatic best-tool selector or pathfinding bot. Public descriptions should keep these points visible:

- ordered queue of concrete tools;
- Until Broken, Durability Budget, and Blocks Broken work modes;
- Preserve at 1;
- Single Block and Line Mining;
- automatic inventory/hotbar relay;
- responsive configuration UI and live Session panel;
- per-tool and live-session theoretical BPS estimation;
- visible mining buffs/debuffs and compatible mining-speed modifiers;
- client-side operation without free resources, reach changes, or mining-speed cheats.

## 1.1.1 release notes

CurseForge/Modrinth should use `docs/RELEASE-NOTES-1.1.1.md`, not only the short 1.1.1 subsection from `CHANGELOG.md`.

Reason: 1.0.1 was the previous public release, so the 1.1.1 upload needs to communicate both the 1.1.0 feature set and the 1.1.1 hotfix in one user-facing changelog.

## Visual identity

Use `src/main/resources/pick-relay-icon.png` for the project icon and the packaged mod metadata. Keep the pixel art unblurred where the platform allows it.

## Upload checks

For both Modrinth and CurseForge:

- version: `1.1.1`;
- Minecraft: `1.21.1`;
- loader: NeoForge;
- environment: client-side;
- source: `https://github.com/NotACelery/pick-relay`;
- upload the exact QA-tested JAR;
- paste `docs/RELEASE-NOTES-1.1.1.md` as the public changelog;
- verify icon, description, loader, game version, and file name from a logged-out/public view when possible.

Never publish two different binaries under the same version number.
