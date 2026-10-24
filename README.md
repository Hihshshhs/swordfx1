# SwordFX (Fabric, Minecraft 26.3)

A client-side "visual client" style mod. It is a mod, not a resource pack, because a
pack can't draw a HUD or react to jumps. Nobody else on a server sees any of it, and
it does nothing that gives a gameplay advantage.

## Features
- **Sword swing animations** (pivot from the hand): slash, chop, stab, spin, flip, sweep (or vanilla)
- **Sword tilt** and size
- **Jump rings**: expanding particle circle at your feet on jump and landing, optional air swirl
- **Info bar** at the top: username | ping | FPS (colour coded)
- **Target health**: enemy / animal / player name, smooth health bar, absorption, distance, armor
- **CPS** (left | right), **coordinates**, **facing direction**
- **Armor durability** panel (+ held tool/weapon)

## Build
Needs **Java 25**.

    ./gradlew build        (Windows: gradlew.bat build)

The jar ends up in `build/libs/swordfx-1.0.0.jar`. Drop it in `.minecraft/mods` next to
Fabric API for 26.3. If Gradle complains about its own version, run
`./gradlew wrapper --gradle-version latest` once and try again.

## Controls
- **G** cycles the sword animation (rebindable under Controls > SwordFX)
- `/swordfx style <slash|chop|stab|spin|flip|sweep|vanilla>`
- `/swordfx ringstyle <starlight|soul|flame|spark|cloud|glow>`
- `/swordfx tilt <-60..60>`  and  `/swordfx scale <50..150>`
- `/swordfx toggle <infobar|target|stats|armor|swordanim|replaceswing|jumpring|landring|airswirl>`
- `/swordfx status`

Settings are saved to `config/swordfx.json`.

## If something doesn't line up
- Animation direction wrong (backwards/mirrored)? Flip the sign of that angle in `SwordAnimator.java`.
- No sword animation at all? Check `latest.log` for `HandRendererMixin`. The hand renderer
  method name/signature is the one piece most likely to differ between 26.x builds.
  The mixin is non-fatal, so the HUD and jump rings still work without it.
