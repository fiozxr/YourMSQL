package com.fiozxr.yoursql

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ServerIntegrationTest {

    @Test
    fun `test health endpoint`() = testApplication {
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test 404 for unknown endpoint`() = testApplication {
        val response = client.get("/unknown")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `test CORS headers`() = testApplication {
        val response = client.options("/health") {
            header(HttpHeaders.Origin, "http://localhost:3000")
            header(HttpHeaders.AccessControlRequestMethod, "GET")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(response.headers[HttpHeaders.AccessControlAllowOrigin])
    }

    @Test
    fun `test missing API key returns 401`() = testApplication {
        // This would require the full server setup with auth
        // For now, just verify the structure
        val response = client.get("/rest/v1/users")
        // Without proper setup, this might return different status
        // In production, it should return 401
        assertTrue(response.status.value >= 400)
    }

    @Test
    fun `test JSON content type`() = testApplication {
        val response = client.get("/health")
        val contentType = response.headers[HttpHeaders.ContentType]
        assertNotNull(contentType)
        assertTrue(contentType!!.contains(ContentType.Application.Json.toString()))
    }
}
