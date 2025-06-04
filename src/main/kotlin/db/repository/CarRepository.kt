// Nuevo archivo: db/repository/CarRepository.kt
package db.repository // O el paquete que prefieras

import Car
import db.tables.CarsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
// Importa tu data class Car si está en un paquete diferente
// import com.example.yourproject.models.Car // Ajusta la importación

object CarRepository {

    // Función para mapear un ResultRow a un objeto Car
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

    fun addCar(car: Car) {
        transaction {
            CarsTable.insert {
                it[id] = car.id
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

    fun getAllCars(): List<Car> {
        return transaction {
            CarsTable.selectAll()
                .map { toCar(it) }
        }
    }
}