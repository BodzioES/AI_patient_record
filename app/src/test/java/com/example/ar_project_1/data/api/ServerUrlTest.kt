package com.example.ar_project_1.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ServerUrlTest {

    @Test
    fun `adds http scheme and api path`() {
        assertEquals(
            "http://192.168.1.10:8080/api/v1/",
            ServerUrl.normalize("192.168.1.10:8080"),
        )
    }

    @Test
    fun `keeps https and replaces any path`() {
        assertEquals(
            "https://example.com/api/v1/",
            ServerUrl.normalize("https://example.com/some/path?query=1"),
        )
    }

    @Test
    fun `accepts already normalized url`() {
        assertEquals(
            "http://10.0.2.2:8080/api/v1/",
            ServerUrl.normalize("http://10.0.2.2:8080/api/v1/"),
        )
    }

    @Test
    fun `trims whitespace`() {
        assertEquals(
            "http://localhost:8080/api/v1/",
            ServerUrl.normalize("  http://localhost:8080  "),
        )
    }

    @Test
    fun `rejects blank input`() {
        assertThrows(IllegalArgumentException::class.java) {
            ServerUrl.normalize("   ")
        }
    }

    @Test
    fun `rejects unsupported scheme`() {
        assertThrows(IllegalArgumentException::class.java) {
            ServerUrl.normalize("ftp://example.com")
        }
    }

    @Test
    fun `display strips api path`() {
        assertEquals(
            "http://192.168.1.10:8080",
            ServerUrl.display("http://192.168.1.10:8080/api/v1/"),
        )
        assertEquals("", ServerUrl.display(null))
    }
}
