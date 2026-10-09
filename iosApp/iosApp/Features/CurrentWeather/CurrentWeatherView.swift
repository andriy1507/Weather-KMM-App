import SharedLogic
import SwiftUI

struct CurrentWeatherView: View {
    let model: CurrentWeatherModel

    var body: some View {
        NavigationStack {
            content
                .navigationTitle(model.state.weather?.location ?? String(localized: "Weather"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        SettingsMenu(
                            unitSystem: model.state.unitSystem,
                            themeMode: model.state.themeMode,
                            send: model.send
                        )
                    }
                }
                .refreshable { await model.refresh() }
                .overlay(alignment: .bottom) { toast }
                .animation(.default, value: model.toastMessage)
        }
        .onAppear { model.send(CurrentWeatherIntentScreenStarted.shared) }
    }

    @ViewBuilder
    private var content: some View {
        let state = model.state
        if let weather = state.weather {
            WeatherContent(
                weather: weather,
                showPermissionBanner: state.permission == .denied,
                onAllowLocation: { model.send(CurrentWeatherIntentGrantPermissionClicked.shared) }
            )
        } else if let error = state.error {
            // Scrollable so pull-to-refresh also works from the error state.
            ScrollView {
                ErrorContent(
                    error: error,
                    onRetry: { model.send(CurrentWeatherIntentRefresh.shared) },
                    onAllowLocation: { model.send(CurrentWeatherIntentGrantPermissionClicked.shared) }
                )
                .containerRelativeFrame(.vertical)
            }
        } else {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    @ViewBuilder
    private var toast: some View {
        if let message = model.toastMessage {
            Text(message)
                .font(.subheadline)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(.thinMaterial, in: Capsule())
                .padding(.bottom, 16)
                .transition(.move(edge: .bottom).combined(with: .opacity))
        }
    }
}

private struct SettingsMenu: View {
    let unitSystem: UnitSystem
    let themeMode: ThemeMode
    let send: (CurrentWeatherIntent) -> Void

    var body: some View {
        Menu {
            Section("Units") {
                Picker("Units", selection: Binding(
                    get: { unitSystem },
                    set: { send(CurrentWeatherIntentChangeUnitSystem(unitSystem: $0)) }
                )) {
                    Text("Metric (°C, m/s)").tag(UnitSystem.metric)
                    Text("Imperial (°F, mph)").tag(UnitSystem.imperial)
                }
                .pickerStyle(.inline)
            }
            Section("Theme") {
                Picker("Theme", selection: Binding(
                    get: { themeMode },
                    set: { send(CurrentWeatherIntentChangeThemeMode(themeMode: $0)) }
                )) {
                    Text("System default").tag(ThemeMode.system)
                    Text("Light").tag(ThemeMode.light)
                    Text("Dark").tag(ThemeMode.dark)
                }
                .pickerStyle(.inline)
            }
        } label: {
            Image(systemName: "ellipsis.circle")
                .accessibilityLabel("Settings")
        }
    }
}

private struct WeatherContent: View {
    let weather: CurrentWeatherUi
    let showPermissionBanner: Bool
    let onAllowLocation: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                if showPermissionBanner {
                    PermissionBanner(onAllowLocation: onAllowLocation)
                }

                AsyncImage(url: URL(string: weather.iconUrl)) { image in
                    image.resizable().scaledToFit()
                } placeholder: {
                    ProgressView()
                }
                .frame(width: 128, height: 128)
                .accessibilityLabel(weather.conditionText)

                Text(weather.temperature)
                    .font(.system(size: 72, weight: .thin))
                Text(weather.conditionText)
                    .font(.title2)
                Text("Feels like \(weather.feelsLike)  ·  H: \(weather.maxTemperature) · L: \(weather.minTemperature)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible())], spacing: 12) {
                    DetailCard(title: "Humidity", value: weather.humidity)
                    DetailCard(title: "Wind", value: weather.wind) {
                        // Meteorological degrees say where the wind comes from; the arrow shows where it blows.
                        Image(systemName: "arrow.up")
                            .rotationEffect(.degrees(Double(weather.windDirectionDegrees) + 180))
                    }
                    DetailCard(title: "Pressure", value: weather.pressure)
                    if let visibility = weather.visibility {
                        DetailCard(title: "Visibility", value: visibility)
                    }
                    DetailCard(title: "Cloudiness", value: weather.cloudiness)
                    DetailCard(title: "Sunrise", value: weather.sunrise)
                    DetailCard(title: "Sunset", value: weather.sunset)
                }
                .padding(.top, 8)

                Text("Updated at \(weather.updatedAt)")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            .padding()
        }
    }
}

private struct DetailCard<Accessory: View>: View {
    let title: LocalizedStringKey
    let value: String
    @ViewBuilder var accessory: Accessory

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
            HStack(spacing: 8) {
                Text(value).font(.title3.weight(.semibold))
                accessory
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(.fill.tertiary, in: RoundedRectangle(cornerRadius: 12))
    }
}

extension DetailCard where Accessory == EmptyView {
    init(title: LocalizedStringKey, value: String) {
        self.init(title: title, value: value) { EmptyView() }
    }
}

private struct PermissionBanner: View {
    let onAllowLocation: () -> Void

    var body: some View {
        HStack {
            Text("Location access is off. Showing weather for the last known place.")
                .font(.subheadline)
            Spacer()
            Button("Allow", action: onAllowLocation)
                .buttonStyle(.bordered)
        }
        .padding()
        .background(.tint.opacity(0.12), in: RoundedRectangle(cornerRadius: 12))
    }
}

private struct ErrorContent: View {
    let error: CurrentWeatherError
    let onRetry: () -> Void
    let onAllowLocation: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label("Couldn't load the weather", systemImage: "cloud.sun")
        } description: {
            Text(error.message)
        } actions: {
            if error == .locationPermissionDenied {
                Button("Allow", action: onAllowLocation)
                    .buttonStyle(.borderedProminent)
            }
            Button("Retry", action: onRetry)
                .buttonStyle(.bordered)
        }
    }
}
