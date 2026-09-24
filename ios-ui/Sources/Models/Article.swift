import Foundation

/// Native model of the website's Articles data files (articles/index.js + articles/<id>.js).
/// The files are JS assignments of *strict JSON* objects, so we cut the object literal out and
/// hand it to JSONSerialization. Same schema and same language fallback as the Android app.
enum ArticleBlock {
    case heading(level: Int, text: String)
    case paragraph(String)
    case quote(String)
    case list(items: [String], ordered: Bool)
    case code(lang: String, title: String?, text: String)
    case note(kind: String, html: String)
    case table(head: [String], rows: [[String]])
}

struct Article: Identifiable {
    let id: String
    let date: String
    let author: String
    let readingMinutes: Int
    let icon: String
    let tags: [String]
    let title: [String: String]
    let summary: [String: String]
    let content: [String: [ArticleBlock]]

    func titleFor(_ l: String) -> String { localized(title, l) }
    func summaryFor(_ l: String) -> String { localized(summary, l) }
    func blocks(_ l: String) -> [ArticleBlock] { content[l] ?? content["en"] ?? content["pl"] ?? [] }
    func isFallback(_ l: String) -> Bool { content[l] == nil }

    func searchText(_ l: String) -> String {
        var s = titleFor(l) + " " + summaryFor(l) + " "
        for b in blocks(l) {
            switch b {
            case .heading(_, let t): s += t + " "
            case .paragraph(let h), .quote(let h), .note(_, let h): s += stripTags(h) + " "
            case .list(let items, _): s += items.map(stripTags).joined(separator: " ") + " "
            case .table(let head, let rows): s += (head + rows.flatMap { $0 }).map(stripTags).joined(separator: " ") + " "
            case .code: break
            }
        }
        return s.lowercased()
    }
}

struct ArticlesData {
    var tags: [String: [String: String]]
    var articles: [Article]
    func tagLabel(_ id: String, _ l: String) -> String { tags[id].map { localized($0, l) } ?? id }
}

func localized(_ m: [String: String], _ l: String) -> String { m[l] ?? m["en"] ?? m["pl"] ?? m.values.first ?? "" }
func stripTags(_ s: String) -> String { s.replacingOccurrences(of: "<[^>]+>", with: " ", options: .regularExpression) }

enum ArticleParser {
    /// Returns the `{...}` literal following `marker ... =` as a dictionary.
    static func extractObject(_ js: String, marker: String) -> [String: Any]? {
        guard let m = js.range(of: marker),
              let eq = js[m.upperBound...].firstIndex(of: "="),
              let start = js[eq...].firstIndex(of: "{"),
              let end = js.lastIndex(of: "}"), start < end,
              let data = String(js[start...end]).data(using: .utf8) else { return nil }
        return (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
    }

    static func parseIndex(_ js: String) -> (tags: [String: [String: String]], files: [String])? {
        guard let root = extractObject(js, marker: "HACKEROS_ARTICLES_INDEX"),
              let arr = root["articles"] as? [[String: Any]] else { return nil }
        let files = arr.compactMap { e -> String? in
            guard let id = e["id"] as? String else { return nil }
            return (e["file"] as? String) ?? "\(id).js"
        }
        return (parseTags(root["tags"]), files)
    }

    static func articleObject(_ js: String) -> [String: Any]? { extractObject(js, marker: "HACKEROS_ARTICLES[\"") }

    static func bundleData(tags: [String: [String: String]], articles: [[String: Any]]) -> Data? {
        try? JSONSerialization.data(withJSONObject: ["tags": tags, "articles": articles])
    }

    static func parseBundle(_ data: Data) -> ArticlesData? {
        guard let root = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
              let arr = root["articles"] as? [[String: Any]] else { return nil }
        let list = arr.compactMap(parseArticle).sorted { $0.date > $1.date }
        return ArticlesData(tags: parseTags(root["tags"]), articles: list)
    }

    private static func parseTags(_ any: Any?) -> [String: [String: String]] {
        guard let d = any as? [String: Any] else { return [:] }
        return d.mapValues { ($0 as? [String: String]) ?? [:] }
    }

    private static func parseArticle(_ o: [String: Any]) -> Article? {
        guard let id = o["id"] as? String, let c = o["content"] as? [String: Any] else { return nil }
        var content: [String: [ArticleBlock]] = [:]
        for (lang, v) in c { content[lang] = (v as? [[String: Any]] ?? []).compactMap(parseBlock) }
        return Article(
            id: id, date: o["date"] as? String ?? "1970-01-01", author: o["author"] as? String ?? "",
            readingMinutes: o["readingMinutes"] as? Int ?? 1, icon: o["icon"] as? String ?? "📄",
            tags: o["tags"] as? [String] ?? [],
            title: o["title"] as? [String: String] ?? [:], summary: o["summary"] as? [String: String] ?? [:],
            content: content)
    }

    private static func parseBlock(_ b: [String: Any]) -> ArticleBlock? {
        let s = { (k: String) in b[k] as? String ?? "" }
        switch b["t"] as? String {
        case "h2": return .heading(level: 2, text: s("text"))
        case "h3": return .heading(level: 3, text: s("text"))
        case "p": return .paragraph(s("html"))
        case "quote": return .quote(s("html"))
        case "ul": return .list(items: b["items"] as? [String] ?? [], ordered: false)
        case "ol": return .list(items: b["items"] as? [String] ?? [], ordered: true)
        case "code":
            let text = (b["code"] as? [String])?.joined(separator: "\n") ?? s("code")
            let title = s("title")
            return .code(lang: b["lang"] as? String ?? "text", title: title.isEmpty ? nil : title, text: text)
        case "note": return .note(kind: b["kind"] as? String ?? "info", html: s("html"))
        case "table": return .table(head: b["head"] as? [String] ?? [], rows: b["rows"] as? [[String]] ?? [])
        default: return nil // unknown block types from a newer website are skipped
        }
    }
}
