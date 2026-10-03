# Build verification — 4 October 2026

Recovery voice assistant and backend removed. Home and analysis results now open https://grievance-clock.vercel.app/ through the browser. The link contains no message or analysis data.

- Clean build, debug APK, unit tests and lint: **BUILD SUCCESSFUL**.
- JVM tests: **29 passed, 0 failed, 0 skipped**.
- Android lint: **0 errors, 13 warnings**.
- Packaged permissions: RECEIVE_SMS, POST_NOTIFICATIONS and AndroidX's internal receiver permission. No INTERNET or RECORD_AUDIO permission.
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- APK size: 61,008,343 bytes.
- SHA-256: `58774a72c41271ce779b817c0334bf439e99ef6b92661b234e3cb83467de0f8b`.

Reports: `app/build/reports/tests/testDebugUnitTest/index.html` and `app/build/reports/lint-results-debug.html`.

No phone was detected by ADB during this update. This revised build has not been installed or tested on a physical device. Verify that both help buttons open the browser and that returning to the app preserves the analysis result.
