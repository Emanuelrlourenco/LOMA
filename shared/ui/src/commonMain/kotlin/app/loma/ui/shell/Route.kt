// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.shell

/** Telas empilhadas por cima das abas. Cada rota vira texto para ser salva com o estado. */
sealed interface Route {
    data class ClientDetail(val id: Long) : Route
    data class ClientEdit(val id: Long?) : Route
    data class AppointmentDetail(val id: Long) : Route
    data class AppointmentEdit(val id: Long?, val clientId: Long?, val rescheduleOf: Long?) : Route
    data class CompleteAppointment(val id: Long) : Route
    data object Settings : Route
    data object Services : Route
    data class ServiceEdit(val id: Long?) : Route
    data object MessageTemplate : Route

    fun encode(): String = when (this) {
        is ClientDetail -> "ClientDetail:$id"
        is ClientEdit -> "ClientEdit:${id.orDash()}"
        is AppointmentDetail -> "AppointmentDetail:$id"
        is AppointmentEdit -> "AppointmentEdit:${id.orDash()},${clientId.orDash()},${rescheduleOf.orDash()}"
        is CompleteAppointment -> "CompleteAppointment:$id"
        Settings -> "Settings:"
        Services -> "Services:"
        is ServiceEdit -> "ServiceEdit:${id.orDash()}"
        MessageTemplate -> "MessageTemplate:"
    }

    companion object {
        fun decode(text: String): Route {
            val name = text.substringBefore(':')
            val args = text.substringAfter(':').split(',').map { if (it == "-" || it.isEmpty()) null else it.toLong() }
            return when (name) {
                "ClientDetail" -> ClientDetail(args[0]!!)
                "ClientEdit" -> ClientEdit(args[0])
                "AppointmentDetail" -> AppointmentDetail(args[0]!!)
                "AppointmentEdit" -> AppointmentEdit(args[0], args[1], args[2])
                "CompleteAppointment" -> CompleteAppointment(args[0]!!)
                "Settings" -> Settings
                "Services" -> Services
                "ServiceEdit" -> ServiceEdit(args[0])
                "MessageTemplate" -> MessageTemplate
                else -> error("rota desconhecida: $text")
            }
        }

        private fun Long?.orDash(): String = this?.toString() ?: "-"
    }
}
