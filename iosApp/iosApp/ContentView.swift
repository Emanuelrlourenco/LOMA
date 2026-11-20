// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

import SwiftUI
import UIKit
import LomaShared

/// Hospeda a interface Compose do LOMA (shared/ui).
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea(.all)
    }
}
