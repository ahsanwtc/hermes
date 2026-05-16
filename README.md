# Hermes

Android app that automatically backs up local folders to [Filen](https://filen.io) end-to-end encrypted cloud storage.

## Features

- **Automatic sync** — watches folders for new files and uploads them in the background
- **End-to-end encrypted** — files are encrypted on-device before upload using Filen's E2E encryption
- **All file types** — PDFs, images, videos, documents — anything in the watched folder
- **Flexible rules** — configure multiple sync rules, each with its own local folder and cloud destination
- **Delete after upload** — optionally remove local files after successful upload, with extension-based retain filters (e.g. keep `.raw` files)
- **Wi-Fi only mode** — optionally restrict uploads to unmetered connections
- **Foreground service** — continues watching folders even when the app is in the background

## Setup

1. Install the APK from [Releases](../../releases)
2. Open the app and go to **Settings**
3. Sign in with your Filen account (email + password + 2FA if enabled)
4. Go to **Rules** and add a sync rule:
   - Pick a local folder using the folder picker
   - Enter a cloud destination path (e.g. `Phone/Camera`)
5. Tap **Start Sync** on the Home screen

## Tech Stack

- Kotlin + Jetpack Compose
- Room (local database)
- WorkManager (background uploads)
- Hilt (dependency injection)
- Ktor (HTTP client)
- DataStore + EncryptedSharedPreferences (settings)
- Foreground Service + FileObserver (folder watching)

## Building

```bash
./gradlew assembleDebug
```

Requires Android Studio Meerkat or later, JDK 21.

## CI/CD

Tagged releases (`v*`) automatically build and publish a signed APK via GitHub Actions.

### Releasing a new version

1. Update `versionCode` and `versionName` in `app/build.gradle.kts`
2. Commit the change: `git commit -am "chore: bump version to x.y.z"`
3. Tag the release: `git tag vx.y.z`
4. Push: `git push && git push origin vx.y.z`

GitHub Actions will build, sign, and publish the APK to GitHub Releases automatically.

## License

MIT
