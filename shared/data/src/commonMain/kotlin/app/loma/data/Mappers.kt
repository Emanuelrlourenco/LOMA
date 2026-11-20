// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import app.loma.salon.Appointment
import app.loma.salon.AppointmentStatus
import app.loma.salon.Birthday
import app.loma.salon.BookedService
import app.loma.salon.CancelReason
import app.loma.salon.Client
import app.loma.salon.Money
import app.loma.salon.Service
import kotlin.time.Instant

internal fun ServiceEntity.toDomain() = Service(id, name, durationMinutes, Money(priceCents), colorArgb, active, sortOrder)

internal fun Service.toEntity() = ServiceEntity(id, name, durationMinutes, price.cents, colorArgb, active, sortOrder)

internal fun ClientEntity.toDomain() = Client(
    id = id,
    name = name,
    phoneDigits = phoneDigits,
    birthday = if (birthdayDay != null && birthdayMonth != null) Birthday(birthdayDay, birthdayMonth, birthdayYear) else null,
    photoFile = photoFile,
    allergies = allergies,
    preferences = preferences,
    notes = notes,
    createdAt = Instant.fromEpochMilliseconds(createdAtMs),
    archived = archived,
)

internal fun Client.toEntity() = ClientEntity(
    id = id,
    name = name,
    phoneDigits = phoneDigits,
    birthdayDay = birthday?.day,
    birthdayMonth = birthday?.month,
    birthdayYear = birthday?.year,
    photoFile = photoFile,
    allergies = allergies,
    preferences = preferences,
    notes = notes,
    createdAtMs = createdAt.toEpochMilliseconds(),
    archived = archived,
)

internal fun AppointmentWithServices.toDomain() = Appointment(
    id = appointment.id,
    clientId = appointment.clientId,
    start = Instant.fromEpochMilliseconds(appointment.startMs),
    end = Instant.fromEpochMilliseconds(appointment.endMs),
    zoneId = appointment.zoneId,
    status = AppointmentStatus.valueOf(appointment.status),
    cancelReason = appointment.cancelReason?.let { CancelReason.valueOf(it) },
    expected = Money(appointment.expectedCents),
    finalValue = appointment.finalCents?.let { Money(it) },
    completedAt = appointment.completedAtMs?.let { Instant.fromEpochMilliseconds(it) },
    note = appointment.note,
    services = orderedServices().map { BookedService(it.serviceId, it.name, it.durationMinutes, Money(it.priceCents)) },
)

internal fun Appointment.toEntity() = AppointmentEntity(
    id = id,
    clientId = clientId,
    startMs = start.toEpochMilliseconds(),
    endMs = end.toEpochMilliseconds(),
    zoneId = zoneId,
    status = status.name,
    cancelReason = cancelReason?.name,
    expectedCents = expected.cents,
    finalCents = finalValue?.cents,
    completedAtMs = completedAt?.toEpochMilliseconds(),
    note = note,
)

internal fun Appointment.serviceEntities() = services.mapIndexed { i, s ->
    AppointmentServiceEntity(appointmentId = id, serviceId = s.serviceId, name = s.name, durationMinutes = s.durationMinutes, priceCents = s.price.cents, position = i)
}
