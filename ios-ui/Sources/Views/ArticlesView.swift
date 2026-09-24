import SwiftUI

@MainActor
final class ArticlesStore: ObservableObject {
    @Published var data: ArticlesData?
    @Published var loading = true
    @Published var failed = false
    @Published var fromCache = false

    private var cacheURL: URL {
        FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0].appendingPathComponent("articles-bundle.json")
    }

    /// Fetches articles/index.js and every article file it lists (in parallel) and caches the
    /// result. One broken article file is skipped; if nothing loads, the cached bundle is used.
    func load() async {
        loading = true; failed = false
        do {
            let stamp = "?t=\(Int(Date().timeIntervalSince1970))"
            let indexJS = try await fetchText(Constants.articlesIndex + stamp)
            guard let index = ArticleParser.parseIndex(indexJS) else { throw URLError(.cannotParseResponse) }
            let texts = await withTaskGroup(of: String?.self, returning: [String].self) { group in
                for f in index.files { group.addTask { try? await fetchText(Constants.articlesBase + f + stamp) } }
                var r: [String] = []
                for await t in group { if let t { r.append(t) } }
                return r
            }
            let objects = texts.compactMap(ArticleParser.articleObject)
            guard !objects.isEmpty,
                  let bundle = ArticleParser.bundleData(tags: index.tags, articles: objects),
                  let parsed = ArticleParser.parseBundle(bundle), !parsed.articles.isEmpty else { throw URLError(.zeroByteResource) }
            data = parsed; fromCache = false
            try? bundle.write(to: cacheURL)
        } catch {
            if let cached = try? Data(contentsOf: cacheURL), let parsed = ArticleParser.parseBundle(cached), !parsed.articles.isEmpty {
                data = parsed; fromCache = true
            } else { failed = true }
        }
        loading = false
    }
}

func fetchText(_ url: String) async throws -> String {
    guard let u = URL(string: url) else { throw URLError(.badURL) }
    let (d, resp) = try await URLSession.shared.data(from: u)
    guard (resp as? HTTPURLResponse)?.statusCode == 200, let s = String(data: d, encoding: .utf8) else { throw URLError(.badServerResponse) }
    return s
}

struct ArticlesView: View {
    @EnvironmentObject var app: AppState
    @StateObject private var store = ArticlesStore()
    @State private var query = ""
    @State private var tag: String? = nil

    private var lang: String { app.language.rawValue }

    private var filtered: [Article] {
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        return (store.data?.articles ?? []).filter { a in
            (tag == nil || a.tags.contains(tag!)) && (q.isEmpty || a.searchText(lang).contains(q))
        }
    }

    var body: some View {
        NavigationStack {
            Group {
                if store.loading && store.data == nil { ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity) }
                else if store.failed && store.data == nil { ErrorRetry(app: app) { Task { await store.load() } } }
                else { list }
            }
            .navigationTitle(app.t("articles"))
            .searchable(text: $query, prompt: app.t("search_articles"))
            .task { if store.data == nil { await store.load() } }
            .refreshable { await store.load() }
        }
    }

    private var list: some View {
        List {
            if store.fromCache { Label(app.t("offline"), systemImage: "icloud.slash").font(.footnote).foregroundStyle(.secondary) }
            let used = Array(Set((store.data?.articles ?? []).flatMap { $0.tags })).sorted()
            if !used.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        chip(app.t("all"), tag == nil) { tag = nil }
                        ForEach(used, id: \.self) { id in
                            chip(store.data?.tagLabel(id, lang) ?? id, tag == id) { tag = (tag == id) ? nil : id }
                        }
                    }
                }.listRowSeparator(.hidden)
            }
            if filtered.isEmpty { Text(app.t("no_results")).foregroundStyle(.secondary).frame(maxWidth: .infinity) }
            ForEach(filtered) { a in
                NavigationLink { ArticleReader(article: a, data: store.data!) } label: { card(a) }
            }
        }
        .listStyle(.plain)
    }

    private func chip(_ label: String, _ on: Bool, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label).font(.footnote.weight(on ? .bold : .regular))
                .padding(.horizontal, 12).padding(.vertical, 6)
                .background(on ? Color.accentColor : Color.clear, in: Capsule())
                .overlay(Capsule().stroke(on ? Color.accentColor : Color.gray.opacity(0.4)))
                .foregroundStyle(on ? Color.black : Color.secondary)
        }.buttonStyle(.plain)
    }

    private func card(_ a: Article) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 10) { Text(a.icon).font(.title); Text(a.titleFor(lang)).font(.headline) }
            Text(a.summaryFor(lang)).font(.subheadline).foregroundStyle(.secondary)
            Text("\(formatDate(a.date, lang))  ·  \(a.readingMinutes) \(app.t("min_read"))")
                .font(.caption.monospaced()).foregroundStyle(.secondary)
        }.padding(.vertical, 6)
    }
}

func formatDate(_ iso: String, _ lang: String) -> String {
    let p = DateFormatter(); p.locale = Locale(identifier: "en_US_POSIX"); p.dateFormat = "yyyy-MM-dd"
    guard let d = p.date(from: iso) else { return iso }
    let f = DateFormatter(); f.locale = Locale(identifier: lang); f.dateStyle = .medium
    return f.string(from: d)
}

struct ErrorRetry: View {
    let app: AppState
    let action: () -> Void
    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "wifi.slash").font(.largeTitle).foregroundStyle(.red)
            Text(app.t("error")).foregroundStyle(.secondary)
            Button(app.t("retry"), action: action).buttonStyle(.borderedProminent).tint(.red)
        }.frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
