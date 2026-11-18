# Midox Poster

Android app that helps the owner post Midox products on Facebook Marketplace every day, built for an
ADHD brain: no decisions, one button, a streak, and nags that get louder until today's post is up.

- Products and photos come from **Midox** (`ma.midoxshop.prices`) through its signature-protected
  catalog provider, so Poster is signed with the same key (`app/midox-release.jks`).
- Posting is **by hand**: photos in a gallery album, copy buttons for title, price (always 1 dh) and
  text. Poster never automates or touches Facebook (risk to the account).
- Everything stays on the phone (SharedPreferences).

Build: `gradle --no-daemon assembleRelease` (Gradle 8.14.3, Java 17). The version is the top
`## 1.0.N` section of `CHANGELOG.md`. Every push to `main` publishes a release APK.
