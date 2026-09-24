import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var app: AppState
    var body: some View {
        NavigationStack {
            Form {
                Picker(app.t("language"), selection: $app.language) {
                    ForEach(Language.allCases) { Text($0.name).tag($0) }
                }
                Section {
                    ForEach(AppSection.allCases) { s in
                        Toggle(isOn: Binding(get: { app.isOn(s) }, set: { app.set(s, on: $0) })) {
                            Label(app.t(s.rawValue), systemImage: s.icon)
                        }
                    }
                } header: { Text(app.t("sections")) } footer: { Text(app.t("sections_desc")) }
                Section {
                    Link(app.t("docs"), destination: URL(string: Constants.docsWeb)!)
                    Link(app.t("source"), destination: URL(string: Constants.sourceCode)!)
                    LabeledContent(app.t("version"), value: Constants.appVersion)
                }
            }
            .navigationTitle(app.t("settings"))
        }
    }
}
