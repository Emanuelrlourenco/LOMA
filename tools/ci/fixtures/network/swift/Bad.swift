// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

// Fixture: código que acessa a rede; check-no-network.sh deve recusar.
import Foundation
let tarefa = URLSession.shared.dataTask(with: URL(string: "https://example.com")!)
