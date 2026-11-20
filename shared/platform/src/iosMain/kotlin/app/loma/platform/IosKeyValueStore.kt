// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.platform

import platform.Foundation.NSUserDefaults

class IosKeyValueStore(private val prefix: String = "loma.seguranca.") : KeyValueStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun get(key: String): String? = defaults.stringForKey(prefix + key)

    override fun put(key: String, value: String?) {
        if (value == null) defaults.removeObjectForKey(prefix + key) else defaults.setObject(value, prefix + key)
    }
}
