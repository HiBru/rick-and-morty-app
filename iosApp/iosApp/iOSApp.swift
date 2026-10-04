import SwiftUI
import Shared

@main
struct iOSApp: App {

    /// Starts Koin before any composable can inject.
    ///
    /// iOS has no `Application` class, so this is the platform's only pre-UI
    /// hook. The Kotlin function is `initKoinIos`; the `do` prefix is added by
    /// the Kotlin/Native exporter, which renames anything starting with `init`
    /// so it cannot collide with Objective-C's initialiser convention.
    ///
    /// It takes no argument on purpose — only Android needs the config block, to
    /// hand over its `Context`, and Kotlin default arguments are invisible here.
    init() {
        InitKoinKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
