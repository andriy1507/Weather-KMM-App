import CoreLocation
import UIKit

/// Asks for "when in use" location access, mirroring the Android flow:
/// the system prompt while it can still be shown, otherwise the app's Settings page —
/// but only when the user explicitly asks again (not on the automatic request at launch).
@MainActor
final class LocationPermissionRequester: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var pendingCompletion: ((Bool) -> Void)?
    private var settingsObserver: NSObjectProtocol?
    private var hasRequested = false

    override init() {
        super.init()
        manager.delegate = self
    }

    func request(completion: @escaping (Bool) -> Void) {
        defer { hasRequested = true }
        switch manager.authorizationStatus {
        case .notDetermined:
            pendingCompletion = completion
            manager.requestWhenInUseAuthorization()
        case .denied, .restricted:
            if hasRequested {
                openSettings(completion: completion)
            } else {
                completion(false)
            }
        default:
            completion(true)
        }
    }

    private var isAuthorized: Bool {
        switch manager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways: true
        default: false
        }
    }

    private func openSettings(completion: @escaping (Bool) -> Void) {
        guard let url = URL(string: UIApplication.openSettingsURLString) else {
            completion(false)
            return
        }
        // Report whatever the user chose in Settings once they come back.
        settingsObserver = NotificationCenter.default.addObserver(
            forName: UIApplication.didBecomeActiveNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            MainActor.assumeIsolated {
                guard let self else { return }
                if let observer = self.settingsObserver {
                    NotificationCenter.default.removeObserver(observer)
                }
                self.settingsObserver = nil
                completion(self.isAuthorized)
            }
        }
        UIApplication.shared.open(url)
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        // Delivered on the main thread: the manager was created there.
        MainActor.assumeIsolated {
            // Also called once right after the delegate is set; only answer a pending request.
            guard manager.authorizationStatus != .notDetermined, let completion = pendingCompletion else { return }
            pendingCompletion = nil
            completion(isAuthorized)
        }
    }
}
