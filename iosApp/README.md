<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 The LOMA Authors -->
# LOMA para iOS

App SwiftUI que hospeda a interface Compose do LOMA (`shared/ui`, framework estático `LomaShared`).
O projeto do Xcode não fica no repositório: ele é gerado a partir de `project.yml`.

```bash
brew install xcodegen
xcodegen generate --spec iosApp/project.yml
open iosApp/LOMA.xcodeproj
```

No Xcode, escolha um simulador (Mac com Apple Silicon) ou o seu iPhone. Para instalar no iPhone,
selecione o seu time em Signing & Capabilities. A fase de build "Compilar o código Kotlin" roda
`./gradlew :shared:ui:embedAndSignAppleFrameworkForXcode` sozinha.

Ícone: `Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png`, gerado por `tools/icone/gerar_icone.py`.
