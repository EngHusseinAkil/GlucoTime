# Verification record — 13 October 2026

The initial cache-only `gradlew.bat --offline testDebugUnitTest assembleDebug` failed to resolve Android Gradle Plugin 9.3.1. An online retry is recorded in the final audit after completion.

Existing unit tests cover glucose-input validation and reminder schedule calculations. No instrumentation/device test was performed during this audit. Existing emulator captures use synthetic demo data according to their original capture notes; they are historical artifacts.

Pre-existing debug APK and release AAB files were found locally. Their existence alone is not proof of this source's current build, a release installation, or Google Play availability. Signing secrets and those binaries are excluded from the public source snapshot.

## Confirmed build

The online retry on the original tree passed `testDebugUnitTest assembleDebug` (43 tasks; 5m 43s). XML reports confirm **4 tests passed** across glucose-input validation and reminder scheduling, with no failures/errors/skips.

The selected publication snapshot was built separately with cached dependencies using `gradlew.bat --offline testDebugUnitTest assembleDebug`: **BUILD SUCCESSFUL**, all 43 actionable tasks executed, 1m 2s. Its XML reports likewise confirm **4 passing tests**. A local SDK path was supplied via ignored `local.properties`; no SDK path or signing key is published. This validates a debug build and unit tests, not clinical behavior, release signing, device notification delivery, or Google Play approval.

The final public snapshot was rechecked after removing optional private phone/payment contact values: debug build succeeded in 11 seconds, with four unit tests passing. The original local app was not changed.
