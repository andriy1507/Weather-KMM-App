import Observation
import SharedLogic
import SwiftUI

/// Swift side of the shared `AppViewModel` (app-wide theme).
@MainActor
@Observable
final class AppModel {
    private(set) var themeMode: ThemeMode

    @ObservationIgnored private let owner = AppViewModelOwner()
    @ObservationIgnored private var subscription: FlowSubscription?

    init() {
        themeMode = owner.currentThemeMode
        subscription = owner.subscribeThemeMode { [weak self] themeMode in
            // Kotlin collects on the main thread.
            MainActor.assumeIsolated { self?.themeMode = themeMode }
        }
    }

    deinit {
        subscription?.cancel()
        owner.clear()
    }

    /// `nil` follows the system appearance.
    var colorScheme: ColorScheme? {
        switch themeMode {
        case .light: .light
        case .dark: .dark
        default: nil
        }
    }
}
