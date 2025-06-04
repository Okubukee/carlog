// Nuevo archivo: db/tables/RemindersTable.kt
package db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption

object RemindersTable : Table("reminders") {
    val id = varchar("id", 255) // Primary Key
    val carId = varchar("car_id", 255).references(CarsTable.id, onDelete = ReferenceOption.CASCADE) // Si se borra el coche, se borran sus recordatorios
    val title = varchar("title", 255)
    val subtitle = varchar("subtitle", 512)
    // Podrías añadir una columna dueDate (date o varchar) y isCompleted (bool) si quieres más funcionalidad

    override val primaryKey = PrimaryKey(id)
}
