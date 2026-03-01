package com.fiozxr.yoursql

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiozxr.yoursql.domain.model.ColumnInfo
import com.fiozxr.yoursql.domain.model.ColumnType
import com.fiozxr.yoursql.domain.model.TableInfo
import com.fiozxr.yoursql.presentation.screens.TableCard
import com.fiozxr.yoursql.presentation.theme.YourSQLTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TableBrowserUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTableCardDisplaysCorrectly() {
        val table = TableInfo(
            id = 1,
            databaseName = "test",
            name = "users",
            displayName = "Users",
            createdAt = 0,
            updatedAt = 0,
            rowCount = 100,
            columns = listOf(
                ColumnInfo.create("id", ColumnType.INTEGER, isPrimaryKey = true),
                ColumnInfo.create("name", ColumnType.TEXT),
                ColumnInfo.create("email", ColumnType.TEXT)
            )
        )

        composeTestRule.setContent {
            YourSQLTheme {
                TableCard(
                    table = table,
                    onClick = {},
                    onDelete = {}
                )
            }
        }

        // Verify table name is displayed
        composeTestRule.onNodeWithText("users").assertIsDisplayed()

        // Verify row count is displayed
        composeTestRule.onNodeWithText("100 rows").assertIsDisplayed()

        // Verify column count is displayed
        composeTestRule.onNodeWithText("3 columns").assertIsDisplayed()
    }

    @Test
    fun testEmptyStateDisplaysCorrectly() {
        composeTestRule.setContent {
            YourSQLTheme {
                com.fiozxr.yoursql.presentation.screens.EmptyState(
                    message = "No tables",
                    action = "Create your first table"
                )
            }
        }

        composeTestRule.onNodeWithText("No tables").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create your first table").assertIsDisplayed()
    }
}
