package db // Ajusta el paquete a tu estructura

import db.tables.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
// Importa aquí los objetos de tus tablas (ej: CarsTable, WorkshopsTable, etc.)
// import com.example.yourproject.db.tables.CarsTable
// import com.example.yourproject.db.tables.WorkshopsTable
// import com.example.yourproject.db.tables.MaintenancesTable
// ... y el resto de tus tablas

object DatabaseManager {

    fun init() {
        // TODO: Estos detalles de conexión deberían venir de un archivo de configuración seguro
        // o variables de entorno, no hardcodeados directamente.
        val dbHost = "localhost" // o la IP/hostname de tu servidor PostgreSQL
        val dbPort = 5432 // Puerto por defecto de PostgreSQL
        val dbName = "carlog" // El nombre de tu base de datos
        val dbUser = "postgres" // Tu usuario de PostgreSQL
        val dbPassword = "postgres" // Tu contraseña

        val jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/$dbName"
        val driver = "org.postgresql.Driver"

        try {
            Database.connect(jdbcUrl, driver = driver, user = dbUser, password = dbPassword)

            transaction {
                // Ahora puedes incluir CarsTable en la creación del esquema
                println("DatabaseManager: Dentro de la transacción para crear tablas...") // Log
                SchemaUtils.createMissingTablesAndColumns(
                    CarsTable
                    , WorkshopsTable // Descomenta y añade otras tablas a medida que las creas
                    , MaintenancesTable
                    , InvoicesTable
                    , ExpenseItemsTable
                    , RemindersTable
                )
                println("DatabaseManager: Creación/verificación de tablas completada.") // Log

                println("DatabaseManager: init() completado exitosamente.") // Log final de init
            }

        } catch (e: Exception) {
            System.err.println("DatabaseManager: ERROR DENTRO DE init(): ${e.message}")
            e.printStackTrace() // Imprime el stack trace completo
            throw e
        }
    }

    // Opcional: podrías añadir una función para ejecutar transacciones genéricas si lo necesitas
    // fun <T> dbQuery(block: () -> T): T =
    //     transaction { block() }
}