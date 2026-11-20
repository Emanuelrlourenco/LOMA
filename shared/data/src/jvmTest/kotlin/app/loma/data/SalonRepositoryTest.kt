// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import app.loma.salon.AppointmentRules
import app.loma.salon.AppointmentStatus
import app.loma.salon.Birthday
import app.loma.salon.CancelReason
import app.loma.salon.Client
import app.loma.salon.Money
import app.loma.salon.Service
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class SalonRepositoryTest {

    private val sp = TimeZone.of("America/Sao_Paulo")
    private fun at(day: Int, h: Int, m: Int = 0): Instant = LocalDateTime(2026, 11, day, h, m).toInstant(sp)
    private val agora = at(20, 10)
    private val clock = object : Clock { override fun now(): Instant = agora }

    private val file = File.createTempFile("loma-test", ".db").also { it.delete() }
    private val db = lomaDatabaseBuilder(file).buildLoma()
    private val repo = SalonRepository(db, clock)

    @AfterTest
    fun fechar() {
        db.close()
        file.delete()
    }

    private suspend fun servico(nome: String, min: Int, reais: Long, ordem: Int = 0): Service {
        val s = Service(name = nome, durationMinutes = min, price = Money.ofReais(reais), colorArgb = 0xFF5ED6B4, sortOrder = ordem)
        return s.copy(id = repo.saveService(s))
    }

    private suspend fun cliente(nome: String, foto: String? = null): Long =
        repo.saveClient(Client(name = nome, phoneDigits = "5511987654321", birthday = Birthday(14, 3), photoFile = foto, allergies = "amônia", createdAt = agora))

    @Test
    fun idaEVoltaDoAtendimento() = runTest {
        val c = cliente("Maria Souza")
        val a = AppointmentRules.book(c, listOf(servico("Coloração", 90, 180), servico("Escova", 60, 70, 1)), at(20, 13), sp)
        val id = repo.save(a)
        assertEquals(a.copy(id = id, services = a.services), repo.appointment(id).first())
        val lida = repo.client(c).first()!!
        assertEquals(Birthday(14, 3), lida.birthday)
        assertEquals("amônia", lida.allergies)
    }

    @Test
    fun diaIncluiConcluidoHojeDeOntem() = runTest {
        val c = cliente("Maria")
        val ontem = AppointmentRules.book(c, listOf(servico("Escova", 60, 70)), at(19, 15), sp)
        val id = repo.save(AppointmentRules.complete(ontem, Money.ofReais(70), at(20, 9), ""))
        assertEquals(listOf(id), repo.appointmentsOn(LocalDate(2026, 11, 20), sp).first().map { it.id })
        // Começou ontem: continua na lista de ontem também.
        assertEquals(listOf(id), repo.appointmentsOn(LocalDate(2026, 11, 19), sp).first().map { it.id })
        assertEquals(emptyList(), repo.appointmentsOn(LocalDate(2026, 11, 18), sp).first().map { it.id })
    }

    @Test
    fun apagarClienteDevolveFotoEDesmarcaFuturos() = runTest {
        val c = cliente("Maria", foto = "c1.jpg")
        val s = servico("Escova", 60, 70)
        val passado = repo.save(AppointmentRules.complete(AppointmentRules.book(c, listOf(s), at(10, 13), sp), Money.ofReais(70), at(10, 14), ""))
        val futuro = repo.save(AppointmentRules.book(c, listOf(s), at(25, 13), sp))

        assertEquals("c1.jpg", repo.deleteClient(c))

        val p = repo.appointment(passado).first()!!
        assertNull(p.clientId)
        assertEquals(AppointmentStatus.CONCLUIDO, p.status)
        assertEquals(AppointmentStatus.DESMARCADO, repo.appointment(futuro).first()!!.status)
        assertNull(repo.client(c).first())
    }

    @Test
    fun moverServicoTrocaOrdem() = runTest {
        servico("A", 30, 10, ordem = 0)
        val b = servico("B", 30, 10, ordem = 1)
        repo.moveService(b.id, up = true)
        assertEquals(listOf("B", "A"), repo.services().first().map { it.name })
    }

    @Test
    fun flowReageNaHora() = runTest {
        val c = cliente("Maria")
        val id = repo.save(AppointmentRules.book(c, listOf(servico("Escova", 60, 70)), at(20, 13), sp))
        val hoje = LocalDate(2026, 11, 20)
        assertEquals(AppointmentStatus.AGENDADO, repo.appointmentsOn(hoje, sp).first().single().status)

        val a = repo.appointment(id).first()!!
        repo.save(AppointmentRules.unschedule(a, CancelReason.CANCELOU))

        assertEquals(AppointmentStatus.DESMARCADO, repo.appointmentsOn(hoje, sp).first().single().status)
    }

    @Test
    fun semBaixaEAjustes() = runTest {
        val c = cliente("Maria")
        repo.save(AppointmentRules.book(c, listOf(servico("Escova", 60, 70)), at(19, 13), sp))
        assertEquals(1, repo.withoutSettlement().first().size)

        assertNull(repo.setting(SettingKeys.PROFILE_NAME).first())
        repo.putSetting(SettingKeys.PROFILE_NAME, "Carla")
        assertEquals("Carla", repo.setting(SettingKeys.PROFILE_NAME).first())
    }
}
