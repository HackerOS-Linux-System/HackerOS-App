import Foundation

struct Release: Identifiable {
    let id = UUID()
    let version: String
    let desc: String
    let dates: [String]
    let changelog: [String]
}

/// Reads `window.HACKEROS_RELEASES_ALL.<lang> = [ {version:"..", desc:"..", dates:[..], changelog:[..]}, ... ]`
/// from translations/files/all/<lang>.js (JS object literals with unquoted keys and
/// double-quoted strings), the same way WebsiteReleaseParser.kt does.
enum ReleaseParser {
    static func parse(_ js: String, lang: String) -> [Release] {
        let r = block(js, lang: lang)
        return r.isEmpty && lang != "en" ? block(js, lang: "en") : r
    }

    private static func block(_ js: String, lang: String) -> [Release] {
        guard let m = js.range(of: "HACKEROS_RELEASES_ALL.\(lang)"),
              let open = js[m.upperBound...].firstIndex(of: "[") else { return [] }
        let chars = Array(js[open...])
        var depth = 0, objStart = -1, inStr = false, esc = false
        var out: [Release] = []
        for (i, c) in chars.enumerated() {
            if inStr { if esc { esc = false } else if c == "\\" { esc = true } else if c == "\"" { inStr = false }; continue }
            switch c {
            case "\"": inStr = true
            case "{": if depth == 1 { objStart = i }; depth += 1
            case "[": depth += 1
            case "}": depth -= 1
                if depth == 1, objStart >= 0, let r = release(String(chars[objStart...i])) { out.append(r); objStart = -1 }
            case "]": depth -= 1; if depth == 0 { return out }
            default: break
            }
        }
        return out
    }

    private static func release(_ obj: String) -> Release? {
        guard let v = strings(obj, key: "version", array: false).first else { return nil }
        return Release(version: v, desc: strings(obj, key: "desc", array: false).first ?? "",
                       dates: strings(obj, key: "dates", array: true), changelog: strings(obj, key: "changelog", array: true))
    }

    private static func strings(_ obj: String, key: String, array: Bool) -> [String] {
        let pattern = array ? "\(key)\\s*:\\s*\\[(.*?)\\]\\s*[,}]?" : "\(key)\\s*:\\s*(\"(?:[^\"\\\\]|\\\\.)*\")"
        guard let re = try? NSRegularExpression(pattern: pattern, options: [.dotMatchesLineSeparators]),
              let m = re.firstMatch(in: obj, range: NSRange(obj.startIndex..., in: obj)),
              let r = Range(m.range(at: 1), in: obj) else { return [] }
        let body = String(obj[r])
        let strRe = try! NSRegularExpression(pattern: "\"((?:[^\"\\\\]|\\\\.)*)\"")
        return strRe.matches(in: body, range: NSRange(body.startIndex..., in: body)).compactMap { mm in
            Range(mm.range(at: 1), in: body).map { unescape(String(body[$0])) }
        }
    }

    private static func unescape(_ s: String) -> String {
        s.replacingOccurrences(of: "\\\"", with: "\"").replacingOccurrences(of: "\\n", with: "\n").replacingOccurrences(of: "\\\\", with: "\\")
    }
}
