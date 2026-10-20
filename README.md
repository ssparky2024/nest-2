# Nest

A minimalist, highly customizable Android launcher: a clock that happens to be a launcher.
Built with Kotlin + Jetpack Compose, targeting an Amazon Fire HD 11 (Fire OS 8, Android 11 based).

## Get an APK (no Android Studio needed)

1. Create a private GitHub repo and push this folder to the `main` branch.
2. Open the repo's **Actions** tab. The "Build APK" workflow runs on every push
   (or press **Run workflow**).
3. When it finishes, open the run and download the **nest-debug-apk** artifact (a zip containing `app-debug.apk`).
4. Move the APK to the tablet, enable "Install unknown apps" for your file manager or browser, and install it.

## Using Nest

- **Swipe down** on the home screen: app drawer (search bar at the top).
- **Long-press an app** (drawer, home or dock), then drag it:
  - onto the home screen to place it anywhere,
  - onto the dock area at the bottom to add it (drop between icons to reorder),
  - onto the top strip: **Remove** (home/dock only), **App info**, **Uninstall**.
- **Long-press empty home screen**: settings (clock, date, background, icons, dock, drawer, gestures).
- Back and Home never leave Nest; they only close the drawer or settings.

## Making it the Home app on Fire OS

1. Install and open Nest once, then press the Home button and see whether Fire OS offers a chooser
   ("Just once / Always"). If it does, pick Nest and "Always".
2. If Fire OS ignores it, use Launcher Hijack to redirect the Home button to Nest.
3. Another option to try, if you have ADB set up:
   `adb shell cmd package set-home-activity com.nest.launcher/.MainActivity`

## Project layout

- `NestStore.kt`: all settings plus dock/home layout (Jetpack DataStore)
- `Apps.kt`: app list, launching, app info, uninstall
- `Drag.kt`: shared long-press drag-and-drop
- `HomeScreen.kt`: clock, free-positioned shortcuts, dock, swipe and tap gestures
- `AppDrawer.kt`: full-screen grid with search and sorting
- `SettingsPanel.kt`: the settings UI
- `NestApp.kt` / `MainActivity.kt`: wiring, drop logic, and the launcher entry point
