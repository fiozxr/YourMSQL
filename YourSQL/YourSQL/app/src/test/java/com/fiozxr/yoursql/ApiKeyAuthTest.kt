package com.fiozxr.yoursql

import com.fiozxr.yoursql.domain.model.ApiKey
import com.fiozxr.yoursql.domain.model.ApiKeyScope
import org.junit.Assert.*
import org.junit.Test

class ApiKeyAuthTest {

    @Test
    fun `test API key generation`() {
        val key = ApiKey.generate("Test Key", ApiKeyScope.READ_ONLY)

        assertNotNull(key.key)
        assertTrue(key.key.isNotBlank())
        assertEquals("Test Key", key.name)
        assertEquals(ApiKeyScope.READ_ONLY, key.scope)
        assertTrue(key.isActive)
    }

    @Test
    fun `test API key scopes`() {
        val readOnly = ApiKey.generate("Read Only", ApiKeyScope.READ_ONLY)
        val readWrite = ApiKey.generate("Read Write", ApiKeyScope.READ_WRITE)
        val admin = ApiKey.generate("Admin", ApiKeyScope.ADMIN)

        assertEquals(ApiKeyScope.READ_ONLY, readOnly.scope)
        assertEquals(ApiKeyScope.READ_WRITE, readWrite.scope)
        assertEquals(ApiKeyScope.ADMIN, admin.scope)
    }

    @Test
    fun `test scope permissions`() {
        val readOnly = ApiKeyScope.READ_ONLY
        val readWrite = ApiKeyScope.READ_WRITE
        val admin = ApiKeyScope.ADMIN

        // Read-only can read
        assertTrue(hasReadPermission(readOnly))
        assertFalse(hasWritePermission(readOnly))

        // Read-write can read and write
        assertTrue(hasReadPermission(readWrite))
        assertTrue(hasWritePermission(readWrite))

        // Admin can do everything
        assertTrue(hasReadPermission(admin))
        assertTrue(hasWritePermission(admin))
    }

    private fun hasReadPermission(scope: ApiKeyScope): Boolean {
        return scope == ApiKeyScope.READ_ONLY ||
               scope == ApiKeyScope.READ_WRITE ||
               scope == ApiKeyScope.ADMIN
    }

    private fun hasWritePermission(scope: ApiKeyScope): Boolean {
        return scope == ApiKeyScope.READ_WRITE ||
               scope == ApiKeyScope.ADMIN
    }
}
