# LOMA — Fase 2A (Salão funcionando) — Plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fazer o LOMA ser usado no salão: serviços, clientes com ficha completa, atendimentos com status (agendar, confirmar, concluir com valor final, desmarcar, reabrir), Início "o dia do salão", detalhe do atendimento e "Confirmar pelo WhatsApp" — publicado como novo `LOMA.apk` que instala por cima.

**Architecture:** As regras ficam puras em `:shared:salon` (sem Room, sem Compose), testadas no JVM e no simulador iOS. `:shared:data` guarda tudo no Room (migração automática 1→2) e expõe `SalonRepository` com `Flow`s de tipos do domínio. Um novo módulo `:shared:platform` traz o que depende do sistema (abrir WhatsApp/SMS, guardar fotos). `:shared:ui` ganha um grafo de dependências manual (`LomaGraph` num `CompositionLocal`) e uma pilha de telas própria (`Route`) por cima das 4 abas; nada de ViewModel nem biblioteca de navegação.

**Tech Stack:** o da fase 1 + `kotlinx-datetime` 0.8.0 (datas; `Instant`/`Clock` vêm de `kotlin.time`) · Material 3 `DatePicker`/`TimePicker` · `ActivityResultContracts.PickVisualMedia` (fotos, sem permissão) · `Intent.ACTION_PICK` em `Phone.CONTENT_URI` (contatos, sem permissão).

**Spec:** `docs/design/2026-11-19-loma-design.md` (rev. 5) §7.4, §7.5 (definições de "previsto"/"recebido"), §7.7, §7.11, §7.12. Telas aprovadas (fonte da verdade visual, "faça exatamente assim"): `docs/design/telas/T-Inicio.dc.html`, `T-Atendimento.dc.html`, `T-Concluir.dc.html`, `T-Cliente.dc.html`.

**Fora desta fase (vêm na 2B e 2C):** Agenda Dia/Semana/Mês/Lista, busca geral, tela de Entrada e bloqueio por PIN/digital (2B); resumo diário, lembretes por atendimento, notificações e backup/restauração (2C). Financeiro (fase 3) e contas fixas no Início (fase 3) também ficam fora. As abas Agenda e Financeiro continuam com a tela "Em construção".

## Global Constraints

- Tudo da fase 1 continua valendo: sem rede, sem `INTERNET`, dependências só da lista permitida, cabeçalho SPDX, pt-BR padrão + `values-en`, dinheiro em centavos (`Money`), APK único assinado com a mesma chave.
- Nenhuma permissão nova. Fotos pelo seletor do sistema e contato pelo seletor de contatos, os dois **sem permissão**. WhatsApp detectado por `<queries>` para `com.whatsapp` e `com.whatsapp.w4b` (não é permissão).
- Telefone guardado só com dígitos e DDI; padrão `55` quando a pessoa digita sem DDI.
- Datas: início e fim do atendimento como `kotlin.time.Instant` + fuso IANA (`zoneId`) do momento do agendamento; telas mostram no fuso do aparelho. Horas em 24h ("13:00").
- Status: `AGENDADO`, `CONFIRMADO`, `CONCLUIDO`, `DESMARCADO`; motivo do desmarque: `CANCELOU`, `NAO_COMPARECEU`, `REMARCOU` (gravados como texto).
- Preço do serviço é **copiado** no agendamento; mudar o preço depois não muda atendimentos já marcados.
- Nada é somado e guardado: "previsto" e "recebido" são sempre calculados das consultas.
- Modelo padrão da mensagem (§7.7): `Oi {nome}! Tudo bem? Passando para confirmar seu horário {quando} às {hora} para {servicos}. Posso confirmar?`
- Textos e visual iguais às telas aprovadas; quando a tela não mostra um estado (vazio, erro), seguir o mesmo estilo (cartões 26 dp, títulos 30 sp ExtraBold, pílulas).
- `versionName` = `0.2.<GITHUB_RUN_NUMBER>` a partir desta fase.

## Decisões do dono (20/11/2026)

1. **Aniversário vindo dos contatos:** o seletor de contatos sem permissão entrega nome e telefone, mas **não** o aniversário (para isso o app teria de pedir a permissão de Contatos). **Decidido:** copiar nome e telefone; aniversário digitado na ficha. A importação em massa de aniversários (com permissão, sob demanda) fica na fase 4, como no design.
2. **`{nome}` na mensagem do WhatsApp:** **decidido:** só o primeiro nome ("Oi Maria!").

## Review Focus

1. **Telefones digitados de vários jeitos** — "(11) 98765-4321", "11987654321", "+55 11 98765-4321", "011 98765 4321", fixo de 8 dígitos, número estrangeiro com "+" — o WhatsApp abre no número certo; lixo ("abc", 5 dígitos) é recusado com aviso → testes na Task 2.
2. **Concluir perto da meia-noite** — atendimento das 23:30 concluído 00:10 com data do pagamento padrão — o "recebido" entra no dia do pagamento, e um "sem baixa" de ontem concluído hoje paga no dia do atendimento → testes nas Tasks 1 e 2.
3. **Apagar cliente com histórico** — concluídos continuam contando como "Cliente removida", futuros viram `DESMARCADO`, a foto some do armazenamento → teste na Task 4.
4. **Girar a tela ou o Android matar o app no meio de um formulário** (agendar, concluir, ficha) — o que foi digitado continua lá → teste na Task 7.
5. **Valor final digitado de vários jeitos** — "R$ 1.420,00", "280,5", "300", vazio, negativo — aceita os válidos e mostra a diferença para o esperado; vazio/negativo bloqueia "Concluir e lançar no caixa" com aviso → teste na Task 11.

---

### Task 1: Domínio dos atendimentos (`:shared:salon`)

**Files:**
- Modify: `gradle/libs.versions.toml` (`kotlinx-datetime = "0.8.0"`, biblioteca `kotlinx-datetime`), `shared/salon/build.gradle.kts` (dependência `api` em commonMain)
- Create: `shared/salon/src/commonMain/kotlin/app/loma/salon/Service.kt`, `Client.kt`, `Appointment.kt`, `AppointmentRules.kt`
- Test: `shared/salon/src/commonTest/kotlin/app/loma/salon/AppointmentRulesTest.kt`

**Interfaces:**
- Produces:
  - `data class Service(val id: Long = 0, val name: String, val durationMinutes: Int, val price: Money, val colorArgb: Long, val active: Boolean = true, val sortOrder: Int = 0)`
  - `data class Birthday(val day: Int, val month: Int, val year: Int? = null)`
  - `data class Client(val id: Long = 0, val name: String, val phoneDigits: String? = null, val birthday: Birthday? = null, val photoFile: String? = null, val allergies: String = "", val preferences: String = "", val notes: String = "", val createdAt: Instant, val archived: Boolean = false)`
  - `enum class AppointmentStatus { AGENDADO, CONFIRMADO, CONCLUIDO, DESMARCADO }`, `enum class CancelReason { CANCELOU, NAO_COMPARECEU, REMARCOU }`
  - `data class BookedService(val serviceId: Long?, val name: String, val durationMinutes: Int, val price: Money)`
  - `data class Appointment(val id: Long = 0, val clientId: Long?, val start: Instant, val end: Instant, val zoneId: String, val status: AppointmentStatus, val cancelReason: CancelReason? = null, val expected: Money, val finalValue: Money? = null, val completedAt: Instant? = null, val note: String = "", val services: List<BookedService>)`
  - `object AppointmentRules` com:
    - `fun book(clientId: Long, services: List<Service>, start: Instant, zone: TimeZone): Appointment` — fim = início + soma das durações; esperado = soma dos preços; `AGENDADO`; serviços copiados na ordem
    - `fun confirm(a: Appointment): Appointment` (só de `AGENDADO`)
    - `fun complete(a: Appointment, finalValue: Money, paidAt: Instant, note: String): Appointment` (de `AGENDADO`/`CONFIRMADO`; `finalValue` ≥ 0)
    - `fun unschedule(a: Appointment, reason: CancelReason): Appointment` (de `AGENDADO`/`CONFIRMADO`)
    - `fun reopen(a: Appointment): Appointment` (de `CONCLUIDO` → `CONFIRMADO`, limpa `finalValue` e `completedAt`)
    - `fun isWithoutSettlement(a: Appointment, now: Instant): Boolean` — aberto e `end < now`
    - `fun defaultPaidAt(a: Appointment, now: Instant, zone: TimeZone): Instant` — `now`; se o atendimento é "sem baixa" de **outro dia**, o meio-dia do dia do atendimento
    - `fun conflicts(candidate: Appointment, others: List<Appointment>): List<Appointment>` — abertos ou concluídos (não desmarcados), com outro id, cujo intervalo se sobrepõe (encostar não conta)
    - `fun repeatWeekly(a: Appointment, times: Int): List<Appointment>` — `times` cópias (sem a original) a +7, +14… dias no mesmo horário local do `zoneId`, status `AGENDADO`, id 0
  - Transição inválida lança `IllegalStateException`.

- [ ] **Step 1: Testes que falham** (`AppointmentRulesTest`; serviços Coloração 90 min R$ 180 e Escova 60 min R$ 70; fuso `America/Sao_Paulo`; 20/11/2026 13:00):
  - `bookSomaDuracaoEValor`: `end` = 15:30, `expected == Money.ofReais(250)`, status `AGENDADO`, `services.map { it.name } == listOf("Coloração", "Escova")`.
  - `precoCopiadoNaoMudaComOServico`: agendar, depois criar outro `Service` com o mesmo id e preço R$ 200 → `appointment.services[0].price == Money.ofReais(180)`.
  - `confirmarConcluirReabrir`: `confirm` → `CONFIRMADO`; `complete(…, Money.ofReais(280), paidAt, "Fez hidratação")` → `CONCLUIDO`, `finalValue == 280`, `completedAt == paidAt`; `reopen` → `CONFIRMADO`, `finalValue == null`, `completedAt == null`.
  - `transicoesInvalidas`: `confirm` de `CONCLUIDO`, `unschedule` de `DESMARCADO`, `reopen` de `AGENDADO`, `complete` com `Money(-1)` → `assertFailsWith<IllegalStateException>` (o último pode ser `IllegalArgumentException`; escolha um e use nos dois lugares).
  - `semBaixa`: atendimento 13:00–15:30 `AGENDADO`, now 15:31 → `true`; now 15:30 → `false`; `CONCLUIDO` → `false`.
  - `pagamentoPadraoSemBaixaDeOntem`: atendimento ontem 23:30–00:30, now hoje 10:00 → `defaultPaidAt` = ontem 12:00 local; atendimento hoje, now hoje 16:00 → `now`.
  - `conflitos`: 13:00–15:30 × 15:30–16:30 → vazio; × 15:00–16:00 → 1; × desmarcado 14:00 → vazio; consigo mesmo (mesmo id) → vazio.
  - `repetirSemanal`: `times = 3` → 3 itens, inícios +7/+14/+21 dias às 13:00 locais, inclusive atravessando a data de mudança de horário de qualquer fuso (use `Europe/Lisbon`, 20/03 → 27/03 → 03/04: hora local continua 13:00).
- [ ] **Step 2:** `./gradlew :shared:salon:jvmTest` → FAIL (tipos inexistentes).
- [ ] **Step 3:** Implementar os tipos e `AppointmentRules`.
- [ ] **Step 4:** Mesmo comando → PASS. (O job iOS roda o mesmo `commonTest` no simulador.)
- [ ] **Step 5:** Commit: `feat(salon): atendimentos, clientes e serviços com regras de status`.

### Task 2: Telefone, mensagem do WhatsApp, resumo do dia e totais da cliente

**Files:**
- Create: `shared/salon/src/commonMain/kotlin/app/loma/salon/PhoneBr.kt`, `ConfirmationMessage.kt`, `DaySummary.kt`, `ClientStats.kt`
- Test: `…/commonTest/kotlin/app/loma/salon/PhoneBrTest.kt`, `ConfirmationMessageTest.kt`, `DaySummaryTest.kt`, `ClientStatsTest.kt`

**Interfaces:**
- Produces:
  - `object PhoneBr { fun normalize(input: String): String?; fun format(digits: String): String }` — `normalize` devolve dígitos com DDI ou `null` se inválido; `format` mostra "(11) 98765-4321" para números brasileiros e "+<dígitos>" para estrangeiros.
  - `object ConfirmationMessage { const val DEFAULT_TEMPLATE: String; fun render(template: String, clientName: String, start: Instant, zone: TimeZone, today: LocalDate, services: List<String>, labels: WhenLabels): String }` com `data class WhenLabels(val today: String, val tomorrow: String)`; `{quando}` = "hoje"/"amanhã"/"dd/MM"; `{hora}` = "HH:mm"; `{servicos}` = nomes em minúsculas unidos por ", " e " e " no último ("coloração e escova"); `{nome}` = primeiro nome.
  - `data class DaySummary(val expected: Money, val received: Money)` e `fun DaySummary.Companion.of(appointments: List<Appointment>, day: LocalDate, zone: TimeZone): DaySummary` — previsto = esperado dos `AGENDADO`/`CONFIRMADO` que começam no dia; recebido = valor final dos `CONCLUIDO` com `completedAt` no dia.
  - `data class ClientStats(val lastVisit: Instant?, val visits: Int, val total: Money, val averageTicket: Money?, val cancellations: Int)` e `fun ClientStats.Companion.of(appointments: List<Appointment>): ClientStats` — visitas = concluídos; última visita = maior `start` concluído; ticket = total / visitas arredondado para baixo em centavos; desmarques = `DESMARCADO` com motivo `CANCELOU` ou `NAO_COMPARECEU` (remarcar não conta).

- [ ] **Step 1: Testes que falham:**
  - `PhoneBrTest.normaliza`: "(11) 98765-4321", "11987654321", "+55 11 98765-4321", "011 98765 4321", "55 11 98765-4321" → todos `"5511987654321"`; fixo "(11) 3456-7890" → `"551134567890"`; "+351 912 345 678" → `"351912345678"`; "abc", "12345", "" → `null`.
  - `PhoneBrTest.formata`: `"5511987654321"` → `"(11) 98765-4321"`; `"551134567890"` → `"(11) 3456-7890"`; `"351912345678"` → `"+351912345678"`.
  - `ConfirmationMessageTest.modeloPadrao`: Maria Souza, hoje 13:00, Coloração + Escova → `"Oi Maria! Tudo bem? Passando para confirmar seu horário hoje às 13:00 para coloração e escova. Posso confirmar?"`; amanhã → "amanhã"; daqui a 5 dias (25/11) → "25/11"; um serviço só → "para manicure."; três → "a, b e c".
  - `ConfirmationMessageTest.camposDesconhecidosFicamComoEstao`: modelo `"{nome} {xyz}"` → `"Maria {xyz}"`.
  - `DaySummaryTest.previstoERecebido`: hoje Maria 13:00 R$ 250 `AGENDADO`, Lorena 16:00 R$ 80 `CONFIRMADO`, Ana 09:00 concluída hoje R$ 70, Bia desmarcada R$ 100, Carla de ontem concluída hoje R$ 60 → `expected == 330`, `received == 130`.
  - `DaySummaryTest.meiaNoite`: concluído 00:10 de hoje (atendimento de ontem 23:30) → conta em hoje, não ontem.
  - `ClientStatsTest`: 12 concluídos somando R$ 2.860 → `visits == 12`, `total == 2860`, `averageTicket == Money(23833)`; sem concluídos → `averageTicket == null`, `lastVisit == null`; 1 cancelou + 1 não compareceu + 1 remarcou → `cancellations == 2`.
- [ ] **Step 2:** `./gradlew :shared:salon:jvmTest` → FAIL.
- [ ] **Step 3:** Implementar.
- [ ] **Step 4:** Mesmo comando → PASS.
- [ ] **Step 5:** Commit: `feat(salon): telefone, mensagem de confirmação, resumo do dia e totais da cliente`.

### Task 3: Banco — tabelas do salão e migração 1→2

**Files:**
- Create: `shared/data/src/commonMain/kotlin/app/loma/data/ClientEntity.kt`, `AppointmentEntity.kt`, `AppointmentServiceEntity.kt`, `SettingEntity.kt`, `ClientDao.kt`, `AppointmentDao.kt`, `SettingDao.kt`
- Modify: `LomaDatabase.kt` (versão 2, `autoMigrations = [AutoMigration(from = 1, to = 2)]`), `ServiceDao.kt` (`observeById`, `swapOrder`), `shared/data/build.gradle.kts` (depende de `:shared:salon`)
- Create (gerado no CI e copiado do log, como na fase 1): `shared/data/schemas/app.loma.data.LomaDatabase/2.json`
- Test: `shared/data/src/jvmTest/kotlin/app/loma/data/MigrationTest.kt`, `AppointmentDaoTest.kt`

**Interfaces:**
- Produces (colunas; tempos em epoch ms `Long`; enums como `TEXT`):
  - `clients`: `id` PK auto, `name`, `phoneDigits?`, `birthdayDay?`, `birthdayMonth?`, `birthdayYear?`, `photoFile?`, `allergies`, `preferences`, `notes`, `createdAtMs`, `archived`
  - `appointments`: `id` PK auto, `clientId?` (FK `clients.id` `ON DELETE SET NULL`, índice), `startMs` (índice), `endMs`, `zoneId`, `status`, `cancelReason?`, `expectedCents`, `finalCents?`, `completedAtMs?` (índice), `note`
  - `appointment_services`: `id` PK auto, `appointmentId` (FK `CASCADE`, índice), `serviceId?` (FK `services.id` `SET NULL`), `name`, `durationMinutes`, `priceCents`, `position`
  - `settings`: `key` PK, `value`
  - `AppointmentDao`: `@Transaction` `insertWithServices(a: AppointmentEntity, services: List<AppointmentServiceEntity>): Long`, `replaceWithServices(…)`, `observeBetween(fromMs: Long, toMs: Long): Flow<List<AppointmentWithServices>>` (por `startMs`), `observeCompletedBetween(fromMs, toMs)` (por `completedAtMs`), `observeById(id)`, `observeByClient(clientId)`, `observeOpenEndingBefore(nowMs)`, `delete(id)`, `unscheduleFutureOfClient(clientId: Long, nowMs: Long)` (põe `DESMARCADO`/`CANCELOU` nos abertos com `startMs >= nowMs`)
  - `ClientDao`: `insert`, `update`, `delete(id)`, `observeAll(includeArchived: Boolean)` (por nome, sem diferenciar maiúsculas), `observeById(id)`
  - `SettingDao`: `observe(key): Flow<String?>`, `put(SettingEntity)` (REPLACE)

- [ ] **Step 1: Testes que falham:**
  - `MigrationTest.migra1para2SemPerderServicos`: cria o banco com o schema 1 (`1.json` já versionado), insere um serviço, migra para 2 com `MigrationTestHelper` → o serviço continua e `clients`, `appointments`, `appointment_services`, `settings` existem.
  - `AppointmentDaoTest.servicosVoltamNaOrdem`: insere atendimento com 2 serviços (posições 1, 0) → `observeById` devolve na ordem 0, 1.
  - `AppointmentDaoTest.apagarClienteMantemHistorico`: cliente com 1 concluído; `ClientDao.delete` → o atendimento continua com `clientId == null`.
  - `AppointmentDaoTest.apagarAtendimentoApagaServicos`: `delete(id)` → nenhuma linha em `appointment_services` com aquele `appointmentId`.
  - `AppointmentDaoTest.desmarcaFuturosDaCliente`: 1 passado aberto, 1 futuro aberto, 1 futuro concluído → só o futuro aberto vira `DESMARCADO`.
- [ ] **Step 2:** `./gradlew :shared:data:jvmTest` → FAIL.
- [ ] **Step 3:** Implementar entidades, DAOs e a migração automática; no CI, copiar o `2.json` do log (passo "Schema do banco") e versionar.
- [ ] **Step 4:** Mesmo comando → PASS; `2.json` no repositório.
- [ ] **Step 5:** Commit: `feat(data): tabelas de clientes, atendimentos e ajustes (migração 1→2)`.

### Task 4: `SalonRepository` (domínio ↔ banco)

**Files:**
- Create: `shared/data/src/commonMain/kotlin/app/loma/data/SalonRepository.kt`, `Mappers.kt`, `SettingKeys.kt`
- Test: `shared/data/src/jvmTest/kotlin/app/loma/data/SalonRepositoryTest.kt`

**Interfaces:**
- Consumes: tipos da Task 1, DAOs da Task 3.
- Produces: `class SalonRepository(db: LomaDatabase, clock: Clock)` com:
  - Serviços: `fun services(includeInactive: Boolean = false): Flow<List<Service>>`, `suspend fun saveService(s: Service): Long`, `suspend fun moveService(id: Long, up: Boolean)`
  - Clientes: `fun clients(includeArchived: Boolean = false): Flow<List<Client>>`, `fun client(id: Long): Flow<Client?>`, `suspend fun saveClient(c: Client): Long`, `suspend fun setArchived(id: Long, archived: Boolean)`, `suspend fun deleteClient(id: Long): String?` (desmarca futuros, apaga a ficha, devolve o `photoFile` para a UI apagar o arquivo)
  - Atendimentos: `fun appointmentsOn(day: LocalDate, zone: TimeZone): Flow<List<Appointment>>` (começam no dia **ou** foram concluídos no dia), `fun appointmentsBetween(from: LocalDate, toExclusive: LocalDate, zone: TimeZone): Flow<List<Appointment>>`, `fun appointment(id: Long): Flow<Appointment?>`, `fun appointmentsOfClient(clientId: Long): Flow<List<Appointment>>` (mais recente primeiro), `fun withoutSettlement(): Flow<List<Appointment>>`, `suspend fun save(a: Appointment): Long`, `suspend fun saveAll(list: List<Appointment>)`, `suspend fun delete(id: Long)`
  - Ajustes: `fun setting(key: String): Flow<String?>`, `suspend fun putSetting(key: String, value: String)`; `object SettingKeys { const val PROFILE_NAME = "perfil.nome"; const val WHATSAPP_TEMPLATE = "whatsapp.modelo"; const val WHATSAPP_APP = "whatsapp.app" }`

- [ ] **Step 1: Testes que falham** (banco em memória no JVM, `Clock` fixo):
  - `idaEVoltaDoAtendimento`: `save(book(...))` e `appointment(id).first()` igual ao salvo (inclusive `zoneId`, serviços e `Money`).
  - `diaIncluiConcluidoHojeDeOntem`: atendimento de ontem concluído hoje aparece em `appointmentsOn(hoje)` e não em `appointmentsOn(ontem)`.
  - `apagarClienteDevolveFotoEDesmarcaFuturos`: cliente com `photoFile = "c1.jpg"`, 1 concluído passado, 1 agendado futuro → retorno `"c1.jpg"`; concluído com `clientId == null`; futuro `DESMARCADO`.
  - `moverServicoTrocaOrdem`: A(0), B(1); `moveService(B, up = true)` → ordem B, A.
  - `flowReageNaHora`: coletando `appointmentsOn(hoje)`, `unschedule` + `save` → próxima emissão mostra `DESMARCADO` (regra 3 do design: deduz na hora).
- [ ] **Step 2:** `./gradlew :shared:data:jvmTest` → FAIL.
- [ ] **Step 3:** Implementar.
- [ ] **Step 4:** Mesmo comando → PASS.
- [ ] **Step 5:** Commit: `feat(data): repositório do salão com fluxos do domínio`.

### Task 5: `:shared:platform` — WhatsApp/SMS e fotos

**Files:**
- Create: `shared/platform/build.gradle.kts` (KMP: jvm, android, iosArm64, iosSimulatorArm64; namespace `app.loma.platform`), `settings.gradle.kts` (incluir)
- Create: `shared/platform/src/commonMain/kotlin/app/loma/platform/MessagingLauncher.kt`, `PhotoStore.kt`, `WhatsAppLinks.kt`; `androidMain/…/AndroidMessagingLauncher.kt`, `AndroidPhotoStore.kt`; `iosMain/…/IosMessagingLauncher.kt`, `IosPhotoStore.kt`; `jvmMain` com versões falsas para testes
- Modify: `androidApp/src/main/AndroidManifest.xml` (`<queries>` com `<package android:name="com.whatsapp"/>` e `com.whatsapp.w4b`), `ManifestTest.kt`
- Test: `shared/platform/src/commonTest/kotlin/app/loma/platform/WhatsAppLinksTest.kt`

**Interfaces:**
- Produces:
  - `enum class MessagingApp { WHATSAPP, WHATSAPP_BUSINESS, SMS }`
  - `interface MessagingLauncher { fun available(): Set<MessagingApp>; fun open(app: MessagingApp, phoneDigits: String, text: String): Boolean }`
  - `object WhatsAppLinks { fun waMe(phoneDigits: String, text: String): String; fun sms(phoneDigits: String, text: String): String }` → `https://wa.me/5511987654321?text=<texto em percent-encoding UTF-8, espaço como %20>` e `smsto:+5511987654321` (o corpo vai no extra `sms_body`)
  - Android: `ACTION_VIEW` no link `wa.me` com `setPackage` do app escolhido; SMS com `ACTION_SENDTO`; `FLAG_ACTIVITY_NEW_TASK`; devolve `false` se não houver quem abra
  - iOS: `whatsapp://send?phone=…&text=…` via `UIApplication.sharedApplication.openURL(…, options = emptyMap<Any?, Any>(), completionHandler = null)`; SMS `sms:+…&body=…`; `available()` com `canOpenURL` (o `LSApplicationQueriesSchemes` entra no `Info.plist`)
  - `interface PhotoStore { suspend fun importPhoto(sourceUri: String): String?; suspend fun read(file: String): ByteArray?; suspend fun delete(file: String) }` — Android: copia do `content://` para `filesDir/clientes/<uuid>.jpg`, reduzindo o maior lado para 512 px (JPEG 85); iOS: guarda em Application Support com `isExcludedFromBackup` (seletor iOS chega na fase 7)
- [ ] **Step 1: Testes que falham:** `WhatsAppLinksTest`: texto `"Oi Maria! Às 13:00 & tal"` → `https://wa.me/5511987654321?text=Oi%20Maria%21%20%C3%80s%2013%3A00%20%26%20tal`; `sms` → `smsto:+5511987654321`. `ManifestTest.declaraConsultaAoWhatsApp`: `<queries>` contém os dois pacotes e continua sem nenhuma `uses-permission` nova.
- [ ] **Step 2:** `./gradlew :shared:platform:jvmTest :androidApp:testDebugUnitTest` → FAIL.
- [ ] **Step 3:** Implementar; incluir `:shared:platform` no CI (testes JVM e `iosSimulatorArm64Test`).
- [ ] **Step 4:** Mesmo comando → PASS; `check-no-network.sh` continua passando (o link `https://wa.me` é só texto entregue a outro app).
- [ ] **Step 5:** Commit: `feat(platform): abrir WhatsApp/SMS e guardar fotos das clientes`.

### Task 6: Grafo do app e pilha de telas

**Files:**
- Create: `shared/ui/src/commonMain/kotlin/app/loma/ui/LomaGraph.kt`, `shell/Route.kt`, `shell/Navigator.kt`; `androidApp/src/main/kotlin/app/loma/android/LomaApplication.kt`
- Modify: `shell/LomaApp.kt` (recebe `graph: LomaGraph`), `MainActivity.kt`, `MainViewController.kt` (iOS cria o grafo com o banco do iOS), `AndroidManifest.xml` (`android:name=".LomaApplication"` — nome completo `app.loma.android.LomaApplication`)
- Test: `shared/ui/src/jvmTest/kotlin/app/loma/ui/shell/NavigatorTest.kt`

**Interfaces:**
- Produces:
  - `class LomaGraph(val repository: SalonRepository, val messaging: MessagingLauncher, val photos: PhotoStore, val clock: Clock, val zone: () -> TimeZone)` e `val LocalLomaGraph = staticCompositionLocalOf<LomaGraph> { error("sem grafo") }`
  - `sealed interface Route` (serializável para `rememberSaveable` como `String`): `ClientDetail(id)`, `ClientEdit(id: Long?)`, `AppointmentDetail(id)`, `AppointmentEdit(id: Long?, clientId: Long?, rescheduleOf: Long?)`, `CompleteAppointment(id)`, `Settings`, `Services`, `ServiceEdit(id: Long?)`, `MessageTemplate`
  - `class Navigator` com `val stack: List<Route>`, `fun push(r: Route)`, `fun pop(): Boolean`, `fun replaceTop(r: Route)` e `@Composable fun rememberNavigator(): Navigator` (salvo com `rememberSaveable` como lista de strings)
  - `LomaApp`: pilha vazia → abas (fase 1); pilha com telas → a do topo, sem barra inferior; voltar do sistema faz `pop()` antes da regra das abas
- [ ] **Step 1: Testes que falham** (`NavigatorTest`): push A, push B, `pop()` → topo A; `pop()` com pilha vazia → `false`; salvar/restaurar pelo `SaveableStateRegistry` (mesma técnica do `LomaAppTest` da fase 1) com `AppointmentDetail(42)` no topo → continua `AppointmentDetail(42)`.
- [ ] **Step 2:** `./gradlew :shared:ui:jvmTest` → FAIL.
- [ ] **Step 3:** Implementar; Android cria `SalonRepository` uma vez em `LomaApplication`; abertura (fase 1) continua por cima de tudo.
- [ ] **Step 4:** PASS; `LomaAppTest` e `LomaIntroTest` da fase 1 continuam verdes.
- [ ] **Step 5:** Commit: `feat(ui): grafo de dependências e pilha de telas`.

### Task 7: Ajustes — perfil, serviços e modelo da mensagem

**Files:**
- Create: `shared/ui/src/commonMain/kotlin/app/loma/ui/settings/SettingsScreen.kt`, `ServicesScreen.kt`, `ServiceEditScreen.kt`, `MessageTemplateScreen.kt`; componentes comuns em `ui/components/` (`LomaTopBar`, `LomaTextField`, `MoneyField`, `PillButton`, `LomaCard`)
- Test: `shared/ui/src/jvmTest/kotlin/app/loma/ui/settings/ServicesScreenTest.kt`, `ui/components/MoneyFieldTest.kt`

**Interfaces:**
- Consumes: `SalonRepository`, `SettingKeys`, `Navigator`.
- Produces: `@Composable fun MoneyField(value: String, onValueChange: (String) -> Unit, label: String)` que aceita o que `Money.parseBrl` aceita e mostra erro abaixo quando o texto não vira valor; formulários guardam o texto em `rememberSaveable`.
- Ajustes: Perfil (nome para a saudação do Início), Serviços, Mensagem do WhatsApp (editor do modelo com os campos `{nome}`, `{quando}`, `{hora}`, `{servicos}` como chips que inserem o campo; botão "Restaurar padrão"; prévia ao vivo com uma cliente de exemplo).
- Serviços: lista com nome, duração ("1h30") e preço; setas ↑↓ para reordenar; chave "Ativo"; "+ Novo serviço" com nome, duração (minutos, passos de 5), preço e cor (paleta de 8 cores da marca).
- [ ] **Step 1: Testes que falham:**
  - `ServicesScreenTest.criarServicoApareceNaLista`: preencher "Coloração", 90, "180" → salvar → lista mostra "Coloração", "1h30", "R$ 180".
  - `ServicesScreenTest.desativarEscondeDoAgendamento`: desativar → `repository.services().first()` não contém; `services(includeInactive = true)` contém.
  - `MoneyFieldTest`: "R$ 1.420,00" sem erro; "abc" mostra o erro "Valor inválido".
  - `ServicesScreenTest.formularioSobreviveARecriacao`: digitar nome e preço, salvar/restaurar o estado → campos iguais.
- [ ] **Step 2–4:** `./gradlew :shared:ui:jvmTest` FAIL → implementar → PASS.
- [ ] **Step 5:** Commit: `feat(ui): ajustes de perfil, serviços e mensagem do WhatsApp`.

### Task 8: Clientes — lista, ficha e cadastro

**Files:**
- Create: `shared/ui/src/commonMain/kotlin/app/loma/ui/clients/ClientsScreen.kt`, `ClientDetailScreen.kt`, `ClientEditScreen.kt`, `ClientAvatar.kt`; `shared/ui/src/androidMain/kotlin/app/loma/ui/clients/Pickers.android.kt` (+ `expect` em commonMain, `actual` vazio em jvm/iOS)
- Test: `shared/ui/src/jvmTest/kotlin/app/loma/ui/clients/ClientsScreenTest.kt`, `ClientDetailScreenTest.kt`

**Interfaces:**
- Consumes: `SalonRepository`, `ClientStats`, `PhoneBr`, `PhotoStore`, `MessagingLauncher`.
- Produces:
  - `@Composable expect fun rememberContactPicker(onPicked: (PickedContact?) -> Unit): (() -> Unit)?` (`null` quando a plataforma não tem) com `data class PickedContact(val name: String, val phone: String?)`
  - `@Composable expect fun rememberPhotoPicker(onPicked: (String?) -> Unit): (() -> Unit)?` (devolve o `content://` escolhido)
- **Lista (aba Clientes):** campo de busca por nome ou telefone; ordenar por Nome / Última visita; arquivadas escondidas (filtro "Mostrar arquivadas"); botão "+ Nova cliente".
- **Ficha (T-Cliente):** iniciais ou foto; nome; "(11) 98765-4321 · aniversário 14/03"; botões WhatsApp (abre conversa sem texto), Ligar (`tel:`), Agendar (abre `AppointmentEdit(clientId)`); faixa de alergia em coral suave quando houver; cartões Última visita / Visitas / Total gasto / Ticket médio (de `ClientStats`); Preferências; Observações; Histórico (data, serviços, valor final, anotação; desmarcados com o motivo), com a linha "N desmarques" acima quando houver (§7.4). Menu: Editar, Arquivar/Desarquivar, Apagar (confirmação: "Apagar Maria Souza? Os atendimentos concluídos continuam no financeiro como "Cliente removida" e os futuros serão desmarcados.").
- **Cadastro:** "Escolher dos contatos" (preenche nome e telefone), foto (escolher/remover), nome (obrigatório), WhatsApp (mostra erro se `PhoneBr.normalize` der `null` e o campo não estiver vazio), aniversário (dia/mês, ano opcional), alergias e sensibilidades, preferências, observações.
- [ ] **Step 1: Testes que falham:**
  - `ClientsScreenTest.buscaPorNomeETelefone`: clientes Maria (11 98765-4321) e Lorena → buscar "lor" mostra só Lorena; buscar "98765" mostra só Maria.
  - `ClientsScreenTest.telefoneInvalidoBloqueiaSalvar`: nome "Ana", WhatsApp "123" → aviso "Número inválido" e nada salvo.
  - `ClientDetailScreenTest.totaisDaFicha`: Maria com 2 concluídos (R$ 250 e R$ 70) → "Visitas" 2, "Total gasto" R$ 320, "Ticket médio" R$ 160.
  - `ClientDetailScreenTest.alergiaEmDestaque`: alergias "amônia" → faixa com "Alergia:" visível; vazio → não existe.
  - `ClientDetailScreenTest.apagarPedeConfirmacao`: tocar Apagar → diálogo; cancelar → cliente continua; confirmar → volta para a lista, ela some e o `PhotoStore` falso registrou `delete` da foto dela.
- [ ] **Step 2–4:** FAIL → implementar → PASS.
- [ ] **Step 5:** Commit: `feat(ui): clientes com ficha completa, contatos e foto`.

### Task 9: Agendar e editar atendimento

**Files:**
- Create: `shared/ui/src/commonMain/kotlin/app/loma/ui/appointments/AppointmentEditScreen.kt`, `ClientChooser.kt`, `ServiceChooser.kt`
- Test: `shared/ui/src/jvmTest/kotlin/app/loma/ui/appointments/AppointmentEditScreenTest.kt`

**Interfaces:**
- Consumes: `AppointmentRules.book`, `conflicts`, `repeatWeekly`; `Route.AppointmentEdit(id, clientId, rescheduleOf)`.
- Campos: cliente (busca + "Nova cliente" que abre o cadastro e volta com ela escolhida), serviços (vários; soma duração e valor mostrados e editáveis), data (`DatePicker`), hora de início (`TimePicker`, 24h; fim calculado e mostrado), anotação, "Repetir toda semana" com quantidade (2–12 vezes).
- Conflito: aviso "Já existe atendimento nesse horário: 13:00 Maria Souza" acima do botão; deixa salvar.
- `rescheduleOf`: vem preenchido com cliente, serviços e anotação do original, sem data.
- Editar um atendimento existente só é possível enquanto aberto; salva por cima mantendo o status.
- [ ] **Step 1: Testes que falham:**
  - `agendarSomaEMostra`: escolher Maria, Coloração + Escova, 20/11 13:00 → mostra "13:00 – 15:30" e "R$ 250"; salvar → repositório tem 1 `AGENDADO` com `expected == 250`.
  - `valorEditavel`: trocar valor para "230" → salvo com `expected == 230`.
  - `avisaConflitoMasSalva`: já existe 14:00–15:00 → aviso visível; salvar funciona.
  - `repetir4Semanas`: "Repetir toda semana" 4 vezes → 4 atendimentos (original + 3 cópias).
  - `semClienteOuSemServicoNaoSalva`: botão "Agendar" desabilitado.
- [ ] **Step 2–4:** FAIL → implementar → PASS.
- [ ] **Step 5:** Commit: `feat(ui): agendar atendimento com serviços, conflito e repetição`.

### Task 10: Detalhe do atendimento e mudanças de status

**Files:**
- Create: `shared/ui/src/commonMain/kotlin/app/loma/ui/appointments/AppointmentDetailScreen.kt`, `UnscheduleSheet.kt`, `UndoSnackbar.kt`
- Test: `shared/ui/src/jvmTest/kotlin/app/loma/ui/appointments/AppointmentDetailScreenTest.kt`

**Interfaces:**
- Consumes: `AppointmentRules`, `ConfirmationMessage`, `MessagingLauncher`, `SettingKeys.WHATSAPP_TEMPLATE`/`WHATSAPP_APP`.
- Tela cheia marinho como **T-Atendimento**: status no topo ("Agendado · não confirmada", "Confirmado", "Concluído · R$ 280", "Desmarcado · cancelou"), nome em duas linhas, serviços, faixa de alergia, "13:00 – 15:30", "Sex, 20 nov", valor, lista de serviços com duração e preço, anotação.
- Ações por status: `AGENDADO` → "Confirmar pelo WhatsApp", "Confirmado", "Concluir", "Desmarcar"; `CONFIRMADO` → "Concluir", "Desmarcar"; `CONCLUIDO` → "Reabrir" (confirmação: "Reabrir? O valor sai dos ganhos realizados."); `DESMARCADO` → "Remarcar" (abre `AppointmentEdit(rescheduleOf = id)`). Menu: Editar (abertos), Apagar (concluído pede confirmação: "Apagar este atendimento? O valor sai dos ganhos realizados.").
- "Confirmar pelo WhatsApp": monta a mensagem com o modelo; se houver WhatsApp e Business e nenhum escolhido, pergunta uma vez ("Abrir com…") e grava em `WHATSAPP_APP`; sem nenhum, oferece SMS; cliente sem telefone → aviso "Cadastre o WhatsApp da cliente". Ao voltar ao app (`Lifecycle` `ON_RESUME` depois de abrir), mostra o cartão "A cliente confirmou?" com **Confirmado** / "Ainda não".
- Desmarcar: folha com os três motivos; "Remarcou" grava e já abre o agendamento preenchido.
- Toda mudança de status mostra "Desfazer" por 5 s (snackbar) que grava o atendimento anterior.
- [ ] **Step 1: Testes que falham** (`MessagingLauncher` falso que registra as chamadas):
  - `confirmarPeloWhatsAppMontaMensagem`: abre com o texto `ConfirmationMessage.render(...)` e o telefone da cliente; depois de `ON_RESUME` aparece "A cliente confirmou?"; tocar Confirmado → `CONFIRMADO`.
  - `perguntaQualWhatsAppUmaVez`: launcher com os dois → diálogo; escolher Business → `WHATSAPP_APP = "WHATSAPP_BUSINESS"`; segunda vez abre direto.
  - `semWhatsAppOfereceSms`: launcher só com SMS → botão vira "Confirmar por SMS".
  - `desmarcarComMotivoEDesfazer`: Desmarcar → "Cancelou" → `DESMARCADO`/`CANCELOU`; tocar Desfazer → volta `AGENDADO`.
  - `acoesPorStatus`: para cada status, os botões listados acima existem e os outros não.
- [ ] **Step 2–4:** FAIL → implementar → PASS.
- [ ] **Step 5:** Commit: `feat(ui): detalhe do atendimento, WhatsApp, desmarcar, reabrir e desfazer`.

### Task 11: Concluir atendimento

**Files:**
- Create: `shared/ui/src/commonMain/kotlin/app/loma/ui/appointments/CompleteAppointmentScreen.kt`
- Test: `shared/ui/src/jvmTest/kotlin/app/loma/ui/appointments/CompleteAppointmentScreenTest.kt`

**Interfaces:**
- Consumes: `AppointmentRules.complete`, `defaultPaidAt`, `MoneyField`.
- Folha como **T-Concluir**: "Concluir atendimento", "Maria Souza · Coloração + escova", "Valor esperado R$ 250,00", "Valor final" pré-preenchido com o esperado, ajuda "Ajuste se teve serviço extra ou desconto.", diferença ao vivo ("+R$ 30,00 em relação ao esperado" / "−R$ 20,00 …" / nada se igual), "Data do pagamento" (padrão `defaultPaidAt`, editável com `DatePicker`), "Anotação do atendimento (vai para a ficha)" pré-preenchida com a anotação atual, botões "Concluir e lançar no caixa" e "Cancelar".
- [ ] **Step 1: Testes que falham:**
  - `concluiComValorAjustado`: valor "280" → mostra "+R$ 30,00 em relação ao esperado"; concluir → `CONCLUIDO`, `finalValue == 280`, anotação gravada, volta para o detalhe com "Desfazer".
  - `valoresDigitados`: "R$ 1.420,00" e "280,5" habilitam o botão; "" e "-10" desabilitam com "Informe o valor final".
  - `semBaixaDeOntemPagaNoDiaDoAtendimento`: atendimento de ontem aberto → data do pagamento mostrada = ontem.
  - `sobreviveARecriacao`: digitar "300", salvar/restaurar o estado → continua "300".
- [ ] **Step 2–4:** FAIL → implementar → PASS.
- [ ] **Step 5:** Commit: `feat(ui): concluir atendimento com valor final e data do pagamento`.

### Task 12: Início — o dia do salão

**Files:**
- Create: `shared/ui/src/commonMain/kotlin/app/loma/ui/home/HomeScreen.kt`, `MonthStrip.kt`, `MiniMonth.kt`, `AppointmentCard.kt`, `HomeNotices.kt`
- Modify: `shell/LomaApp.kt` (aba Início usa `HomeScreen`; aba Clientes usa `ClientsScreen`)
- Test: `shared/ui/src/jvmTest/kotlin/app/loma/ui/home/HomeScreenTest.kt`; capturas Roborazzi `inicio-claro.png`, `inicio-escuro.png`, `ficha-claro.png`, `atendimento-claro.png`, `concluir-claro.png`

**Interfaces:**
- Consumes: `SalonRepository.appointmentsOn`, `appointmentsBetween`, `withoutSettlement`, `clients`, `setting(PROFILE_NAME)`, `DaySummary`.
- Igual a **T-Inicio**: avatar com a inicial (abre Ajustes); "Bom dia," / "Boa tarde," / "Boa noite," (05–12, 12–18, 18–05) e o nome do perfil (sem nome: só a saudação); pílulas de meses (2 antes, atual, 2 depois) roláveis; mini mês com DOM–SÁB, pontinho nos dias com atendimento aberto ou concluído, hoje e dia selecionado destacados; avisos (atendimentos sem baixa: "2 atendimentos sem baixa — concluir ou desmarcar", abre o primeiro; aniversário de cliente hoje: "Hoje é aniversário de Maria Souza" com atalho para a ficha e botão WhatsApp que abre com "Parabéns, {nome}! Feliz aniversário! 🎉" — §7.7); título "Atendimentos de hoje" (ou "Atendimentos de 25/11" quando outro dia está selecionado); "previsto R$ 330 · recebido R$ 70"; cartões grandes (nome, serviços, hora, duração · valor, status; botões WhatsApp e Confirmado quando `AGENDADO`) — tocar abre o detalhe; botão "+" agenda no dia selecionado; dia sem atendimentos: "Nenhum atendimento neste dia" + "Agendar".
- Desmarcados não aparecem nos cartões.
- [ ] **Step 1: Testes que falham** (`Clock` fixo 20/11/2026 10:00, fuso `America/Sao_Paulo`):
  - `saudacaoComNome`: perfil "Carla" → "Bom dia," e "Carla"; às 19:00 → "Boa noite,".
  - `previstoERecebido`: dados do `DaySummaryTest` → "previsto R$ 330 · recebido R$ 130".
  - `escolherOutroDia`: tocar 25 no mini mês → título "Atendimentos de 25/11" e só os do dia 25.
  - `avisoSemBaixa`: 2 sem baixa → aviso com "2 atendimentos sem baixa".
  - `desmarcadoSomeNaHora`: desmarcar pelo repositório → cartão some e o previsto cai na mesma composição seguinte.
  - Capturas: gravar as 5 telas da lista acima com dados de exemplo iguais aos das telas aprovadas (Maria Souza 13:00 Coloração + escova R$ 250, Lorena Dias 16:00 Manicure e pedicure R$ 80).
- [ ] **Step 2–4:** FAIL → implementar → PASS; comparar as capturas com os HTML aprovados e anotar no commit qualquer diferença visível.
- [ ] **Step 5:** Commit: `feat(ui): Início com o dia do salão`.

### Task 13: Aceite da fase 2A

**Files:**
- Create: `docs/aceite/fase-2a.md`
- Modify: `androidApp/build.gradle.kts` (`versionName = "0.2.$runNumber"`), `iosApp/iosApp/Info.plist` (`LSApplicationQueriesSchemes`: `whatsapp`)

- [ ] **Step 1:** Roteiro no celular, passo a passo: atualizar pelo mesmo link (dados da fase 1 continuam); Ajustes › Perfil com o nome → saudação; criar 3 serviços; criar cliente pelos contatos (nome e telefone vêm) com foto e alergia; agendar hoje com 2 serviços; ver no Início o cartão, o "previsto"; Confirmar pelo WhatsApp (abre a conversa certa com a mensagem) → voltar → "Confirmado"; Concluir com valor diferente → "recebido" muda; Reabrir e Desfazer; Desmarcar outro com "Remarcou" → novo horário; apagar uma cliente de teste; girar a tela no meio do "Concluir"; conferir que nenhuma permissão foi pedida.
- [ ] **Step 2:** Commit: `docs: roteiro de aceite da fase 2A`.
