// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Relation

/** Serviço de um atendimento, copiado no agendamento (nome, duração e preço). */
@Entity(
    tableName = "appointment_services",
    foreignKeys = [
        ForeignKey(entity = AppointmentEntity::class, parentColumns = ["id"], childColumns = ["appointmentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ServiceEntity::class, parentColumns = ["id"], childColumns = ["serviceId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("appointmentId"), Index("serviceId")],
)
data class AppointmentServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appointmentId: Long,
    val serviceId: Long?,
    val name: String,
    val durationMinutes: Int,
    val priceCents: Long,
    val position: Int,
)

/** Atendimento com os seus serviços. */
data class AppointmentWithServices(
    @Embedded val appointment: AppointmentEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["appointmentId"])
    val services: List<AppointmentServiceEntity>,
) {
    fun orderedServices(): List<AppointmentServiceEntity> = services.sortedBy { it.position }
}
