import Observation
import SharedLogic
import SwiftUI

/// Swift side of the shared `SettingsViewModel`.
@MainActor
@Observable
final class SettingsModel {
    private(set) var state: SettingsState

    @ObservationIgnored private let owner = SettingsViewModelOwner()
    @ObservationIgnored private var subscription: FlowSubscription?

    init() {
        state = owner.currentState
        subscription = owner.subscribeState { [weak self] state in
            MainActor.assumeIsolated { self?.state = state }
        }
    }

    deinit {
        subscription?.cancel()
        owner.clear()
    }

    func send(_ intent: SettingsIntent) {
        owner.onIntent(intent: intent)
    }
}

struct SettingsView: View {
    let model: SettingsModel

    var body: some View {
        NavigationStack {
            Form {
                Section("Units") {
                    Picker("Units", selection: Binding(
                        get: { model.state.unitSystem },
                        set: { model.send(SettingsIntentChangeUnitSystem(unitSystem: $0)) }
                    )) {
                        Text("Metric (°C, m/s)").tag(UnitSystem.metric)
                        Text("Imperial (°F, mph)").tag(UnitSystem.imperial)
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }
                Section("Theme") {
                    Picker("Theme", selection: Binding(
                        get: { model.state.themeMode },
                        set: { model.send(SettingsIntentChangeThemeMode(themeMode: $0)) }
                    )) {
                        Text("System default").tag(ThemeMode.system)
                        Text("Light").tag(ThemeMode.light)
                        Text("Dark").tag(ThemeMode.dark)
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }
            }
            .navigationTitle("Settings")
        }
    }
}
