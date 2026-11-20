# LOMA — Design do app de agenda e gestão do salão (Android + iOS)

*Data: 2026-11-19 · Status: **aprovado** (rev. 5, 2026-11-20: salão, assistente e financeiro incluídos; agenda pessoal passa a ser camada secundária; telas aprovadas pelo usuário)*

## 1. Intenção e o porquê

O LOMA é o app do **salão da família do dono do projeto**. Cada profissional (manicure, cabeleireira, esteticista…) instala o LOMA **no próprio celular** e usa para:

- **Agenda de atendimentos:** suas clientes, os serviços marcados, horários e status.
- **Assistente:** um resumo diário no horário que ela escolher (ex.: 8h) com os atendimentos do dia — cliente, procedimento, tempo estimado, ganho esperado e um atalho para confirmar pelo WhatsApp — e avisos de contas que vencem.
- **Financeiro:** quanto já entrou, quanto ainda vai entrar (puxado da agenda), quanto saiu (materiais e contas fixas) e o lucro líquido real.
- **Agenda pessoal:** os compromissos dela, vindos das contas do celular (Google, Outlook, iCloud) ou criados no próprio app, numa camada separada que não entra nas contas do salão.

**Não há cargos nem conta central.** Ninguém vê a agenda ou o dinheiro de outra profissional. Contas fixas do salão (aluguel, luz, água, internet) entram no app de cada uma **pela parte que ela paga**.

**Privacidade continua sendo o centro: o app nunca acessa a rede.** Clientes, atendimentos e o financeiro ficam só no aparelho. A sincronização da agenda pessoal acontece pelas contas do sistema (Android: `CalendarContract`; iOS: EventKit). O botão "Confirmar pelo WhatsApp" apenas **abre o WhatsApp** com a mensagem pronta; quem envia é o WhatsApp. No Android isso é verificável: o app não declara a permissão `INTERNET`. No iOS, a garantia vem de não haver código nem dependência de rede (checado no CI) e do selo "Dados não coletados" da App Store.

Código próprio, escrito do zero; o Fossify Calendar (GPL-3.0) segue como referência funcional para a parte de calendário, sem copiar código. O dono quer poder lançar no futuro uma versão fechada/comercial — por isso a licença é permissiva (§5).

## 2. Metas

**Salão**
- **Ficha completa da cliente** (§7.4): nome, WhatsApp, aniversário, foto opcional, alergias e sensibilidades, preferências, observações, histórico automático de atendimentos com anotação por atendimento (ex.: fórmula da cor) e totais.
- **Lista de serviços**: nome, duração e preço. Ao agendar, a profissional escolhe um ou mais serviços e o app preenche tempo estimado e ganho esperado.
- **Atendimentos com status**: Agendado → Confirmado → Concluído/Pago, ou Desmarcado. Concluir pede o **valor final** (editável). O financeiro contabiliza **somente o valor** — sem forma de pagamento nem taxa de maquininha.
- **Confirmar pelo WhatsApp**: abre o WhatsApp (ou WhatsApp Business) com mensagem pronta; a profissional marca "Confirmado" quando a cliente responde.
- **Assistente**: resumo diário em horário configurável + avisos de contas na véspera e no dia do vencimento enquanto estiverem pendentes (§7.6).
- **Financeiro em 3 abas** (Visão Geral, Ganhos e Projeção, Gastos e Contas) com microtextos explicativos e as regras de negócio do §7.5.

**Calendário (base e agenda pessoal)**
- Visualizações **Dia, Semana, Mês, Ano** e **Lista**, com navegação por deslize e botão "Hoje"; atendimentos e eventos pessoais na mesma grade, em camadas distintas.
- Eventos pessoais com horário, dia inteiro, fuso, local, descrição, cor e **múltiplos lembretes**; **recorrência avançada** RFC 5545 (RRULE: "última sexta do mês", exceções, "só este / este e os seguintes / todos").
- **Sincronizar a agenda pessoal com Google, Outlook e Nextcloud** pelas contas do sistema (Android: contas do Android, Nextcloud via DAVx5; iOS: Ajustes do iPhone).
- **Feriados** (por país/região, offline) e **aniversários** (contatos e clientes).
- **Importar/exportar `.ics`** (agenda pessoal).
- **Widgets** de tela inicial (§7.13).
- Busca (clientes, atendimentos e eventos).
- Tema claro/escuro/sistema e preto AMOLED; visual da **direção B + referência escolhida** (§7.12).
- Idiomas: **pt-BR e inglês** desde o início; strings 100% em recursos.

## 3. Não-metas (fora desta versão)

- Várias profissionais compartilhando agenda ou caixa; visão consolidada do salão; sincronização entre celulares.
- Agendamento online pela cliente; envio automático de mensagens (o app só abre o WhatsApp; quem envia é a profissional).
- Comissões, controle de estoque, nota fiscal, integração com maquininha/banco/Pix.
- Forma de pagamento, taxa de maquininha, pagamento parcial, sinal antecipado e "fiado" (decisão do usuário: contabilizar só o valor).
- Exportar relatórios financeiros (CSV/PDF).
- Atendimentos recorrentes com regra própria: "repetir" cria cópias individuais (§7.4).
- Tarefas/to-do; cliente CalDAV/Google/Outlook embutido; convidados/RSVP em eventos locais (em eventos sincronizados, convidados são preservados intactos, não editáveis).
- Desktop, web, Apple Watch, layout próprio para iPad.
- Clima, mapas, anexos, videoconferência; visual "Cupertino" no iOS.

## 4. Anti-metas (falha mesmo que "funcione")

- Qualquer acesso à rede. Android: `INTERNET` não pode aparecer no manifest final. Ambas: nenhuma dependência de rede (Ktor, OkHttp, URLSession em código próprio etc.) — checado no CI por lista de dependências permitidas e busca no código. Abrir um link para outro app (WhatsApp) não é acesso à rede do LOMA.
- Qualquer biblioteca de analytics, crash reporting, anúncios, Google Play Services/Firebase ou equivalentes da Apple.
- **Um número do financeiro que não bate com a agenda**: um desmarcado que continua em "Ganhos Futuros", um concluído contado duas vezes, centavos perdidos por arredondamento.
- **Dados de clientes saindo do aparelho** por qualquer caminho que não seja um backup ou exportação feitos pela própria profissional.
- Pedir qualquer permissão ao abrir o app pela primeira vez.
- Perder ou corromper dados de agendas sincronizadas (o app nunca apaga uma agenda do sistema nem reescreve campos que não entende).
- Lembretes ou resumo diário que não disparam, ou disparam em duplicidade sem que a usuária possa resolver.
- Interface lenta ou poluída: com a abertura desligada (ou pulada), o Início aparece em < 1 s num aparelho médio; com ela ligada, o Início já está pronto quando a abertura (≤ 4,6 s) termina.

## 5. Restrições

- **Tecnologia:** Kotlin Multiplatform (KMP) + Compose Multiplatform (iOS estável desde 1.8.0) + Material 3. Widgets: Jetpack Glance (Android); WidgetKit/SwiftUI (iOS). Datas com `kotlinx-datetime` no código comum.
- **Dinheiro:** sempre em **centavos (`Long`)**, nunca ponto flutuante; moeda BRL; formatação pela localidade.
- **Versões mínimas:** Android 8.0 (API 26) — confirmado pelo usuário. iOS 17.
- **Licença:** Apache-2.0 (escolhida pelo usuário). Cabeçalho SPDX em todos os arquivos, `LICENSE` + `NOTICE`. Dependências só com licenças permissivas (Apache-2.0, MIT, BSD). Contribuições externas sob Apache-2.0 com DCO.
- **Distribuição:** Android — APK pelo GitHub Releases (instalação direta nos celulares da família), F-Droid e Play Store opcionais. iOS — TestFlight/App Store; exige conta Apple Developer (US$ 99/ano). Identificadores provisórios: `app.loma.calendar`; extensão de widget `app.loma.calendar.widgets`; App Group `group.app.loma.calendar`.
- **Build:** o ambiente de desenvolvimento não alcança o SDK do Android nem tem Xcode. Tudo compila e testa no **GitHub Actions** (Ubuntu para Android; macOS para iOS, inclusive assinatura e TestFlight). O usuário não tem Mac.
- **Permissões** (todas sob demanda, com explicação antes do pedido do sistema):

| Permissão | Android | iOS | Quando |
|---|---|---|---|
| Notificações | `POST_NOTIFICATIONS` (13+) | Autorização de notificações | Ao ligar o resumo diário ou salvar o 1º atendimento/evento com lembrete |
| Alarme exato | `USE_EXACT_ALARM` (13+) / `SCHEDULE_EXACT_ALARM` (12; se negado, inexato + aviso) | — | Instalação |
| Reagendar após reinício | `RECEIVE_BOOT_COMPLETED` | — | Instalação |
| Agendas do sistema | `READ_CALENDAR`/`WRITE_CALENDAR` | Acesso total a calendários | Ao ligar "Sincronizar agendas do sistema" |
| Contatos (aniversários em massa) | `READ_CONTACTS` | `NSContactsUsageDescription` | Ao tocar "Importar aniversários" |
| — | Seletor de contato (`ACTION_PICK`) e seletor de fotos do sistema: **sem permissão** | `CNContactPickerViewController` e `PHPicker`: **sem permissão** | Ao criar cliente "a partir dos contatos" ou escolher foto |
| Digital / rosto (bloqueio do app) | `USE_BIOMETRIC` (normal, sem diálogo) | `NSFaceIDUsageDescription` | Ao ligar o bloqueio com digital |
| Ver se o WhatsApp está instalado | `<queries>` para `com.whatsapp` e `com.whatsapp.w4b` (não é permissão) | `LSApplicationQueriesSchemes` com `whatsapp` | — |

## 6. Abordagens consideradas

**6.1 Onde ficam os atendimentos do salão** — escolhida pelo usuário: **A**.

| | Abordagem | Prós | Contras |
|---|---|---|---|
| **A ✅** | O LOMA vira o app do salão: clientes, serviços, atendimentos e financeiro no banco do próprio app; agenda pessoal como camada vinda das contas do sistema | Tudo privado; status e valores sempre consistentes; financeiro calculado em tempo real | Atendimentos não aparecem no Google Agenda nem em outro aparelho |
| B | Atendimentos como eventos no Google Agenda + dados extras no app | Horário visível em qualquer lugar | Dados das clientes na nuvem do Google; dados divididos em dois lugares; edição externa desalinha o financeiro |
| C | Dois apps (agenda pessoal e salão) | Separação total | ~2× trabalho, sem ganho para a família |

**6.2 Como sincronizar a agenda pessoal** — escolhida pelo usuário: **pelas contas do sistema** (sem rede no app). Rejeitada: cliente CalDAV/OAuth próprio.

**6.3 Como cobrir Android e iOS** — escolhida pelo usuário: **KMP + Compose Multiplatform**. Rejeitadas: Flutter (refazer tudo, Android menos nativo) e dois apps nativos (~2× trabalho).

**6.4 Bases prontas avaliadas e descartadas:** Cal.diy, Zero Calendar, Someday (web/nuvem) e pacotes Flutter (exigiriam Flutter).

**6.5 Uma profissional por aparelho** — confirmado pelo usuário: cada profissional usa o próprio celular, sem compartilhamento; é o que permite manter o app sem internet.

## 7. Como funciona

### 7.1 Módulos

```
:shared:model       modelos puros (Event, EventInstance, CalendarInfo, Reminder, RecurrenceRule)
:shared:salon       domínio do salão: Client, Service, Appointment, Expense, RecurringExpense,
                    Money (centavos), e as calculadoras puras do financeiro e do resumo diário — §7.5, §7.6
:shared:recurrence  motor RRULE próprio (commonMain), sem dependências — §7.9
:shared:ical        parser/escritor ICS próprio — §7.15
:shared:data        Room KMP (BundledSQLiteDriver), repositórios, backup, feriados
:shared:platform    interfaces das capacidades de cada SO + implementações androidMain/iosMain — §7.2
:shared:ui          telas Compose Multiplatform, ViewModels, tema, strings (compose resources)
:androidApp         Activity, Glance widgets, receivers, WorkManager, manifest
iosApp/ (Xcode)     ponto de entrada SwiftUI que hospeda o Compose, extensão WidgetKit (Swift)
```

Fluxo: UI → ViewModel → repositórios → fontes de dados. Repositórios expõem `Flow`s observando o Room; qualquer mudança (agendar, concluir, desmarcar, apagar, lançar gasto) recalcula as telas **em tempo real** sem passo manual de "atualizar". Grades de **Mês**, **Ano** e faixa de dias usam **kizitonwose/Calendar** (`compose-multiplatform`, MIT); a grade de horas de Dia/Semana é escrita no projeto.

### 7.2 Capacidades por plataforma (interfaces em `:shared:platform`)

| Interface | Android | iOS |
|---|---|---|
| `SystemCalendarSource` | `CalendarContract` | EventKit |
| `NotificationScheduler` | `AlarmManager` exato + notificação | `UNUserNotificationCenter` com janela de 64 (§7.10) |
| `MessagingLauncher` (WhatsApp/SMS) | `Intent.ACTION_VIEW` para `https://wa.me/…` direcionado a `com.whatsapp`/`com.whatsapp.w4b`; sem WhatsApp → `smsto:` | `UIApplication.open` em `whatsapp://send?…` ou `https://wa.me/…`; sem WhatsApp → `sms:` |
| `ContactPicker` | `ACTION_PICK` em Contatos | `CNContactPickerViewController` |
| `PhotoPicker` | Photo Picker do sistema (API 26+ via AndroidX) | `PHPickerViewController` |
| `BirthdayReader` | `ContactsContract` | `CNContactStore` |
| `FileAccess` | Storage Access Framework | `UIDocumentPicker` + security-scoped bookmarks |
| `BackgroundWork` | WorkManager | BGTaskScheduler (melhor esforço) + execução ao abrir o app |
| `WidgetBridge` | Glance lê o banco | snapshot JSON no App Group + `WidgetCenter.reloadAllTimelines()` |
| `PlatformTheme` | paleta da marca; cores dinâmicas (12+) como opção | paleta da marca |

### 7.3 Fontes de dados

```
                        Repositórios
            /                 |                  \
  SalonStore (Room)     LocalSource (Room)     SystemSource
  - clientes            - agendas pessoais      (CalendarContract | EventKit)
  - serviços              do app                - agendas das contas do sistema
  - atendimentos        - feriados              - Google / Outlook / Nextcloud / iCloud
  - gastos e contas     - aniversários
  - modelos de conta    - expansão própria
    fixa                  via :shared:recurrence
```

- **SalonStore** — só no aparelho; nunca vai para agendas do sistema.
- **LocalSource** — mesmo esquema da rev. 4: `calendars`, `events` (RRULE texto RFC 5545, EXDATEs, fuso IANA), `event_overrides`, `reminders`.
- **SystemSource** — Android lê `CalendarContract.Instances` e escreve `Events`/`Reminders` (exceções com `ORIGINAL_ID`/`ORIGINAL_INSTANCE_TIME`), observa com `ContentObserver`. iOS lê com `predicateForEvents` em blocos de até 4 anos, escreve `EKEvent` com `.thisEvent`/`.futureEvents`, observa `EKEventStoreChanged`; RRULE ↔ `EKRecurrenceRule`; regras que o EventKit não representa ficam somente leitura. Cor por evento: paleta do provedor no Android; cor da agenda no iOS. **Não há cópia dos dados sincronizados para o Room.**
- Nas grades da Agenda, o usuário escolhe quais camadas aparecem: **Salão** (atendimentos), agendas pessoais locais, agendas do sistema, Feriados, Aniversários.

### 7.4 Salão: dados e fluxos

**Tabelas (Room, em `:shared:data`)**

| Tabela | Campos principais |
|---|---|
| `clients` | id, nome, telefone (dígitos com DDI; padrão +55), aniversário (dia/mês, ano opcional), foto (arquivo no armazenamento privado do app), alergias/sensibilidades, preferências, observações, criada_em, arquivada |
| `services` | id, nome, duração (min), preço (centavos), cor, ativo |
| `appointments` | id, client_id, início (instante + fuso IANA), fim, status (`AGENDADO`, `CONFIRMADO`, `CONCLUIDO`, `DESMARCADO`), motivo do desmarque (`CANCELOU`, `NAO_COMPARECEU`, `REMARCOU`), valor esperado (centavos), valor final (centavos, só em `CONCLUIDO`), concluído_em, anotação do atendimento, lembretes |
| `appointment_services` | appointment_id, service_id, **cópia** de nome, duração e preço no momento do agendamento, ordem |
| `expenses` | id, tipo (`FIXA`, `MATERIAL`), categoria (`ALUGUEL`, `ENERGIA`, `AGUA`, `INTERNET`, `OUTRA_FIXA`; materiais: `PRODUTOS`, `DESCARTAVEIS`, `EQUIPAMENTOS`, `OUTRO_MATERIAL`), descrição, valor (centavos), vencimento (fixas) ou data (materiais), status (`PENDENTE`, `PAGO`), pago_em, observação, recurring_id |
| `recurring_expenses` | id, categoria, descrição, valor padrão (centavos), dia do vencimento, ativo, mês de início |

**Ficha da cliente**
- Cabeçalho com foto/iniciais, nome, botões **WhatsApp**, **Ligar** e **Agendar**.
- Dados: telefone, aniversário, alergias e sensibilidades (em destaque, com ícone de alerta, também mostradas ao abrir um atendimento dela), preferências, observações.
- **Histórico automático**: atendimentos concluídos e desmarcados (data, serviços, valor final, anotação do atendimento), do mais recente ao mais antigo.
- Resumo: última visita, número de atendimentos, total gasto, ticket médio, número de desmarques.
- Criar cliente: manual ou **a partir dos contatos** (seletor do sistema, sem permissão; copia nome, telefone e aniversário).
- **Apagar cliente** (direito de exclusão): remove a ficha e a foto; atendimentos concluídos continuam no financeiro com o nome trocado por "Cliente removida"; atendimentos futuros dela são desmarcados (e saem da previsão) após confirmação. **Arquivar** esconde da lista sem apagar.

**Serviços**
- Tela de serviços em Ajustes: criar, editar, reordenar, desativar. Mudar o preço de um serviço **não altera** atendimentos já marcados (o preço é copiado no agendamento); o atendimento pode ser editado individualmente.

**Agendar**
- Campos: cliente (busca ou "nova cliente"), um ou mais serviços (soma duração e valor esperado; ambos editáveis), data/hora de início (fim calculado), lembretes, anotação.
- Conflito de horário com outro atendimento: aviso, permite salvar.
- **Repetir** (ex.: toda semana por 8 semanas): cria cópias independentes, cada uma com seu status.
- Status inicial `AGENDADO`.

**Mudar status**
- `AGENDADO` → `CONFIRMADO`: um toque ("Confirmado") no atendimento, na lista de hoje ou após voltar do WhatsApp.
- → `CONCLUIDO`: folha "Concluir atendimento" com valor final (pré-preenchido com o esperado, editável — serviço extra ou desconto), **data do pagamento** (padrão: agora; para um atendimento sem baixa de dia anterior, o dia do atendimento) e anotação do atendimento; só então consolida no caixa. `concluído_em` = data do pagamento.
- → `DESMARCADO`: escolhe o motivo (cancelou, não compareceu, remarcou). "Remarcou" oferece criar o novo horário já preenchido.
- **Desfazer** está disponível por alguns segundos após qualquer mudança; depois, um concluído pode ser "Reabrir" (volta a `CONFIRMADO` e sai dos realizados) com confirmação.
- Apagar um atendimento: futuro/aberto → some da previsão; concluído → pede confirmação explícita porque remove o valor dos ganhos realizados.

**Atendimentos passados sem baixa**: um atendimento `AGENDADO`/`CONFIRMADO` cujo fim já passou fica marcado "sem baixa". Ele continua em Ganhos Futuros (ainda não foi finalizado) e aparece num aviso no Início e no Financeiro ("2 atendimentos sem baixa — concluir ou desmarcar").

### 7.5 Financeiro: definições e regras

Todas as somas são em centavos, por período de calendário no fuso do aparelho. "Semana" segue o primeiro dia da semana configurado.

| Indicador | Definição |
|---|---|
| **Ganhos realizados** (período) | Soma do **valor final** dos atendimentos `CONCLUIDO` cujo `concluído_em` cai no período |
| **Ganhos de Hoje** | Ganhos realizados de hoje |
| **Ganhos do Mês** | Ganhos realizados do mês corrente |
| **Ganhos Futuros / A Receber** | Soma do **valor esperado** dos atendimentos `AGENDADO` ou `CONFIRMADO` (inclui os sem baixa) |
| — Restante da semana / do mês | Mesma soma, restrita aos atendimentos com início entre o começo de hoje e o fim da semana / do mês, mais os sem baixa |
| **Total de Gastos** (período) | Materiais com data no período + contas fixas com vencimento no período (pagas e pendentes); o cartão mostra também "R$ X pagos · R$ Y a vencer" |
| **Lucro Líquido** (período) | Ganhos realizados − **gastos pagos** no período (materiais + contas fixas com status `PAGO` e pagamento no período) — decisão do usuário |
| **Entradas vs. Saídas** | Colunas por semana do mês: ganhos realizados × gastos pagos |

**Regras de negócio (do usuário, obrigatórias)**
1. **Inclusão na projeção:** agendar soma o valor esperado em Ganhos Futuros imediatamente.
2. **Conclusão:** marcar Concluído/Pago tira o valor de Ganhos Futuros e soma o **valor final** em Ganhos de Hoje e do Mês (pelo dia da conclusão).
3. **Dedução por desmarcação:** status `DESMARCADO` ou exclusão do atendimento tira o valor de Ganhos Futuros na hora e recalcula as telas, **sem tocar nos ganhos realizados**.
4. **Valor final editável** ao concluir, antes de consolidar.

Como todos os indicadores são derivados de consultas sobre `appointments` e `expenses` (nada é "acumulado" em contadores), as regras 1–3 valem por construção: não existe somatória guardada que possa ficar desatualizada.

**Contas fixas recorrentes**
- Ao marcar "Despesa fixa mensal", o app guarda um modelo (`recurring_expenses`) e **lança a conta de cada mês** (status `PENDENTE`, valor padrão, vencimento no dia escolhido; se o mês não tiver esse dia, no último dia). O lançamento do mês corrente e do seguinte é garantido ao abrir o app e pelo trabalho diário em segundo plano; nunca duplica (chave única modelo + mês).
- Valor do mês editável (a conta de luz varia); editar o modelo afeta só os meses ainda não lançados.
- Botão de status **Pendente / Pago** em cada conta; pagar registra a data.
- Categorias pré-cadastradas com ícone: Aluguel, Energia (Luz), Água, Internet; "Outra conta fixa" com nome livre.

**Materiais**
- Cadastro: descrição (ex.: "Tintas e oxidantes"), categoria (produtos, descartáveis, equipamentos, outro), valor, data e observação opcional. Lançado já como pago na data informada.

**Microtextos** (abaixo de cada indicador, em pt-BR e inglês): ex. Lucro Líquido "Valor que realmente sobrou no caixa"; Ganhos Futuros "Valores estimados com base nos clientes já agendados. Eles passam para 'Ganhos Realizados' assim que o atendimento é concluído e são deduzidos automaticamente caso o cliente seja desmarcado."; Total de Gastos "Materiais + contas fixas do período".

### 7.6 Assistente: resumo diário e avisos

- **Resumo diário** num horário configurável (padrão 8h), ligado por padrão após a 1ª permissão de notificação. Conteúdo:
  - "Hoje você tem N atendimentos (previsto R$ X)";
  - para cada atendimento de hoje: hora, cliente, serviços, tempo estimado, ganho esperado e status ("ainda não confirmada" / "confirmada");
  - aniversário de clientes no dia;
  - contas: que vencem **amanhã**, que vencem **hoje** e **vencidas** ainda pendentes;
  - atendimentos sem baixa de dias anteriores.
- Em dia sem nada disso, não há notificação.
- A notificação mostra o texto expandido; tocar abre a tela **Hoje** do Início, onde cada atendimento tem **Confirmar pelo WhatsApp** e **Confirmado**. A notificação tem a ação "Abrir hoje" (notificações não comportam um botão por cliente).
- **Avisos de conta**: as contas pendentes entram no resumo na véspera e no dia do vencimento. Se o resumo estiver desligado, a conta gera uma notificação própria no mesmo horário. Some ao marcar como Paga.
- **Lembrete por atendimento** (opcional, padrão configurável, ex.: 1 h antes): "13:00 Maria · Coloração", com ação "WhatsApp".
- **Android**: o conteúdo do resumo é calculado na hora do disparo, lendo o banco.
- **iOS**: o iOS não roda código na entrega, então o resumo dos próximos 14 dias é **pré-calculado** e agendado; como toda mudança de dados acontece dentro do app, cada mudança recalcula os resumos afetados. Os 14 resumos ocupam vagas da janela de 64 notificações (§7.10).

### 7.7 WhatsApp

- Botão **Confirmar pelo WhatsApp** no atendimento, na lista de hoje e na ficha.
- Mensagem a partir de um **modelo editável** em Ajustes, com campos `{nome}`, `{quando}` (hoje/amanhã/data), `{hora}`, `{servicos}`. Padrão: "Oi {nome}! Tudo bem? Passando para confirmar seu horário {quando} às {hora} para {servicos}. Posso confirmar?"
- Abre o WhatsApp Business se instalado, senão o WhatsApp; se houver os dois, pergunta uma vez e lembra a escolha. Sem nenhum, oferece SMS com o mesmo texto.
- Ao voltar ao app, mostra "A cliente confirmou?" com o botão **Confirmado** (não muda o status sozinho).
- Também: "Parabéns pelo aniversário" a partir da ficha e do resumo.

### 7.8 Agenda pessoal

Igual à rev. 4: eventos locais (agendas do app) e eventos das contas do sistema, com filtro por agenda, agenda padrão configurável, mover entre fontes = criar no destino + apagar na origem (com confirmação). Eventos pessoais nunca entram no financeiro.

### 7.9 Recorrência (eventos pessoais)

- Canônica em **RRULE RFC 5545**; **motor próprio em `:shared:recurrence`**: FREQ, INTERVAL, COUNT, UNTIL, BYDAY (com ordinal), BYMONTHDAY (incl. negativos), BYMONTH, BYSETPOS, WKST, EXDATE, RDATE; expansão por intervalo em fuso IANA. Fora do escopo (BYWEEKNO, BYYEARDAY, BYHOUR…) é preservado como texto e mostrado uma vez com aviso.
- Editor com presets + "Personalizado"; resumo em linguagem natural.
- Edição: "Só este", "Este e os seguintes", "Todos".
- Testes obrigatórios (JVM e simulador iOS): última sexta, dia 31, 29/02, horário de verão, UNTIL em outro fuso, COUNT com EXDATE, BYSETPOS e casos da RFC 5545 §3.8.5.3.

### 7.10 Lembretes e notificações (plataforma)

**Android**
- Agendador próprio para tudo (resumo diário, lembretes de atendimento, avisos de conta, eventos pessoais locais e sincronizados): `AlarmManager.setExactAndAllowWhileIdle` para o próximo alarme; ao disparar, notifica e agenda o seguinte. Reagenda em `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`, ao editar dados e em mudanças do `CalendarContract`. WorkManager diário como rede de segurança (também lança contas fixas do mês).
- Canais: "Resumo diário", "Atendimentos", "Contas", e um por agenda pessoal (som por agenda).
- Duplicatas com o app Google Agenda: configuração por agenda "Lembretes desta agenda: pelo LOMA / pelo outro app".

**iOS**
- Atendimentos, resumo diário, avisos de conta e eventos pessoais locais: notificações locais com `UNUserNotificationCenter`. **Limite de 64 pendentes**: o agendador prioriza os 14 próximos resumos diários e preenche o restante com os lembretes mais próximos; recalcula ao abrir o app, ao mudar dados, ao tocar uma ação de notificação e no BGTask. A tela de Notificações avisa se houver itens fora da janela e o app não for aberto há dias.
- Eventos pessoais sincronizados: `EKAlarm`, disparados pelo próprio iOS (abrem o Calendário da Apple, sem Adiar do LOMA, som do sistema) — limitação aceita da rev. 4.

**Ambas:** ações "Adiar" e "Dispensar" (exceto eventos sincronizados no iOS); lembretes de dia inteiro num horário configurável.

### 7.11 Telas e navegação

Barra inferior: **Início · Agenda · Clientes · Financeiro**. Ajustes pelo avatar no topo do Início. Botão "+" contextual (novo atendimento, nova cliente, novo gasto).

- **Início (o dia do salão):** saudação; meses em pílulas roláveis + mini mês (dias com atendimentos marcados; hoje e dia selecionado destacados); **"Atendimentos de hoje"** em cartões grandes (hora, cliente com foto/iniciais, serviços, valor, status, botões WhatsApp/Confirmado/Concluir); faixa "Hoje: previsto R$ X · recebido R$ Y"; avisos (contas que vencem, atendimentos sem baixa, aniversários). Tocar outro dia no mini mês mostra os atendimentos daquele dia.
- **Agenda:** seletor Dia / Semana / Mês / Ano / Lista; atendimentos e eventos pessoais nas mesmas grades, filtro de camadas; Dia/Semana com faixa horizontal de datas e linha do tempo, linha da hora atual, pinça para zoom, toque longo cria atendimento.
- **Detalhe do atendimento:** tela cheia na cor principal (estilo da referência): cliente (com alergias em destaque), serviços, horário, valor, status, anotação; ações conforme o status.
- **Clientes:** lista com busca e ordenação (nome, última visita); ficha (§7.4).
- **Financeiro:** abas no topo **Visão Geral | Ganhos | Gastos**.
  - *Visão Geral:* seletor Hoje | Esta Semana | Este Mês | Personalizado; cartão grande **Lucro Líquido**; grade 2×2 (Ganhos de Hoje, Ganhos do Mês, A Receber, Total de Gastos), cada um com microtexto; gráfico de colunas Entradas × Saídas. O seletor afeta Lucro Líquido, A Receber, Total de Gastos e o gráfico; "Ganhos de Hoje" e "Ganhos do Mês" são fixos, como os nomes dizem.
  - *Ganhos:* sub-seções **Realizados** (filtro dia/mês; lista cronológica: cliente, serviço, data/hora, valor; total do período) e **Ganhos Futuros** (Restante da semana, Restante do mês, lista dos atendimentos previstos, aviso de sem baixa, texto explicativo do usuário).
  - *Gastos:* barra superior "Total pago" × "Contas a vencer no mês"; seções **Contas fixas** (com Pendente/Pago e marcação de fixa mensal) e **Materiais**; botão flutuante **+ Novo gasto**.
- **Ajustes:** Perfil (nome para a saudação), Serviços, Assistente (horário do resumo, o que incluir, modelo da mensagem do WhatsApp, lembrete padrão por atendimento), Aparência, Calendário (1º dia da semana, 12/24h, números de semana, visualização inicial), Agendas pessoais, Notificações, Feriados, Aniversários, Importar/Exportar .ics, Backup e restauração, Sobre ("este app não acessa a internet").

### 7.12 Visual

- **Direção B (Expressiva) + referência escolhida pelo usuário** (Dribbble "Calendar Mobile App", shot 22450749): Material 3 com formas bem arredondadas, títulos grandes e grossos, cartões em cores chapadas.
- Paleta da marca (claro): fundo lilás muito claro, **azul-marinho profundo** como cor principal, **verde-menta** para "hoje"/sucesso/concluído e **lilás** para seleção/agendado; coral reservado para alertas (alergia, conta vencida). Escuro e AMOLED derivados da mesma paleta. Cores dinâmicas do Android 12+ disponíveis como opção, desligadas por padrão.
- Status dos atendimentos por cor **e** por rótulo/ícone (nunca só pela cor): Agendado (lilás), Confirmado (marinho), Concluído (menta), Desmarcado (riscado, oculto por padrão nas grades).
- Tipografia: Outfit (OFL; embutida, sem download).
- **Referência visual aprovada (fonte da verdade):** canvas "LOMA — Direções visuais", página **Telas do LOMA** (https://claude.ai/artifact/RCnFcjkiWygU6stCx4L4ng#page-telas, versão `1795176413-08cf`), 10 telas: Entrada, Desbloqueio, Início, Detalhe do atendimento, Concluir atendimento, Agenda — Dia, Ficha da cliente, Financeiro — Visão Geral, Ganhos e Gastos. O usuário aprovou: "faça exatamente assim". O app deve reproduzir layout, hierarquia, cores, raios e textos dessas telas; o HTML das telas vai para o repositório em `docs/design/telas/` como referência.
- **Tokens (claro):** fundo `#F3F2FA`; superfície `#FFFFFF`; contêiner `#E7E4F8`; marinho (primária) `#17165C`, marinho claro `#2A2985`; menta `#5ED6B4` (texto sobre menta `#0B3A2E`; fundo suave `#E3F8F1`); lilás `#B69CF2`; coral `#FF7A66` (alerta forte), alerta suave `#FFE2DC` com texto `#7A1A0B`; texto secundário `#5B5A7A`; borda `#C9C6E6`. Raios: 14–18 (itens), 22–28 (cartões), 32–34 (folhas e telas cheias); botões em pílula. Pesos Outfit 400–800; títulos de tela 30 px/800; nomes de cliente em cartões 26 px/800.
- Microtextos explicativos sob cada número do financeiro.
- Contraste mínimo 4,5:1 em textos; alvos de toque ≥ 48 dp.

### 7.12b Ícone e abertura

**Ícone (decidido pelo usuário: modelo 6B "O menta"):** fundo azul-marinho `#17165C`; o nome **loma** em Outfit ExtraBold, `l`, `m` e `a` em branco e o **`o` em menta `#5ED6B4`**; sem ponto. Referência: canvas, página "Abertura e ícone", variação 6B. No app as letras viram **contornos vetoriais** (extraídos da fonte Outfit por script em `tools/icone/`), dentro da zona segura do ícone adaptativo do Android (66/108); camada monocromática (Android 13+ "ícones temáticos") com o mesmo desenho; iOS com PNG 1024×1024 gerado do mesmo vetor.

**Abertura animada** (aprovada pelo usuário; referência: canvas, quadro "Abertura animada"): cerca de **4,6 s**, sincronizada com o som **"Brilho lento"** (`loma-abertura-brilho-lento`, original, sintetizado por `tools/som/`).
- 0–2 s: tela marinho; um brilho menta cresce devagar no centro e pequenas faíscas sobem (o som cresce junto).
- 2,1 s: no toque principal do som, um ponto menta "estoura" no centro com um anel de luz.
- 2,35–3,2 s: o ponto se transforma no **"o" menta** enquanto `l`, `m` e `a` sobem uma a uma — o nome termina idêntico ao ícone.
- 3,3 s: um reflexo de luz passa pelo nome (último brilho do som).
- 4,0–4,6 s: o logo se dissolve e o Início aparece.

Regras: toca só quando o app é aberto do zero (não ao voltar de outro app nem ao girar a tela); **um toque pula**; o som segue o modo silencioso (Android: modo silencioso/vibrar → sem som; iOS: categoria de áudio *ambient*, respeita a chave de silencioso); Ajustes › Aparência tem "Abertura animada" (ligada) e "Som da abertura" (ligado); com "remover animações" do sistema ligado, mostra o nome estático por 0,6 s, sem som. O Início é preparado durante a abertura. No Android 12+, a tela de abertura do sistema é marinho **sem ícone** (ícone transparente), para a animação começar sem "pulo"; no iOS, a LaunchScreen é marinho lisa.

### 7.12a Bloqueio do app (PIN e digital)

- Opcional, oferecido na configuração inicial e em Ajustes › Segurança. Tela "Bem-vinda de volta, <nome>" com PIN de 4 dígitos e botão **Usar digital** (Android `BiometricPrompt` via AndroidX Biometric; iOS `LocalAuthentication`), como na tela Desbloqueio aprovada.
- Bloqueia ao abrir o app e após 1 minuto em segundo plano (configurável). O conteúdo das notificações continua visível conforme a configuração de privacidade do sistema; o resumo diário pode ocultar valores ("Mostrar valores nas notificações", ligado por padrão).
- O PIN é guardado como hash com sal (PBKDF2) no armazenamento privado; não há recuperação pela internet: esquecer o PIN exige apagar os dados do app ou restaurar um backup, e isso é dito na hora de criar o PIN.
- Não é login: não existe conta, e-mail nem senha online.

### 7.13 Widgets

Três widgets nas duas plataformas:
1. **Hoje no salão:** próximos atendimentos do dia (hora, cliente, serviço); valores ocultos por padrão (opção para mostrar). Toque abre o atendimento; botão "+".
2. **Próximos dias:** atendimentos e eventos pessoais dos próximos N dias.
3. **Mês:** grade com marcação de dias com atendimentos/eventos; setas para trocar o mês.

Configuração: camadas incluídas, dias à frente, fundo, transparência, fonte, mostrar valores. Dados: Android — Glance lê o Room. iOS — snapshot JSON no App Group com atendimentos e eventos locais dos próximos 12 meses e as configurações; eventos sincronizados lidos direto do EventKit pela extensão; snapshot excluído do backup do iCloud.

### 7.14 Feriados e aniversários

- **Feriados:** `.ics` por país embutidos, gerados pela biblioteca Python `holidays` (MIT) por script fora do build (`tools/holidays/`), arquivos versionados. Importar cria agenda local somente-leitura "Feriados – <país>[ – <região>]".
- **Aniversários:** dos contatos (agenda local somente-leitura "Aniversários") **e das clientes** (aparecem no Início e no resumo diário, com atalho para mensagem de parabéns).

### 7.15 Importar/exportar ICS (agenda pessoal)

Igual à rev. 4: parser/escritor próprio em `:shared:ical` (VCALENDAR, VEVENT, VALARM, VTIMEZONE leitura, RRULE/EXDATE/RDATE, RECURRENCE-ID, line folding, escapes, UTF-8); deduplicação por `UID`; destinos: agendas locais (ambas) e do sistema (Android); iOS v1 só agendas locais. Atendimentos do salão **não** são exportados em `.ics` (contêm dados de clientes).

### 7.16 Backup e restauração (sem nuvem)

- **Android:** backup do Google Drive desligado (`dataExtractionRules` exclui `cloud-backup`, mantém `device-transfer`; `fullBackupContent` exclui tudo no Android 8–11).
- **iOS:** banco, configurações, fotos de clientes e snapshot do widget com `isExcludedFromBackup`; configurações fora de `UserDefaults`. Consequência: trocar de iPhone depende do arquivo de backup do app.
- **"Fazer backup" / "Restaurar"**: ZIP com JSON versionado (clientes e fotos, serviços, atendimentos, gastos, modelos de conta fixa, agendas pessoais locais, eventos, exceções, lembretes, configurações, widgets). Agendas sincronizadas não entram. Restaurar mostra resumo e pergunta "substituir" ou "mesclar" (por id/UID). O mesmo arquivo restaura em qualquer plataforma.
- **Aviso ao salvar:** "Este arquivo contém dados das suas clientes, incluindo informações de saúde (alergias). Guarde em local seguro."
- **Backup automático diário** opcional para uma pasta escolhida, mantendo os últimos 7.

## 8. Fases de entrega (cada fase termina com um APK utilizável)

O alvo iOS **compila no CI desde a fase 1**; as capacidades próprias do iOS chegam na fase 7.

1. **Fundação:** projeto KMP multi-módulo, CI (Android no Ubuntu; iOS simulador no macOS), checagens "sem rede", lint, tema da marca (direção B + referência), navegação com barra inferior, Room KMP, `Money` em centavos. App iOS "casca".
2. **Salão (Android):** tela de Entrada e bloqueio por PIN/digital, serviços, clientes com ficha completa (contatos e foto), atendimentos com status e "Concluir" (valor final), Início "o dia do salão", Agenda Dia/Semana/Mês/Lista com atendimentos, detalhe do atendimento, WhatsApp, assistente (resumo diário, lembrete por atendimento), busca, backup/restauração.
3. **Financeiro (Android):** Visão Geral, Ganhos e Projeção, Gastos e Contas, contas fixas recorrentes, materiais, avisos de conta no resumo, gráfico, microtextos.
4. **Agenda pessoal (Android):** eventos locais com motor de recorrência, `SystemSource` Android, permissões sob demanda, filtro de camadas, edição de eventos sincronizados, controle de duplicatas, aniversários (contatos e clientes), vista Ano.
5. **Widgets Android, feriados, ICS e personalização** (sons por agenda, cores, AMOLED).
6. **Polimento Android e lançamento:** acessibilidade (TalkBack, fontes grandes), RTL, desempenho, ícone, release assinado, metadados F-Droid.
7. **iOS:** implementações iOS de `:shared:platform`, notificações com janela de 64 e resumos pré-calculados, WidgetKit, ajustes de UI (VoiceOver, Dynamic Type, safe areas), ícone, TestFlight/App Store, rótulo de privacidade.

## 9. Testes

- `:shared:salon`: testes unitários em `commonTest` (JVM e simulador iOS) das **regras 1–4** e de cada indicador: agendar/confirmar/concluir/desmarcar/apagar/reabrir, valor final diferente do esperado, conclusão perto da meia-noite, períodos semana/mês/personalizado, primeiro dia da semana, sem baixa, contas fixas no dia 31 e em fevereiro, lançamento mensal sem duplicar, centavos sem perda.
- Resumo diário: dado um banco de teste, o texto gerado bate com o esperado (pt-BR e inglês), incluindo dia sem atendimentos (sem notificação).
- `:shared:recurrence` e `:shared:ical`: como na rev. 4.
- `:shared:data`: DAOs e backup/restauração ida e volta (com fotos).
- `SystemSource`: instrumentados (Android) e simulador (iOS), como na rev. 4.
- UI: fluxos críticos (agendar → confirmar → concluir com valor ajustado → ver no financeiro; desmarcar → previsão cai na hora; criar evento pessoal recorrente).
- CI falha se o manifest Android contiver `INTERNET` ou aparecer dependência fora da lista permitida.
- Screenshots das telas principais (claro/escuro, pt-BR/inglês) no CI.

**Aparelhos disponíveis (informado pelo usuário):** um celular Android e um iPhone; sem Mac e sem conta Apple Developer por enquanto. Android: ao fim de cada fase o CI publica um APK; o usuário instala e segue o roteiro de aceite da fase. iOS: só simulador no CI até existir a conta Apple Developer, necessária antes do fim da fase 7.

## 10. Decisões

**Decididas pelo usuário:** nome **LOMA**; licença **Apache-2.0**; Android mínimo **8.0**; sincronização da agenda pessoal pelas contas do sistema; **KMP + Compose Multiplatform**; backup sem nuvem com arquivo local e backup automático para pasta; **direção visual B + referência do Dribbble**; **as 10 telas da página "Telas do LOMA" aprovadas como estão** (inclui a tela de desbloqueio por PIN/digital); **ícone 6B "O menta"**; som de abertura **Brilho**, em versão lenta e progressiva de 4–5 s; **abertura animada aprovada** (§7.12b); LOMA é o **app do salão da família + agenda pessoal**; **cada profissional no próprio celular**, sem compartilhar agenda nem caixa, contas fixas pela parte de cada uma; **abordagem A** (dados do salão só no app) e **salão primeiro** na ordem das fases; botão **Confirmar pelo WhatsApp** (abre o WhatsApp; a profissional marca Confirmado); **lista de serviços** com nome, duração e preço; **aviso de conta** na véspera e no dia enquanto pendente; financeiro contabiliza **somente o valor** (sem forma de pagamento, taxa de maquininha, sinal ou fiado); **Lucro Líquido desconta só gastos já pagos**; **ficha completa** da cliente; módulo financeiro com 3 abas e as regras 1–4 como especificadas pelo usuário.

**Tomadas por quem escreveu este design (o usuário pode trocar):**
- Total de Gastos mostra pagos e a vencer separados (§7.5).
- Atendimentos sem baixa continuam em Ganhos Futuros e geram aviso.
- Preço do serviço é copiado no agendamento.
- "Desmarcado" com motivo (cancelou, não compareceu, remarcou).
- Conclusão pode ser desfeita ("Reabrir"); apagar concluído pede confirmação.
- Repetir atendimento cria cópias independentes.
- Seletor de período da Visão Geral não altera "Ganhos de Hoje" e "Ganhos do Mês".
- Resumo diário ligado por padrão às 8h; sem notificação em dia vazio; ação única "Abrir hoje".
- WhatsApp Business tem preferência; fallback SMS; modelo de mensagem editável.
- Apagar cliente mantém o histórico financeiro anônimo.
- Fotos de clientes guardadas no armazenamento privado do app e incluídas no backup.
- Widget "Hoje no salão" oculta valores por padrão.
- Fonte Outfit; cores dinâmicas do Android como opção desligada.
- Barra inferior Início · Agenda · Clientes · Financeiro; Ajustes pelo avatar.
- Bloqueio por PIN/digital opcional, bloqueia após 1 minuto em segundo plano; esquecer o PIN exige apagar dados ou restaurar backup.
- Identificadores `app.loma.calendar` / `group.app.loma.calendar`; iOS mínimo 17; pt-BR e inglês; repositório público no GitHub; `USE_EXACT_ALARM`.
- Itens da rev. 4 mantidos: não duplicar eventos sincronizados no banco; motor RRULE e ICS próprios; kizitonwose/Calendar; feriados com `holidays`; no iOS, lembretes de eventos sincronizados pelo próprio iOS (`EKAlarm`) e dados fora do backup do iCloud.

## 11. Fica a critério de quem construir

- Injeção de dependência (Koin ou manual) e biblioteca de navegação multiplataforma.
- Algoritmo de layout de eventos sobrepostos.
- Estrutura de pacotes, nomes de classes, formato exato do snapshot do widget e do backup (versionados).
- Biblioteca ou código próprio para o gráfico de colunas (licença permissiva).
- Detalhes visuais (espaçamentos, animações) dentro da direção escolhida.

## 12. Pontos em aberto

Nenhum. Respondidos pelo usuário na revisão: Lucro Líquido desconta só gastos pagos; o financeiro contabiliza somente o valor (sem forma de pagamento, taxa de maquininha, sinal ou fiado). Resolvidos por quem escreveu: data do pagamento na folha de conclusão (§7.4); semana segue o 1º dia configurado (§7.5).
