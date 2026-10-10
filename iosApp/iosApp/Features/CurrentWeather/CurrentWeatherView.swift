import SharedLogic
import SwiftUI

struct CurrentWeatherView: View {
    let model: CurrentWeatherModel

    var body: some View {
        NavigationStack {
            content
                .navigationTitle(model.state.weather?.location ?? String(localized: "Weather"))
                .navigationBarTitleDisplayMode(.inline)
                .refreshable { await model.refresh() }
                .overlay(alignment: .bottom) {
                    if let message = model.toastMessage {
                        ToastView(toast: Toast(message: message))
                    }
                }
                .animation(.default, value: model.toastMessage)
        }
        .onAppear { model.send(CurrentWeatherIntentScreenStarted.shared) }
    }

    @ViewBuilder
    private var content: some View {
        let state = model.state
        if let weather = state.weather {
            WeatherContent(weather: weather) {
                if state.permission == .denied {
                    PermissionBanner(onAllowLocation: { model.send(CurrentWeatherIntentGrantPermissionClicked.shared) })
                }
            }
        } else if let error = state.error {
            ErrorContent(error: error, onRetry: { model.send(CurrentWeatherIntentRefresh.shared) }) {
                if error == .locationPermissionDenied {
                    Button("Allow") { model.send(CurrentWeatherIntentGrantPermissionClicked.shared) }
                        .buttonStyle(.borderedProminent)
                }
            }
        } else {
            LoadingContent()
        }
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
