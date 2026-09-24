# HackerOS App - iOS (SwiftUI)

Jetpack Compose (used by the Android app in `../source-code`) is not supported on iOS, so this
folder contains an alternative UI written in Swift + SwiftUI. It reads the same website data as
the Android app.

| Section | Source |
|---|---|
| Articles | `articles/index.js` + `articles/<id>.js` on the website (`Sources/Models/Article.swift`) |
| Releases | `translations/files/all/<lang>.js` |
| Wallpapers / Gallery | GitHub contents API of `phone-wallpapers/` and `gallery/` |
| Team, Settings | local |

Notes
- iOS 16+, no third-party dependencies. The Xcode project is generated from `project.yml` (XcodeGen).
- UI language: Polish and English are translated, other languages fall back to English. Article
  content follows the website: `pl` + `en`, everything else shows English.
- Not ported yet: Games Store (Android APKs), native Documentation renderer, release notifications,
  themes. Setting the system wallpaper is not possible on iOS, so wallpapers are saved to Photos.
- This code was written without access to Xcode; the first `ios` workflow run is its first compile.
