import Observation
import SharedLogic

/// Swift side of the shared `CurrentWeatherViewModel`: mirrors its state, sends intents, handles effects.
@MainActor
@Observable
final class CurrentWeatherModel {
    private(set) var state: CurrentWeatherState
    /// Transient error shown over cached weather (the `ShowError` effect).
    private(set) var toastMessage: String?

    @ObservationIgnored private let owner = CurrentWeatherViewModelOwner()
    @ObservationIgnored private let permissionRequester = LocationPermissionRequester()
    @ObservationIgnored private var subscriptions: [FlowSubscription] = []
    @ObservationIgnored private var toastTask: Task<Void, Never>?

    init() {
        state = owner.currentState
        // Kotlin collects on the main thread.
        subscriptions.append(owner.subscribeState { [weak self] state in
            MainActor.assumeIsolated { self?.state = state }
        })
        subscriptions.append(owner.subscribeEffects { [weak self] effect in
            MainActor.assumeIsolated { self?.handle(effect) }
        })
    }

    deinit {
        subscriptions.forEach { $0.cancel() }
        owner.clear()
    }

    func send(_ intent: CurrentWeatherIntent) {
        owner.onIntent(intent: intent)
    }

    /// For `.refreshable`: sends `Refresh` and suspends until the fetch finishes.
    func refresh() async {
        send(CurrentWeatherIntentRefresh.shared)
        // The ViewModel flips `isFetching` synchronously, so reading the current value is reliable.
        while owner.currentState.isFetching && !Task.isCancelled {
            try? await Task.sleep(for: .milliseconds(100))
        }
    }

    private func handle(_ effect: CurrentWeatherEffect) {
        switch effect {
        case is CurrentWeatherEffectRequestLocationPermission:
            permissionRequester.request { [weak self] granted in
                self?.send(CurrentWeatherIntentPermissionResult(granted: granted))
            }
        case let showError as CurrentWeatherEffectShowError:
            showToast(showError.error.message)
        default:
            break
        }
    }

    private func showToast(_ message: String) {
        toastTask?.cancel()
        toastMessage = message
        toastTask = Task { [weak self] in
            try? await Task.sleep(for: .seconds(3))
            guard !Task.isCancelled else { return }
            self?.toastMessage = nil
        }
    }
}
