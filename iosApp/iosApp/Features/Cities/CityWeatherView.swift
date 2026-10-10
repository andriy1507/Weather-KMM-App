import Observation
import SharedLogic
import SwiftUI

/// Swift side of the shared `CityWeatherViewModel` for one saved city.
@MainActor
@Observable
final class CityWeatherModel {
    private(set) var state: CityWeatherState
    /// Set when the ViewModel asks to leave the screen (city deleted).
    private(set) var isClosed = false
    private(set) var toastMessage: String?

    @ObservationIgnored private let owner: CityWeatherViewModelOwner
    @ObservationIgnored private var subscriptions: [FlowSubscription] = []
    @ObservationIgnored private let toastPresenter = ToastPresenter()

    init(cityId: Int64) {
        owner = CityWeatherViewModelOwner(cityId: cityId)
        state = owner.currentState
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

    func send(_ intent: CityWeatherIntent) {
        owner.onIntent(intent: intent)
    }

    /// For `.refreshable`: sends `Refresh` and suspends until the fetch finishes.
    func refresh() async {
        send(CityWeatherIntentRefresh.shared)
        while owner.currentState.isFetching && !Task.isCancelled {
            try? await Task.sleep(for: .milliseconds(100))
        }
    }

    private func handle(_ effect: CityWeatherEffect) {
        switch effect {
        case is CityWeatherEffectClose:
            isClosed = true
        case let showError as CityWeatherEffectShowError:
            toastPresenter.show(Toast(message: showError.error.message)) { [weak self] in
                self?.toastMessage = $0?.message
            }
        default:
            break
        }
    }
}

/// Pushed from the Cities list.
struct CityWeatherScreen: View {
    let cityId: Int64

    var body: some View {
        ModelHost(make: { CityWeatherModel(cityId: cityId) }) { model in
            CityWeatherView(model: model)
        }
    }
}

private struct CityWeatherView: View {
    let model: CityWeatherModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        content
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    VStack(spacing: 0) {
                        Text(model.state.title).font(.headline)
                        Text(model.state.subtitle)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(role: .destructive) {
                        model.send(CityWeatherIntentDeleteClicked.shared)
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
            }
            .refreshable { await model.refresh() }
            .onAppear { model.send(CityWeatherIntentScreenStarted.shared) }
            .alert(
                "Remove \(model.state.title)?",
                isPresented: Binding(
                    get: { model.state.showDeleteConfirmation },
                    set: { isPresented in
                        if !isPresented { model.send(CityWeatherIntentDeleteDismissed.shared) }
                    }
                )
            ) {
                Button("Delete", role: .destructive) { model.send(CityWeatherIntentDeleteConfirmed.shared) }
                Button("Cancel", role: .cancel) { model.send(CityWeatherIntentDeleteDismissed.shared) }
            } message: {
                Text("Its weather will no longer be shown in your list.")
            }
            .onChange(of: model.isClosed) { _, isClosed in
                if isClosed { dismiss() }
            }
            .overlay(alignment: .bottom) {
                if let message = model.toastMessage {
                    ToastView(toast: Toast(message: message))
                }
            }
            .animation(.default, value: model.toastMessage)
    }

    @ViewBuilder
    private var content: some View {
        let state = model.state
        if let weather = state.weather {
            WeatherContent(weather: weather)
        } else if let error = state.error {
            ErrorContent(error: error, onRetry: { model.send(CityWeatherIntentRefresh.shared) })
        } else {
            LoadingContent()
        }
    }
}
