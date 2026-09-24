import SwiftUI

/// Language + visible sections, persisted in UserDefaults.
final class AppState: ObservableObject {
    @Published var language: Language { didSet { UserDefaults.standard.set(language.rawValue, forKey: "lang") } }
    @Published var enabled: Set<String> { didSet { UserDefaults.standard.set(Array(enabled), forKey: "sections") } }

    init() {
        let saved = UserDefaults.standard.string(forKey: "lang")
        let device = Locale.current.language.languageCode?.identifier
        language = Language(rawValue: saved ?? device ?? "pl") ?? .pl
        let s = UserDefaults.standard.stringArray(forKey: "sections")
        enabled = Set(s ?? AppSection.allCases.map { $0.rawValue })
    }

    func t(_ key: String) -> String { L.t(key, language) }
    func isOn(_ s: AppSection) -> Bool { enabled.contains(s.rawValue) }
    func set(_ s: AppSection, on: Bool) {
        if on { enabled.insert(s.rawValue) } else if enabled.count > 1 { enabled.remove(s.rawValue) }
    }
}
