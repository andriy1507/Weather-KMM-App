import SharedLogic
import SwiftUI

/// Full weather display shared by the current-location and city screens.
struct WeatherContent<Header: View>: View {
    let weather: CurrentWeatherUi
    @ViewBuilder var header: Header

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                header

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

extension WeatherContent where Header == EmptyView {
    init(weather: CurrentWeatherUi) {
        self.init(weather: weather) { EmptyView() }
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

/// Full-screen error with retry. Wrapped in a ScrollView so pull-to-refresh also works from it.
struct ErrorContent<PrimaryAction: View>: View {
    let error: WeatherError
    let onRetry: () -> Void
    @ViewBuilder var primaryAction: PrimaryAction

    var body: some View {
        ScrollView {
            ContentUnavailableView {
                Label("Couldn't load the weather", systemImage: "cloud.sun")
            } description: {
                Text(error.message)
            } actions: {
                primaryAction
                Button("Retry", action: onRetry)
                    .buttonStyle(.bordered)
            }
            .containerRelativeFrame(.vertical)
        }
    }
}

extension ErrorContent where PrimaryAction == EmptyView {
    init(error: WeatherError, onRetry: @escaping () -> Void) {
        self.init(error: error, onRetry: onRetry) { EmptyView() }
    }
}

struct LoadingContent: View {
    var body: some View {
        ProgressView()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
