// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class AppointmentDaoTest {

    private val file = File.createTempFile("loma-test", ".db").also { it.delete() }
    private val db = lomaDatabaseBuilder(file).buildLoma()
    private val appointments = db.appointmentDao()
    private val clients = db.clientDao()

    @AfterTest
    fun fechar() {
        db.close()
        file.delete()
    }

    private val agora = 1_800_000_000_000L
    private val hora = 3_600_000L

    private fun cliente(nome: String) = ClientEntity(
        name = nome, phoneDigits = null, birthdayDay = null, birthdayMonth = null, birthdayYear = null, photoFile = null,
        allergies = "", preferences = "", notes = "", createdAtMs = agora, archived = false,
    )

    private fun atendimento(clientId: Long?, startMs: Long, status: String) = AppointmentEntity(
        clientId = clientId, startMs = startMs, endMs = startMs + hora, zoneId = "America/Sao_Paulo", status = status,
        cancelReason = null, expectedCents = 10_000, finalCents = if (status == "CONCLUIDO") 10_000 else null,
        completedAtMs = if (status == "CONCLUIDO") startMs + hora else null, note = "",
    )

    private fun servico(nome: String, posicao: Int) = AppointmentServiceEntity(
        appointmentId = 0, serviceId = null, name = nome, durationMinutes = 30, priceCents = 5_000, position = posicao,
    )

    @Test
    fun servicosVoltamNaOrdem() = runTest {
        val c = clients.insert(cliente("Maria"))
        val id = appointments.insertWithServices(atendimento(c, agora, "AGENDADO"), listOf(servico("Escova", 1), servico("Coloração", 0)))
        val lido = appointments.observeById(id).first()!!
        assertEquals(listOf("Coloração", "Escova"), lido.orderedServices().map { it.name })
    }

    @Test
    fun apagarClienteMantemHistorico() = runTest {
        val c = clients.insert(cliente("Maria"))
        val id = appointments.insertWithServices(atendimento(c, agora - 24 * hora, "CONCLUIDO"), listOf(servico("Escova", 0)))
        clients.delete(c)
        val lido = appointments.observeById(id).first()!!
        assertNull(lido.appointment.clientId)
        assertEquals("CONCLUIDO", lido.appointment.status)
    }

    @Test
    fun apagarAtendimentoApagaServicos() = runTest {
        val id = appointments.insertWithServices(atendimento(null, agora, "AGENDADO"), listOf(servico("Escova", 0), servico("Corte", 1)))
        appointments.delete(id)
        assertEquals(0, appointments.countServicesOf(id))
    }

    @Test
    fun desmarcaFuturosDaCliente() = runTest {
        val c = clients.insert(cliente("Maria"))
        val passado = appointments.insertWithServices(atendimento(c, agora - 2 * hora, "AGENDADO"), emptyList())
        val futuro = appointments.insertWithServices(atendimento(c, agora + 2 * hora, "CONFIRMADO"), emptyList())
        val futuroConcluido = appointments.insertWithServices(atendimento(c, agora + 3 * hora, "CONCLUIDO"), emptyList())

        appointments.unscheduleFutureOfClient(c, agora)

        assertEquals("AGENDADO", appointments.observeById(passado).first()!!.appointment.status)
        val f = appointments.observeById(futuro).first()!!.appointment
        assertEquals("DESMARCADO", f.status)
        assertEquals("CANCELOU", f.cancelReason)
        assertEquals("CONCLUIDO", appointments.observeById(futuroConcluido).first()!!.appointment.status)
    }
}
