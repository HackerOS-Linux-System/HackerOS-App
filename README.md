# HackerOS App

The official companion Android app for [HackerOS](https://hackeros-linux-system.github.io/HackerOS-Website/) -
browse releases, wallpapers, the gallery, documentation and the team, right from your phone.

## Features

- **Releases** - live release notes pulled directly from the official HackerOS website's release
  data, in your selected language, with background notifications for new releases.
- **Wallpapers** - browse official HackerOS wallpapers, install them to your device with a live
  download progress bar, then set them as your Home screen, Lock screen, or both.
- **Gallery** - community screenshots and visual archive.
- **Documentation** - the full official HackerOS documentation, shown natively in-app.
- **Team** - meet the people behind HackerOS.
- **Settings** - 10 languages, 8 selectable themes (Monochrome by default), release
  notifications, and in-app update checks.

## What's new in v0.5

- Fixed release fetching: the app now reads the website's real per-language release data files
  instead of an endpoint that only ever contained an empty placeholder, so releases actually show
  up.
- Gallery images are now sourced directly from the [HackerOS Website repo's `gallery`
  folder](https://github.com/HackerOS-Linux-System/HackerOS-Website/tree/main/gallery), instead
  of a separate copy in the App repo, so the app always mirrors the website's gallery.
- Added a native, in-app **Documentation** tab.
- Added a new default **Monochrome** theme (gray/white/black); the previous green theme is still
  available as an optional theme in Settings.
- Wallpapers can now be installed to the device with a live progress bar, then set directly as
  the Home screen, Lock screen, or both.
- CI: added `.github/workflows/build.yml`, which builds signed-ready debug and release APKs via
  Gradle and publishes them as a downloadable `outputs` artifact.

## Building

```bash
cd source-code
./gradlew assembleDebug assembleRelease
```

APKs are produced under `source-code/app/build/outputs/apk/{debug,release}/`.

You can also trigger `.github/workflows/build.yml` on GitHub (Actions tab -> Build APKs -> Run
workflow) to build both APKs in CI; they're packaged into a zip and published as the `outputs`
artifact on the workflow run.

## License

See [LICENSE](LICENSE).
