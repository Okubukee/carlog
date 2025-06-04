// Nuevo archivo: db/tables/ExpenseItemsTable.kt
package db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption

object ExpenseItemsTable : Table("expense_items") {
    val id = varchar("id", 255) // Primary Key
    val carId = varchar("car_id", 255).references(CarsTable.id, onDelete = ReferenceOption.CASCADE) // Si se borra el coche, se borran sus gastos
    val description = varchar("description", 1024)
    val date = varchar("expense_date", 20) // Renombrado
    val amount = double("amount")
    val iconName = varchar("icon_name", 100) // Para almacenar el nombre del icono (ej: "LocalGasStation")

    override val primaryKey = PrimaryKey(id)
}