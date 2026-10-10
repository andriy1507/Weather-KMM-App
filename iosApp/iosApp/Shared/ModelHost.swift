import SwiftUI

/// Creates a screen's model once per view identity and hands it to `content`.
///
/// A `@State` default value is re-evaluated every time SwiftUI re-creates the view struct, which
/// would create (and immediately discard) extra Kotlin ViewModels. This creates the model lazily
/// on first appearance and keeps it for the lifetime of the view.
struct ModelHost<Model: AnyObject, Content: View>: View {
    let make: @MainActor () -> Model
    @ViewBuilder let content: (Model) -> Content

    @State private var model: Model?

    var body: some View {
        if let model {
            content(model)
        } else {
            Color.clear.onAppear { model = make() }
        }
    }
}
