import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    // Created in init (not as property defaults) so Koin is started before any ViewModel is resolved.
    @State private var appModel: AppModel
    @State private var currentWeatherModel: CurrentWeatherModel

    init() {
        KoinHelper.shared.start()
        _appModel = State(initialValue: AppModel())
        _currentWeatherModel = State(initialValue: CurrentWeatherModel())
    }

    var body: some Scene {
        WindowGroup {
            CurrentWeatherView(model: currentWeatherModel)
                .preferredColorScheme(appModel.colorScheme)
        }
    }
}
