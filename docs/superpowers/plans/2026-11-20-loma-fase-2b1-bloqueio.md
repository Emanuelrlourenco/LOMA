# LOMA — Fase 2B parte 1: bloqueio por PIN e digital — plano

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans. Steps use `- [ ]`.

**Goal:** PIN de 4 dígitos + digital/Face ID, "Esqueci o PIN" pelo bloqueio do celular, Ajustes › Segurança.

**Architecture:** hash e sal em `:shared:platform` (expect/actual), armazenamento síncrono
(`KeyValueStore`) e autenticação do aparelho (`DeviceAuth`) por plataforma; regras do bloqueio em
`AppLock` (`:shared:ui`, sem Compose, testável com relógio); telas em `ui/lock` e `ui/settings`.

**Tech Stack:** Kotlin 2.4.20, CMP 1.12.2, `androidx.biometric:biometric:1.4.0-rc01`, LocalAuthentication.

**Spec:** `docs/superpowers/specs/2026-11-20-loma-bloqueio-design.md` (+ §7.12a e T-Bloqueio).

## Global Constraints
- Sem rede; nenhuma permissão perigosa (ManifestTest); dependências só da lista permitida.
- PBKDF2-HMAC-SHA256, sal 16 bytes, 120.000 iterações (testes podem usar menos), comparação em tempo constante.
- Formato salvo: `pbkdf2-sha256$<iter>$<sal base64>$<hash base64>`.
- Chaves (`KeyValueStore`, arquivo `loma_seguranca`): `pin`, `digital` ("1"), `tempo` (segundos: 0, 60, 300; padrão 60), `erros`, `espera_ate` (epoch ms).
- 5 erros seguidos → espera de 30 s; a contagem sobrevive a fechar o app.
- Tela de bloqueio: cores e medidas de T-Bloqueio; strings pt e en.

## Review Focus
1. App aberto do zero com PIN ligado nunca mostra o Início antes do bloqueio.
2. Voltar do segundo plano antes/depois do tempo (0, 60, 300 s) e com relógio que andou para trás.
3. Digital cancelada ou indisponível (sem cadastro, bloqueada por tentativas) volta ao PIN sem travar.
4. "Esqueci o PIN" cancelado no meio não desbloqueia nem apaga o PIN antigo.
5. Celular sem bloqueio de tela: aviso ao criar o PIN e "Esqueci" explica que não dá.

---

### Task 1: plataforma — PBKDF2, armazenamento e autenticação do aparelho
**Files:** `shared/platform/src/commonMain/.../Pbkdf2.kt`, `KeyValueStore.kt`, `DeviceAuth.kt`;
actuals em `androidMain`, `jvmMain`, `iosMain`; `commonTest/.../Pbkdf2Test.kt`; `gradle/libs.versions.toml`, `shared/platform/build.gradle.kts`.

**Interfaces — Produces:**
- `expect fun pbkdf2Sha256(password: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int = 32): ByteArray`
- `expect fun secureRandomBytes(size: Int): ByteArray`
- `interface KeyValueStore { fun get(key: String): String?; fun put(key: String, value: String?) }`;
  `AndroidKeyValueStore(context)` (SharedPreferences `loma_seguranca`, `commit()`), `IosKeyValueStore()` (NSUserDefaults, chaves com prefixo `loma.seguranca.`).
- `enum class AuthKind { BIOMETRIC, DEVICE }`;
  `interface DeviceAuth { fun biometricAvailable(): Boolean; fun deviceSecure(): Boolean; suspend fun authenticate(kind: AuthKind, title: String, subtitle: String, cancel: String): Boolean }`.
  Android: `AndroidDeviceAuth(context)` com `var activity: FragmentActivity?` (fraca); BIOMETRIC = `BIOMETRIC_WEAK` + botão negativo `cancel`; DEVICE = `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` (sem botão negativo). iOS: `IosDeviceAuth()` com `LAContext` (`DeviceOwnerAuthenticationWithBiometrics` / `DeviceOwnerAuthentication`).

- [ ] **RED:** `Pbkdf2Test` — vetor RFC 7914 §11 (`"passwd"`, `"salt"`, c=1, 64 bytes → `55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc49ca9cccf179b645991664b39d77ef317c71b845b1e30bd509112041d3a19783`);
  (`"password"`, `"salt"`, c=1, 32 bytes → `120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b`);
  `secureRandomBytes(16)` tem 16 bytes e duas chamadas diferem. Rodar `:shared:platform:jvmTest` → falha de compilação.
- [ ] **GREEN:** actuals (javax.crypto `PBKDF2WithHmacSHA256` + `SecureRandom` em Android/JVM; `CCKeyDerivationPBKDF` + `SecRandomCopyBytes` no iOS), `KeyValueStore`, `DeviceAuth`. jvm/iOS no CI verdes.
- [ ] Commit.

### Task 2: `AppLock` — regras do bloqueio
**Files:** `shared/ui/src/commonMain/kotlin/app/loma/ui/lock/AppLock.kt`, `shared/ui/src/jvmTest/.../lock/AppLockTest.kt`, `TestGraph.kt` (fakes).

**Interfaces — Produces:**
```kotlin
data class LockState(val enabled: Boolean, val locked: Boolean, val biometric: Boolean, val timeoutSeconds: Int)
sealed interface PinCheck { data object Ok; data class Wrong(val attemptsLeft: Int); data class Wait(val seconds: Int) }
class AppLock(store: KeyValueStore, clock: Clock, iterations: Int = 120_000) {
    val state: StateFlow<LockState>          // começa locked = enabled (abrir do zero bloqueia)
    suspend fun setPin(pin: String)          // liga, grava hash, zera erros, desbloqueia
    suspend fun check(pin: String): PinCheck // Ok desbloqueia
    fun disable(); fun setBiometric(on: Boolean); fun setTimeout(seconds: Int)
    fun unlock()                             // depois da digital
    fun onBackground(); fun onForeground()
}
```
`TestGraph`: `FakeKeyValueStore`, `FakeDeviceAuth(biometric = true, secure = true, result = true)` com `calls: MutableList<AuthKind>`, `lock = AppLock(store, clock, iterations = 1_000)`; `LomaGraph` ganha `lock: AppLock` e `deviceAuth: DeviceAuth`.

- [ ] **RED:** `AppLockTest`: `semPinNaoBloqueia`; `abrirDoZeroComPinBloqueia` (novo `AppLock` sobre o mesmo store); `pinGuardadoComoHash` (valor começa `pbkdf2-sha256$1000$` e não contém "1234"); `pinCertoDesbloqueia`; `cincoErrosEsperam30s` (4× Wrong com attemptsLeft 4..1, 5º → Wait(30), certo durante a espera → Wait, após 30 s → Ok); `esperaSobreviveAReabrir`; `segundoPlanoRespeitaTempo` (60 s: volta em 59 s livre, em 61 s bloqueado; 0: bloqueia já; relógio para trás → bloqueia); `desligarApagaPin`.
- [ ] **GREEN** e commit.

### Task 3: tela de bloqueio e o app bloqueado
**Files:** `ui/lock/PinPad.kt`, `ui/lock/LockScreen.kt`, `ui/lock/PinSetup.kt`, `shell/LomaApp.kt`, `MainActivity.kt`, `LomaApplication.kt`, `MainViewController.kt`, `iosApp/iosApp/Info.plist`, strings; testes `jvmTest/.../lock/LockScreenTest.kt`.

**Interfaces — Produces:** `PinPad(length: Int, entered: Int, onDigit: (Char) -> Unit, onBack: () -> Unit, error: String?)` (tags `pin_key_0`…`pin_key_9`, `pin_key_back`, `pin_dots`);
`PinSetup(onDone: (String) -> Unit, onCancel: (() -> Unit)?)` (digitar e confirmar; tags `pin_setup_title`, `pin_setup_error`);
`LockScreen()` (tags `lock_screen`, `lock_biometric`, `lock_forgot`, `lock_message`).

- [ ] **RED:** `comPinAbreBloqueado` (`lock_screen` existe e `tab_inicio` não); `pinCertoDesbloqueia`; `pinErradoAvisa` ("PIN incorreto. Restam 4 tentativas."); `digitalAbreSozinhaEDesbloqueia` (FakeDeviceAuth chamado 1× com BIOMETRIC); `digitalCanceladaFicaNoPin` (result=false → continua bloqueado, botão chama de novo); `esqueciPinCriaOutro` (DEVICE ok → PinSetup → novo PIN vale, antigo não); `esqueciCanceladoMantemPin`; `semBloqueioNoCelularExplica` (secure=false → mensagem, sem chamar DeviceAuth).
- [ ] **GREEN:** gate em `LomaApp` (bloqueado → só `LockScreen`; Voltar não é tratado → sai do app); avatar com foto do perfil; MainActivity vira `FragmentActivity`, chama `lock.onBackground/onForeground` em `onStop/onStart` e entrega a activity ao `AndroidDeviceAuth`; iOS observa `UIApplicationDidEnterBackground/WillEnterForeground`; `NSFaceIDUsageDescription`. Commit.

### Task 4: Ajustes › Segurança
**Files:** `ui/settings/SecurityScreen.kt`, `ui/settings/SettingsScreen.kt`, `shell/Route.kt`, `shell/LomaApp.kt`, strings; `jvmTest/.../settings/SecurityScreenTest.kt`.

**Interfaces — Produces:** `Route.Security`, `Route.PinChange(mode: String)` (`"new"`, `"change"`, `"disable"`), menu `menu_security`;
tags `security_pin_switch`, `security_biometric_switch`, `security_timeout_0|60|300`, `security_change_pin`, `security_warning`.

- [ ] **RED:** `ligarPinPedeDuasVezes` (confirmação diferente → "Os PINs não são iguais"; iguais → ligado e volta); `avisoSemBloqueioDoCelular`; `chaveDigitalSoComDigital`; `tempoSalva`; `trocarPinPedeOAtual`; `desligarPedePinAtual`.
- [ ] **GREEN** e commit.

### Task 5: capturas, aceite e versão
- [ ] Capturas `bloqueio` (Carla, 2 dígitos) e `seguranca`; conferir com T-Bloqueio.
- [ ] `docs/aceite/fase-2b-bloqueio.md`; `versionName = "0.3.$runNumber"`.
- [ ] Suíte completa verde (Android e iOS); revisão final; PR.
