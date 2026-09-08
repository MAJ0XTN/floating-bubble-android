# Floating Bubble (حباب شناور)

A floating bubble for Android that lets you perform text/keyboard actions on any
other app from an overlay — no need to switch away. Long-press the bubble to
open a radial menu of actions.

[فارسی — راهنمای کامل در ادامه](#فارسی)

## Features
- Floating, draggable bubble shown over any app (`TYPE_APPLICATION_OVERLAY`).
- Radial menu (long-press ~400ms or tap) with 8 actions around the bubble:
  - Select All, Copy, Paste, Enter
  - Alt, Tab, Shift
  - Screenshot
- Works on **Android 5 (API 21) through Android 16 (API 36)**.
  - Overlay permission + foreground-service type handled for modern versions.
- Actions that do not need root (Select All / Copy / Paste / Screenshot) use the
  **AccessibilityService**.
- `Ctrl / Alt / Tab / Shift` key injection requires **root** (see `RootHelper.kt`,
  sends `input keyevent` via `su`). On a non-rooted device those keys show a
  "requires root" toast.

## Build it yourself
1. Open this folder in **Android Studio** (File → Open).
2. Run ▶ on a device/emulator.
3. In the app: grant Overlay permission, enable the Accessibility service, then
   press **Start floating bubble**.
4. Long-press the bubble to open the radial menu.

## Build via CI (GitHub Actions)
Pushing to `main` triggers `.github/workflows/build.yml`, which builds a release
APK on GitHub's x86 runners and attaches it to a **Draft Release** you can
download. No local Android SDK required.

## Root section
The "Test root" button reports root availability. With root, key injection is
exact; without root, only Accessibility-backed actions work.

## License
[MIT](LICENSE)
