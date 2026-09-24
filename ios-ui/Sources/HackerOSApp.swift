import SwiftUI

@main
struct HackerOSApp: App {
    @StateObject private var app = AppState()
    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(app)
                .preferredColorScheme(.dark)
                .tint(Color(red: 0.67, green: 0.43, blue: 1.0))
        }
    }
}
