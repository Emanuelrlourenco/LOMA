// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AppointmentDao {
    @Insert
    protected abstract suspend fun insertAppointment(a: AppointmentEntity): Long

    @Update
    protected abstract suspend fun updateAppointment(a: AppointmentEntity)

    @Insert
    protected abstract suspend fun insertServices(services: List<AppointmentServiceEntity>)

    @Query("DELETE FROM appointment_services WHERE appointmentId = :appointmentId")
    protected abstract suspend fun deleteServicesOf(appointmentId: Long)

    @Transaction
    open suspend fun insertWithServices(a: AppointmentEntity, services: List<AppointmentServiceEntity>): Long {
        val id = insertAppointment(a.copy(id = 0))
        insertServices(services.map { it.copy(id = 0, appointmentId = id) })
        return id
    }

    @Transaction
    open suspend fun replaceWithServices(a: AppointmentEntity, services: List<AppointmentServiceEntity>) {
        updateAppointment(a)
        deleteServicesOf(a.id)
        insertServices(services.map { it.copy(id = 0, appointmentId = a.id) })
    }

    @Transaction
    @Query("SELECT * FROM appointments WHERE startMs >= :fromMs AND startMs < :toMs ORDER BY startMs, id")
    abstract fun observeBetween(fromMs: Long, toMs: Long): Flow<List<AppointmentWithServices>>

    @Transaction
    @Query("SELECT * FROM appointments WHERE completedAtMs >= :fromMs AND completedAtMs < :toMs ORDER BY startMs, id")
    abstract fun observeCompletedBetween(fromMs: Long, toMs: Long): Flow<List<AppointmentWithServices>>

    @Transaction
    @Query("SELECT * FROM appointments WHERE id = :id")
    abstract fun observeById(id: Long): Flow<AppointmentWithServices?>

    @Transaction
    @Query("SELECT * FROM appointments WHERE clientId = :clientId ORDER BY startMs DESC, id DESC")
    abstract fun observeByClient(clientId: Long): Flow<List<AppointmentWithServices>>

    @Transaction
    @Query("SELECT * FROM appointments WHERE status IN ('AGENDADO', 'CONFIRMADO') AND endMs < :nowMs ORDER BY startMs, id")
    abstract fun observeOpenEndingBefore(nowMs: Long): Flow<List<AppointmentWithServices>>

    @Query("DELETE FROM appointments WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM appointment_services WHERE appointmentId = :appointmentId")
    abstract suspend fun countServicesOf(appointmentId: Long): Int

    @Query(
        "UPDATE appointments SET status = 'DESMARCADO', cancelReason = 'CANCELOU' " +
            "WHERE clientId = :clientId AND status IN ('AGENDADO', 'CONFIRMADO') AND startMs >= :nowMs",
    )
    abstract suspend fun unscheduleFutureOfClient(clientId: Long, nowMs: Long)
}
