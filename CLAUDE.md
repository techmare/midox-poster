# Midox Poster

Android app (Kotlin, Jetpack Compose, minSdk 26) that helps the owner of a camera shop in Morocco post
the shop's products on Facebook Marketplace every day, built for an ADHD brain: no decisions, one
button, a streak, nags that get louder until today's post is up. Used on the owner's Pixel only.

## Rules

- Products and photos come from **Midox** (`ma.midoxshop.prices`, repo techmare/midox-app) through its
  signature-protected `CatalogProvider`, so Poster is signed with the same key (`app/midox-release.jks`).
- **Nothing touches Facebook**: Poster only opens the new-listing page and copies text. No overlay,
  no paste bubble, no automation (risk to the owner's account; the owner said no).
- Posts: price always 1 DH, no price in the text, no shop name; text in French. App text in English.
- At most 10 posts a day, 10 minutes apart (`Posts.MAX_A_DAY`, `Posts.GAP_MINUTES`).
- Look: the first design (warm off-white `#F4F2EC`, ink `#15171C`, orange `#EA580C`, teal, a dark card),
  Space Grotesk + Inter, dark mode. Not Midox's cream/coral palette (the owner's choice). Everything
  animates (the owner asked), through `ui/Theme.kt`'s `Motion`, `enter`, `pop`, `Rolling`.
- Logic without Android goes in plain objects (`Posts`, `Streak`, `NagPlan`) with tests in `app/src/test`.

## Build, check, release

- `gradle --no-daemon testDebugUnitTest assembleDebug` (Gradle 8.14.3, Java 17). In a cloud session see
  techmare/midox-app's CLAUDE.md for the SDK and Maven mirror workarounds; never commit them.
- **Screen check** (`.github/workflows/screen-check.yml`) runs by itself on every push to a branch other
  than main: debug APK on an emulator with demo products (`Midox.demo`, debug only), screenshots light
  and dark, recordings of the opening, `crashes.txt`, all force-pushed to `check-results`. Read them.
- **Every push to main publishes a release** (`v1.0.N`, N = top `## 1.0.N` of CHANGELOG.md = versionCode).
  Work on a branch; fast-forward main only after a clean Screen check.
