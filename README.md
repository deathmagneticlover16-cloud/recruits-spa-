# Recruits: Serious Animations

A Minecraft Forge 1.20.1 animation compatibility addon for Villager Recruits. It connects Recruits with Serious Player Animations, Player Animator, Mob Player Animator, and optional combat mods while leaving movement and AI under Recruits' control.

## Requirements

- Java 17
- Minecraft 1.20.1
- Forge 47.4.10
- Villager Recruits 1.15.2
- Serious Player Animations 1.1.1
- Player Animator 1.0.2-rc1+1.20
- Mob Player Animator 1.3.3

Better Combat 1.8.6 and Betsy Ross 2.1.2 are supported optional integrations.

## Building

Download the required mod JARs and place them in [`libs/`](libs/README.md). Then run:

```bash
./gradlew build
```

The reobfuscated mod JAR will be written to `build/libs/`.

For a development client:

```bash
./gradlew runClient
```

The dependency JARs in `libs/` are used only on the development classpath and are not bundled into the finished addon.

## Shield-wall animation preview

Look directly at a Recruit and run:

```text
/recruitspa test
```

The selected Recruit plays `shield_rank_one` and holds its final frame. Look at the same Recruit and run `/recruitspa stop` to clear the preview. This tool is client-side and does not change Recruit movement, AI, position, or yaw.

## License

This project is available under the [MIT License](LICENSE).

## Versioning

Releases follow [Semantic Versioning](https://semver.org/): patch releases contain compatible fixes, minor releases add compatible functionality, and major releases may include breaking changes.
