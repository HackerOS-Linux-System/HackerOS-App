import Foundation

/// Mirrors Constants.kt of the Android app: the iOS app reads the very same website data files.
enum Constants {
    static let appVersion = "0.8"
    static let site = "https://hackeros-linux-system.github.io/HackerOS-Website/"
    static let raw = "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/"
    static let articlesBase = raw + "articles/"
    static let articlesIndex = articlesBase + "index.js"
    static let articlesWeb = site + "articles.html"
    static let docsWeb = site + "hackeros-documentation.html"
    static let galleryAPI = "https://api.github.com/repos/HackerOS-Linux-System/HackerOS-Website/contents/gallery"
    static let wallpapersAPI = "https://api.github.com/repos/HackerOS-Linux-System/HackerOS-Website/contents/phone-wallpapers"
    static let sourceCode = "https://github.com/HackerOS-Linux-System/HackerOS-App"
    static func releasesURL(_ lang: String) -> String { raw + "translations/files/all/\(lang).js" }
}

enum Language: String, CaseIterable, Identifiable {
    case pl, en, de, es, fr, it, ru, uk, zh, ja
    var id: String { rawValue }
    var name: String {
        switch self {
        case .pl: return "🇵🇱 Polski"; case .en: return "🇺🇸 English"; case .de: return "🇩🇪 Deutsch"
        case .es: return "🇪🇸 Español"; case .fr: return "🇫🇷 Français"; case .it: return "🇮🇹 Italiano"
        case .ru: return "🇷🇺 Русский"; case .uk: return "🇺🇦 Українська"; case .zh: return "🇨🇳 中文"; case .ja: return "🇯🇵 日本語"
        }
    }
}

enum AppSection: String, CaseIterable, Identifiable {
    case articles, releases, wallpapers, gallery, team
    var id: String { rawValue }
    var icon: String {
        switch self {
        case .articles: return "doc.text"; case .releases: return "list.bullet"
        case .wallpapers: return "photo"; case .gallery: return "camera"; case .team: return "person.2"
        }
    }
}
