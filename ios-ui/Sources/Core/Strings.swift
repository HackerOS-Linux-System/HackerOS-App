import Foundation

/// UI strings. Polish and English are translated; every other language falls back to English
/// (article *content* has the same pl/en + English-fallback rule as the website).
enum L {
    private static let table: [String: [String: String]] = [
        "pl": [
            "articles": "Artykuły", "releases": "Wydania", "wallpapers": "Tapety", "gallery": "Galeria",
            "team": "Zespół", "settings": "Opcje", "search_articles": "Szukaj artykułów",
            "all": "Wszystkie", "min_read": "min czytania", "no_results": "Brak artykułów pasujących do wyszukiwania.",
            "fallback": "Ten artykuł nie jest jeszcze dostępny w Twoim języku - wyświetlamy wersję angielską.",
            "open_website": "Otwórz na stronie", "offline": "Brak połączenia - pokazuję ostatnio zapisane dane",
            "error": "Nie udało się pobrać danych.", "retry": "Spróbuj ponownie", "copied": "Skopiowano!",
            "language": "Język", "sections": "Sekcje", "sections_desc": "Wybierz, które sekcje mają być widoczne",
            "docs": "Dokumentacja (strona www)", "source": "Kod źródłowy", "version": "Wersja",
            "save": "Zapisz w Zdjęciach", "saved": "Zapisano w Zdjęciach", "contact": "Kontakt",
            "contact_desc": "Pytania, pomysły albo znalazłeś błąd? Napisz do nas.",
            "role_founder": "Założyciel HackerOS", "role_wallpapers": "Główny twórca tapet", "empty": "Brak elementów",
        ],
        "en": [
            "articles": "Articles", "releases": "Releases", "wallpapers": "Wallpapers", "gallery": "Gallery",
            "team": "Team", "settings": "Settings", "search_articles": "Search articles",
            "all": "All", "min_read": "min read", "no_results": "No articles match your search.",
            "fallback": "This article isn't available in your language yet - showing the English version.",
            "open_website": "Open on website", "offline": "No connection - showing last saved data",
            "error": "Couldn't load data.", "retry": "Retry", "copied": "Copied!",
            "language": "Language", "sections": "Sections", "sections_desc": "Choose which sections are shown",
            "docs": "Documentation (website)", "source": "Source code", "version": "Version",
            "save": "Save to Photos", "saved": "Saved to Photos", "contact": "Contact",
            "contact_desc": "Questions, ideas, or found a bug? Reach out.",
            "role_founder": "Founder of HackerOS", "role_wallpapers": "Lead wallpaper artist", "empty": "Nothing here yet",
        ],
    ]
    static func t(_ key: String, _ lang: Language) -> String {
        table[lang.rawValue]?[key] ?? table["en"]?[key] ?? key
    }
}
