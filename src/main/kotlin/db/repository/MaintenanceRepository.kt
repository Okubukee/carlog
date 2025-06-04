// Nuevo archivo: db/repository/MaintenanceRepository.kt
package db.repository

import Maintenance
import db.tables.MaintenancesTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
// import com.example.yourproject.models.Maintenance // Ajusta la importación

object MaintenanceRepository {
    private fun toMaintenance(row: ResultRow): Maintenance = Maintenance(
        id = row[MaintenancesTable.id],
        carId = row[MaintenancesTable.carId],
        workshopId = row[MaintenancesTable.workshopId],
        date = row[MaintenancesTable.date],
        description = row[MaintenancesTable.description],
        cost = row[MaintenancesTable.cost],
        type = row[MaintenancesTable.type],
        km = row[MaintenancesTable.km]
    )

    fun addMaintenance(maintenance: Maintenance) {
        transaction {
            MaintenancesTable.insert {
                it[id] = maintenance.id
                it[carId] = maintenance.carId
                it[workshopId] = maintenance.workshopId
                it[date] = maintenance.date
                it[description] = maintenance.description
                it[cost] = maintenance.cost
                it[type] = maintenance.type
                it[km] = maintenance.km
            }
        }
    }

    fun updateMaintenance(maintenance: Maintenance) {
        transaction {
            MaintenancesTable.update({ MaintenancesTable.id eq maintenance.id }) {
                it[carId] = maintenance.carId
                it[workshopId] = maintenance.workshopId
                it[date] = maintenance.date
                it[description] = maintenance.description
                it[cost] = maintenance.cost
                it[type] = maintenance.type
                it[km] = maintenance.km
            }
        }
    }

    fun deleteMaintenance(maintenanceId: String) {
        transaction {
            MaintenancesTable.deleteWhere { MaintenancesTable.id eq maintenanceId }
        }
    }

    fun getMaintenanceById(maintenanceId: String): Maintenance? {
        return transaction {
            MaintenancesTable.select { MaintenancesTable.id eq maintenanceId }
                .mapNotNull { toMaintenance(it) }
                .singleOrNull()
        }
    }

    fun getMaintenancesByCarId(targetCarId: String): List<Maintenance> {
        return transaction {
            MaintenancesTable.select { MaintenancesTable.carId eq targetCarId }
                .map { toMaintenance(it) }
                .sortedByDescending { it.date } // Opcional: ordenar
        }
    }

    fun getAllMaintenances(): List<Maintenance> {
        return transaction {
            MaintenancesTable.selectAll()
                .map { toMaintenance(it) }
                .sortedByDescending { it.date } // Opcional: ordenar
        }
    }
}