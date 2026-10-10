import Observation
import SharedLogic
import SwiftUI

/// Swift side of the shared `CitySearchViewModel`.
@MainActor
@Observable
final class CitySearchModel {
    private(set) var state: CitySearchState
    /// Set once a city was saved; the sheet closes in response.
    private(set) var addedCityId: Int64?

    @ObservationIgnored private let owner = CitySearchViewModelOwner()
    @ObservationIgnored private var subscriptions: [FlowSubscription] = []

    init() {
        state = owner.currentState
        subscriptions.append(owner.subscribeState { [weak self] state in
            MainActor.assumeIsolated { self?.state = state }
        })
        subscriptions.append(owner.subscribeEffects { [weak self] effect in
            MainActor.assumeIsolated {
                if let added = effect as? CitySearchEffectCityAdded {
                    self?.addedCityId = added.cityId
                }
            }
        })
    }

    deinit {
        subscriptions.forEach { $0.cancel() }
        owner.clear()
    }

    /// Bindable query: reads the state, writes go to the ViewModel as intents.
    var query: String {
        get { state.query }
        set { send(CitySearchIntentQueryChanged(query: newValue)) }
    }

    func send(_ intent: CitySearchIntent) {
        owner.onIntent(intent: intent)
    }
}

/// Presented as a sheet from the Cities tab.
struct CitySearchScreen: View {
    var body: some View {
        ModelHost(make: { CitySearchModel() }) { model in
            CitySearchView(model: model)
        }
    }
}

private struct CitySearchView: View {
    @Bindable var model: CitySearchModel
    @Environment(\.dismiss) private var dismiss
    @State private var isSearchFieldActive = true

    var body: some View {
        NavigationStack {
            content
                .navigationTitle("Add city")
                .navigationBarTitleDisplayMode(.inline)
                .searchable(
                    text: $model.query,
                    isPresented: $isSearchFieldActive,
                    placement: .navigationBarDrawer(displayMode: .always),
                    prompt: "Search for a city"
                )
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { dismiss() }
                    }
                }
        }
        .onChange(of: model.addedCityId) { _, cityId in
            if cityId != nil { dismiss() }
        }
    }

    @ViewBuilder
    private var content: some View {
        let state = model.state
        if let error = state.error {
            ErrorContent(error: error, onRetry: { model.send(CitySearchIntentRetry.shared) })
        } else if state.showNoResults {
            ContentUnavailableView.search(text: state.query)
        } else {
            List(state.results, id: \.id) { result in
                Button {
                    model.send(CitySearchIntentResultClicked(resultId: result.id))
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(result.name)
                            Text(result.subtitle)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        if result.isSaved {
                            Image(systemName: "checkmark")
                                .foregroundStyle(.tint)
                                .accessibilityLabel("Already saved")
                        }
                    }
                    .contentShape(Rectangle())
                }
                .foregroundStyle(.primary)
            }
            .listStyle(.plain)
            .overlay {
                if state.isSearching && state.results.isEmpty {
                    ProgressView()
                }
            }
        }
    }
}
