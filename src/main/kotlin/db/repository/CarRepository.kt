// okubukee/carlog/carlog-85f24ad3fcac7d1e9f062e45ec567bd98e8875d6/src/main/kotlin/db/repository/CarRepository.kt

package db.repository // O el paquete que prefieras

import Car
import db.tables.CarsTable
import db.tables.UsersTable // Asegúrate de importar UsersTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CarRepository {

    private fun toCar(row: ResultRow): Car = Car(
        id = row[CarsTable.id],
        brand = row[CarsTable.brand],
        model = row[CarsTable.model],
        year = row[CarsTable.year],
        plate = row[CarsTable.plate],
        km = row[CarsTable.km],
        imageUrl = row[CarsTable.imageUrl],
        nextServiceDate = row[CarsTable.nextServiceDate],
        color = row[CarsTable.color],
        transmission = row[CarsTable.transmission],
        fuelType = row[CarsTable.fuelType],
        purchaseDate = row[CarsTable.purchaseDate]
    )

    // --- MODIFICADO: Se añade el parámetro 'userId' ---
    fun addCar(car: Car, userId: String) {
        transaction {
            CarsTable.insert {
                it[id] = car.id
                it[CarsTable.userId] = userId // Se asocia el coche al usuario
                it[brand] = car.brand
                it[model] = car.model
                it[year] = car.year
                it[plate] = car.plate
                it[km] = car.km
                it[imageUrl] = car.imageUrl
                it[nextServiceDate] = car.nextServiceDate
                it[color] = car.color
                it[transmission] = car.transmission
                it[fuelType] = car.fuelType
                it[purchaseDate] = car.purchaseDate
            }
        }
    }

    fun updateCar(car: Car) {
        transaction {
            CarsTable.update({ CarsTable.id eq car.id }) {
                it[brand] = car.brand
                it[model] = car.model
                it[year] = car.year
                it[plate] = car.plate
                it[km] = car.km
                it[imageUrl] = car.imageUrl
                it[nextServiceDate] = car.nextServiceDate
                it[color] = car.color
                it[transmission] = car.transmission
                it[fuelType] = car.fuelType
                it[purchaseDate] = car.purchaseDate
            }
        }
    }

    fun deleteCar(carId: String) {
        transaction {
            CarsTable.deleteWhere { CarsTable.id eq carId }
        }
    }

    fun getCarById(carId: String): Car? {
        return transaction {
            CarsTable.select { CarsTable.id eq carId }
                .mapNotNull { toCar(it) }
                .singleOrNull()
        }
    }

    // --- MODIFICADO: Acepta 'userId' y filtra la consulta ---
    fun getAllCars(userId: String): List<Car> {
        return transaction {
            // Se añade la condición para seleccionar solo los coches del usuario
            CarsTable.select { CarsTable.userId eq userId }
                .map { toCar(it) }
        }
    }
}