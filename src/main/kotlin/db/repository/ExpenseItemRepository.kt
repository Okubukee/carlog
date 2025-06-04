// Nuevo archivo: db/repository/ExpenseItemRepository.kt
package db.repository

import ExpenseItem
import db.tables.ExpenseItemsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
// import com.example.yourproject.models.ExpenseItem // Ajusta la importación
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Para los iconos de ejemplo
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq


// Helper para mapear nombres de iconos a ImageVector (debes expandir esto)
private fun mapIconNameToImageVector(iconName: String): ImageVector {
    return when (iconName) {
        "LocalGasStation" -> Icons.Filled.LocalGasStation
        "Shield" -> Icons.Filled.Shield
        "Wash" -> Icons.Filled.Wash
        "Build" -> Icons.Filled.Build // Para reparaciones menores o herramientas
        "CreditCard" -> Icons.Filled.CreditCard // Para peajes, parking
        "Receipt" -> Icons.Filled.Receipt // Impuestos, otros
        else -> Icons.Filled.Error // Icono por defecto o error
    }
}
private fun mapImageVectorToIconName(imageVector: ImageVector): String {
    return when (imageVector) {
        Icons.Filled.LocalGasStation -> "LocalGasStation"
        Icons.Filled.Shield -> "Shield"
        Icons.Filled.Wash -> "Wash"
        Icons.Filled.Build -> "Build"
        Icons.Filled.CreditCard -> "CreditCard"
        Icons.Filled.Receipt -> "Receipt"
        else -> "Error"
    }
}


object ExpenseItemRepository {
    private fun toExpenseItem(row: ResultRow): ExpenseItem = ExpenseItem(
        id = row[ExpenseItemsTable.id],
        carId = row[ExpenseItemsTable.carId],
        description = row[ExpenseItemsTable.description],
        date = row[ExpenseItemsTable.date],
        amount = row[ExpenseItemsTable.amount],
        icon = mapIconNameToImageVector(row[ExpenseItemsTable.iconName])
    )

    fun addExpenseItem(expenseItem: ExpenseItem) {
        transaction {
            ExpenseItemsTable.insert {
                it[id] = expenseItem.id
                it[carId] = expenseItem.carId
                it[description] = expenseItem.description
                it[date] = expenseItem.date
                it[amount] = expenseItem.amount
                it[iconName] = mapImageVectorToIconName(expenseItem.icon)
            }
        }
    }

    fun getExpenseItemsByCarId(targetCarId: String): List<ExpenseItem> {
        return transaction {
            ExpenseItemsTable.select { ExpenseItemsTable.carId eq targetCarId }
                .map { toExpenseItem(it) }
                .sortedByDescending { it.date }
        }
    }

    fun updateExpenseItem(expenseItem: ExpenseItem) {
        transaction {
            ExpenseItemsTable.update({ ExpenseItemsTable.id eq expenseItem.id }) {
                it[description] = expenseItem.description
                it[date] = expenseItem.date
                it[amount] = expenseItem.amount
                it[iconName] = mapImageVectorToIconName(expenseItem.icon)
                // carId generalmente no se actualiza para un gasto existente
            }
        }
    }

    fun deleteExpenseItem(expenseItemId: String) {
        transaction {
            ExpenseItemsTable.deleteWhere { ExpenseItemsTable.id eq expenseItemId }
        }
    }
    fun getAllExpenseItems(): List<ExpenseItem> {
        return transaction {
            ExpenseItemsTable.selectAll()
                .map { toExpenseItem(it) }
                .sortedByDescending { it.date }
        }
    }
}