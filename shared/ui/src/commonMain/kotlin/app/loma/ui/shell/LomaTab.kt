// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.shell

/** As quatro abas da barra inferior, na ordem em que aparecem. */
enum class LomaTab { Inicio, Agenda, Clientes, Financeiro }

/** Aba para onde "voltar" leva; `null` no Início deixa o sistema fechar o app. */
fun LomaTab.back(): LomaTab? = if (this == LomaTab.Inicio) null else LomaTab.Inicio
