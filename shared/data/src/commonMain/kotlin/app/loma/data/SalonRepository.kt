// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import app.loma.salon.Appointment
import app.loma.salon.Client
import app.loma.salon.Service
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

/** Acesso do app aos dados do salão, sempre em tipos do domínio e com fluxos que reagem na hora. */
class SalonRepository(db: LomaDatabase, private val clock: Clock) {

    private val services = db.serviceDao()
    private val clients = db.clientDao()
    private val appointments = db.appointmentDao()
    private val settings = db.settingDao()

    // Serviços

    fun services(includeInactive: Boolean = false): Flow<List<Service>> =
        (if (includeInactive) services.observeAll() else services.observeActive()).map { list -> list.map { it.toDomain() } }

    suspend fun saveService(s: Service): Long =
        if (s.id == 0L) services.insert(s.toEntity()) else s.id.also { services.update(s.toEntity()) }

    suspend fun moveService(id: Long, up: Boolean) {
        val list = services.observeAll().first().toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in list.indices) return
        list.add(j, list.removeAt(i))
        list.forEachIndexed { order, s -> if (s.sortOrder != order) services.setOrder(s.id, order) }
    }

    // Clientes

    fun clients(includeArchived: Boolean = false): Flow<List<Client>> =
        clients.observeAll(includeArchived).map { list -> list.map { it.toDomain() } }

    fun client(id: Long): Flow<Client?> = clients.observeById(id).map { it?.toDomain() }

    suspend fun saveClient(c: Client): Long =
        if (c.id == 0L) clients.insert(c.toEntity()) else c.id.also { clients.update(c.toEntity()) }

    suspend fun setArchived(id: Long, archived: Boolean) = clients.setArchived(id, archived)

    /** Desmarca os futuros, apaga a ficha (concluídos ficam como "Cliente removida") e devolve a foto a apagar. */
    suspend fun deleteClient(id: Long): String? {
        val photo = clients.getById(id)?.photoFile
        appointments.unscheduleFutureOfClient(id, clock.now().toEpochMilliseconds())
        clients.delete(id)
        return photo
    }

    // Atendimentos

    /** Atendimentos que começam no dia ou foram concluídos (pagos) no dia. */
    fun appointmentsOn(day: LocalDate, zone: TimeZone): Flow<List<Appointment>> {
        val from = day.atStartOfDayIn(zone).toEpochMilliseconds()
        val to = day.plus(DatePeriod(days = 1)).atStartOfDayIn(zone).toEpochMilliseconds()
        return combine(appointments.observeBetween(from, to), appointments.observeCompletedBetween(from, to)) { started, paid ->
            (started + paid).distinctBy { it.appointment.id }.map { it.toDomain() }.sortedWith(compareBy({ it.start }, { it.id }))
        }
    }

    fun appointmentsBetween(from: LocalDate, toExclusive: LocalDate, zone: TimeZone): Flow<List<Appointment>> =
        appointments.observeBetween(from.atStartOfDayIn(zone).toEpochMilliseconds(), toExclusive.atStartOfDayIn(zone).toEpochMilliseconds())
            .map { list -> list.map { it.toDomain() } }

    fun appointment(id: Long): Flow<Appointment?> = appointments.observeById(id).map { it?.toDomain() }

    fun appointmentsOfClient(clientId: Long): Flow<List<Appointment>> =
        appointments.observeByClient(clientId).map { list -> list.map { it.toDomain() } }

    /** Abertos que já terminaram ("sem baixa"), em relação ao momento da inscrição. */
    fun withoutSettlement(): Flow<List<Appointment>> =
        appointments.observeOpenEndingBefore(clock.now().toEpochMilliseconds()).map { list -> list.map { it.toDomain() } }

    suspend fun save(a: Appointment): Long =
        if (a.id == 0L) {
            appointments.insertWithServices(a.toEntity(), a.serviceEntities())
        } else {
            appointments.replaceWithServices(a.toEntity(), a.serviceEntities())
            a.id
        }

    suspend fun saveAll(list: List<Appointment>) = list.forEach { save(it) }

    suspend fun delete(id: Long) = appointments.delete(id)

    // Ajustes

    fun setting(key: String): Flow<String?> = settings.observe(key)

    suspend fun putSetting(key: String, value: String) = settings.put(SettingEntity(key, value))
}
