package db.repository

import db.tables.UsersTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.UUID

data class User(val id: String, val email: String)

object UserRepository {

    fun createUser(email: String, password: String): User? {
        // Asegurarse de que el usuario no exista ya
        if (findUserByEmail(email) != null) {
            println("El email '$email' ya está registrado.")
            return null
        }

        val userId = UUID.randomUUID().toString()
        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())

        transaction {
            UsersTable.insert {
                it[id] = userId
                it[UsersTable.email] = email
                it[UsersTable.passwordHash] = passwordHash
            }
        }
        return User(id = userId, email = email)
    }

    fun findUserByEmail(email: String): User? {
        return transaction {
            UsersTable.select { UsersTable.email eq email }
                .map { User(id = it[UsersTable.id], email = it[UsersTable.email]) }
                .singleOrNull()
        }
    }

    fun checkPassword(email: String, password: String): Boolean {
        val hash = transaction {
            UsersTable.select { UsersTable.email eq email }
                .map { it[UsersTable.passwordHash] }
                .singleOrNull()
        } ?: return false

        return BCrypt.checkpw(password, hash)
    }
}