package db.tables // O el paquete que prefieras

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption // Para las claves foráneas

object MaintenancesTable : Table("maintenances") {
    val id = varchar("id", 255) // Primary Key
    // Clave foránea a CarsTable
    val carId = varchar("car_id", 255).references(CarsTable.id, onDelete = ReferenceOption.CASCADE)
    // Clave foránea a WorkshopsTable
    val workshopId = varchar("workshop_id", 255).references(WorkshopsTable.id, onDelete = ReferenceOption.SET_NULL).nullable() // O NO ACTION, o RESTRICT
    val date = varchar("date", 20) // Para "YYYY-MM-DD". Podrías usar date("date") si conviertes a LocalDate
    val description = varchar("description", 1024)
    val cost = double("cost")
    val type = varchar("type", 100)
    val km = integer("km")

    override val primaryKey = PrimaryKey(id)
}