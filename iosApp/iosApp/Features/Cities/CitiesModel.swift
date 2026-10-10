import Observation
import SharedLogic

enum CitiesDestination: Hashable {
    case city(id: Int64)
}

/// Swift side of the shared `CitiesViewModel`. Navigation effects become observable `path` / sheet state.
@MainActor
@Observable
final class CitiesModel {
    private(set) var state: CitiesState
    var path: [CitiesDestination] = []
    var isSearchPresented = false
    private(set) var toast: Toast?

    @ObservationIgnored private let owner = CitiesViewModelOwner()
    @ObservationIgnored private var subscriptions: [FlowSubscription] = []
    @ObservationIgnored private let toastPresenter = ToastPresenter()

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

    func send(_ intent: CitiesIntent) {
        owner.onIntent(intent: intent)
    }

    /// For `.refreshable`: sends `Refresh` and suspends until the refresh finishes.
    func refresh() async {
        send(CitiesIntentRefresh.shared)
        while owner.currentState.isRefreshing && !Task.isCancelled {
            try? await Task.sleep(for: .milliseconds(100))
        }
    }

    func undoDelete() {
        toastPresenter.hide { toast = $0 }
        send(CitiesIntentUndoDelete.shared)
    }

    private func handle(_ effect: CitiesEffect) {
        switch effect {
        case let openCity as CitiesEffectOpenCity:
            path.append(.city(id: openCity.cityId))
        case is CitiesEffectOpenSearch:
            isSearchPresented = true
        case let showUndo as CitiesEffectShowUndo:
            showToast(
                Toast(message: String(localized: "\(showUndo.cityName) removed"), actionTitle: "Undo"),
                for: .seconds(5)
            )
        case let showError as CitiesEffectShowError:
            showToast(Toast(message: showError.error.message))
        default:
            break
        }
    }

    private func showToast(_ toast: Toast, for duration: Duration = .seconds(3)) {
        toastPresenter.show(toast, for: duration) { [weak self] in self?.toast = $0 }
    }
}
