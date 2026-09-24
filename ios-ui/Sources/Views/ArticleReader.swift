import SwiftUI
import UIKit

struct ArticleReader: View {
    @EnvironmentObject var app: AppState
    let article: Article
    let data: ArticlesData
    @State private var copied = false

    private var lang: String { app.language.rawValue }

    var body: some View {
        let blocks = article.blocks(lang)
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text(article.icon).font(.system(size: 44))
                Text(article.titleFor(lang)).font(.title.bold())
                Text(article.summaryFor(lang)).foregroundStyle(.secondary)
                Text("\(article.author.isEmpty ? "" : article.author + "  ·  ")\(formatDate(article.date, lang))  ·  \(article.readingMinutes) \(app.t("min_read"))")
                    .font(.caption.monospaced()).foregroundStyle(.secondary)
                Divider()
                if article.isFallback(lang) { note("info", app.t("fallback")) }
                ForEach(Array(blocks.enumerated()), id: \.offset) { _, b in view(for: b) }
                Link(destination: URL(string: Constants.articlesWeb + "#/" + article.id)!) {
                    Label(app.t("open_website"), systemImage: "arrow.up.right.square").font(.footnote)
                }.padding(.top, 8)
            }
            .padding()
        }
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder private func view(for b: ArticleBlock) -> some View {
        switch b {
        case .heading(let level, let text):
            Text(text).font(level == 2 ? .title3.bold() : .headline).padding(.top, level == 2 ? 10 : 4)
        case .paragraph(let h): Text(inlineHTML(h)).lineSpacing(4)
        case .quote(let h):
            HStack(spacing: 10) { Rectangle().fill(Color.gray.opacity(0.5)).frame(width: 3); Text(inlineHTML(h)).foregroundStyle(.secondary) }
        case .list(let items, let ordered):
            VStack(alignment: .leading, spacing: 6) {
                ForEach(Array(items.enumerated()), id: \.offset) { i, it in
                    HStack(alignment: .firstTextBaseline, spacing: 8) {
                        Text(ordered ? "\(i + 1)." : "•").foregroundStyle(Color.accentColor)
                        Text(inlineHTML(it))
                    }
                }
            }
        case .code(let lang, let title, let text): code(title ?? lang, text)
        case .note(let kind, let h): note(kind, h)
        case .table(let head, let rows): table(head, rows)
        }
    }

    private func code(_ title: String, _ text: String) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text(title).font(.caption.monospaced()).foregroundStyle(.secondary)
                Spacer()
                Button {
                    UIPasteboard.general.string = text
                    copied = true
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) { copied = false }
                } label: { Image(systemName: copied ? "checkmark" : "doc.on.doc") }
            }
            .padding(.horizontal, 12).padding(.vertical, 8).background(Color.white.opacity(0.05))
            ScrollView(.horizontal, showsIndicators: false) {
                Text(text).font(.system(.footnote, design: .monospaced)).padding(12)
            }
        }
        .background(Color.black.opacity(0.35), in: RoundedRectangle(cornerRadius: 10))
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.white.opacity(0.08)))
    }

    private func note(_ kind: String, _ html: String) -> some View {
        let (icon, color): (String, Color) = kind == "tip" ? ("💡", .green) : kind == "warn" ? ("⚠️", .orange) : ("ℹ️", .blue)
        return HStack(alignment: .top, spacing: 10) { Text(icon); Text(inlineHTML(html)).font(.subheadline) }
            .padding(12).frame(maxWidth: .infinity, alignment: .leading)
            .background(color.opacity(0.1), in: RoundedRectangle(cornerRadius: 10))
            .overlay(alignment: .leading) { Rectangle().fill(color).frame(width: 4) }
            .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private func table(_ head: [String], _ rows: [[String]]) -> some View {
        VStack(spacing: 0) {
            if !head.isEmpty {
                HStack(alignment: .top) { ForEach(Array(head.enumerated()), id: \.offset) { _, h in Text(stripTags(h)).font(.caption.bold()).frame(maxWidth: .infinity, alignment: .leading) } }
                    .padding(10).background(Color.white.opacity(0.05))
            }
            ForEach(Array(rows.enumerated()), id: \.offset) { _, r in
                Divider()
                HStack(alignment: .top) { ForEach(Array(r.enumerated()), id: \.offset) { _, c in Text(inlineHTML(c)).font(.caption).frame(maxWidth: .infinity, alignment: .leading) } }
                    .padding(10)
            }
        }
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }
}
