import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    // Created in init (not as property defaults) so Koin is started before any ViewModel is resolved.
    // Tab root models live as long as the app; pushed screens create theirs via ModelHost.
    @State private var appModel: AppModel
    @State private var currentWeatherModel: CurrentWeatherModel
    @State private var citiesModel: CitiesModel
    @State private var settingsModel: SettingsModel

    init() {
        KoinHelper.shared.start()
        _appModel = State(initialValue: AppModel())
        _currentWeatherModel = State(initialValue: CurrentWeatherModel())
        _citiesModel = State(initialValue: CitiesModel())
        _settingsModel = State(initialValue: SettingsModel())
    }

    var body: some Scene {
        WindowGroup {
            TabView {
                Tab("Weather", systemImage: "cloud.sun") {
                    CurrentWeatherView(model: currentWeatherModel)
                }
                Tab("Cities", systemImage: "list.bullet") {
                    CitiesView(model: citiesModel)
                }
                Tab("Settings", systemImage: "gearshape") {
                    SettingsView(model: settingsModel)
                }
            }
            .preferredColorScheme(appModel.colorScheme)
        }
    }
}
