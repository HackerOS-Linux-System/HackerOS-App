import SwiftUI

struct RootView: View {
    @EnvironmentObject var app: AppState

    var body: some View {
        // iPhone shows the first 4 tabs and folds the rest into "More" automatically.
        TabView {
            if app.isOn(.articles) { ArticlesView().tabItem { Label(app.t("articles"), systemImage: AppSection.articles.icon) } }
            if app.isOn(.releases) { ReleasesView().tabItem { Label(app.t("releases"), systemImage: AppSection.releases.icon) } }
            if app.isOn(.wallpapers) {
                ImageFolderView(titleKey: "wallpapers", api: Constants.wallpapersAPI, cacheKey: "wallpapers-cache", canSave: true)
                    .tabItem { Label(app.t("wallpapers"), systemImage: AppSection.wallpapers.icon) }
            }
            if app.isOn(.gallery) {
                ImageFolderView(titleKey: "gallery", api: Constants.galleryAPI, cacheKey: "gallery-cache", canSave: false)
                    .tabItem { Label(app.t("gallery"), systemImage: AppSection.gallery.icon) }
            }
            if app.isOn(.team) { TeamView().tabItem { Label(app.t("team"), systemImage: AppSection.team.icon) } }
            SettingsView().tabItem { Label(app.t("settings"), systemImage: "gearshape") }
        }
    }
}
