// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

/** Chaves da tabela de ajustes. */
object SettingKeys {
    const val PROFILE_NAME = "perfil.nome"
    /** Arquivo da foto do perfil no armazenamento do app; vazio = sem foto. */
    const val PROFILE_PHOTO = "perfil.foto"
    /** Tema escolhido no botão do Início: "claro" ou "escuro"; ausente = segue o celular. */
    const val THEME = "tema"
    const val WHATSAPP_TEMPLATE = "whatsapp.modelo"
    const val WHATSAPP_APP = "whatsapp.app"
}
