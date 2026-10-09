# Ollama Usage Tracker

Android app + Wear OS complication by Erich (built by Odin).
Tracks Ollama Cloud session and weekly usage by scraping the ollama.com dashboard,
with configurable threshold notifications and resizable home-screen widgets.

⚠️ Uses dashboard scraping, not an official API. Ollama didn't build a usage API yet
(github.com/ollama/ollama/issues/15663). When they ship one, this app will switch.

## Setup
1. Install APK from Releases.
2. Log into ollama.com in any browser, open devtools → Application → Cookies, copy the session cookie value.
3. Paste the cookie in the app. Set auto-refresh interval (1-60 min) and thresholds.
4. Add the widget: 2x1 grid cells minimum, resizable to full screen. Tap the gear icon to toggle session/weekly/both.
5. On your Galaxy Watch 7, add the Ollama complication to your watch face, pick session or weekly as a bar.

## Build
./gradlew :app:assembleRelease
APK lands in app/build/outputs/apk/release/