import SwiftUI

/// Transient message at the bottom of the screen, optionally with an action (e.g. Undo).
struct Toast: Equatable {
    let message: String
    var actionTitle: LocalizedStringKey?

    static func == (lhs: Toast, rhs: Toast) -> Bool {
        lhs.message == rhs.message && (lhs.actionTitle == nil) == (rhs.actionTitle == nil)
    }
}

struct ToastView: View {
    let toast: Toast
    var action: () -> Void = {}

    var body: some View {
        HStack(spacing: 16) {
            Text(toast.message)
                .font(.subheadline)
            if let actionTitle = toast.actionTitle {
                Button(actionTitle, action: action)
                    .font(.subheadline.weight(.semibold))
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(.thinMaterial, in: Capsule())
        .padding(.bottom, 16)
        .transition(.move(edge: .bottom).combined(with: .opacity))
    }
}

/// Shows a toast for a few seconds; a newer toast replaces the current one.
@MainActor
final class ToastPresenter {
    private var hideTask: Task<Void, Never>?

    func show(_ toast: Toast, for duration: Duration = .seconds(3), set: @escaping (Toast?) -> Void) {
        hideTask?.cancel()
        set(toast)
        hideTask = Task {
            try? await Task.sleep(for: duration)
            guard !Task.isCancelled else { return }
            set(nil)
        }
    }

    func hide(set: (Toast?) -> Void) {
        hideTask?.cancel()
        set(nil)
    }
}
