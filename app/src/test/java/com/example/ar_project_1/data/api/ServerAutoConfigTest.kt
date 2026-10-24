package com.example.ar_project_1.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerAutoConfigTest {

    @Test
    fun `phone tries loopback first`() {
        val candidates = ServerAutoConfig.candidates(isEmulator = false)
        assertEquals(ServerAutoConfig.LOOPBACK, candidates.first())
        assertEquals(listOf(ServerAutoConfig.LOOPBACK, ServerAutoConfig.EMULATOR_HOST), candidates)
    }

    @Test
    fun `emulator tries host alias first`() {
        val candidates = ServerAutoConfig.candidates(isEmulator = true)
        assertEquals(ServerAutoConfig.EMULATOR_HOST, candidates.first())
        assertEquals(listOf(ServerAutoConfig.EMULATOR_HOST, ServerAutoConfig.LOOPBACK), candidates)
    }

    @Test
    fun `candidates always contain both addresses`() {
        listOf(true, false).forEach { emulator ->
            val candidates = ServerAutoConfig.candidates(emulator)
            assertEquals(2, candidates.size)
            assertTrue(candidates.contains(ServerAutoConfig.LOOPBACK))
            assertTrue(candidates.contains(ServerAutoConfig.EMULATOR_HOST))
        }
    }
}
