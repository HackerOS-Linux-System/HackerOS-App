import SwiftUI

/// Article text only uses <strong> <em> <code> <a href> <br>, so a tiny converter to
/// AttributedString is enough (no WebView). Mirrors InlineHtmlText.kt on Android.
func inlineHTML(_ html: String) -> AttributedString {
    var out = AttributedString()
    var bold = false, italic = false, code = false
    var link: URL? = nil
    let text = html.replacingOccurrences(of: "<br>", with: "\n").replacingOccurrences(of: "<br/>", with: "\n")
    var i = text.startIndex

    func emit(_ raw: String) {
        let decoded = raw.replacingOccurrences(of: "&lt;", with: "<").replacingOccurrences(of: "&gt;", with: ">")
            .replacingOccurrences(of: "&quot;", with: "\"").replacingOccurrences(of: "&amp;", with: "&")
        var run = AttributedString(decoded)
        var intent: InlinePresentationIntent = []
        if bold { intent.insert(.stronglyEmphasized) }
        if italic { intent.insert(.emphasized) }
        if code { intent.insert(.code) }
        if !intent.isEmpty { run.inlinePresentationIntent = intent }
        if let link { run.link = link; run.underlineStyle = .single }
        out += run
    }

    while i < text.endIndex {
        if text[i] == "<", let close = text[i...].firstIndex(of: ">") {
            let tag = String(text[text.index(after: i)..<close])
            let closing = tag.hasPrefix("/")
            let name = tag.trimmingCharacters(in: CharacterSet(charactersIn: "/")).split(separator: " ").first.map { $0.lowercased() } ?? ""
            switch name {
            case "strong", "b": bold = !closing
            case "em", "i": italic = !closing
            case "code": code = !closing
            case "a":
                if closing { link = nil }
                else if let r = tag.range(of: "href=\"[^\"]*\"", options: .regularExpression) {
                    let v = String(tag[r]).dropFirst(6).dropLast()
                    link = resolveLink(String(v))
                }
            default: break
            }
            i = text.index(after: close)
        } else {
            let next = text[i...].dropFirst().firstIndex(of: "<") ?? text.endIndex
            emit(String(text[i..<next]))
            i = next
        }
    }
    return out
}

/// Relative links in article data (e.g. "h-sharp/docs.html") point into the website.
private func resolveLink(_ href: String) -> URL? {
    if href.hasPrefix("http://") || href.hasPrefix("https://") || href.hasPrefix("mailto:") { return URL(string: href) }
    if href.hasPrefix("//") || href.contains(":") { return nil }
    return URL(string: Constants.site + href)
}
