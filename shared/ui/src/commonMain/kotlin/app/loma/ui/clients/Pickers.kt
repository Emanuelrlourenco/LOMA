// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.clients

import androidx.compose.runtime.Composable

data class PickedContact(val name: String, val phone: String?)

/** Seletor de contatos do sistema (sem permissão). `null` quando a plataforma não tem. */
@Composable
expect fun rememberContactPicker(onPicked: (PickedContact?) -> Unit): (() -> Unit)?

/** Seletor de fotos do sistema (sem permissão); devolve o endereço da foto escolhida. */
@Composable
expect fun rememberPhotoPicker(onPicked: (String?) -> Unit): (() -> Unit)?
