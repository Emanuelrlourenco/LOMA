# LOMA — Design do app de calendário (Android + iOS)

*Data: 2026-11-19 · Status: rascunho para aprovação (rev. 4, 2026-11-20: iOS incluído, revisado)*

## 1. Intenção e o porquê

Um app de calendário **próprio** (código escrito do zero, não um fork), para **Android e iOS**, que seja uma alternativa limpa e simples aos calendários inchados: **sem anúncios, open-source, sem rastreamento e sem permissões intrusivas**. O Fossify Calendar (Android, GPL-3.0) é a referência funcional e de arquitetura, mas nenhum código dele é copiado.

A privacidade é o argumento central do produto: **o app nunca acessa a rede**. Toda sincronização com provedores externos acontece pelas contas do próprio sistema operacional (Android: `CalendarContract`; iOS: EventKit), nunca pelo app. No Android isso é verificável: o app não declara a permissão `INTERNET`. No iOS, que não tem permissão de rede, a garantia vem de não haver nenhum código ou dependência de rede (checado no CI) e do selo de privacidade da App Store "Dados não coletados".

O dono do projeto quer poder, no futuro, lançar uma versão fechada/comercial — por isso a licença é permissiva (§5).

## 2. Metas

- Criar e editar eventos com horário, duração, dia inteiro, fuso horário, local (texto), descrição, cor e **múltiplos lembretes**.
- **Recorrência avançada** compatível com RFC 5545 (RRULE): diária/semanal/mensal/anual, intervalo, contagem, data final, "última sexta do mês" (`BYDAY=-1FR`), "último dia do mês" (`BYMONTHDAY=-1`), "2ª e 4ª terça", exceções (EXDATE) e edição de "só esta / esta e as seguintes / todas".
- **Sincronizar com Google Agenda, Outlook e Nextcloud** lendo e escrevendo nas agendas do sistema.
  - Android: Google e Outlook pelas contas do Android; Nextcloud via DAVx5 (app externo, gratuito no F-Droid, open-source).
  - iOS: iCloud, Google, Outlook/Exchange e CalDAV (Nextcloud) são configurados nos Ajustes do iPhone; nenhum app extra.
- **Importar feriados** (por país e região, offline) e **aniversários** (dos contatos) com poucos toques.
- **Importar/exportar `.ics`**.
- Visualizações **Dia, Semana, Mês, Ano** e uma lista **Agenda** (próximos eventos), com navegação por deslize e botão "Hoje".
- Visual **Material 3**. No Android: Material You com cores dinâmicas (12+). No iOS: o mesmo Material 3 com paleta fixa e destaque escolhível, respeitando comportamentos nativos (gesto de voltar, física de rolagem, tamanho de fonte do sistema).
- Tema claro/escuro/sistema e opção de preto AMOLED.
- Personalização: cor por agenda e por evento, cor de destaque, primeiro dia da semana, formato 12/24h, números de semana, **som de notificação por agenda**.
- **Widgets** de tela inicial bonitos e configuráveis nas duas plataformas (§7.7).
- Busca de eventos.
- Idiomas: **pt-BR e inglês** desde o início; strings 100% em recursos para facilitar traduções da comunidade.

## 3. Não-metas (fora do escopo desta versão)

- Tarefas/to-do.
- Cliente CalDAV/Google/Outlook embutido no app (pode virar opção futura).
- Convidados/RSVP para eventos locais. Em eventos sincronizados, convidados existentes são **preservados intactos**, mas não editáveis.
- Desktop, web, Apple Watch, iPad com layout próprio (iPad roda o layout de celular ampliado).
- Clima, mapas, anexos, videoconferência.
- Visual "Cupertino" diferente no iOS (o app é Material 3 nas duas plataformas).

## 4. Anti-metas (falha mesmo que "funcione")

- Qualquer acesso à rede. Android: `INTERNET` não pode aparecer no manifest final. Ambas: nenhuma dependência de rede (Ktor, OkHttp, URLSession em código próprio etc.) — checado no CI por lista de dependências permitidas e busca no código.
- Qualquer biblioteca de analytics, crash reporting, anúncios, Google Play Services/Firebase ou SDKs equivalentes da Apple que enviem dados.
- Pedir qualquer permissão ao abrir o app pela primeira vez.
- Perder ou corromper dados de agendas sincronizadas (o app nunca apaga uma agenda do sistema, nem reescreve campos que não entende).
- Lembretes que não disparam ou disparam em duplicidade sem que o usuário possa resolver.
- Interface lenta ou poluída: abrir o app até o mês visível em < 1 s num aparelho médio, nas duas plataformas.

## 5. Restrições

- **Tecnologia:** Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP, iOS estável desde 1.8.0) + Material 3. Widgets: Jetpack Glance no Android; WidgetKit/SwiftUI (Swift) no iOS. Datas com `kotlinx-datetime` no código comum (`java.time` não existe no iOS).
- **Versões mínimas:** Android 8.0 (API 26) — confirmado pelo usuário. iOS 17 (widgets interativos via App Intents; cobre iPhone XS/XR em diante).
- **Licença:** Apache-2.0 (escolhida pelo usuário). Cabeçalho SPDX `Apache-2.0` em todos os arquivos, `LICENSE` + `NOTICE` na raiz. Dependências só com licenças permissivas (Apache-2.0, MIT, BSD) — nada de GPL/LGPL, para não impedir a versão fechada. Contribuições externas entram sob Apache-2.0 (cláusula 5) com DCO (`Signed-off-by`).
- **Distribuição:** Android — F-Droid (sem dependências proprietárias, build reproduzível) e GitHub Releases; Play Store opcional. iOS — App Store e TestFlight; exige conta Apple Developer (US$ 99/ano, do dono do projeto). Identificadores: Android `app.loma.calendar`; iOS bundle `app.loma.calendar` + extensão de widget `app.loma.calendar.widgets` + App Group `group.app.loma.calendar` (provisórios).
- **Build:** o ambiente onde o código é escrito não alcança o SDK do Android nem tem Xcode. Tudo compila e testa no **GitHub Actions**: runner Ubuntu para Android (APK debug/release como artefatos), runner macOS para iOS (build do simulador + testes; mais tarde, upload para TestFlight). O usuário não tem Mac: a assinatura iOS e o envio ao TestFlight também rodam no CI (§9).
- **Permissões** (todas sob demanda, com explicação antes do pedido do sistema):

| Permissão | Android | iOS | Quando |
|---|---|---|---|
| Agendas do sistema | `READ_CALENDAR`/`WRITE_CALENDAR` | Acesso total a calendários (`NSCalendarsFullAccessUsageDescription`) | Ao ligar "Sincronizar agendas do sistema" |
| Contatos | `READ_CONTACTS` | `NSContactsUsageDescription` | Ao tocar "Importar aniversários" |
| Notificações | `POST_NOTIFICATIONS` (13+) | Autorização de notificações | Ao salvar o 1º evento com lembrete |
| Alarme exato | `USE_EXACT_ALARM` (13+, sem diálogo) / `SCHEDULE_EXACT_ALARM` (12; se negado, inexato + aviso) | — (notificações locais já são na hora) | Instalação |
| Reagendar após reinício | `RECEIVE_BOOT_COMPLETED` | — (o iOS guarda as notificações agendadas) | Instalação |

## 6. Abordagens consideradas

**6.1 Como sincronizar** — escolhida pelo usuário: **pelas contas do sistema** (sem rede no app). Rejeitada: cliente CalDAV/OAuth próprio (exige rede, credencial OAuth verificada pelo Google, API à parte para Outlook, ~3× mais código).

**6.2 Como cobrir Android e iOS** — escolhida pelo usuário: **A**.

| | Abordagem | Prós | Contras |
|---|---|---|---|
| **A ✅** | KMP + Compose Multiplatform | Aproveita todo o design Kotlin/Compose; Android 100% nativo; Room, ViewModel e kizitonwose/Calendar já são multiplataforma | Motor de recorrência e ICS precisam ser escritos em Kotlin comum; camadas por plataforma (calendário do sistema, lembretes, widgets) |
| B | Flutter | Ecossistema multiplataforma mais antigo | Refazer o design; Android menos nativo; widgets e calendário do sistema continuam nativos |
| C | Dois apps nativos (Kotlin + Swift) | Melhor sensação nativa em cada um | ~2× trabalho e manutenção |

**6.3 Bases prontas avaliadas e descartadas:** Cal.diy, Zero Calendar, Someday (são web/nuvem, não apps de celular) e os pacotes Flutter kalender e calendar_view (exigiriam Flutter).

## 7. Como funciona

### 7.1 Módulos

```
:shared:model       modelos puros (Event, EventInstance, CalendarInfo, Reminder, RecurrenceRule)
:shared:recurrence  motor RRULE próprio (commonMain), sem dependências — §7.3
:shared:ical        parser/escritor ICS próprio (commonMain) — §7.9
:shared:data        Room KMP (BundledSQLiteDriver), repositórios, backup, feriados
:shared:platform    interfaces das capacidades de cada SO + implementações androidMain/iosMain — §7.2
:shared:ui          telas Compose Multiplatform, ViewModels, tema, strings (compose resources)
:androidApp         Activity, Glance widgets, receivers, WorkManager, manifest
iosApp/ (Xcode)     ponto de entrada SwiftUI que hospeda o Compose, extensão WidgetKit (Swift)
```

Fluxo: UI → ViewModel → `EventRepository` → fontes de dados. A UI só conhece `EventInstance` (uma ocorrência concreta, com início/fim já resolvidos). Grades de **Mês**, **Ano** e faixa de dias da **Semana** usam **kizitonwose/Calendar** (artefato `compose-multiplatform`, MIT); a grade de horas de Dia/Semana é escrita no projeto.

### 7.2 Capacidades por plataforma (interfaces em `:shared:platform`)

| Interface | Android | iOS |
|---|---|---|
| `SystemCalendarSource` | `CalendarContract` (Instances, Events, Reminders, Colors) | EventKit (`EKEventStore`, `EKEvent`, `EKAlarm`, `EKRecurrenceRule`) |
| `ReminderScheduler` | `AlarmManager` exato + notificação | `UNUserNotificationCenter` com janela de 64 (§7.5) |
| `BirthdayReader` | `ContactsContract` | `CNContactStore` |
| `FileAccess` (import/export/backup) | Storage Access Framework | `UIDocumentPicker` + security-scoped bookmarks |
| `BackgroundWork` | WorkManager | BGTaskScheduler (melhor esforço) + execução ao abrir o app |
| `WidgetBridge` | Glance lê o banco direto e é atualizado por `updateAll` | app grava um snapshot JSON (eventos locais + configurações) no App Group e chama `WidgetCenter.reloadAllTimelines()`; eventos sincronizados o widget lê do EventKit (§7.7) |
| `PlatformTheme` | cores dinâmicas (12+) | paleta fixa |

### 7.3 Duas fontes de eventos atrás de um repositório

```
                 EventRepository
                /               \
     LocalSource (Room KMP)   SystemSource (CalendarContract | EventKit)
     - agendas do app         - agendas das contas do sistema
     - feriados               - Google / Outlook / Nextcloud / iCloud
     - aniversários           - expansão de recorrência feita pelo sistema
     - expansão própria
       via :shared:recurrence
```

- **LocalSource:** Room com tabelas `calendars`, `events` (RRULE como texto RFC 5545, EXDATEs, fuso IANA), `event_overrides` (ocorrências editadas individualmente, ligadas por `original_event_id` + `original_start`) e `reminders`. Mesmo esquema nas duas plataformas.
- **SystemSource — Android:** lê `CalendarContract.Instances` por intervalo de datas e escreve em `Events`/`Reminders`. Exceções nativas (`ORIGINAL_ID`, `ORIGINAL_INSTANCE_TIME`) para "só esta ocorrência". Observa mudanças com `ContentObserver`.
- **SystemSource — iOS:** lê com `predicateForEvents(withStart:end:calendars:)` (o EventKit já expande recorrências; cada consulta cobre no máximo 4 anos, então busca e lista Agenda consultam em blocos) e escreve `EKEvent` com `EKSpan.thisEvent`/`.futureEvents`. Observa `EKEventStoreChanged`. Regras de recorrência são convertidas RRULE ↔ `EKRecurrenceRule` (dias da semana com `weekNumber` −1 cobrem "última sexta"; `setPositions` cobre BYSETPOS). Regras que o EventKit não representa são **somente leitura** em eventos do iOS: o editor mostra a regra em texto e oferece "substituir por nova regra".
- **Cor de evento sincronizado:** Android — só a paleta do provedor (`CalendarContract.Colors` via `EVENT_COLOR_KEY`), oculto se não houver paleta. iOS — o EventKit não tem cor por evento; o seletor de cor fica oculto para eventos do sistema (vale a cor da agenda). Sem tabela paralela no Room.
- O usuário escolhe, por agenda do sistema, se ela aparece (filtro) — **não há cópia dos dados sincronizados para o Room**; isso elimina toda uma classe de bugs de conflito.
- Agenda padrão para novos eventos: configurável; se a sincronização estiver desligada, é a agenda local "Pessoal".
- Mover um evento entre fonte local e do sistema = criar no destino + apagar na origem (com confirmação).

### 7.4 Recorrência

- Representação canônica: **RRULE RFC 5545**, para que eventos locais exportem/importem sem perda e possam ser movidos para agendas sincronizadas.
- **Motor próprio em `:shared:recurrence`** (a lib-recur é só Java e não roda no iOS). Escopo: FREQ (DAILY…YEARLY), INTERVAL, COUNT, UNTIL, BYDAY (com ordinal, ex. `-1FR`, `2TU`), BYMONTHDAY (incl. negativos), BYMONTH, BYSETPOS, WKST, EXDATE, RDATE; expansão por intervalo de datas em fuso IANA. Regras fora desse escopo (BYWEEKNO, BYYEARDAY, BYHOUR/BYMINUTE/BYSECOND) são **preservadas como texto** e expandidas só pela data inicial (o evento aparece uma vez com aviso "regra não suportada").
- Editor de recorrência na UI: presets ("Não repete", "Todo dia", "Toda semana em <dia>", "Todo mês no dia <n>", "Todo mês na <última sexta>", "Todo ano") + tela "Personalizado" com frequência, intervalo, dias da semana, modo mensal (dia do mês × "n-ésimo/último <dia da semana>"), fim (nunca / em data / após N vezes). Resumo em linguagem natural ("Toda última sexta-feira do mês, até 31/12/2027").
- Edição de evento recorrente: "Só este", "Este e os seguintes" (divide a série: encerra a original com UNTIL e cria nova), "Todos".
- Testes obrigatórios (rodam em JVM e no simulador iOS): última sexta, 31 de cada mês (meses sem 31 são pulados, conforme RFC), 29/02, mudança de horário de verão (o horário local se mantém), UNTIL em fuso diferente, COUNT com EXDATE, BYSETPOS, e uma bateria de casos de conformidade derivados dos exemplos da própria RFC 5545 (§3.8.5.3).

### 7.5 Lembretes e notificações

**Android**
- Agendador próprio para **todas** as fontes (locais e sincronizadas): `AlarmManager.setExactAndAllowWhileIdle` (ou inexato, se negado) para o próximo lembrete; ao disparar, mostra a notificação e agenda o seguinte. Reagenda em `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`, ao editar eventos e em mudanças observadas no `CalendarContract`. WorkManager diário como rede de segurança.
- **Duplicatas:** se o app Google Agenda também estiver instalado, ele também notifica. Configuração por agenda "Lembretes desta agenda: pelo LOMA / pelo outro app", com dica na primeira sincronização.
- **Som por agenda:** no Android 8+ o som pertence ao canal de notificação. Cada agenda ganha um canal próprio ("Lembretes – Trabalho"); o seletor de som abre a tela de canal do sistema.

**iOS**
- **Eventos locais:** ficam no Room (não numa agenda local do EventKit, que exigiria a permissão de calendário mesmo para quem não sincroniza nada). O LOMA agenda notificações locais com `UNUserNotificationCenter`. Como o iOS aceita **no máximo 64 notificações pendentes por app**, o agendador mantém sempre os 64 lembretes mais próximos. **O iOS não acorda o app quando uma notificação é entregue**, então a janela é recalculada ao abrir o app, ao editar eventos, quando o usuário toca uma ação da notificação (Adiar/Dispensar acordam o app em segundo plano) e no BGTask (melhor esforço). A tela de Notificações avisa se houver lembretes além da janela e o app não for aberto há dias.
- **Eventos sincronizados:** os lembretes são gravados como `EKAlarm` e **disparados pelo próprio iOS** (mecanismo do app Calendário da Apple) — sem duplicatas e sem consumir a janela de 64. **Limitações aceitas e mostradas na tela de Notificações:** o toque abre o Calendário da Apple (não o LOMA), não há "Adiar/Dispensar" do LOMA, o som é o do sistema, e o horário de lembrete de dia inteiro do LOMA é gravado no próprio `EKAlarm` como deslocamento no momento em que o lembrete é criado. A fase 7 verifica se os alarmes continuam disparando com o app Calendário da Apple removido do aparelho.
- **Som por agenda (eventos locais):** um conjunto de sons embutidos no app (`UNNotificationSound(named:)`) + o padrão do sistema; escolha salva por agenda.

**Ambas (exceto eventos sincronizados no iOS, acima):** ações "Adiar" (5/10/15/30 min, padrão configurável) e "Dispensar"; toque abre o evento. Lembretes de dia inteiro disparam num horário configurável (ex.: 9h do dia ou 18h da véspera).

### 7.6 Telas (código comum em `:shared:ui`)

- **Principal:** barra superior com mês/ano e seletor de visualização (Dia / Semana / Mês / Ano / Agenda), botão Hoje, busca e menu. FAB "+". Gaveta ou folha com filtro de agendas.
- **Mês:** grade 6×7 com até ~3 "pílulas" coloridas por dia e "+n"; toque no dia abre uma folha com os eventos; deslize horizontal troca o mês.
- **Semana e Dia:** grade de horas com eventos posicionados; sobrepostos lado a lado; faixa de dia inteiro no topo; linha da hora atual; pinça para zoom vertical; toque longo num horário cria evento. Variante de 3 dias.
- **Ano:** 12 minimeses com marcação de dias com evento; toque abre o mês.
- **Agenda:** lista contínua de próximos eventos agrupada por dia.
- **Editor de evento:** título; dia inteiro; início/fim (data, hora, fuso — oculto por padrão); atalhos de duração (30 min, 1 h, 2 h); repetição; lembretes (vários); agenda; cor; local; descrição. Valida fim ≥ início.
- **Detalhe do evento:** Editar / Duplicar / Apagar / Compartilhar (.ics via folha de compartilhamento do sistema).
- **Configurações:** Aparência; Calendário (1º dia da semana, 12/24h, números de semana, visualização inicial, duração e lembrete padrão); Agendas (locais e do sistema); Notificações; Feriados; Aniversários; Importar/Exportar .ics; Backup e restauração (§7.10); Sobre (licença, código-fonte, "este app não acessa a internet").

```
┌──────────────────────────────────┐
│ ☰  Novembro 2026      🔍  📅Hoje  │
│ [Dia][Semana][●Mês][Ano][Agenda] │
│ D   S   T   Q   Q   S   S        │
│ 1   2   3   4   5   6   7        │
│     ▬Reun       ▬Dent            │
│ 8   9   10  11  12  13  14       │
│ ...                         (+)  │
└──────────────────────────────────┘
```

### 7.7 Widgets

Três widgets nas duas plataformas, com a mesma aparência geral:

1. **Próximos eventos** (redimensionável): próximos N dias agrupados por dia, com a cor da agenda; toque abre o evento; botão "+".
2. **Mês:** grade do mês com pontos nos dias com eventos e destaque em hoje; setas para trocar o mês (iOS: botões via App Intents).
3. **Hoje / próximo evento** (compacto): data grande + próximo evento e horário.

Configuração: agendas incluídas, dias à frente, fundo (dinâmico [só Android] / claro / escuro / personalizado), transparência, tamanho da fonte. Android: tela de configuração do widget; iOS: "Editar widget" via `AppIntentConfiguration`.

Dados: Android — Glance lê o Room. iOS — a extensão **não** carrega o framework Kotlin/Native nem o banco (o limite de memória das extensões de widget é baixo); em vez disso:
- **Eventos locais:** o app grava no App Group um snapshot JSON com as ocorrências locais dos próximos 12 meses (agendas locais são pequenas) e as configurações visuais; regravado a cada mudança.
- **Eventos sincronizados:** a extensão (Swift) lê o **EventKit diretamente** a cada timeline, usando a mesma permissão de calendário do app e o filtro de agendas gravado no snapshot. Assim, um evento criado no site do Google aparece no widget sem precisar abrir o LOMA.
- A timeline tem entradas à meia-noite e no início/fim de cada evento; `WidgetCenter.reloadAllTimelines()` é chamado quando o app altera dados. O snapshot também é excluído do backup do iCloud (§7.10).

Atualização ao mudar eventos, à meia-noite e na troca de fuso — sem atualização periódica frequente.

### 7.8 Feriados e aniversários

- **Feriados:** arquivos `.ics` por país embutidos (compose resources), gerados pela biblioteca Python `holidays` (MIT) para ~5 anos. O script gerador (`tools/holidays/`) roda **fora do build**; os `.ics` ficam versionados (build sem Python, reproduzível para o F-Droid). Feriados nacionais de todos os países suportados + regionais onde houver dados, com seletor opcional de região (ex.: Brasil → São Paulo). Importar cria uma agenda local somente-leitura "Feriados – <país>[ – <região>]" sem lembrete por padrão. Sugere o país pela localidade do aparelho.
- **Aniversários:** lê aniversários e datas comemorativas dos contatos e cria/atualiza uma agenda local somente-leitura "Aniversários" (evento anual, "Fulano — 34 anos" quando o ano é conhecido), com lembrete padrão configurável. Reimporta ao abrir o app se a permissão continuar concedida.

### 7.9 Importar/exportar ICS

- **Parser/escritor próprio em `:shared:ical`** (as bibliotecas maduras são só Java). Escopo: VCALENDAR, VEVENT, VALARM, VTIMEZONE (leitura; na escrita usa TZID IANA), RRULE/EXDATE/RDATE, RECURRENCE-ID, line folding, escapes e UTF-8. Componentes desconhecidos (VTODO, VJOURNAL, X-props) são ignorados na importação com contagem no resumo.
- Importar: escolhe a agenda de destino; mostra resumo (N eventos, M ignorados) e pode desfazer. Deduplicação por `UID`.
  - Destinos possíveis: agendas locais (ambas as plataformas) e agendas do sistema no **Android** (`UID_2445`, `EXDATE` e `RRULE` são colunas do provedor, então a importação é fiel).
  - **iOS, v1:** só agendas locais como destino. O EventKit não permite definir UID nem expõe EXDATE/RDATE/RRULE cru, então importar direto numa agenda do sistema perderia dados.
- Exportar: uma, várias ou todas as agendas. Agendas locais: fiel nas duas plataformas. Agendas do sistema: fiel no Android; no iOS, melhor esforço (regra reconstruída a partir do `EKRecurrenceRule`, exceções inferidas comparando a expansão com as ocorrências reais), com aviso no resumo.
- Abrir um `.ics` de outro app (Android: intent `text/calendar`; iOS: "Abrir com" / tipo de documento) leva à importação.
- Teste de ida e volta (importar → exportar → comparar) com arquivos reais exportados por Google, Outlook, Nextcloud e Apple.

### 7.10 Backup e restauração (decisão do usuário: sem backup na nuvem)

- **Android:** backup do Google Drive desligado — `dataExtractionRules` (Android 12+) exclui tudo de `cloud-backup` e mantém `device-transfer`; `fullBackupContent` (Android 8–11) exclui tudo, já que nessas versões não há como separar nuvem de transferência.
- **iOS:** banco, arquivo de configurações e snapshot do widget (App Group) marcados com `isExcludedFromBackup`, para não irem ao backup do iCloud. As configurações ficam num arquivo próprio ou no banco, **não** em `UserDefaults` (que não pode ser excluído de forma confiável). **Consequência:** no iOS isso também os exclui da transferência para um iPhone novo — no iOS, mudar de aparelho depende do arquivo de backup do próprio app (abaixo).
- **"Fazer backup" / "Restaurar"** (ambas): um arquivo ZIP com JSON versionado (agendas locais, eventos, exceções, lembretes, configurações, configurações de widgets), salvo onde a pessoa escolher. Agendas sincronizadas não entram. Restaurar mostra resumo e pergunta "substituir" ou "mesclar" (por `UID`). O mesmo arquivo restaura em qualquer plataforma.
- **Backup automático diário** opcional para uma pasta escolhida, mantendo os últimos 7. Android: WorkManager. iOS: BGTaskScheduler quando o sistema permitir e, garantidamente, ao abrir o app se o último backup tiver mais de 24 h. Se a pasta deixar de existir, mostra aviso.
- Tela "Sobre": "seus eventos nunca saem do aparelho, a não ser pelo backup que você mesmo salvar".

## 8. Fases de entrega (cada fase termina com um build utilizável)

O alvo iOS **compila no CI desde a fase 1** (o código comum não pode quebrar o iOS), mas as capacidades próprias do iOS chegam na fase 7.

1. **Fundação:** projeto KMP multi-módulo, CI (Android no Ubuntu; iOS simulador no macOS), checagens "sem rede", lint, tema Material 3, navegação, Room KMP. App iOS "casca" abrindo a tela principal.
2. **Núcleo (Android):** motor de recorrência, CRUD de eventos locais, lembretes, visualizações Mês/Semana/Dia/Ano/Agenda, busca, configurações básicas, backup/restauração.
3. **Sincronização (Android):** `SystemSource` Android, permissões sob demanda, filtro de agendas, edição de eventos sincronizados, controle de duplicatas.
4. **Feriados, aniversários e ICS** (código comum + leitor de contatos Android).
5. **Widgets Android** e personalização (sons por agenda, cores, AMOLED).
6. **Polimento Android e lançamento:** acessibilidade (TalkBack, fontes), RTL, desempenho, ícone, metadados F-Droid (fastlane), release assinado.
7. **iOS:** implementações iOS de `:shared:platform` (EventKit, notificações com janela de 64, contatos, arquivos, BGTask), extensão WidgetKit com os três widgets, ajustes de UI para iOS (VoiceOver, Dynamic Type, safe areas), ícone, App Store/TestFlight, rótulo de privacidade.

## 9. Testes

- `:shared:recurrence` e `:shared:ical`: testes unitários extensos em `commonTest`, rodando em JVM e no simulador iOS.
- `:shared:data`: testes de DAO Room e de backup/restauração (ida e volta).
- `SystemSource` Android: testes instrumentados com conta local de teste (`ACCOUNT_TYPE_LOCAL`). iOS: testes no simulador com uma agenda local do EventKit.
- UI: testes Compose dos fluxos críticos (criar evento recorrente, editar "este e os seguintes").
- CI falha se o manifest Android contiver `INTERNET` ou se aparecer dependência fora da lista permitida.
- Testes de captura de tela (screenshot) das telas principais em claro/escuro e pt-BR/inglês, rodando no CI, já que o desenvolvimento não tem aparelho físico.

**Aparelhos disponíveis (informado pelo usuário):** um celular Android e um iPhone; **sem Mac e sem conta Apple Developer** por enquanto.
- **Android (fases 1–6):** ao fim de cada fase, o CI publica um APK de teste; o usuário instala no próprio celular e faz o roteiro de aceite da fase (lista curta de passos escrita junto com o plano). A meta de abrir em < 1 s é conferida nesse aparelho.
- **iOS (fase 7):** sem Mac, toda a assinatura é feita no **GitHub Actions** (runner macOS + fastlane com chave de API do App Store Connect) — nenhum Mac é necessário. Até existir a conta Apple Developer, o iOS é validado só no simulador do CI (testes + screenshots). **A conta Apple Developer (US$ 99/ano) precisa ser criada antes do fim da fase 7**, porque instalar no iPhone real exige TestFlight.

## 10. Decisões tomadas por quem escreveu este design (o usuário pode trocar)

- Pacote/bundle `app.loma.calendar` e App Group `group.app.loma.calendar`.
- iOS mínimo 17.
- Idiomas pt-BR e inglês.
- Mesmo visual Material 3 nas duas plataformas (sem visual "Cupertino" no iOS).
- Não duplicar eventos sincronizados no banco local.
- Motor de recorrência e parser ICS próprios em Kotlin comum; kizitonwose/Calendar para grades; Room KMP; feriados gerados com `holidays`.
- iOS: lembretes de eventos sincronizados disparados pelo próprio iOS (`EKAlarm`); eventos locais pela janela de 64.
- iOS: dados excluídos do backup do iCloud (coerente com "sem nuvem"), o que também os tira da transferência automática para um iPhone novo.
- Três widgets (Próximos, Mês, Hoje) nas duas plataformas; no iOS, o widget lê eventos sincronizados direto do EventKit e eventos locais de um snapshot.
- iOS: eventos locais ficam no Room (não numa agenda local do EventKit), para não exigir permissão de calendário de quem não sincroniza.
- iOS: lembretes de eventos sincronizados seguem o comportamento da Apple (abrem o Calendário da Apple, sem Adiar do LOMA, som do sistema).
- iOS v1: importar `.ics` só para agendas locais; exportar agendas do sistema em melhor esforço.
- Repositório público no GitHub (natural para um projeto open-source, exigido pelo F-Droid, e dá minutos ilimitados de CI, inclusive macOS).
- `USE_EXACT_ALARM` no Android.
- Cor de eventos sincronizados: paleta do provedor no Android; cor da agenda no iOS.
- Android primeiro (fases 1–6), iOS em seguida (fase 7), com o iOS compilando no CI desde o início.

Decididas pelo usuário: nome **LOMA**, licença **Apache-2.0**, Android mínimo **8.0**, sincronização pelas contas do sistema, **KMP + Compose Multiplatform**, backup sem nuvem com arquivo local e backup automático para pasta.

## 11. Fica a critério de quem construir

- Framework de injeção de dependência (Koin ou manual) e biblioteca de navegação multiplataforma.
- Algoritmo de layout de eventos sobrepostos na grade de horas.
- Estrutura de pacotes dentro de cada módulo, nomes de classes.
- Formato exato do snapshot JSON do widget iOS e do arquivo de backup (desde que versionados).
- Detalhes visuais (espaçamentos, animações), dentro do Material 3.
