# LOMA — Fase 3: Financeiro — plano

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans. Steps use `- [ ]`.

**Goal:** aba Financeiro com Visão Geral, Ganhos e Gastos (telas T-Fin-Geral, T-Fin-Ganhos, T-Fin-Gastos), contas fixas mensais, materiais e aviso de conta no Início.

**Architecture:** regras puras em `:shared:salon` (`Finance`, `Period`, `Expense`, `RecurringExpense`), tudo derivado de consultas (nada acumulado); tabelas `expenses` e `recurring_expenses` no Room (versão 3, migração automática 2→3); telas em `ui/finance`.

**Tech Stack:** Kotlin 2.4.20, CMP 1.12.2 (material3 `DateRangePicker`), Room3 3.0.3, kotlinx-datetime 0.8.0.

**Spec:** `docs/design/2026-11-19-loma-design.md` §7.5, §8 (Financeiro) e telas `docs/design/telas/T-Fin-*.dc.html`.
Decisões aprovadas em 20/11: aviso de conta dentro do app (Início e Gastos; notificação fica para a 2C); semana começa no domingo; tela Novo gasto no estilo das outras.

## Global Constraints
- Somas em centavos (`Money`), por período de calendário no fuso do aparelho; nada guardado como total.
- Regras 1–4 (§7.5): agendar entra em A receber; concluir sai da previsão e soma o **valor final** pelo dia da conclusão; desmarcar/apagar tira da previsão sem tocar nos realizados; valor final editável ao concluir (já existe).
- Lucro líquido = ganhos realizados − **gastos pagos** no período (materiais + contas fixas com `paidOn` no período).
- Total de gastos = materiais com data no período + contas fixas com vencimento no período (pagas e pendentes), com "R$ X pagos · R$ Y a vencer".
- Conta fixa mensal: vencimento no dia escolhido; mês sem esse dia → último dia; nunca duplica (único `recurringId+year+month`); lança mês corrente e seguinte ao abrir o app; não lança meses anteriores à criação; editar o modelo afeta só meses não lançados.
- Materiais lançados já pagos na data informada. Categorias fixas: Aluguel, Energia, Água, Internet, Outra; materiais: Produtos, Descartáveis, Equipamentos, Outro.
- Microtextos e textos pt/en exatamente como as telas aprovadas.
- Semana: domingo a sábado.

## Review Focus
1. Conclusão perto da meia-noite e atendimento de ontem pago hoje: conta no dia da conclusão.
2. Conta fixa com dia 31 em fevereiro, abril etc.; abrir o app várias vezes no mês não duplica.
3. Conta paga em outro mês que o vencimento: entra no lucro do mês em que foi paga e no total de gastos do mês do vencimento.
4. Cliente apagada: ganhos realizados continuam e aparecem como "Cliente removida".
5. Período personalizado de um só dia e cruzando meses (gráfico e totais).

---

### Task 1: regras do financeiro (domínio)
**Files:** `shared/salon/src/commonMain/kotlin/app/loma/salon/Expense.kt`, `Finance.kt`; testes `shared/salon/src/commonTest/kotlin/app/loma/salon/FinanceTest.kt`, `ExpenseTest.kt`.

**Produces:**
```kotlin
enum class ExpenseKind { FIXA, MATERIAL }
enum class ExpenseCategory(val kind: ExpenseKind) { ALUGUEL(FIXA), ENERGIA(FIXA), AGUA(FIXA), INTERNET(FIXA), OUTRA_FIXA(FIXA), PRODUTOS(MATERIAL), DESCARTAVEIS(MATERIAL), EQUIPAMENTOS(MATERIAL), OUTRO_MATERIAL(MATERIAL) }
data class Expense(val id: Long = 0, val category: ExpenseCategory, val name: String, val amount: Money,
    val date: LocalDate /* vencimento (fixa) ou data (material) */, val paidOn: LocalDate?, val recurringId: Long? = null, val note: String = "") {
    val kind: ExpenseKind get() = category.kind; val paid: Boolean get() = paidOn != null }
data class RecurringExpense(val id: Long = 0, val category: ExpenseCategory, val name: String, val amount: Money,
    val dueDay: Int, val startYear: Int, val startMonth: Int, val active: Boolean = true) {
    fun dueDate(year: Int, month: Int): LocalDate          // dia > último do mês → último dia
    fun billFor(year: Int, month: Int): Expense? }          // null antes do início ou inativo; PENDENTE, recurringId = id
data class Period(val start: LocalDate, val endExclusive: LocalDate) {
    companion object { fun day(d: LocalDate); fun week(d: LocalDate) /* domingo */; fun month(d: LocalDate); fun custom(from: LocalDate, toInclusive: LocalDate) } }
data class ExpenseTotals(val total: Money, val paid: Money, val due: Money)
data class Column(val label: String, val income: Money, val outcome: Money)
object Finance {
    fun realized(a: List<Appointment>, p: Period, zone: TimeZone): Money          // CONCLUIDO com completedAt no período
    fun realizedList(a: List<Appointment>, p: Period, zone: TimeZone): List<Appointment> // mais recente primeiro
    fun receivable(a: List<Appointment>, p: Period?, now: Instant, zone: TimeZone): Money // abertos com início no período (null = todos) + sem baixa
    fun expenseTotals(e: List<Expense>, p: Period): ExpenseTotals                 // pelo campo date
    fun paidIn(e: List<Expense>, p: Period): Money                                // pelo paidOn
    fun netProfit(a: List<Appointment>, e: List<Expense>, p: Period, zone: TimeZone): Money
    fun columns(a: List<Appointment>, e: List<Expense>, p: Period, zone: TimeZone): List<Column> }
```
`columns`: período ≤ 7 dias → uma coluna por dia (rótulo = dia do mês, "20"); senão blocos de 7 dias a partir do início, rótulo "1–7" (dias do mês; cruzando meses "28/11–4/12"); entrada = realizados, saída = pagos.

- [ ] **RED** `FinanceTest`: `agendarEntraEmAReceber`; `concluirSaiDaPrevisaoESomaValorFinal` (esperado 250, final 230 → realizados 230, a receber 0); `desmarcarTiraDaPrevisaoSemTocarRealizados`; `conclusaoPertoDaMeiaNoiteContaNoDiaDaConclusao` (23:59 dia 19 vs 00:01 dia 20); `pagoHojeDeAtendimentoDeOntem`; `semBaixaContinuaEmAReceber`; `receberRestanteDaSemana` (só início ≥ hoje até sábado + sem baixa); `semanaComecaNoDomingo` (`Period.week(sex 20/11/2026)` = 15/11..22/11); `lucroDescontaSoPagos` (4380 recebidos, gastos 1420 pagos + 180 pendentes → 2960; totals 1600/1420/180 — números da tela aprovada); `contaPagaEmOutroMes`; `centavosSemPerda` (3 × 33,33); `colunasDoMes` (rótulos "1–7","8–14","15–21","22–28","29–30" para novembro); `colunasDeUmDia`; `colunasCruzandoMeses`.
- [ ] **RED** `ExpenseTest`: `dia31EmFevereiro` (2026-02-28; 2028-02-29), `dia31EmAbril` (30), `antesDoInicioNaoLanca`, `inativoNaoLanca`, `contaLancadaPendente`.
- [ ] Rodar `:shared:salon:jvmTest` no CI → falha de compilação. **GREEN**, commit.

### Task 2: banco e repositório
**Files:** `shared/data/.../ExpenseEntity.kt`, `RecurringExpenseEntity.kt`, `ExpenseDao.kt`, `LomaDatabase.kt` (versão 3, `AutoMigration(2, 3)`), `Mappers.kt`, `SalonRepository.kt`, `AppointmentDao.kt`; `schemas/.../3.json` (tirado do log do CI); testes `ExpenseRepositoryTest.kt`, `MigrationTest.kt`.

**Produces (SalonRepository):**
`fun expensesDatedBetween(from: LocalDate, toExclusive: LocalDate): Flow<List<Expense>>`;
`fun expensesPaidBetween(from: LocalDate, toExclusive: LocalDate): Flow<List<Expense>>`;
`fun pendingFixedDueBefore(toExclusive: LocalDate): Flow<List<Expense>>` (vencidas + do mês, para Gastos e Início);
`fun expense(id: Long): Flow<Expense?>`; `suspend fun saveExpense(e: Expense): Long`; `suspend fun deleteExpense(id: Long)`;
`suspend fun setPaid(id: Long, paidOn: LocalDate?)`;
`fun recurring(id: Long): Flow<RecurringExpense?>`; `suspend fun saveRecurring(r: RecurringExpense): Long`;
`suspend fun ensureRecurringLaunched(today: LocalDate)` (mês de hoje e o seguinte; `INSERT OR IGNORE` com índice único);
`fun openAppointments(): Flow<List<Appointment>>`; `fun completedBetween(from: LocalDate, toExclusive: LocalDate, zone: TimeZone): Flow<List<Appointment>>`.
Datas guardadas como `epochDay` (Long); `expenses.recurringId` FK `SET NULL`; índices em `dateDay` e `paidDay`.

- [ ] **RED** `ExpenseRepositoryTest`: `materialSalvoComoPago`; `lancaMesAtualESeguinteSemDuplicar` (3 chamadas → 2 contas); `naoLancaMesesAntigos`; `editarModeloAfetaSoMesesNaoLancados`; `pagarEDesfazer`; `pendentesAteFimDoMes` (inclui vencida do mês anterior); `abertosEConcluidosNoPeriodo`. `MigrationTest.migra2para3` (atendimentos preservados; tabelas `expenses`, `recurring_expenses`).
- [ ] **GREEN**; extrair `3.json` do log e versionar; commit.

### Task 3: Financeiro — abas e Visão Geral
**Files:** `ui/finance/FinanceScreen.kt`, `ui/finance/OverviewTab.kt`, `ui/finance/FinanceChart.kt`, `shell/LomaApp.kt`, strings; testes `jvmTest/.../finance/OverviewTabTest.kt`, captura `financeiro-geral`.

**Produces:** `FinanceScreen(initialTab: FinanceTab = FinanceTab.Geral)`, `enum class FinanceTab { Geral, Ganhos, Gastos }` (tags `fin_tab_geral|ganhos|gastos`, título `screen_title` "Financeiro"); `LocalOpenFinance: (FinanceTab) -> Unit` fornecido pelo `LomaRoot` (troca para a aba Financeiro na sub-aba pedida).
Tags: `period_today|week|month|custom`, `net_profit`, `card_today`, `card_month`, `card_receivable`, `card_expenses`, `chart`.
Textos da tela aprovada: "Lucro líquido · novembro", "Valor que realmente sobrou no caixa", "R$ 4.380 recebidos − R$ 1.420 em gastos pagos", "Atendimentos concluídos e pagos hoje", "Concluídos em novembro até agora", "Agendados que ainda serão atendidos", "R$ 1.420 pagos · R$ 180 a vencer", "Entradas × Saídas", legendas "Entradas"/"Saídas". Personalizado abre `DateRangePicker`.
Ganhos de hoje e do mês não mudam com o seletor.

- [ ] **RED**: `abaFinanceiroMostraVisaoGeral`; `lucroDoMesComNumerosDaTela` (dados da tela aprovada → "R$ 2.960,00"); `seletorNaoMudaGanhosDeHojeEDoMes`; `desmarcarBaixaAReceberNaHora`; `concluirPassaParaGanhos`.
- [ ] **GREEN**, captura, commit.

### Task 4: Ganhos
**Files:** `ui/finance/EarningsTab.kt`, strings; `EarningsTabTest.kt`, captura `financeiro-ganhos`.
Previsão: "Restante da semana", "Restante do mês" + texto explicativo da tela; aviso de sem baixa quando houver (abre o atendimento). Realizados: alternância Dia | Mês com ‹ › (tags `earn_day`, `earn_month`, `earn_prev`, `earn_next`), cabeçalho "Novembro · 31 atendimentos", "Total R$ 4.380", itens (iniciais, nome, serviços, "hoje, 10:00" / "19/11, 17:00" pelo início, valor final), mais recente primeiro; tocar abre o atendimento; cliente apagada = "Cliente removida".

- [ ] **RED**: `restanteDaSemanaEDoMes`; `realizadosDoMesComTotal`; `filtroDiaENavegacao`; `clienteRemovidaNaLista`; `tocarAbreAtendimento`.
- [ ] **GREEN**, captura, commit.

### Task 5: Gastos, novo/editar gasto e aviso de conta
**Files:** `ui/finance/ExpensesTab.kt`, `ui/finance/ExpenseEditScreen.kt`, `shell/Route.kt` (`Route.ExpenseEdit(id: Long?)`), `home/HomeScreen.kt`, `shell/LomaApp.kt` (`ensureRecurringLaunched` ao abrir), `shell/LomaIcons.kt` (casa/luz/água/internet da tela), strings; `ExpensesTabTest.kt`, `ExpenseEditScreenTest.kt`, `HomeScreenTest`; capturas `financeiro-gastos`, `novo-gasto`.
Gastos: "Total pago" (pagos no mês) × "Contas a vencer no mês"; "Contas fixas do salão" (do mês + vencidas pendentes; "Venceu 10/11 · fixa mensal", "Vence amanhã, 21/11 · fixa mensal", "Vence hoje"), pílula Pago/Pendente (`bill_toggle_<id>`); "Materiais" ("12/11 · Produtos"); botão "+ Novo gasto" (`new_expense`).
Novo/editar: tipo Conta fixa | Material (`expense_kind_fixa|material`), categoria (chips), nome (Outra/Outro: livre; demais preenchem o nome padrão, editável), valor (`MoneyField`), data/vencimento, "Despesa fixa mensal" (só conta fixa; cria/atualiza o modelo com o dia do vencimento; desligar inativa o modelo), "Pago" (conta fixa), observação, Salvar (`expense_save`), Apagar (`expense_delete`, com confirmação).
Início: aviso coral para contas pendentes vencidas, de hoje ou de amanhã: 1 conta → "Água vence amanhã · R$ 60" / "Água vence hoje" / "Água venceu 21/11"; várias → "3 contas a pagar até amanhã"; toque abre Financeiro › Gastos.

- [ ] **RED**: `totaisDoTopo`; `pagarPelaPilula` (Pendente → Pago grava hoje; e volta); `contaVencidaDoMesPassadoAparece`; `novoMaterialEntraPago`; `novaContaFixaMensalCriaModeloELanca`; `editarValorDoMes`; `apagarPedeConfirmacao`; Início: `avisoContaVenceAmanha`, `avisoVariasContas`, `avisoAbreGastos`.
- [ ] **GREEN**, capturas, commit.

### Task 6: aceite, versão e PR
- [ ] `docs/aceite/fase-3-financeiro.md`; `versionName = "0.4.$runNumber"`; conferir capturas com as telas aprovadas; suíte verde (Android e iOS); revisão final; PR.
