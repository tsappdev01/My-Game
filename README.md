# Maths Quest

A gamified maths practice app for Grades 3–9. Children solve questions, earn Gold Coins, and swap them for rewards a parent approves. Leo the lion cheers them on.

**Stack:** Android · Kotlin · Jetpack Compose · MVVM · Hilt · Room. The app is fully offline: no server, no account, no internet permission.

## Get the APK

Every push to `main` builds a debug APK in GitHub Actions:

1. Open the **Actions** tab → latest **Android build** run.
2. Download the **maths-quest-debug-apk** artifact and unzip it.
3. Copy `app-debug.apk` to an Android phone (Android 8.0+), allow "Install unknown apps", and install.

Every APK is also attached to a `build-N` pre-release. Debug builds are signed with the shared `app/debug.keystore`, so a new build installs over the old one and keeps the app's data. That key is public: release builds must be signed with a private key kept out of the repository.

## Demo family

On the first-run setup screen, tap **Load demo family** to try the app with sample data: **Musfira** (Grade 5, 140 coins, 6-day streak, 82% right first time) and **Musab** (Grade 3, a screen-time request waiting for approval). The parent PIN is **1234**. If the app is already set up, use **Add demo family** at the bottom of the parent dashboard.

## Build locally

Requires JDK 17 and the Android SDK (API 35).

```bash
./gradlew :core:test          # question engine + coin rules unit tests
./gradlew :app:assembleDebug  # app/build/outputs/apk/debug/app-debug.apk
```

## Project layout

| Path | What it holds |
| --- | --- |
| `core/` | Pure Kotlin, unit-tested: question engine, worked explanations and hints, coin rules, streaks, badges, Daily Challenge |
| `app/src/main/java/com/mathsquest/app/data` | Room database (append-only coin ledger), repository, demo family |
| `app/src/main/java/com/mathsquest/app/sound` | Sound effects player (sounds can be turned off on the home screen) |
| `app/src/main/java/com/mathsquest/app/ui` | Compose screens and view models, one package per feature |
| `tools/leo/` | Leo's 15 expressions as SVG fragments, plus the script that turns them into Android vector drawables |
| `tools/sfx/` | Script that synthesises the sound effects (`python3 tools/sfx/make_sounds.py`) |

## Rules the app enforces

- **Payouts:** Easy 1, Moderate 2, Tough 3 coins. 1 coin = AED 0.10 of reward value; children only see coins.
- **No farming:** questions 2+ grades below the child's grade are practice (XP, no coins).
- **No guessing:** answers faster than 1.5 / 2.5 / 4 s (Easy / Moderate / Tough) earn no coins. A second try pays one coin less.
- **Monthly limit:** set by the parent (default 300 coins = AED 30), enforced on every payout.
- **Rewards:** requesting puts coins on hold; a parent approves (coins redeemed) or declines (hold released).
- **Daily Challenge:** 10 mixed questions at the child's grade (3 Easy, 4 Moderate, 3 Tough), up to 20 coins, once a day.
- **Streak:** the first finished round each day extends it; reaching 7 days pays a 5-coin bonus (inside the limit).

## Privacy

No internet permission, ads or analytics in V1. Data stays on the device and is excluded from cloud backup. A parent gives consent and sets a PIN on first launch.

## Regenerating Leo

```bash
python3 tools/leo/make_vectors.py
```
