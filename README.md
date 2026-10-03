# SANGYAN Shield

**Check before you click, pay, or trust.**

**Help and support:** “Got scammed? Get help” on Home and analysis results opens [Grievance Clock](https://grievance-clock.vercel.app/) in your browser. The app does not attach message text or other analysis data to the link.

Built for the **SANGYAN Investor Resilience Hackathon**.

Native Kotlin / Jetpack Compose / Material 3 Android investor-protection prototype. Detects warning signs in financial messages without offering trading advice. Minimum API 26; target and compile API 35. Android 10+ supported.

## Quick start

### Requirements

- Android Studio with Java 17 configured as the Gradle JDK.
- Android SDK Platform 35 and SDK Build Tools installed through SDK Manager.
- Android phone running API 26 or newer (Android 10+ supported).

Open the repository folder in Android Studio and let Gradle sync. Android Studio creates a local `local.properties` file for your SDK path; this file is excluded from Git. The Gradle 8.9 wrapper is included.

From the repository root:

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

On Windows, use `gradlew.bat` instead of `./gradlew`. For terminal builds, set `JAVA_HOME` to Java 17 and configure `ANDROID_HOME` or `sdk.dir` in `local.properties` for your Android SDK.

Build dependencies require Internet on the laptop on first build. **SMS detection, manual analysis, and bundled OCR work offline.** The external help website requires Internet access in your browser. No server or API key is required.

### Install on a phone

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

Enable Developer options and USB debugging, connect a USB cable, and approve the computer's debugging prompt. With Android SDK platform-tools on your PATH:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy `app-debug.apk` to the phone, open it in the phone's Files app, allow installation from that source when Android prompts, and install. Open **SANGYAN Shield** after installation. Manual analysis, URL checking, OCR and demos do not need SMS permission.

### Hackathon demo

Open **Demo Mode** from Home and select a sample. Examples cover fake NSDL KYC, guaranteed returns, withdrawal fees, remote access, protective OTP notices and Hinglish threats. Scam examples should score high/severe; the protective OTP example should score low. Review the detected warning signs and recommended actions, then try pasting your own message or scanning a screenshot.

The **Got scammed? Get help** button opens the team's [Grievance Clock support website](https://grievance-clock.vercel.app/). Returning from the browser keeps the current analysis available.

### Live SMS testing

1. Open Home → Live SMS Protection. Read the disclosure and tap Continue.
2. Grant SMS permission. Grant notification permission on Android 13+.
3. Check that Live Protection shows SMS granted and notifications enabled. Default alert threshold is 70; change it in Settings.
4. Send the NSDL demo text to the test phone by actual SMS from another phone. Long multipart messages are joined before analysis.
5. A notification should appear. Tap it to see the score, component scores, categories and explanations. Original evidence is omitted from notification payloads for privacy.
6. Disable Live SMS Protection and repeat: no alert should appear.

`RECEIVE_SMS` is an Android **hard-restricted permission**. The installer must allowlist it before the user can grant it. Modern `adb install` normally allowlists restricted permissions; do **not** pass `--restrict-permissions`. A file-manager installer, managed device, OEM policy or Play policy may prevent granting SMS access. If manual installation blocks SMS permission, reinstall with adb using the command above, then grant permission **inside the app**. This project does not request the default SMS role and does not read the existing inbox. It does not depend on Play Store approval for manual/demo/OCR features.

Force-stop prevents SMS delivery until the app is reopened. OEM battery policies can affect background delivery. RCS and third-party messaging apps do not send the SMS broadcast: paste or scan those messages. Notification permissions or disabled alert channels can prevent visible warnings even when SMS analysis works. This is a prototype, not guaranteed interception or fraud prevention.

Official platform references:
- [Android RECEIVE_SMS permission](https://developer.android.com/reference/android/Manifest.permission#RECEIVE_SMS)
- [SMS broadcast API](https://developer.android.com/reference/android/provider/Telephony.Sms.Intents)
- [Notification runtime permission](https://developer.android.com/develop/ui/views/notifications/notification-permission)
- [Bundled ML Kit OCR](https://developers.google.com/ml-kit/vision/text-recognition/v2/android)

## Features

- Home, Live Protection, Analyze Message, Screenshot Scan, URL Checker, History, Settings, About / Privacy, Demo Mode, and a help website button.
- Explicit opt-in for receiving new SMS. Broadcast receiver uses `goAsync`, bounded asynchronous work and multipart joining.
- Offline rule/NLP detection with contextual negation, English + Hindi/Hinglish local JSON rules.
- URL inspection without opening links, resolving redirects, or making reputation claims.
- Organization/domain mismatches and basic one-character lookalikes, with uncertainty language.
- Bundled on-device Latin and Devanagari OCR using the modern image picker; extracted text is editable before analysis. Camera capture is not included (optional in the specification).
- Room summaries with optional retention (off initially), latest 100 entries, disable and delete controls.
- Dark/light themes, Material 3 UI, score gauge, explanations and component scores.
- Extension interface `ScamMlClassifier`, with an inactive no-op implementation. No ML probability claims.

## Scoring

`RED FLAG SCORE` is a bounded **heuristic score**, not a probability. Bands:

| Score | Label |
| --- | --- |
| 0–24 | LOW RED-FLAG SCORE |
| 25–49 | SOME WARNING SIGNS |
| 50–74 | HIGH RISK |
| 75–100 | SEVERE WARNING |

The base score is a normalized weighted sum: language 30%, URL 25%, impersonation 20%, financial pattern 15%, credential request 10%. Component values are 0–100. Strong patterns set explicit floors configured in phrase JSON (e.g. credential theft 80, advance fee 75, guaranteed returns 78). A domain mismatch plus a risky phrase sets a floor of 85; a mismatch alone sets 60. Final score is `max(weighted score, rule floors, impersonation floor)`, capped at 100. The result tells the user when a floor raises the base score. These floors avoid missing a severe credential request just because it contains no URL.

Patterns are deduplicated rather than counted repeatedly. Protective context is scoped to clauses so a benign sentence does not excuse a later malicious request. Detection remains heuristic: quoted scams, unusual phrasing, deceptive negation, URL parsers, and incomplete domain lists can produce errors. HTTPS is not evidence of legitimacy. A short URL alone receives a small component increase, not a malicious verdict. Registration IDs are extracted but **not verified**.

Edit local files in `app/src/main/assets/config/`:

- `scam_phrases.json`: English patterns, severity and floors.
- `hinglish_phrases.json`: Hinglish and Devanagari patterns.
- `organizations.json`: starter official-domain reference map (not exhaustive; review before production).
- `url_rules.json`: shorteners, domain endings, component weights.

## Privacy and threat boundaries

- The app has no Internet or microphone permission. SMS/OCR analysis stays local. Help opens a fixed website URL in your browser without message data. No raw message logging.
- No READ_SMS permission. Sender is not persisted in SMS history.
- History starts off. No message body, matched evidence, extracted numbers, URL path/query or credentials is persisted. Saved previews consist only of rule category names. Scores, source type, timestamp, categories, explanations and known organization names can be stored.
- Notification intents use the same sanitized result structure, so tapping an alert works even after process recreation without preserving its SMS body. Notification content is private on the lock screen.
- Text and OCR output exist in memory while you analyze them. They are not placed into saved-instance-state or a persistent draft. Displayed sensitive values are conservatively masked; original pasted text stays visible in the editor for user review.
- History disable does not erase earlier summaries. Use Delete All History to remove them. Backups are disabled. Ordinary SQLite deletion is not a claim of forensic secure erasure on flash storage.
- No raw screenshot is copied into app storage. Access is through the selected image URI.
- Rule config files are shipped inside the APK; updates require a new build.

## Architecture

`domain/detector` is plain Kotlin and tested on the JVM. `data/local` is Room; `data/repository` coordinates analysis and opt-in history with a mutex. `ShieldViewModel` uses coroutines and StateFlow. `receiver` connects new SMS; `notifications` creates a high-importance alert channel and sanitized deep-link payload. `privacy` owns persistence sanitization. UI uses Compose, adaptive scroll layouts and system light/dark themes.

## Validation

The latest local build passed **29 JVM tests**, generated a debug APK, and completed Android lint with **0 errors**. See [BUILD_VERIFICATION.md](BUILD_VERIFICATION.md) for the exact build scope and remaining device checks.

```bash
./gradlew testDebugUnitTest lintDebug
# Optional: requires an authorized device or emulator; run on a test device.
./gradlew connectedDebugAndroidTest
```

Unit tests cover urgency, credential theft, protective notices, investment scams, impersonation, URLs, Hindi/Hinglish, normal transactions and persistence redaction. Scores are explainable rules, not calibrated fraud probabilities. A low score does not guarantee legitimacy. The app does not provide stock tips, buy/sell/hold advice, price predictions or trading recommendations.

## Phone acceptance checklist

- Run all demo messages; protective OTP should be low and scam samples high/severe.
- Paste a legitimate transaction and an OTP theft request; inspect reasons and components.
- Analyze an official URL and an imitation domain; links must never open themselves.
- Choose a Latin and a Hindi screenshot in airplane mode; check/edit OCR output, then Analyze.
- Enable history, analyze an OTP-containing message, and reopen history; no original evidence or OTP should remain.
- Disable history; new analyses should not appear. Delete history and restart the app.
- Exercise SMS and notification permissions granted/denied, high-risk SMS, notification tap, and disabled protection.
- Check large system fonts, dark/light themes, rotation, and background/foreground transitions.

Compilation and unit tests cannot establish that SMS delivery or OCR works on your particular physical phone. Record those checks after installation.
