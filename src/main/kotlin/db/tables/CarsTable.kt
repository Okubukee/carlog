package db.tables // O el paquete que prefieras

import org.jetbrains.exposed.sql.Table

object CarsTable : Table("cars") {
    val id = varchar("id", 255) // Using String ID as in your Car data class
    val brand = varchar("brand", 255)
    val model = varchar("model", 255)
    val year = integer("year")
    val plate = varchar("plate", 50)
    val km = integer("km")
    val imageUrl = varchar("image_url", 512).nullable()
    val nextServiceDate = varchar("next_service_date", 20).nullable()
    val color = varchar("color", 50)
    val transmission = varchar("transmission", 50)
    val fuelType = varchar("fuel_type", 50)
    val purchaseDate = varchar("purchase_date", 20).nullable()

    override val primaryKey = PrimaryKey(id)
}