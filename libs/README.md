# Local mod dependencies

The repository currently includes these Minecraft 1.20.1 Forge dependencies:

- Villager Recruits 1.15.2
- Player Animator 1.0.2-rc1+1.20
- Mob Player Animator 1.3.3

Add Serious Player Animations 1.1.1 before running the development client. It is a required runtime dependency, although this addon's Java sources do not compile directly against it.

Optional integrations can also be placed here:

- Better Combat 1.8.6
- Better Mob Combat / Recruits Fix 1.3.1
- Better Formations 1.0.3
- Betsy Ross 2.1.2
- Recruits RTS Camera / Commander

The three included compile dependencies are declared explicitly in `build.gradle` so ForgeGradle can deobfuscate them. Add any further runtime-only mods to the generated `run/mods/` directory when testing locally. Dependencies are not bundled in the addon JAR.
