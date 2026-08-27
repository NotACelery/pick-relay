# Build status

Release: **1.0.0**

## Target

- Minecraft 1.21.1
- NeoForge 21.1.235+
- Java 21
- ModDevGradle 2.0.143
- Local build helper: Gradle 8.10.2

## Validation status

The complete 1.0 feature set has been built and validated in Minecraft during development, including:

- queue ordering and reordering;
- concrete tool relocation across inventory slots;
- full-hotbar relay behaviour;
- missing-tool skipping;
- Until Broken, Durability Budget and Blocks Broken;
- Preserve at 1;
- Unbreaking and Mending durability accounting;
- hopper-compatible block budgets;
- Single Block and Line Mining;
- 36-tool queues;
- supported axes, shovels and hoes;
- movement, death, disconnect and dimension safety;
- GUI lifecycle and active-session inspection.

The first successful external Windows build used Java 21 with Gradle 8.10.2. Java 17, 21 and 25 may coexist on the same machine; Pick Relay's helper only selects Java 21 for this project build.

## Release artifact

Expected output:

```text
build/libs/pickrelay-1.21.1-1.0.0.jar
```

Before publishing, perform the short regression pass in `docs/TESTING-1.0.0.md` against the exact final JAR.

## Visual identity

- The official 1.0.0 icon is bundled as `src/main/resources/pick-relay-icon.png`.
- NeoForge metadata references it through `logoFile="pick-relay-icon.png"` with `logoBlur=false` so the pixel art stays crisp in mod-list UIs.
- The same image is the canonical project icon to upload to Modrinth and CurseForge.
