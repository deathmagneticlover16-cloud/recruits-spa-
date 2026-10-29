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

Better Combat 1.8.6, Betsy Ross 2.1.2, Icy's Better Horses 2.1.4,
Entity Model Features 3.3.12, and Entity Texture Features 7.2.6 are supported
optional integrations.

Animation layers sleep while their one-shot actions are inactive, EMF LOD
decisions and compatibility lookups are cached, and server animation packets
are limited to players tracking the affected Recruit. Mounted collision scans
are staggered across ticks to keep large cavalry formations from producing a
single server-tick spike.

## Mounted compatibility

- The bundled Better Combat weapon attributes make the supported Epic Knights,
  Antique Legacy, Dark Ages Armory, and Slavic Armory spears use
  `bettercombat:one_handed_stab_mounted` while mounted. Their existing foot
  attacks and one-handed shield use are retained. Epic Knights pikes keep their
  native two-handed definitions. The special mounted weapon offset applies only
  to item IDs containing `lance`.
- Mounted Recruits keep full movement speed while blocking with a shield.
- RTS mods can trigger the 2.25-second `TriumphVar1` mounted weapon wave through
  `RecruitsAnimationsApi.playMountedTriumph(recruit)`. The API validates that
  the Recruit is riding a horse and has a main-hand item before broadcasting it.
- Icy's Better Horses gait, stop, jump, and turning motion drives the rider.
  The addon suppresses its own duplicate mounted bob and corrects the Recruit
  seat height to Icy's humanoid rider anchor, including the additional model
  origin correction required by Mob Player Animator's Recruit model. Recruit
  horses use walk while walking and transition through a short trot and a
  twice-as-long canter before reaching gallop while running.
- A Recruit riding an Icy-managed horse gets the same gallop collision strike
  as a player rider after maintaining forward movement for three seconds. Owners,
  same-owner Recruits, same-team units, and diplomatically allied units are
  excluded from collision damage so cavalry formations do not trample their
  own mounts. Unowned Recruits remain valid charge targets. Successful Recruit
  cavalry impacts broadcast Icy's charge thud at 25% volume from the collision
  for nearby players.
- On server start, all Icy horse breeds and the horse cart are appended to
  Recruits' `MountWhitelist` if Icy's Better Horses is installed.

## EMF/ETF performance compatibility

When Entity Model Features is installed, Recruit Player Animator layers follow
EMF's **Animation LOD Distance** setting. Distant Recruits evaluate their SPA
poses less often using the same distance cadence while animation time continues
normally, including EMF's
**Retain Detail on Low FPS** preference. A value of `0` keeps full-quality
updates at every distance. ETF remains fully optional and continues to manage
texture features independently.

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

Look directly at a Recruit and use the rank-specific preview commands:

```text
/recruitspa test r1
/recruitspa stop r1
/recruitspa test r2
/recruitspa stop r2
/recruitspa test r3
/recruitspa stop r3
```

`test r1`, `test r2`, and `test r3` enter and hold their respective shield-wall rank poses. The matching `stop` command plays that rank's leave animation and then restores normal animation control. Shield-wall previews preserve the right-arm position authored in the animation while leaving its rotation under locomotion control.

These tools are client-side and do not change Recruit movement, AI, position, or yaw.

## License

This project is available under the [MIT License](LICENSE).

## Versioning

Releases follow [Semantic Versioning](https://semver.org/): patch releases contain compatible fixes, minor releases add compatible functionality, and major releases may include breaking changes.
