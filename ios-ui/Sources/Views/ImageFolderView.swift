import SwiftUI
import UIKit

struct RemoteImage: Identifiable, Hashable, Codable {
    let name: String
    let url: String
    var id: String { url }
    var display: String {
        name.split(separator: ".").dropLast().joined(separator: ".")
            .replacingOccurrences(of: "-", with: " ").replacingOccurrences(of: "_", with: " ").capitalized
    }
}

/// Shared by Wallpapers and Gallery: both list a folder of the website repo via the GitHub
/// contents API, exactly like the Android app. Wallpapers additionally can be saved to Photos
/// (iOS has no API for apps to set the Home/Lock screen wallpaper directly).
struct ImageFolderView: View {
    @EnvironmentObject var app: AppState
    let titleKey: String
    let api: String
    let cacheKey: String
    let canSave: Bool

    @State private var items: [RemoteImage] = []
    @State private var loading = true
    @State private var failed = false
    @State private var fromCache = false
    @State private var selected: RemoteImage?

    var body: some View {
        NavigationStack {
            Group {
                if loading && items.isEmpty { ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity) }
                else if failed && items.isEmpty { ErrorRetry(app: app) { Task { await load() } } }
                else if items.isEmpty { Text(app.t("empty")).foregroundStyle(.secondary) }
                else {
                    ScrollView {
                        if fromCache { Label(app.t("offline"), systemImage: "icloud.slash").font(.footnote).foregroundStyle(.secondary).padding(.horizontal) }
                        LazyVGrid(columns: [GridItem(.adaptive(minimum: 150), spacing: 10)], spacing: 10) {
                            ForEach(items) { item in
                                Button { selected = item } label: {
                                    AsyncImage(url: URL(string: item.url)) { img in img.resizable().scaledToFill() } placeholder: { Color.gray.opacity(0.2) }
                                        .frame(height: 220).clipped().clipShape(RoundedRectangle(cornerRadius: 12))
                                        .overlay(alignment: .bottomLeading) {
                                            Text(item.display).font(.caption.bold()).padding(6)
                                                .background(.black.opacity(0.55), in: RoundedRectangle(cornerRadius: 6)).padding(6)
                                        }
                                }.buttonStyle(.plain)
                            }
                        }.padding(10)
                    }
                }
            }
            .navigationTitle(app.t(titleKey))
            .task { if items.isEmpty { await load() } }
            .refreshable { await load() }
            .sheet(item: $selected) { item in ImageDetail(item: item, canSave: canSave) }
        }
    }

    private func load() async {
        loading = true; failed = false
        do {
            guard let url = URL(string: api) else { throw URLError(.badURL) }
            let (data, _) = try await URLSession.shared.data(from: url)
            guard let arr = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] else { throw URLError(.cannotParseResponse) }
            let parsed: [RemoteImage] = arr.compactMap { o in
                guard o["type"] as? String == "file", let n = o["name"] as? String,
                      n.range(of: "\\.(jpg|jpeg|png|gif|webp)$", options: [.regularExpression, .caseInsensitive]) != nil,
                      let u = o["download_url"] as? String else { return nil }
                return RemoteImage(name: n, url: u)
            }.sorted { $0.name < $1.name }
            if parsed.isEmpty { throw URLError(.zeroByteResource) }
            items = parsed; fromCache = false
            if let enc = try? JSONEncoder().encode(parsed) { UserDefaults.standard.set(enc, forKey: cacheKey) }
        } catch {
            if let d = UserDefaults.standard.data(forKey: cacheKey), let c = try? JSONDecoder().decode([RemoteImage].self, from: d), !c.isEmpty {
                items = c; fromCache = true
            } else { failed = true }
        }
        loading = false
    }
}

struct ImageDetail: View {
    @EnvironmentObject var app: AppState
    @Environment(\.dismiss) private var dismiss
    let item: RemoteImage
    let canSave: Bool
    @State private var status: String?

    var body: some View {
        NavigationStack {
            VStack {
                AsyncImage(url: URL(string: item.url)) { $0.resizable().scaledToFit() } placeholder: { ProgressView() }
                if let status { Text(status).font(.footnote).foregroundStyle(.secondary) }
                HStack {
                    if let u = URL(string: item.url) { ShareLink(item: u) { Label("Share", systemImage: "square.and.arrow.up") } }
                    if canSave { Button { Task { await save() } } label: { Label(app.t("save"), systemImage: "arrow.down.to.line") }.buttonStyle(.borderedProminent) }
                }.padding()
            }
            .navigationTitle(item.display).navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("✕") { dismiss() } } }
        }
    }

    private func save() async {
        guard let url = URL(string: item.url), let result = try? await URLSession.shared.data(from: url), let img = UIImage(data: result.0) else {
            status = app.t("error"); return
        }
        UIImageWriteToSavedPhotosAlbum(img, nil, nil, nil)
        status = app.t("saved")
    }
}
