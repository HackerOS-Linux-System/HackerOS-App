import SwiftUI

struct TeamView: View {
    @EnvironmentObject var app: AppState
    var body: some View {
        NavigationStack {
            List {
                member("michal92299", app.t("role_founder"))
                member("RafeNop", app.t("role_wallpapers"))
                Section {
                    Text(app.t("contact_desc")).font(.footnote).foregroundStyle(.secondary)
                    Link("hackeros068@gmail.com", destination: URL(string: "mailto:hackeros068@gmail.com")!)
                } header: { Text(app.t("contact")) }
            }
            .navigationTitle(app.t("team"))
        }
    }

    private func member(_ name: String, _ role: String) -> some View {
        HStack(spacing: 14) {
            Text(String(name.prefix(2)).uppercased()).font(.headline)
                .frame(width: 44, height: 44).background(Color.gray.opacity(0.25), in: Circle())
            VStack(alignment: .leading) { Text(name).font(.headline); Text(role).font(.subheadline).foregroundStyle(.secondary) }
        }
    }
}
