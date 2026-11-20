// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.clients

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberContactPicker(onPicked: (PickedContact?) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode != Activity.RESULT_OK || uri == null) {
            callback(null)
        } else {
            // O seletor dá acesso só a este contato; nome e telefone (o aniversário não vem sem permissão).
            val picked = runCatching {
                context.contentResolver.query(uri, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER), null, null, null)?.use { c ->
                    if (c.moveToFirst()) PickedContact(c.getString(0).orEmpty(), c.getString(1)) else null
                }
            }.getOrNull()
            callback(picked)
        }
    }
    return remember(launcher) { { launcher.launch(Intent(Intent.ACTION_PICK, Phone.CONTENT_URI)) } }
}

@Composable
actual fun rememberPhotoPicker(onPicked: (String?) -> Unit): (() -> Unit)? {
    val callback by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> callback(uri?.toString()) }
    return remember(launcher) { { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
}
