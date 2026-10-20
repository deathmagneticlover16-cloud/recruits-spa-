Recruits: Serious Animations 0.8.4
Minecraft 1.20.1 Forge

EMERGENCY COMPATIBILITY BUILD
- Based on the clean 0.8.3 baseline.
- Keeps passive specialty idles and ordinary death/fall overrides removed.
- Keeps existing executions/duels, SFX, stable locomotion, bow/crossbow and cavalry behavior.

SPEAR THROW RETRY
- Reproduces Serious Player Animations' actual trident presentation instead of playing trident.json as if it were a complete throw.
- Wind-up uses seriousplayeranimations:trident.
- Adds SPA's ~55-degree charged-trident body turn as a RENDER-ONLY torso/shoulder transform. Entity yaw/navigation/position are never changed.
- Release uses seriousplayeranimations:sword_attack, matching SPA's own TridentItem release behavior.
- 5-tick blend into the charged pose, 12-tick total wind-up, slowed 0.95x release.
- Projectile release tick: 19.
- Total throw duration: 31 ticks.
- API remains playSpearThrow(), spearThrowReleaseTick(), spearThrowDurationTicks(), isSpearThrowActive().

BETSY ROSS EMERGENCY FIX
- Detects Betsy Ross StandardItem without a hard mod dependency.
- While a flag/banner standard is held, locomotion, action, spear and routing layers pass both arm transforms through untouched.
- Generic sword/melee fallback is suppressed for standard bearers.
- Betsy Ross' own HumanoidModel holding pose therefore remains authoritative.

Install 0.8.4 on both server and clients. Network protocol 8.

SHIELD-WALL ANIMATION PREVIEW
- Look directly at a Recruit and run /recruitspa test.
- shield_rank_one plays locally and holds its last frame for inspection.
- Look at the same Recruit and run /recruitspa stop to remove the preview.
- This test layer is client-only and never changes Recruit AI, movement, position or yaw.
