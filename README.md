# Underwater Music (Fabric, client-side)

Makes Minecraft's background music (never music discs) sound submerged when your eyes are underwater.

## STATUS - read first
| Item | State |
|---|---|
| Source code written | Yes |
| Compiled against Minecraft 26.3 | **NOT yet** |
| Run in-game | **NOT yet** |
| Dependency versions in gradle.properties | **Unverified guesses** |

## Build (no installs needed)
1. Create a GitHub repo, upload ALL files from this folder (including `.github`).
2. Actions tab > Build > Run workflow.
3. Download artifact `UnderwaterMusic`, unzip, get `UnderwaterMusic-26.3.jar`.

## If the build fails
Paste the red error text to Claude. Most likely causes: wrong versions in `gradle.properties`
(check https://fabricmc.net/develop), or a renamed Minecraft class/method.

## Install
Put the JAR in your instance's `mods` folder with Fabric API (and optionally Mod Menu to get the settings button).
Open settings via Mod Menu > Underwater Music > Configure.

## Dependencies
Minecraft 26.3, Fabric Loader >= 0.18, Fabric API (matching 26.3), Java 25. Mod Menu optional.

## Settings
- Mod enabled / Low-pass / Volume reduction / Pitch reduction: toggle each part.
- Underwater volume (0-100%): music loudness when fully underwater.
- Low-pass (200-5000 Hz): lower = more muffled.
- Pitch reduction (0-20%): subtle slowing of pitch.
- Transition (0.1-5 s): fade duration.
- Effect intensity (0-100%): blend between dry and fully underwater.
- Detect eyes underwater (default on) / Detect swimming (default off); Fade in / Fade out.
Config file: `config/underwater_music.json`, saved automatically.

## How it works
A mixin on `SoundEngine.play` records only sounds in the MUSIC category (discs are RECORDS, so excluded).
Each tick, an OpenAL EFX low-pass filter (gain + high-frequency gain) and pitch are applied to those sources on the sound thread.
One shared filter object is used, so no per-sound leaks.
