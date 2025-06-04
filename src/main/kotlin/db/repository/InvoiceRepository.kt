// Nuevo archivo: db/repository/InvoiceRepository.kt
package db.repository

import Invoice
import db.tables.InvoicesTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
// import com.example.yourproject.models.Invoice // Ajusta la importación

object InvoiceRepository {
    private fun toInvoice(row: ResultRow): Invoice = Invoice(
        id = row[InvoicesTable.id],
        maintenanceId = row[InvoicesTable.maintenanceId],
        date = row[InvoicesTable.date],
        total = row[InvoicesTable.total],
        status = row[InvoicesTable.status]
    )

    fun addInvoice(invoice: Invoice) {
        transaction {
            InvoicesTable.insert {
                it[id] = invoice.id
                it[maintenanceId] = invoice.maintenanceId
                it[date] = invoice.date
                it[total] = invoice.total
                it[status] = invoice.status
            }
        }
    }

    fun getInvoicesByMaintenanceId(maintId: String): List<Invoice> {
        return transaction {
            InvoicesTable.select { InvoicesTable.maintenanceId eq maintId }
                .map { toInvoice(it) }
        }
    }

    fun getInvoiceById(invoiceId: String): Invoice? {
        return transaction {
            InvoicesTable.select { InvoicesTable.id eq invoiceId }
                .mapNotNull { toInvoice(it) }
                .singleOrNull()
        }
    }

    fun updateInvoiceStatus(invoiceId: String, newStatus: String) {
        transaction {
            InvoicesTable.update({ InvoicesTable.id eq invoiceId }) {
                it[status] = newStatus
            }
        }
    }

    fun deleteInvoice(invoiceId: String) {
        transaction {
            InvoicesTable.deleteWhere { InvoicesTable.id eq invoiceId }
        }
    }

    fun getAllInvoices(): List<Invoice> {
        return transaction {
            InvoicesTable.selectAll().map { toInvoice(it) }
        }
    }
}