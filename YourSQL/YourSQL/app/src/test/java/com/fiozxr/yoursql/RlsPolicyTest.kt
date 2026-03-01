package com.fiozxr.yoursql

import com.fiozxr.yoursql.domain.model.ApiKeyScope
import com.fiozxr.yoursql.domain.model.RlsPolicy
import org.junit.Assert.*
import org.junit.Test

class RlsPolicyTest {

    @Test
    fun `test RLS policy creation`() {
        val policy = RlsPolicy.create(
            tableId = 1L,
            scope = ApiKeyScope.READ_ONLY,
            condition = "is_public = 1",
            description = "Only show public posts"
        )

        assertEquals(1L, policy.tableId)
        assertEquals(ApiKeyScope.READ_ONLY, policy.scope)
        assertEquals("is_public = 1", policy.condition)
        assertEquals("Only show public posts", policy.description)
    }

    @Test
    fun `test RLS policy to WHERE clause`() {
        val policy = RlsPolicy.create(
            tableId = 1L,
            scope = ApiKeyScope.READ_ONLY,
            condition = "is_public = 1"
        )

        val whereClause = policy.toWhereClause()
        assertEquals("(is_public = 1)", whereClause)
    }

    @Test
    fun `test multiple policy conditions`() {
        val policies = listOf(
            RlsPolicy.create(1L, ApiKeyScope.READ_ONLY, "is_public = 1"),
            RlsPolicy.create(1L, ApiKeyScope.READ_WRITE, "user_id = current_user_id()")
        )

        assertEquals(2, policies.size)
        assertTrue(policies.any { it.scope == ApiKeyScope.READ_ONLY })
        assertTrue(policies.any { it.scope == ApiKeyScope.READ_WRITE })
    }

    @Test
    fun `test policy scope matching`() {
        val readOnlyPolicy = RlsPolicy.create(
            tableId = 1L,
            scope = ApiKeyScope.READ_ONLY,
            condition = "is_public = 1"
        )

        // Read-only scope should match read-only policy
        assertTrue(matchesScope(readOnlyPolicy, ApiKeyScope.READ_ONLY))

        // Read-write scope should also match read-only policy (higher privilege)
        assertTrue(matchesScope(readOnlyPolicy, ApiKeyScope.READ_WRITE))

        // Admin should match all policies
        assertTrue(matchesScope(readOnlyPolicy, ApiKeyScope.ADMIN))
    }

    private fun matchesScope(policy: RlsPolicy, scope: ApiKeyScope): Boolean {
        return when (scope) {
            ApiKeyScope.ADMIN -> true
            ApiKeyScope.READ_WRITE -> policy.scope == ApiKeyScope.READ_ONLY ||
                                      policy.scope == ApiKeyScope.READ_WRITE
            ApiKeyScope.READ_ONLY -> policy.scope == ApiKeyScope.READ_ONLY
        }
    }
}
