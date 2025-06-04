// Nuevo archivo: db/tables/InvoicesTable.kt
package db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption

object InvoicesTable : Table("invoices") {
    val id = varchar("id", 255) // Primary Key
    val maintenanceId = varchar("maintenance_id", 255).references(MaintenancesTable.id, onDelete = ReferenceOption.CASCADE) // Si se borra el mantenimiento, se borra la factura
    val date = varchar("invoice_date", 20) // Renombrado para evitar colisión con palabra clave 'date'
    val total = double("total")
    val status = varchar("status", 50) // "Pagada", "Pendiente"

    override val primaryKey = PrimaryKey(id)
}