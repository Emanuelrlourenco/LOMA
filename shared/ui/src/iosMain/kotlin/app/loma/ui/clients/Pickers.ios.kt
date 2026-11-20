// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.clients

import androidx.compose.runtime.Composable

@Composable
actual fun rememberContactPicker(onPicked: (PickedContact?) -> Unit): (() -> Unit)? = null

@Composable
actual fun rememberPhotoPicker(onPicked: (String?) -> Unit): (() -> Unit)? = null
