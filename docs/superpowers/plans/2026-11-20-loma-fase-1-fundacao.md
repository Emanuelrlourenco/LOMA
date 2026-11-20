# LOMA — Fase 1 (Fundação) — Plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar o projeto LOMA no repositório `Emanuelrlourenco/LOMA-` (KMP + Compose Multiplatform) com tema da marca, barra inferior com as 4 abas, banco Room com a tabela de serviços, `Money` em centavos, app iOS "casca" e CI que publica um APK de teste e barra qualquer acesso à rede.

**Architecture:** Módulos `:shared:salon` (domínio puro), `:shared:data` (Room 3 KMP), `:shared:ui` (Compose Multiplatform: tema, shell com abas), `:androidApp` (Activity + manifest) e `iosApp/` (XcodeGen + SwiftUI hospedando o Compose). Abas controladas por estado próprio (`LomaTab`), sem biblioteca de navegação nesta fase. Tudo compila e testa no GitHub Actions; testes comuns rodam no alvo JVM.

**Tech Stack:** Kotlin 2.4.20 · Compose Multiplatform 1.12.0 · Room 3 (`androidx.room3`) 3.0.3 + `androidx.sqlite:sqlite-bundled` · KSP (versão casada com o Kotlin 2.4.20) · AGP 9.x (estável mais recente compatível) · Gradle wrapper (estável mais recente exigido pelo AGP) · kotlinx-coroutines · Roborazzi (screenshots no JVM) · Spotless + ktlint · XcodeGen · GitHub Actions.

**Spec:** `docs/design/2026-11-19-loma-design.md` (rev. 5, aprovado). Telas aprovadas: `docs/design/telas/*.dc.html` (cópia da página "Telas do LOMA" do canvas, versão `1795176413-08cf`).

## Global Constraints

- Sem rede: o manifest Android final **não** pode conter `android.permission.INTERNET`; nenhuma dependência de rede (Ktor, OkHttp, Retrofit, Firebase, Play Services, analytics, crash reporting) e nenhum uso de `java.net.*`, `HttpURLConnection`, `Socket`, `URLSession`/`NSURLSession` em código próprio.
- Dependências só com licença Apache-2.0, MIT ou BSD (fonte Outfit: OFL, permitida por ser recurso, com `OFL.txt` junto).
- Toda fonte `.kt`, `.kts`, `.swift`, `.sh`, `.yml` começa com:
  `// SPDX-License-Identifier: Apache-2.0` (ou `#` em `.sh`/`.yml`) e na linha seguinte `// Copyright 2026 The LOMA Authors`.
- Android: `applicationId` e namespace `app.loma.calendar`; `minSdk 26`; `targetSdk`/`compileSdk` = o mais recente estável suportado pelo AGP escolhido.
- iOS: bundle `app.loma.calendar`, deployment target **17.0**.
- Dinheiro sempre em centavos (`Long`); nunca `Double`/`Float` para valores.
- Strings de UI em compose resources, pt-BR (padrão) e inglês (`values-en`).
- Nenhuma permissão é pedida ao abrir o app.
- Pacotes: `app.loma.salon`, `app.loma.data`, `app.loma.ui`, `app.loma.android`.
- Repositório: `github.com/Emanuelrlourenco/LOMA-` (conta única do usuário para todos os projetos).
- **APK único (mesma lógica do Kiro Yasha):** todo APK é assinado com a **mesma chave** e tem `versionCode` sempre maior, para que cada versão nova instale **por cima** da anterior, mantendo os dados. A chave fica nos Secrets do GitHub (`LOMA_KEYSTORE_BASE64`, `LOMA_KEYSTORE_PASSWORD`, `LOMA_KEY_ALIAS`, `LOMA_KEY_PASSWORD`), nunca no repositório (ele é público); uma cópia vai para o dono guardar. `versionCode` = número da execução do CI (`GITHUB_RUN_NUMBER`); `versionName` = `0.<fase>.<GITHUB_RUN_NUMBER>`. Diferente do Kiro Yasha, o LOMA não se atualiza sozinho (não tem internet): o CI publica sempre no mesmo endereço e o dono baixa e instala por cima.

## Review Focus

1. **Valores com sinal, grandes ou digitados com "R$" e espaços** ("R$ 1.420,00", " 280,5 ", "-25,00") — formatar e ler sem perder centavos nem quebrar → testes na Task 2.
2. **Fonte do sistema em 200%** — rótulos da barra inferior visíveis, sem sobrepor ícones → teste na Task 5.
3. **Modo escuro do sistema** — texto principal e secundário com contraste ≥ 4,5:1 também no esquema escuro → testes na Task 4.
4. **Botão voltar do Android numa aba que não é o Início** — volta ao Início; no Início, sai do app → teste na Task 5.
5. **Rotação/morte do processo** — a aba selecionada continua a mesma → teste na Task 5.

---

### Task 1: Esqueleto do projeto, licença e documentos

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `.gitignore`, `.editorconfig`, `LICENSE` (texto oficial Apache-2.0), `NOTICE`, `README.md`
- Create: `shared/salon/build.gradle.kts`, `shared/data/build.gradle.kts`, `shared/ui/build.gradle.kts`, `androidApp/build.gradle.kts` (vazios de código; só configuração)
- Create: `tools/ci/check-spdx.sh`, `tools/ci/fixtures/spdx/missing.kt`, `tools/ci/fixtures/spdx/ok.kt`
- Create: `docs/design/2026-11-19-loma-design.md` (cópia do spec), `docs/design/telas/` (os 10 `T-*.dc.html` + `README.md` com o link do canvas), `docs/superpowers/plans/2026-11-20-loma-fase-1-fundacao.md` (este plano)

**Interfaces:**
- Produces: módulos Gradle `:shared:salon`, `:shared:data`, `:shared:ui`, `:androidApp`; alvos KMP em todo `:shared:*`: `jvm()`, `androidLibrary` (plugin `com.android.kotlin.multiplatform.library`), `iosArm64()`, `iosSimulatorArm64()`. Aliases no catálogo: `kotlin`, `compose-multiplatform`, `room3`, `ksp`, `agp`.
- Produces: `tools/ci/check-spdx.sh <dir>` → sai 0 se todo arquivo-fonte tem as 2 linhas de cabeçalho; sai 1 e lista os que faltam.

- [ ] **Step 1:** Fixar versões no `libs.versions.toml`: Kotlin `2.4.20`, Compose Multiplatform `1.12.0`, Room3 `3.0.3`; resolver no Maven Central/Google Maven a versão de KSP para Kotlin 2.4.20, a AGP 9.x estável mais recente suportada pelo CMP 1.12 e o Gradle exigido por ela. Registrar as escolhas num comentário no topo do catálogo.
- [ ] **Step 2:** Criar `check-spdx.sh` e as fixtures (`ok.kt` com as 2 linhas, `missing.kt` sem).
- [ ] **Step 3:** Rodar `tools/ci/check-spdx.sh tools/ci/fixtures/spdx` → esperado: exit 1 citando `missing.kt`. Rodar com só `ok.kt` → exit 0. O script ignora `tools/ci/fixtures/`, `build/`, `.gradle/` e `docs/` quando chamado na raiz.
- [ ] **Step 4:** Rodar `./gradlew help` → `BUILD SUCCESSFUL`. Se o ambiente local não alcançar os repositórios Maven, registrar isso e validar no CI (Task 8).
- [ ] **Step 5:** Rodar `tools/ci/check-spdx.sh .` → exit 0.
- [ ] **Step 6:** Commit: `chore: esqueleto KMP, licença Apache-2.0 e documentos de design`.

### Task 2: `Money` em centavos (pt-BR)

**Files:**
- Create: `shared/salon/src/commonMain/kotlin/app/loma/salon/Money.kt`
- Test: `shared/salon/src/commonTest/kotlin/app/loma/salon/MoneyTest.kt`

**Interfaces:**
- Produces:
  - `@JvmInline value class Money(val cents: Long) : Comparable<Money>` com `operator fun plus(other: Money): Money`, `operator fun minus(other: Money): Money`, `companion object { val ZERO: Money; fun ofReais(reais: Long): Money; fun parseBrl(text: String): Money? }`
  - `fun Money.formatBrl(showZeroCents: Boolean = true): String`
- Decisão: BRL é sempre formatado no padrão brasileiro ("R$ 1.420,00"), qualquer que seja o idioma da interface. Separador entre "R$" e o número: espaço comum (U+0020). Sem `java.text`, sem `Double`.

- [ ] **Step 1: Testes que falham** (`MoneyTest`):
  - `formatBrl`: `Money(0)` → `"R$ 0,00"`; `Money(7000)` → `"R$ 70,00"`; `Money(142000)` → `"R$ 1.420,00"`; `Money(296000)` → `"R$ 2.960,00"`; `Money(123456789)` → `"R$ 1.234.567,89"`; `Money(-2500)` → `"-R$ 25,00"`; `Money(5)` → `"R$ 0,05"`.
  - `formatBrl(showZeroCents = false)`: `Money(438000)` → `"R$ 4.380"`; `Money(438050)` → `"R$ 4.380,50"` (centavos não-zero sempre aparecem).
  - `parseBrl`: `"280,00"` → 28000; `"280"` → 28000; `"1.420,5"` → 142050; `"R$ 1.420,00"` → 142000; `" 280,5 "` → 28050; `"-25,00"` → -2500; `""`, `"abc"`, `"12,345"`, `"1,2,3"`, `"1.42,00"` → `null`.
  - Ida e volta: para cada valor em `[0, 5, 7000, 142000, -2500, 123456789]`, `parseBrl(formatBrl(m)) == m`.
  - Soma: `Money(118000) + Money(129000) + Money(191000) == Money(438000)`; `Money(438000) - Money(142000) == Money(296000)`.
- [ ] **Step 2:** `./gradlew :shared:salon:jvmTest --tests app.loma.salon.MoneyTest` → FAIL (não compila).
- [ ] **Step 3:** Implementar em `Money.kt`. Em `parseBrl`, os pontos são aceitos apenas como separador de milhar em grupos de 3 dígitos.
- [ ] **Step 4:** O mesmo comando → PASS.
- [ ] **Step 5:** Commit: `feat(salon): Money em centavos com formato BRL`.

### Task 3: Banco Room 3 com a tabela `services`

**Files:**
- Create: `shared/data/src/commonMain/kotlin/app/loma/data/LomaDatabase.kt`, `.../ServiceEntity.kt`, `.../ServiceDao.kt`, `.../DatabaseFactory.kt`, `androidMain/.../DatabaseFactory.android.kt`, `iosMain/.../DatabaseFactory.ios.kt`, `jvmMain/.../DatabaseFactory.jvm.kt`
- Create: `shared/data/schemas/` (export do schema, versionado)
- Test: `shared/data/src/jvmTest/kotlin/app/loma/data/ServiceDaoTest.kt`

**Interfaces:**
- Consumes: nada de `:shared:salon` além do tipo `Long` para centavos (sem dependência de `Money` no Room).
- Produces:
  - `@Entity(tableName = "services") data class ServiceEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val durationMinutes: Int, val priceCents: Long, val colorArgb: Long, val active: Boolean = true, val sortOrder: Int)`
  - `@Dao interface ServiceDao { suspend fun insert(service: ServiceEntity): Long; suspend fun update(service: ServiceEntity); fun observeActive(): Flow<List<ServiceEntity>> /* active = 1 ORDER BY sortOrder, id */; fun observeAll(): Flow<List<ServiceEntity>> }`
  - `@Database(entities = [ServiceEntity::class], version = 1, exportSchema = true) abstract class LomaDatabase : RoomDatabase { abstract fun serviceDao(): ServiceDao }` com `@ConstructedBy` para KMP
  - Construtores por plataforma (sem `expect`): `fun lomaDatabaseBuilder(context: Context): RoomDatabase.Builder<LomaDatabase>` (androidMain, `context.getDatabasePath("loma.db")`); `fun lomaDatabaseBuilder(): RoomDatabase.Builder<LomaDatabase>` (iosMain, `loma.db` em `NSApplicationSupportDirectory`); `fun lomaDatabaseBuilder(file: java.io.File): RoomDatabase.Builder<LomaDatabase>` (jvmMain, usado nos testes). Em `commonMain`: `fun RoomDatabase.Builder<LomaDatabase>.buildLoma(): LomaDatabase` aplica `BundledSQLiteDriver()` e `Dispatchers.IO` e chama `build()`.

- [ ] **Step 1: Testes que falham** (`ServiceDaoTest`, banco em arquivo temporário no JVM):
  - inserir "Escova" (60 min, 7000) e "Coloração" (90 min, 18000) com `sortOrder` 1 e 0 → `observeActive().first()` devolve `["Coloração", "Escova"]`.
  - `update` com `active = false` em "Escova" → `observeActive` só tem "Coloração"; `observeAll` tem os dois.
  - `priceCents` de 123456789 volta idêntico.
- [ ] **Step 2:** `./gradlew :shared:data:jvmTest` → FAIL.
- [ ] **Step 3:** Implementar entidades, DAO, banco e fábricas; plugin `androidx.room3` com `schemaDirectory("$projectDir/schemas")`; KSP para todos os alvos.
- [ ] **Step 4:** `./gradlew :shared:data:jvmTest` → PASS, e `shared/data/schemas/.../1.json` gerado.
- [ ] **Step 5:** Commit: `feat(data): Room 3 KMP com tabela services`.

### Task 4: Tema LOMA (tokens, Outfit, claro e escuro)

**Files:**
- Create: `shared/ui/src/commonMain/kotlin/app/loma/ui/theme/LomaColors.kt`, `.../LomaTypography.kt`, `.../LomaShapes.kt`, `.../LomaTheme.kt`, `.../Contrast.kt`
- Create: `shared/ui/src/commonMain/composeResources/font/outfit_regular.ttf`, `outfit_medium.ttf`, `outfit_semibold.ttf`, `outfit_bold.ttf`, `outfit_extrabold.ttf` + `licenses/OFL-Outfit.txt` (do repositório oficial `google/fonts`, pasta `ofl/outfit`)
- Test: `shared/ui/src/commonTest/kotlin/app/loma/ui/theme/LomaColorsTest.kt`

**Interfaces:**
- Produces:
  - `object LomaPalette` com os tokens claros do spec §7.12, exatamente: `Background = 0xFFF3F2FA`, `Surface = 0xFFFFFFFF`, `Container = 0xFFE7E4F8`, `Navy = 0xFF17165C`, `NavyLight = 0xFF2A2985`, `Mint = 0xFF5ED6B4`, `OnMint = 0xFF0B3A2E`, `MintSoft = 0xFFE3F8F1`, `Lilac = 0xFFB69CF2`, `Coral = 0xFFFF7A66`, `AlertSoft = 0xFFFFE2DC`, `OnAlertSoft = 0xFF7A1A0B`, `TextSecondary = 0xFF5B5A7A`, `Border = 0xFFC9C6E6`
  - `data class LomaColors(val background: Color, val surface: Color, val container: Color, val primary: Color, val onPrimary: Color, val accent: Color, val onAccent: Color, val selection: Color, val alert: Color, val textPrimary: Color, val textSecondary: Color, val border: Color, val navIndicator: Color, val navIndicatorIcon: Color)`; `val LightLomaColors`; `val DarkLomaColors`
  - Esquema escuro (decisão desta fase): background `0xFF0F0E33`, surface `0xFF1B1A52`, container `0xFF2A2985`, primary `Mint`, onPrimary `OnMint`, textPrimary `0xFFF3F2FA`, textSecondary `0xFFC9C6F0`, border `0xFF3A3994`, navIndicator `Mint`, navIndicatorIcon `OnMint`, accent `Mint`, selection `Lilac`, alert `Coral`
  - `@Composable fun LomaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)`: fornece `LocalLomaColors` + `MaterialTheme` (colorScheme mapeado dos tokens, tipografia Outfit, formas)
  - `val LomaShapes`: itens 16.dp, cartões 26.dp, folhas 32.dp, pílula `CircleShape`
  - `LomaTypography`: título de tela 30.sp/ExtraBold; nome em cartão 26.sp/ExtraBold; corpo 15.sp/Normal; rótulo da barra 12.sp/SemiBold
  - `fun contrastRatio(foreground: Color, background: Color): Double` (WCAG 2.x, luminância relativa)

- [ ] **Step 1: Testes que falham** (`LomaColorsTest`):
  - Tokens claros iguais aos valores acima (um assert por token).
  - Contraste ≥ 4.5 em: textPrimary/background e textSecondary/background (claro e escuro); `Color.White`/Navy; OnMint/Mint; Navy/Lilac; OnAlertSoft/AlertSoft; navIndicatorIcon/navIndicator (claro e escuro).
  - `contrastRatio(Color.Black, Color.White)` = 21.0 (±0.01).
- [ ] **Step 2:** `./gradlew :shared:ui:jvmTest --tests app.loma.ui.theme.LomaColorsTest` → FAIL.
- [ ] **Step 3:** Implementar os arquivos do tema e copiar as fontes e a licença OFL.
- [ ] **Step 4:** Mesmo comando → PASS.
- [ ] **Step 5:** Commit: `feat(ui): tema LOMA com tokens aprovados e fonte Outfit`.

### Task 5: Shell do app com barra inferior (Início, Agenda, Clientes, Financeiro)

**Files:**
- Create: `shared/ui/src/commonMain/kotlin/app/loma/ui/shell/LomaTab.kt`, `.../LomaApp.kt`, `.../LomaBottomBar.kt`, `.../PlaceholderScreen.kt`
- Create: `shared/ui/src/commonMain/composeResources/values/strings.xml`, `values-en/strings.xml`
- Test: `shared/ui/src/jvmTest/kotlin/app/loma/ui/shell/LomaAppTest.kt`, `.../LomaTabTest.kt`, `.../LomaShellScreenshotTest.kt` (Roborazzi)

**Interfaces:**
- Consumes: `LomaTheme`, `LocalLomaColors`, `LomaTypography` (Task 4)
- Produces:
  - `enum class LomaTab { Inicio, Agenda, Clientes, Financeiro }` com `fun LomaTab.back(): LomaTab?` → `Inicio` para as outras abas, `null` para `Inicio` (deixa o sistema fechar o app)
  - `@Composable fun LomaApp()` — raiz usada por Android e iOS: `LomaTheme { … }`, aba em `rememberSaveable` (salva pelo `name`), `BackHandler(enabled = tab != Inicio)` de `org.jetbrains.compose.ui:ui-backhandler` chamando `tab.back()`
  - `@Composable fun LomaBottomBar(selected: LomaTab, onSelect: (LomaTab) -> Unit)` — fiel à tela aprovada `T-Inicio`: altura 84.dp, fundo surface, cantos superiores 28.dp, sombra suave; item selecionado com indicador 56×32.dp `navIndicator` e ícone `navIndicatorIcon`; rótulo 12.sp (700 selecionado, 600 os outros); ícones como `ImageVector` desenhados a partir dos SVG das telas aprovadas (casa, calendário, pessoas, carteira); `Modifier.semantics { selected = … }` e `role = Role.Tab` em cada item; tag de teste `"tab_<nome>"`
  - `PlaceholderScreen(title: String)`: título 30.sp ExtraBold no topo e o texto "Em construção — chega na próxima fase" centralizado (strings `placeholder_soon`)
  - Strings pt-BR: `tab_inicio = Início`, `tab_agenda = Agenda`, `tab_clientes = Clientes`, `tab_financeiro = Financeiro`; inglês: `Home`, `Calendar`, `Clients`, `Finances`

- [ ] **Step 1: Testes que falham:**
  - `LomaTabTest`: `Agenda.back() == Inicio`; `Clientes.back() == Inicio`; `Financeiro.back() == Inicio`; `Inicio.back() == null`.
  - `LomaAppTest` (`runComposeUiTest`): ao abrir, `tab_inicio` está selecionada e o título "Início" aparece; tocar `tab_financeiro` → título "Financeiro" e só essa aba selecionada; com `StateRestorationTester`, selecionar "Clientes", `emulateSaveAndRestore()` → "Clientes" continua selecionada; com `LocalDensity` em `fontScale = 2f`, os 4 rótulos ficam visíveis (`assertIsDisplayed`) e a altura de cada item é ≥ 48.dp.
  - `LomaShellScreenshotTest`: Roborazzi grava `LomaApp` em 390×844 dp, claro e escuro, em `shared/ui/screenshots/`.
- [ ] **Step 2:** `./gradlew :shared:ui:jvmTest` → FAIL.
- [ ] **Step 3:** Implementar `LomaTab`, `LomaApp`, `LomaBottomBar`, `PlaceholderScreen` e as strings.
- [ ] **Step 4:** `./gradlew :shared:ui:jvmTest` → PASS; `./gradlew :shared:ui:recordRoborazziJvm` gera os 2 PNGs. Comparar a barra com `docs/design/telas/T-Inicio.dc.html` e anotar diferenças visíveis no commit, se houver.
- [ ] **Step 5:** Commit: `feat(ui): shell com barra inferior das 4 abas`.

### Task 5b: Abertura animada com som (aprovada pelo usuário)

**Files:**
- Create: `shared/ui/src/commonMain/kotlin/app/loma/ui/intro/IntroTimeline.kt`, `.../LomaIntro.kt`, `.../IntroSound.kt` (expect), `androidMain/.../IntroSound.android.kt`, `iosMain/.../IntroSound.ios.kt`
- Create: `shared/ui/src/commonMain/composeResources/files/loma-abertura-brilho-lento.ogg` e `.m4a` (gerados por `tools/som/synth_lento.py`, que também vai para o repositório com `synth.py`)
- Modify: `shared/ui/.../shell/LomaApp.kt` (mostra `LomaIntro` antes do shell)
- Test: `shared/ui/src/jvmTest/kotlin/app/loma/ui/intro/LomaIntroTest.kt`

**Interfaces:**
- Produces:
  - `object IntroTimeline { const val GLOW_MS = 0; const val DOT_IN_MS = 1400; const val HIT_MS = 2100; const val LETTERS_MS = 2350; const val SHINE_MS = 3300; const val EXIT_MS = 4000; const val TOTAL_MS = 4600; const val REDUCED_MOTION_MS = 600 }` (valores do spec §7.12b, iguais ao protótipo `docs/design/telas/Abertura.dc.html`)
  - `@Composable fun LomaIntro(soundEnabled: Boolean, reducedMotion: Boolean, onFinished: () -> Unit)` — desenha brilho, faíscas, ponto→"o" menta, `l m a`, reflexo e saída; um toque em qualquer ponto chama `onFinished` na hora
  - `expect class IntroSound { fun play(); fun stop() }` — Android: `SoundPool` com o `.ogg`, sem tocar se `AudioManager.ringerMode != RINGER_MODE_NORMAL`; iOS: `AVAudioPlayer` com o `.m4a` e `AVAudioSession` categoria `.ambient`
  - Em `LomaApp`: `rememberSaveable { mutableStateOf(false) }` `introDone`; a abertura aparece só se `introDone == false` (girar a tela não repete)

- [ ] **Step 1: Testes que falham** (`LomaIntroTest`): com o relógio de teste (`mainClock.autoAdvance = false`), avançar `TOTAL_MS` → `onFinished` chamado uma vez; tocar na tela em 1000 ms → `onFinished` chamado imediatamente e não de novo em 4600 ms; `reducedMotion = true` → `onFinished` em `REDUCED_MOTION_MS`; em `LomaApp`, após a abertura e `emulateSaveAndRestore()`, a abertura não volta.
- [ ] **Step 2:** `./gradlew :shared:ui:jvmTest --tests app.loma.ui.intro.*` → FAIL.
- [ ] **Step 3:** Implementar; o ponto que vira "o" e o nome final usam a mesma geometria do ícone 6B (Outfit ExtraBold, `o` menta).
- [ ] **Step 4:** Mesmo comando → PASS; gravar screenshots Roborazzi em 1000, 2200, 3400 e 4600 ms.
- [ ] **Step 5:** Commit: `feat(ui): abertura animada com som Brilho lento`.

### Task 6: App Android

**Files:**
- Create: `androidApp/src/main/AndroidManifest.xml`, `androidApp/src/main/kotlin/app/loma/android/MainActivity.kt`, `androidApp/src/main/res/xml/data_extraction_rules.xml`, `androidApp/src/main/res/xml/backup_rules.xml`, `androidApp/src/main/res/values/themes.xml` (tema de janela sem barra de ação, fundo `#F3F2FA`), ícone **6B "O menta"** adaptativo (`mipmap-anydpi-v26/ic_launcher.xml` com fundo `#17165C`, primeiro plano e camada monocromática em vetor gerados por `tools/icone/gerar_icone.py` a partir dos contornos da Outfit ExtraBold), tema de abertura do sistema marinho com ícone transparente (`Theme.SplashScreen`, `windowSplashScreenAnimatedIcon` transparente)
- Test: `androidApp/src/test/kotlin/app/loma/android/ManifestTest.kt` (lê o manifest mesclado da variante debug)

**Interfaces:**
- Consumes: `LomaApp()` (Task 5)
- Produces: `MainActivity : ComponentActivity` com `enableEdgeToEdge()` e `setContent { LomaApp() }`; `applicationId "app.loma.calendar"`; `versionName "0.1.0"`, `versionCode 1`.
- Backup (spec §7.16): `android:allowBackup="true"`, `android:dataExtractionRules="@xml/data_extraction_rules"` com `<cloud-backup>` excluindo os domínios `root`, `file`, `database`, `sharedpref` e `external`, e `<device-transfer>` incluindo tudo; `android:fullBackupContent="@xml/backup_rules"` excluindo tudo (Android 8–11).

- [ ] **Step 1: Testes que falham** (`ManifestTest`, sobre o manifest mesclado `build/intermediates/merged_manifest/debug/.../AndroidManifest.xml`): nenhum `uses-permission` com `android.permission.INTERNET`; `package`/`applicationId` = `app.loma.calendar`; `dataExtractionRules` e `fullBackupContent` presentes; nenhuma permissão perigosa declarada.
- [ ] **Step 2:** `./gradlew :androidApp:testDebugUnitTest` → FAIL.
- [ ] **Step 3:** Implementar manifest, Activity, regras de backup, tema e ícone.
- [ ] **Step 4:** `./gradlew :androidApp:testDebugUnitTest :androidApp:assembleDebug` → PASS e `androidApp/build/outputs/apk/debug/androidApp-debug.apk` existe.
- [ ] **Step 5:** Commit: `feat(android): app Android com shell, sem INTERNET e sem backup na nuvem`.

### Task 7: App iOS "casca"

**Files:**
- Create: `shared/ui/src/iosMain/kotlin/app/loma/ui/MainViewController.kt`
- Create: `iosApp/project.yml` (XcodeGen), `iosApp/iosApp/LomaApp.swift`, `iosApp/iosApp/ContentView.swift`, `iosApp/iosApp/Info.plist`, `iosApp/iosApp/Assets.xcassets/` (ícone 6B em PNG 1024×1024 gerado pelo mesmo script), `iosApp/iosApp/LaunchScreen.storyboard` (marinho liso)
- Modify: `shared/ui/build.gradle.kts` (framework estático `LomaShared` para `iosArm64` e `iosSimulatorArm64`, `export` de nada além do necessário)

**Interfaces:**
- Consumes: `LomaApp()` (Task 5)
- Produces: `fun MainViewController(): UIViewController = ComposeUIViewController { LomaApp() }`; projeto Xcode gerado por `xcodegen generate` com alvo `LOMA`, bundle `app.loma.calendar`, `IPHONEOS_DEPLOYMENT_TARGET = 17.0`, fase de build que roda `./gradlew :shared:ui:embedAndSignAppleFrameworkForXcode`; `ContentView` com `ComposeView().ignoresSafeArea(.keyboard)`.

- [ ] **Step 1:** Escrever os arquivos acima.
- [ ] **Step 2:** Verificação no CI (Task 8, job iOS): `xcodegen generate --spec iosApp/project.yml` e `xcodebuild -project iosApp/LOMA.xcodeproj -scheme LOMA -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build` → `** BUILD SUCCEEDED **`; e `./gradlew :shared:salon:iosSimulatorArm64Test :shared:ui:iosSimulatorArm64Test` → PASS.
- [ ] **Step 3:** Commit: `feat(ios): app iOS casca hospedando o Compose`.

### Task 8: Guardas de privacidade e CI

**Files:**
- Create: `tools/ci/check-no-network.sh`, `tools/ci/allowed-dependencies.txt`, `tools/ci/check-dependencies.sh`, `tools/ci/fixtures/network/` (`BadClient.kt` usando `java.net.URL`, `Bad.swift` usando `URLSession`, `AndroidManifest-internet.xml` com `INTERNET`, `deps-bad.txt` com `io.ktor:ktor-client-core`)
- Create: `.github/workflows/android.yml`, `.github/workflows/ios.yml`
- Modify: `build.gradle.kts` (Spotless + ktlint em todo `*.kt`/`*.kts`)

**Interfaces:**
- Produces:
  - `check-no-network.sh <src-dir> [merged-manifest]` → exit 1 se achar, fora de `tools/ci/fixtures/`, qualquer um de: `java.net.`, `HttpURLConnection`, `java.net.Socket`, `okhttp`, `io.ktor`, `retrofit`, `URLSession`, `NSURLSession`, `NSURLConnection`, `CFNetwork`; ou se o manifest tiver `android.permission.INTERNET`.
  - `check-dependencies.sh <arquivo-de-dependências-resolvidas>` → exit 1 se algum `grupo:artefato` não casar com um prefixo de `allowed-dependencies.txt`. Lista inicial: `org.jetbrains.kotlin`, `org.jetbrains.kotlinx`, `org.jetbrains.compose`, `org.jetbrains.androidx`, `org.jetbrains.skiko`, `androidx.`, `com.google.devtools.ksp`, `org.jetbrains:annotations`, `com.google.guava:listenablefuture`, `io.github.takahirom.roborazzi` (só testes), `junit`, `org.hamcrest`. Bloqueados sempre, mesmo que casem com um prefixo: `com.google.firebase`, `com.google.android.gms`, `io.ktor`, `com.squareup.okhttp3`, `com.squareup.retrofit2`.
  - `android.yml` (ubuntu-latest, em push/PR): JDK 21 → `tools/ci/check-spdx.sh .` → autoteste dos scripts com as fixtures (cada uma deve dar exit 1) → `./gradlew spotlessCheck` → `./gradlew :shared:salon:jvmTest :shared:data:jvmTest :shared:ui:jvmTest :androidApp:testDebugUnitTest` → `./gradlew :androidApp:assembleDebug :androidApp:lintDebug` → `check-no-network.sh . <manifest mesclado>` → `./gradlew :androidApp:dependencies --configuration releaseRuntimeClasspath` filtrado para `grupo:artefato` → `check-dependencies.sh` → upload dos artefatos `loma-debug-apk` (o APK) e `screenshots` (PNGs do Roborazzi).
  - **Publicação do APK único** (job `publicar`, só em push na `main`, depois de tudo verde): `./gradlew :androidApp:assembleRelease` assinado com a chave dos Secrets (assinatura configurada em `androidApp/build.gradle.kts` lendo variáveis de ambiente; sem Secrets, o build local cai para debug) → atualiza o pré-lançamento de tag fixa `loma-mais-recente` com o arquivo de nome fixo `LOMA.apk` e as notas "versão X, commit Y". Link estável: `https://github.com/Emanuelrlourenco/LOMA-/releases/download/loma-mais-recente/LOMA.apk`. Release R8/minify desligado nesta fase.
  - `ios.yml` (macos-latest, em push/PR): `brew install xcodegen` → testes `iosSimulatorArm64Test` → `xcodegen` → `xcodebuild … build` (Task 7).

- [ ] **Step 0:** Gerar a chave `loma-release.jks` (RSA 4096, validade 30 anos, alias `loma`) com `keytool`, gravar os 4 Secrets no repositório com `gh secret set`, e entregar ao dono o arquivo e as senhas para guardar fora do GitHub (sem ela, uma versão nova não instala por cima e é preciso desinstalar, perdendo os dados que não estiverem em backup).
- [ ] **Step 1:** Escrever os scripts e as fixtures.
- [ ] **Step 2:** Rodar localmente cada script contra a sua fixture → exit 1 com a linha culpada impressa; contra o repositório → exit 0.
- [ ] **Step 3:** Escrever os 2 workflows e configurar o Spotless; `./gradlew spotlessApply` uma vez.
- [ ] **Step 4:** Commit e push: `ci: guardas sem rede, dependências permitidas, Android e iOS`. Verificar no GitHub que os dois workflows ficam verdes e que o artefato `loma-debug-apk` está disponível.

### Task 9: Aceite da fase 1 no celular

**Files:**
- Create: `docs/aceite/fase-1.md`

**Interfaces:**
- Consumes: artefato `loma-debug-apk` (Task 8).

- [ ] **Step 1:** Escrever o roteiro (em pt-BR, para o dono do projeto, passo a passo):
  1. No celular, abrir o link fixo `https://github.com/Emanuelrlourenco/LOMA-/releases/download/loma-mais-recente/LOMA.apk` (salvar nos favoritos) e instalar (permitir "fontes desconhecidas" só para o navegador usado). Para cada atualização futura: abrir o mesmo link e instalar por cima — os dados continuam.
  2. Abrir o LOMA: nenhum pedido de permissão aparece.
  3. Ver a barra inferior com Início, Agenda, Clientes e Financeiro, nas cores das telas aprovadas; tocar em cada aba.
  4. Na aba Financeiro, tocar "voltar" do Android → vai para Início; tocar "voltar" de novo → o app fecha.
  5. Girar o celular na aba Clientes → continua em Clientes.
  6. Ajustes do Android › Apps › LOMA › Permissões → "Nenhuma permissão solicitada"; Ajustes › Apps › LOMA › Uso de dados móveis/Wi-Fi não lista tráfego.
  7. Ativar o modo escuro do sistema → o app troca para o tema escuro, legível.
  8. Cronometrar do toque no ícone até a barra aparecer: menos de 1 s.
- [ ] **Step 2:** Commit: `docs: roteiro de aceite da fase 1`.
- [ ] **Step 3:** Verificar a atualização por cima: instalar o `LOMA.apk` de um commit, fazer um commit trivial, esperar o CI e instalar o novo `LOMA.apk` pelo mesmo link → o Android mostra "Atualizar" (não "conflito de assinatura") e Ajustes › Apps › LOMA mostra o `versionCode` novo.
