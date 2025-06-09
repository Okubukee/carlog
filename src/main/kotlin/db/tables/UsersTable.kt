package db.tables

import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val id = varchar("id", 255)
    val email = varchar("email", 255).uniqueIndex() // El email debe ser único
    val passwordHash = varchar("password_hash", 512) // Almacenaremos un hash, no la contraseña

    override val primaryKey = PrimaryKey(id)
}