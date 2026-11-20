// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

import kotlin.time.Instant

enum class AppointmentStatus { AGENDADO, CONFIRMADO, CONCLUIDO, DESMARCADO }

enum class CancelReason { CANCELOU, NAO_COMPARECEU, REMARCOU }

/** Serviço como foi agendado: nome, duração e preço copiados no momento do agendamento. */
data class BookedService(val serviceId: Long?, val name: String, val durationMinutes: Int, val price: Money)

/** Atendimento. Início/fim em instantes; [zoneId] é o fuso do momento do agendamento. */
data class Appointment(
    val id: Long = 0,
    val clientId: Long?,
    val start: Instant,
    val end: Instant,
    val zoneId: String,
    val status: AppointmentStatus,
    val cancelReason: CancelReason? = null,
    val expected: Money,
    val finalValue: Money? = null,
    val completedAt: Instant? = null,
    val note: String = "",
    val services: List<BookedService>,
) {
    /** Agendado ou confirmado: ainda entra na previsão. */
    val isOpen: Boolean get() = status == AppointmentStatus.AGENDADO || status == AppointmentStatus.CONFIRMADO
}
