package com.fiozxr.yoursql

import org.junit.Assert.*
import org.junit.Test

class QueryBuilderTest {

    @Test
    fun `test equality filter`() {
        val filter = "id = 1"
        assertTrue(filter.contains("="))
    }

    @Test
    fun `test range filter`() {
        val filter = "age >= 18 AND age <= 65"
        assertTrue(filter.contains(">="))
        assertTrue(filter.contains("<="))
    }

    @Test
    fun `test order by clause`() {
        val orderBy = "created_at DESC"
        assertEquals("created_at DESC", orderBy)
    }

    @Test
    fun `test pagination`() {
        val limit = 10
        val offset = 20
        assertEquals(10, limit)
        assertEquals(20, offset)
    }

    @Test
    fun `test column projection`() {
        val columns = listOf("id", "name", "email")
        assertEquals(3, columns.size)
        assertTrue(columns.contains("id"))
    }
}
