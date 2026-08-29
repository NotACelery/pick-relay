# Pick Relay 1.1.1 — Publishing checklist

This document describes the current public-release workflow. Historical alpha and previous-version handoff documents are intentionally left to Git history rather than kept in the active source tree.

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

## Freeze the artifact

1. Run `build.bat` on Windows or `./build.sh` on Linux/WSL.
2. Confirm `build/libs/pickrelay-1.21.1-1.1.1.jar`.
3. Run `docs/TESTING-1.1.1.md` against that exact JAR.
4. Do not change source after the final QA without rebuilding and repeating the affected tests.
5. Tag the exact published commit after the artifact is accepted.

## Public positioning

Pick Relay is an explicit tool scheduler, not an automatic best-tool selector or pathfinding bot. Public descriptions should keep these points visible:

- ordered queue of concrete tools;
- Until Broken, Durability Budget, and Blocks Broken work modes;
- Preserve at 1;
- Single Block and Line Mining;
- automatic inventory/hotbar relay;
- responsive configuration UI and live session panel;
- client-side operation without free resources, reach changes, or mining-speed cheats.

## 1.1.1 release note

Use the 1.1.1 section from `CHANGELOG.md`. The hotfix makes the Pick Relay GUI own manual input while it is open: ongoing item use is released, manual use/attack/movement input is suppressed, and active Pick Relay automining remains allowed.

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
- use the matching 1.1.1 changelog;
- verify icon, description, loader, game version, and file name from a logged-out/public view when possible.

Never publish two different binaries under the same version number.
