import SwiftUI

struct ReleasesView: View {
    @EnvironmentObject var app: AppState
    @State private var releases: [Release] = []
    @State private var loading = true
    @State private var failed = false
    @State private var fromCache = false

    var body: some View {
        NavigationStack {
            Group {
                if loading && releases.isEmpty { ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity) }
                else if failed && releases.isEmpty { ErrorRetry(app: app) { Task { await load() } } }
                else {
                    List {
                        if fromCache { Label(app.t("offline"), systemImage: "icloud.slash").font(.footnote).foregroundStyle(.secondary) }
                        ForEach(releases) { r in
                            VStack(alignment: .leading, spacing: 6) {
                                Text(r.version).font(.headline)
                                Text(r.desc).font(.subheadline).foregroundStyle(.secondary)
                                ForEach(r.dates, id: \.self) { Text($0).font(.caption.monospaced()).foregroundStyle(Color.accentColor) }
                                ForEach(r.changelog, id: \.self) { Text("• " + $0).font(.footnote) }
                            }.padding(.vertical, 6)
                        }
                    }.listStyle(.plain)
                }
            }
            .navigationTitle(app.t("releases"))
            .task(id: app.language) { await load() }
            .refreshable { await load() }
        }
    }

    private var cacheKey: String { "releases-\(app.language.rawValue)" }

    private func load() async {
        loading = true; failed = false
        do {
            let js = try await fetchText(Constants.releasesURL(app.language.rawValue) + "?t=\(Int(Date().timeIntervalSince1970))")
            let parsed = ReleaseParser.parse(js, lang: app.language.rawValue)
            if parsed.isEmpty { throw URLError(.cannotParseResponse) }
            releases = parsed; fromCache = false
            UserDefaults.standard.set(js, forKey: cacheKey)
        } catch {
            if let js = UserDefaults.standard.string(forKey: cacheKey) {
                let parsed = ReleaseParser.parse(js, lang: app.language.rawValue)
                if !parsed.isEmpty { releases = parsed; fromCache = true } else { failed = true }
            } else { failed = true }
        }
        loading = false
    }
}
