# HackerOS App

The official companion Android app for [HackerOS](https://hackeros-linux-system.github.io/HackerOS-Website/) -
browse releases, wallpapers, the gallery, documentation and the team, right from your phone.

## Features

- **Releases** - live release notes pulled directly from the official HackerOS website's release
  data, in your selected language, with background notifications for new releases (filterable by
  edition), and offline fallback to the last successfully fetched copy.
- **Wallpapers** - browse official HackerOS wallpapers, install them to your device with a live
  download progress bar, then set them as your Home screen, Lock screen, or both. Installed
  wallpapers are cached on-device so they're available fully offline afterwards.
- **Gallery** - community screenshots and visual archive, with a pinch-to-zoom/swipe fullscreen
  viewer, download-all, per-image share, and offline fallback to the last fetched copy.
- **Documentation** - the full official HackerOS documentation, parsed and rendered **natively**
  (no WebView) straight from the website's own data file, with a native tab menu, native search,
  offline caching, and an English-only-content notice where relevant.
- **Games Store** - browse, install, update, and launch community-made games for HackerOS, right
  from the app (HackerOS-App acts as the manager/installer for the shared community game catalog).
- **Team** - meet the people behind HackerOS.
- **Settings** - 10 languages, 12 selectable themes (Monochrome by default) plus a custom theme
  builder, release notifications
  (with per-edition filtering), in-app update checks with a full download/verify/install flow, and
  toggles to hide the Documentation and/or Games Store sections entirely.
- **Self-updating** - checks for new app versions, downloads the APK with a live progress bar,
  verifies it against a published checksum when available, then hands off to the system installer.
- **What's new** - a short summary of the latest release is shown once right after updating the
  app.

## What's new in v0.6

- **4 new built-in themes**: Ultraviolet, Cool Mint, Blackout Rose, and Steel Blue - alongside the
  existing 8.
- **Create your own theme.** A new custom theme builder in Settings lets you pick Primary,
  Background, and Card colors from a curated swatch grid or an exact hex code, with a live
  preview, and save it as a selectable theme of your own.
- **Translation audit fix**: several strings across the app (the Team screen description, a
  handful of Settings labels, status badges, and a couple of toast messages) were hardcoded in
  English regardless of the selected app language. All of these now respect the language setting
  across all 10 supported languages.

## What's new in v0.5.2

- **Documentation is now fully native - no WebView.** The app parses the website's own
  `translations/hackeros-documentation.js` data file directly (including a small tolerant
  JS-object-literal-to-JSON converter, since that file isn't strict JSON) and renders it with
  native Compose UI: tab menu, native search, headings, lists, code blocks with copy buttons,
  and the tools/programming reference content - mirroring the website's own renderer
  (`doc-engine.js`) field-for-field so the content matches exactly.
- **New Games Store section.** HackerOS-App now acts as a manager for community-made phone games,
  reading a shared catalog (`games-store/community-games.json` in this repo) and letting you
  browse, install (with a live progress bar and checksum verification, just like app updates),
  open, or update each game - all without leaving the app.
- **Documentation and Games Store can each be hidden** from Settings if you don't want them
  cluttering the navigation bar.

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

## Reliability / offline

- Releases and the gallery are cached locally (DataStore) after every successful fetch; if a
  later fetch fails (no connection), the app shows that last-known-good data instead of an error
  screen, with a small "offline" banner so it's clear the data isn't live.
- Installed wallpapers are cached in app-private storage, so they remain viewable/settable fully
  offline after the first successful install.
- The in-app Documentation WebView is configured to fall back to its HTTP cache when there's no
  connection, so a previously-opened page can still be read offline.

## In-app updates

- Settings now offers a full update flow: check → download (with a live % progress bar) → verify
  (SHA-256 checksum, when the release publishes one) → install via the system Package Installer,
  all without leaving the app.
- Release notifications can be filtered per HackerOS edition (Official, Cybersecurity, NVIDIA,
  etc.) instead of firing for every edition.
- A "What's new" dialog summarizes the latest release once, right after an app update.

- CI: added `.github/workflows/build.yml`, which builds signed-ready debug and release APKs via
  Gradle and publishes them as a downloadable `outputs` artifact.
- CI/build fix: the repo was missing `gradle/wrapper/gradle-wrapper.jar` entirely (so `./gradlew`
  could never even start Gradle), `gradlew` itself contained a broken self-download hack pointing
  at a URL that doesn't host the compiled jar, and a committed `local.properties` hardcoded a
  developer's personal SDK path - which silently overrode the CI runner's own Android SDK and
  broke every build. All three are fixed: a genuine wrapper jar + the standard official
  `gradlew`/`gradlew.bat` scripts are now committed, `local.properties` is generated fresh by the
  workflow (and gitignored), and the workflow no longer uses a third-party Android SDK setup
  action (GitHub's runners already ship one, license-accepted) - which was the actual cause of
  the workflow hanging/"waiting" indefinitely. The job also now has a hard 30-minute timeout as a
  safety net.

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
