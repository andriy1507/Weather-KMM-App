import Foundation
import SharedLogic

extension CurrentWeatherError {
    var message: String {
        switch self {
        case .noInternet: String(localized: "No internet connection.")
        case .timeout: String(localized: "The weather service took too long to respond.")
        case .invalidApiKey: String(localized: "The OpenWeather API key is invalid or not activated yet.")
        case .rateLimited: String(localized: "Too many requests. Try again in a minute.")
        case .server: String(localized: "The weather service is unavailable right now.")
        case .locationPermissionDenied: String(localized: "Allow location access to see the weather where you are.")
        case .locationDisabled: String(localized: "Location is turned off. Turn it on in Settings.")
        case .locationUnavailable: String(localized: "Couldn't determine your location.")
        default: String(localized: "Something went wrong.")
        }
    }
}
