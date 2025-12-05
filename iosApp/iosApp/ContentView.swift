import UIKit
import SwiftUI
import ComposeApp
import FirebaseAnalytics

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Initialize analytics bridge before creating the view controller
        setupAnalyticsBridge()
        return MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
    
    private func setupAnalyticsBridge() {
        // Set up the Kotlin callbacks to call Firebase Analytics
        IosAnalyticsBridge.shared.logEvent = { name, params in
            let swiftParams = params?.compactMapValues { value -> Any? in
                // Convert Kotlin types to Swift types
                if let stringValue = value as? String { return stringValue }
                if let intValue = value as? Int { return intValue }
                if let doubleValue = value as? Double { return doubleValue }
                if let boolValue = value as? Bool { return boolValue }
                return String(describing: value)
            }
            Analytics.logEvent(name, parameters: swiftParams)
        }
        
        IosAnalyticsBridge.shared.setUserId = { userId in
            Analytics.setUserID(userId)
        }
        
        IosAnalyticsBridge.shared.setUserProperty = { name, value in
            Analytics.setUserProperty(value, forName: name)
        }
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
