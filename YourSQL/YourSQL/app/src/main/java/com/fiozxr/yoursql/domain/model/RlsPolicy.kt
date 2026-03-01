package com.fiozxr.yoursql.domain.model

data class RlsPolicy(
    val id: Long = 0,
    val tableId: Long,
    val scope: ApiKeyScope,
    val condition: String,
    val description: String?
) {
    fun toWhereClause(): String {
        return "($condition)"
    }

    companion object {
        fun create(
            tableId: Long,
            scope: ApiKeyScope,
            condition: String,
            description: String? = null
        ): RlsPolicy {
            return RlsPolicy(
                tableId = tableId,
                scope = scope,
                condition = condition,
                description = description
            )
        }
    }
}
