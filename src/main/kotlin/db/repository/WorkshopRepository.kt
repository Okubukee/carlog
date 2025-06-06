// Nuevo archivo: db/repository/WorkshopRepository.kt
package db.repository

import Workshop
import db.tables.WorkshopsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
// import com.example.yourproject.models.Workshop // Ajusta la importación

object WorkshopRepository {
    private fun toWorkshop(row: ResultRow): Workshop = Workshop(
        id = row[WorkshopsTable.id],
        name = row[WorkshopsTable.name],
        specialty = row[WorkshopsTable.specialty],
        phone = row[WorkshopsTable.phone],
        location = row[WorkshopsTable.location],
        hourlyRate = row[WorkshopsTable.hourlyRate]
    )

    fun addWorkshop(workshop: Workshop) {
        transaction {
            WorkshopsTable.insert {
                it[id] = workshop.id
                it[name] = workshop.name
                it[specialty] = workshop.specialty
                it[phone] = workshop.phone
                it[location] = workshop.location
                it[hourlyRate] = workshop.hourlyRate
            }
        }
    }

    fun getWorkshopById(workshopId: String): Workshop? {
        return transaction {
            WorkshopsTable.select { WorkshopsTable.id eq workshopId }
                .mapNotNull { toWorkshop(it) }
                .singleOrNull()
        }
    }

    fun getAllWorkshops(): List<Workshop> {
        return transaction {
            WorkshopsTable.selectAll().map { toWorkshop(it) }
        }
    }
    fun updateWorkshop(workshop: Workshop) {
        transaction {
            WorkshopsTable.update({ WorkshopsTable.id eq workshop.id }) {
                it[name] = workshop.name
                it[specialty] = workshop.specialty
                it[phone] = workshop.phone
                it[location] = workshop.location
                it[hourlyRate] = workshop.hourlyRate
            }
        }
    }

    // --- NUEVA FUNCIÓN DE ELIMINAR ---
    fun deleteWorkshop(workshopId: String) {
        transaction {
            WorkshopsTable.deleteWhere { WorkshopsTable.id eq  workshopId }
        }
    }
}