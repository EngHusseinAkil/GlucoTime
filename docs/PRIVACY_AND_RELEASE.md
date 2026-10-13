# Privacy and release review

Source inspected on 13 October 2026. This is engineering review, not confirmation of store approval.

Room persists local profiles, readings and notes. DataStore stores preferences/reminders. The main manifest does not declare Internet permission; external developer-contact intents are opened in other apps. `allowBackup=true` means an absolute "data never leaves your device" claim needs qualification. Current policy accurately distinguishes developer-operated storage, but its uninstall/retention language should also account for Android backup/restore.

Notifications, exact alarms, full-screen intents and a special-use foreground service are declared. Release eligibility requires checking actual behavior and current Play policy/declarations. Device-specific battery restrictions and user permission choices can prevent reminder delivery. The app is not an emergency service.

Do not publish signing keys/passwords, real readings, personal exports, or device logs. Screenshots in this repository use synthetic data according to their capture provenance. A pre-existing APK/AAB is evidence of a file, not proof of a current successful build or a Play listing.
