// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class ServiceDaoTest {

    private val file = File.createTempFile("loma-test", ".db").also { it.delete() }
    private val db = lomaDatabaseBuilder(file).buildLoma()
    private val dao = db.serviceDao()

    @AfterTest
    fun fechar() {
        db.close()
        file.delete()
    }

    private fun servico(nome: String, minutos: Int, centavos: Long, ordem: Int) =
        ServiceEntity(name = nome, durationMinutes = minutos, priceCents = centavos, colorArgb = 0xFFB69CF2, sortOrder = ordem)

    @Test
    fun listaAtivosNaOrdemEscolhida() = runTest {
        dao.insert(servico("Escova", 60, 7000, ordem = 1))
        dao.insert(servico("Coloração", 90, 18000, ordem = 0))

        assertEquals(listOf("Coloração", "Escova"), dao.observeActive().first().map { it.name })
    }

    @Test
    fun desativadoSaiDosAtivosMasContinuaEmTodos() = runTest {
        val id = dao.insert(servico("Escova", 60, 7000, ordem = 1))
        dao.insert(servico("Coloração", 90, 18000, ordem = 0))

        val escova = dao.observeAll().first().single { it.id == id }
        dao.update(escova.copy(active = false))

        assertEquals(listOf("Coloração"), dao.observeActive().first().map { it.name })
        assertEquals(setOf("Coloração", "Escova"), dao.observeAll().first().map { it.name }.toSet())
    }

    @Test
    fun guardaCentavosSemPerda() = runTest {
        val id = dao.insert(servico("Pacote noiva", 300, 123456789, ordem = 0))

        assertEquals(123456789L, dao.observeAll().first().single { it.id == id }.priceCents)
    }
}
