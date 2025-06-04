// Nuevo archivo: db/repository/ReminderRepository.kt
package db.repository

import Reminder
import db.tables.RemindersTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
// import com.example.yourproject.models.Reminder // Ajusta la importación

object ReminderRepository {
    private fun toReminder(row: ResultRow): Reminder = Reminder(
        id = row[RemindersTable.id],
        carId = row[RemindersTable.carId],
        title = row[RemindersTable.title],
        subtitle = row[RemindersTable.subtitle]
    )

    fun addReminder(reminder: Reminder) {
        transaction {
            RemindersTable.insert {
                it[id] = reminder.id
                it[carId] = reminder.carId
                it[title] = reminder.title
                it[subtitle] = reminder.subtitle
            }
        }
    }

    fun getRemindersByCarId(targetCarId: String): List<Reminder> {
        return transaction {
            RemindersTable.select { RemindersTable.carId eq targetCarId }
                .map { toReminder(it) }
            // Podrías querer ordenar por alguna fecha si la añades a la tabla/modelo
        }
    }

    fun updateReminder(reminder: Reminder) {
        transaction {
            RemindersTable.update({ RemindersTable.id eq reminder.id }) {
                it[title] = reminder.title
                it[subtitle] = reminder.subtitle
                // carId generalmente no se actualiza
            }
        }
    }

    fun deleteReminder(reminderId: String) {
        transaction {
            RemindersTable.deleteWhere { RemindersTable.id eq reminderId }
        }
    }

    fun getAllReminders(): List<Reminder> { // Por si lo necesitas
        return transaction {
            RemindersTable.selectAll().map { toReminder(it) }
        }
    }
}