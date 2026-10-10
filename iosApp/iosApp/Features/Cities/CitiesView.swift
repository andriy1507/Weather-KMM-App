import SharedLogic
import SwiftUI

struct CitiesView: View {
    @Bindable var model: CitiesModel

    var body: some View {
        NavigationStack(path: $model.path) {
            content
                .navigationTitle("Cities")
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            model.send(CitiesIntentAddClicked.shared)
                        } label: {
                            Label("Add city", systemImage: "plus")
                        }
                    }
                }
                .refreshable { await model.refresh() }
                // Fires on tab switch and when coming back from a city: refreshes stale cities.
                .onAppear { model.send(CitiesIntentScreenStarted.shared) }
                .navigationDestination(for: CitiesDestination.self) { destination in
                    switch destination {
                    case .city(let id):
                        CityWeatherScreen(cityId: id)
                    }
                }
                .overlay(alignment: .bottom) {
                    if let toast = model.toast {
                        ToastView(toast: toast, action: model.undoDelete)
                    }
                }
                .animation(.default, value: model.toast)
        }
        .sheet(isPresented: $model.isSearchPresented) {
            CitySearchScreen()
        }
    }

    @ViewBuilder
    private var content: some View {
        let state = model.state
        if !state.isLoaded {
            LoadingContent()
        } else if state.isEmpty {
            ScrollView {
                ContentUnavailableView {
                    Label("No saved cities", systemImage: "building.2")
                } description: {
                    Text("Add cities to see their weather side by side.")
                } actions: {
                    Button("Add city") { model.send(CitiesIntentAddClicked.shared) }
                        .buttonStyle(.borderedProminent)
                }
                .containerRelativeFrame(.vertical)
            }
        } else {
            List {
                ForEach(state.cities, id: \.id) { city in
                    Button {
                        model.send(CitiesIntentCityClicked(cityId: city.id))
                    } label: {
                        CityRow(city: city)
                    }
                    .foregroundStyle(.primary)
                    .swipeActions(edge: .trailing) {
                        Button(role: .destructive) {
                            model.send(CitiesIntentDeleteCity(cityId: city.id))
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
                }
            }
            .listStyle(.plain)
        }
    }
}

private struct CityRow: View {
    let city: CityRowUi

    var body: some View {
        HStack(spacing: 12) {
            Group {
                if let iconUrl = city.iconUrl {
                    AsyncImage(url: URL(string: iconUrl)) { image in
                        image.resizable().scaledToFit()
                    } placeholder: {
                        Color.clear
                    }
                } else {
                    Color.clear
                }
            }
            .frame(width: 44, height: 44)

            VStack(alignment: .leading, spacing: 2) {
                Text(city.name).font(.headline)
                Text([city.subtitle, city.conditionText].compactMap { $0 }.joined(separator: " · "))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Text(city.temperature ?? "–")
                .font(.title2)
        }
        .contentShape(Rectangle())
    }
}
