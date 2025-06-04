// COMIENZO DEL CÓDIGO Main.kt COMPLETO
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.TabRowDefaults

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

// Data Classes
data class Car(
    val id: String,
    val brand: String,
    val model: String,
    val year: Int,
    val plate: String,
    val km: Int,
    val imageUrl: String?,
    val nextServiceDate: String?, // Formato esperado: DD/MM/YYYY
    val color: String,
    val transmission: String,
    val fuelType: String,
    val purchaseDate: String? // Formato esperado: DD/MM/YYYY
)

data class Workshop(
    val id: String,
    val name: String,
    val specialty: String,
    val phone: String,
    val location: String,
    val hourlyRate: Double
)

data class Maintenance(
    val id: String,
    val carId: String,
    val workshopId: String,
    val date: String, // Formato YYYY-MM-DD para ordenar fácil
    val description: String,
    val cost: Double,
    val type: String,
    val km: Int
)

data class Invoice(
    val id: String,
    val maintenanceId: String,
    val date: String, // Fecha de la factura, puede ser igual a la del mantenimiento
    val total: Double,
    val status: String // "Pagada" o "Pendiente"
)

data class ExpenseItem(
    val id: String,
    val carId: String, // Para asociar el gasto a un coche
    val description: String,
    val date: String, // Formato DD/MM/YYYY
    val amount: Double,
    val icon: ImageVector // Icono para el tipo de gasto
)

data class Reminder(
    val id: String,
    val carId: String, // Para asociar el recordatorio a un coche
    val title: String,
    val subtitle: String // Ej: "En 5 días - 15/08/2024"
)


fun main() = application {
    val windowState = rememberWindowState(width = 1450.dp, height = 950.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "AutoTracker v1.3",
        state = windowState
    ) {
        CarMaintenanceTheme {
            CarMaintenanceApp()
        }
    }
}

private val AppLightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color(0xFF495057),
    onSecondary = Color.White,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    onBackground = Color(0xFF212529),
    onSurface = Color(0xFF212529),
    error = Color(0xFFDC3545),
    onError = Color.White,
    surfaceVariant = Color(0xFFE9ECEF),
    outline = Color(0xFFDEE2E6)
)

@Composable
fun CarMaintenanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppLightColorScheme,
        typography = Typography(
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = AppLightColorScheme.onSurface),
            titleMedium = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = AppLightColorScheme.onSurface),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, color = AppLightColorScheme.onSurface),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = AppLightColorScheme.onSurface),
            bodySmall = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = AppLightColorScheme.onSurface.copy(alpha = 0.7f)),
            labelSmall = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = AppLightColorScheme.onSurface.copy(alpha = 0.6f)),
            headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontSize=24.sp, fontWeight = FontWeight.Bold, color = AppLightColorScheme.primary)
        ),
        shapes = Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(12.dp)
        ),
        content = content
    )
}

// --- Funciones de ayuda para fechas ---
fun parseDate(dateStr: String?): LocalDate? {
    if (dateStr.isNullOrBlank()) return null
    return try { LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } catch (e: DateTimeParseException) {
        try { LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e2: DateTimeParseException) { null }
    }
}
fun daysUntil(date: LocalDate?): Long? {
    if (date == null) return null
    return ChronoUnit.DAYS.between(LocalDate.now(), date)
}
fun formatDateToDDMMYYYY(date: LocalDate?): String? = date?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
fun formatDateFromYYYYMMDDToDDMMYYYY(dateStrYYYYMMDD: String?): String? = parseDate(dateStrYYYYMMDD)?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))


// --- Composable Principal de la App ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarMaintenanceApp() {
    var cars by remember {
        mutableStateOf(
            listOf(
                Car("1", "Seat León", "ST 1.5 TSI", 2019, "1234 JKL", 45000, "https://placehold.co/800x400/E1E1E1/31343C?text=Seat+Le%C3%B3n&font=raleway", "15/07/2025", "Blanco Nieve", "Manual", "Gasolina", "10/01/2019"),
                Car("2", "Volkswagen Golf", "2.0 TDI", 2017, "5678 MNP", 78500, "https://placehold.co/800x400/DCDCDC/31343C?text=VW+Golf&font=raleway", "02/08/2025", "Negro Profundo", "Automática DSG", "Diesel", "15/03/2017"),
                Car("3", "Audi A3", "Sedan 35 TFSI", 2020, "9012 XYZ", 22000, null, "01/09/2025", "Gris Daytona", "S Tronic", "Gasolina", "20/06/2020")
            )
        )
    }
    var workshops by remember { mutableStateOf(workshopsSampleData) }
    var maintenances by remember { mutableStateOf(maintenancesSampleData) }
    var invoices by remember { mutableStateOf(invoicesSampleData) }
    var expenses by remember { mutableStateOf(sampleExpenses) }
    var reminders by remember { mutableStateOf(sampleReminders) }

    var selectedCarId by remember { mutableStateOf<String?>(null) }
    var selectedTabInMain by remember { mutableStateOf(0) }

    var showAddCarDialog by remember { mutableStateOf(false) }
    var showAddMaintenanceDialogForCarId by remember { mutableStateOf<String?>(null) }
    var showAddExpenseDialogForCarId by remember { mutableStateOf<String?>(null) }
    var showEditCarDialogForCarId by remember { mutableStateOf<String?>(null) }
    var showAddReminderDialogForCarId by remember { mutableStateOf<String?>(null) }

    fun getCarById(id: String): Car? = cars.find { it.id == id }
    fun getMaintenancesByCarId(carId: String): List<Maintenance> = maintenances.filter { it.carId == carId }
    fun getLastServiceDateForCar(carId: String): String? = maintenances.filter { it.carId == carId }.maxByOrNull { it.date }?.date?.let { formatDateFromYYYYMMDDToDDMMYYYY(it) }

    val totalCars = cars.size
    val maintenancesPendientes = invoices.count { it.status == "Pendiente" }
    val totalExpensesAllCars = maintenances.sumOf { it.cost } + expenses.sumOf { it.amount }
    val soonestNextService = cars
        .mapNotNull { car -> car.nextServiceDate?.let { dateStr -> parseDate(dateStr)?.let { date -> Triple(car, date, daysUntil(date)) } } }
        .filter { it.third != null && it.third!! >= 0 }.minByOrNull { it.third!! }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (selectedCarId != null) {
            val car = getCarById(selectedCarId!!)
            if (car != null) {
                CarDetailView(
                    car = car,
                    maintenances = getMaintenancesByCarId(selectedCarId!!),
                    workshops = workshops,
                    expenses = expenses.filter { it.carId == car.id },
                    reminders = reminders.filter { it.carId == car.id },
                    onBack = { selectedCarId = null },
                    onShowAddMaintenanceDialog = { showAddMaintenanceDialogForCarId = car.id },
                    onShowAddExpenseDialog = { showAddExpenseDialogForCarId = car.id },
                    onShowEditCarDialog = { showEditCarDialogForCarId = car.id },
                    onShowAddReminderDialog = { showAddReminderDialogForCarId = car.id }
                )
            } else { selectedCarId = null }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                AppHeader()
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    TopDashboardCardsRow(totalCars, maintenancesPendientes, soonestNextService?.third, soonestNextService?.first?.let{"${it.brand} ${it.model}".trim().take(15)+"... (${it.nextServiceDate})"} ?: "N/A", totalExpensesAllCars)
                    Spacer(modifier = Modifier.height(24.dp))
                    TabRow(selectedTabIndex = selectedTabInMain, containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.primary, indicator = {}, divider = {}) {
                        val tabs = listOf("Mis Vehículos", "Mantenimiento", "Gastos")
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabInMain == index, onClick = { selectedTabInMain = index },
                                text = { Text(title, style = if (selectedTabInMain == index) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodyMedium) },
                                selectedContentColor = MaterialTheme.colorScheme.primary, unselectedContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    when (selectedTabInMain) {
                        0 -> CarsTab(cars, { selectedCarId = it }, ::getLastServiceDateForCar, { showAddCarDialog = true }, { carId -> showAddMaintenanceDialogForCarId = carId })
                        1 -> AllMaintenancesTab(maintenances, cars, workshops)
                        2 -> GlobalExpensesTab(maintenances, expenses, cars)
                    }
                }
            }
        }
    }

    if (showAddCarDialog) AddCarDialog({ showAddCarDialog = false }) { car -> cars = cars.plusElement(car.copy(id = System.currentTimeMillis().toString())); showAddCarDialog = false }
    showAddMaintenanceDialogForCarId?.let { carId ->
        val car = getCarById(carId)
        if (car != null) AddMaintenanceDialog(car.id, car.km, workshops, { showAddMaintenanceDialogForCarId = null }) { maintenance ->
            val newMaintenanceWithId = maintenance.copy(id = System.currentTimeMillis().toString())
            maintenances = maintenances.plusElement(newMaintenanceWithId)
            cars = cars.map { if (it.id == newMaintenanceWithId.carId && newMaintenanceWithId.km > it.km) it.copy(km = newMaintenanceWithId.km) else it }
            invoices = invoices.plusElement(Invoice(System.currentTimeMillis().toString(),newMaintenanceWithId.id,newMaintenanceWithId.date,newMaintenanceWithId.cost,"Pendiente"))
            showAddMaintenanceDialogForCarId = null
        }
    }
    if (showAddExpenseDialogForCarId != null) {
        AddExpenseDialog(
            carId = showAddExpenseDialogForCarId!!,
            onDismiss = { showAddExpenseDialogForCarId = null },
            onAddExpense = { expenseItem ->
                expenses = expenses.plusElement(expenseItem.copy(id = System.currentTimeMillis().toString()))
                showAddExpenseDialogForCarId = null
            }
        )
    }
    if (showEditCarDialogForCarId != null) {
        val carToEdit = cars.find { it.id == showEditCarDialogForCarId }
        if (carToEdit != null) {
            EditCarDialog(
                car = carToEdit,
                onDismiss = { showEditCarDialogForCarId = null },
                onEditCar = { updatedCar ->
                    cars = cars.map { if (it.id == updatedCar.id) updatedCar else it }
                    showEditCarDialogForCarId = null
                }
            )
        }
    }
    if (showAddReminderDialogForCarId != null) {
        AddReminderDialog(
            carId = showAddReminderDialogForCarId!!,
            onDismiss = { showAddReminderDialogForCarId = null },
            onAddReminder = { reminder ->
                reminders = reminders.plusElement(reminder.copy(id = System.currentTimeMillis().toString()))
                showAddReminderDialogForCarId = null
            }
        )
    }
}

// --- COMPOSABLES DE UI ---
@Composable
fun AppHeader() {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AutoTracker", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = { /* TODO: Settings action */ }) {
                Icon(Icons.Filled.Settings, "Configuración", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    }
}

@Composable
fun TopDashboardCardsRow(totalCars: Int, pendingMaintenances: Int, nextMaintenanceDays: Long?, nextMaintenanceCarInfo: String?, totalExpenses: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatDisplayCard("Vehículos", totalCars.toString(), Icons.Filled.DirectionsCar, Modifier.weight(1f))
        StatDisplayCard("Mantenim. Pendientes", pendingMaintenances.toString(), Icons.Filled.Build, Modifier.weight(1f))
        StatDisplayCard(
            title = "Próximo Mantenimiento",
            value = nextMaintenanceDays?.let { if (it >=0) "$it días" else "Revisar" } ?: "N/A",
            subtitle = if (nextMaintenanceDays !=null && nextMaintenanceDays >=0) nextMaintenanceCarInfo else null,
            icon = Icons.Filled.EventNote,
            modifier = Modifier.weight(1f)
        )
        StatDisplayCard(
            title = "Gastos Totales",
            value = "€${String.format("%.2f", totalExpenses)}",
            subtitle = "",
            icon = Icons.Filled.EuroSymbol,
            modifier = Modifier.weight(1f)
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatDisplayCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, subtitle: String? = null) {
    Card(
        modifier = modifier.height(100.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, title, Modifier.size(24.dp).padding(end = 10.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
@Composable
fun CarsTab(cars: List<Car>, onCarSelected: (String) -> Unit, getLastServiceDate: (String) -> String?, onShowAddCarDialog: () -> Unit, onShowAddMaintenanceDialog: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)) {
        items(cars) { car -> CarCard(car, getLastServiceDate(car.id), { onCarSelected(car.id) }, { onShowAddMaintenanceDialog(car.id) }) }
        item { AddVehicleCard(onClick = onShowAddCarDialog) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarCard(car: Car, lastServiceDate: String?, onDetailsClick: () -> Unit, onMaintenanceClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.width(280.dp).height(380.dp), shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, hoveredElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                val painter = asyncPainterResource(data = car.imageUrl ?: "https://placehold.co/600x400/EFEFEF/CCC?text=+)&font=raleway")
                KamelImage(
                    resource = painter, contentDescription = car.brand, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                    onLoading = { CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary) },
                    onFailure = { Icon(Icons.Filled.PhotoCamera, "Sin imagen", Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                )
            }
            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Text("${car.brand} ${car.model}".trim(), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(car.year.toString(), style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)))
                Text(
                    text = "${car.color} • ${car.transmission} • ${car.fuelType}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                InfoRow(icon = Icons.Filled.Speed, text = "${String.format("%,d", car.km)} km")
                InfoRow(icon = Icons.Filled.EventAvailable, text = "Último: ${lastServiceDate ?: "N/A"}")
                InfoRow(icon = Icons.Filled.EventBusy, text = "Próximo: ${car.nextServiceDate ?: "N/A"}")
            }
            Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 12.dp, top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = onDetailsClick, modifier = Modifier.weight(1f)) { Text("Detalles", fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = onMaintenanceClick, modifier = Modifier.weight(1f)) { Text("Mantenimiento", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
@Composable
fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick, modifier = modifier.width(280.dp).height(360.dp), shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, hoveredElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.Add, "Añadir Vehículo", Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("Añadir Vehículo", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// --- DIÁLOGOS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarDialog(onDismiss: () -> Unit, onAddCar: (Car) -> Unit) {
    var brandAndModel by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var km by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var nextServiceDate by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var transmission by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf("") } // Campo para fecha de compra
    var formError by remember { mutableStateOf<String?>(null) }

    val transmissionTypes = listOf("Manual", "Automática", "CVT", "Secuencial", "Otro")
    var transmissionDropdownExpanded by remember { mutableStateOf(false) }
    val fuelTypes = listOf("Gasolina", "Diesel", "Híbrido", "Eléctrico", "GLP", "GNC", "Otro")
    var fuelDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Añadir Nuevo Vehículo", style = MaterialTheme.typography.titleLarge) },
        text = {
            LazyColumn(modifier = Modifier.widthIn(min = 480.dp, max = 600.dp).padding(vertical = 8.dp)) {
                item { Spacer(Modifier.height(8.dp)) }
                item { OutlinedTextField(brandAndModel, { brandAndModel=it; formError=null }, label={Text("Marca y Modelo")}, modifier=Modifier.fillMaxWidth(), isError=formError!=null&&brandAndModel.isBlank()) }
                item { Spacer(Modifier.height(10.dp)) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)){
                    OutlinedTextField(year, { year=it.filter{c->c.isDigit()}.take(4); formError=null }, label={Text("Año")}, modifier=Modifier.weight(1f), isError=formError!=null&&(year.isBlank()||year.toIntOrNull()==null||year.length!=4))
                    OutlinedTextField(plate.uppercase(), { plate=it.take(10); formError=null }, label={Text("Matrícula")}, modifier=Modifier.weight(1f), isError=formError!=null&&plate.isBlank())
                }}
                item { Spacer(Modifier.height(10.dp)) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)){
                    OutlinedTextField(km, { km=it.filter{c->c.isDigit()}; formError=null }, label={Text("Kilómetros")}, modifier=Modifier.weight(1f), isError=formError!=null&&(km.isBlank()||km.toIntOrNull()==null))
                    OutlinedTextField(color, { color=it; formError=null }, label={Text("Color")}, modifier=Modifier.weight(1f), isError=formError!=null&&color.isBlank())
                }}
                item { Spacer(Modifier.height(10.dp)) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically){
                    ExposedDropdownMenuBox(transmissionDropdownExpanded, { transmissionDropdownExpanded = !it }, Modifier.weight(1f)) {
                        OutlinedTextField(transmission, {}, label = { Text("Transmisión") }, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transmissionDropdownExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), isError = formError != null && transmission.isBlank())
                        ExposedDropdownMenu(transmissionDropdownExpanded, {transmissionDropdownExpanded=false}) { transmissionTypes.forEach { type -> DropdownMenuItem(text = { Text(type) }, onClick = { transmission = type; transmissionDropdownExpanded = false; formError = null }) } }
                    }
                    ExposedDropdownMenuBox(fuelDropdownExpanded, { fuelDropdownExpanded = !it }, Modifier.weight(1f)) {
                        OutlinedTextField(fuelType, {}, label = { Text("Combustible") }, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelDropdownExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), isError = formError != null && fuelType.isBlank())
                        ExposedDropdownMenu(fuelDropdownExpanded, {fuelDropdownExpanded=false}) { fuelTypes.forEach { type -> DropdownMenuItem(text = { Text(type) }, onClick = { fuelType = type; fuelDropdownExpanded = false; formError = null }) } }
                    }
                }}
                item { Spacer(Modifier.height(10.dp)) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(purchaseDate, { purchaseDate=it.filter{c->c.isDigit()||c=='/'}.take(10); formError=null }, label={Text("Fecha Compra (DD/MM/YYYY)")}, modifier=Modifier.weight(1f))
                    OutlinedTextField(nextServiceDate, { nextServiceDate=it.filter{c->c.isDigit()||c=='/'}.take(10); formError=null }, label={Text("Próximo Serv. (DD/MM/YYYY)")}, modifier=Modifier.weight(1f))
                }}
                item { Spacer(Modifier.height(10.dp)) }
                item { OutlinedTextField(imageUrl, { imageUrl=it; formError=null }, label={Text("URL de Imagen (Opcional)")}, modifier=Modifier.fillMaxWidth()) }
                item { Spacer(Modifier.height(10.dp)) }
                if (formError != null) { item { Text(formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) } }
            }
        },
        confirmButton = { Button(onClick = {
            val yearInt = year.toIntOrNull(); val kmInt = km.toIntOrNull()
            if (brandAndModel.isNotBlank() && plate.isNotBlank() && yearInt != null && year.length == 4 && kmInt != null && color.isNotBlank() && transmission.isNotBlank() && fuelType.isNotBlank()) {
                val parts = brandAndModel.split(" ", limit = 2)
                val brandValue = parts.getOrElse(0) { "" }
                val modelValue = parts.getOrElse(1) { "" }
                onAddCar(Car(System.currentTimeMillis().toString(), brandValue, modelValue, yearInt, plate.trim().uppercase(), kmInt, imageUrl.ifBlank{null}, nextServiceDate.ifBlank{null}, color.trim(), transmission.trim(), fuelType.trim(), purchaseDate.ifBlank { null }))
                onDismiss()
            } else formError = "Rellena todos los campos obligatorios (excepto imagen y fechas opcionales)."
        }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)) { Text("Añadir", color = MaterialTheme.colorScheme.onPrimary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = MaterialTheme.colorScheme.secondary) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkshopDialog(onDismiss: () -> Unit, onAddWorkshop: (Workshop) -> Unit) {
    var name by remember { mutableStateOf("") }; var specialty by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }; var hourlyRate by remember { mutableStateOf("") }; var formError by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface, textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("Añadir Nuevo Taller", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(min=400.dp, max=500.dp).padding(vertical=8.dp)) {
                OutlinedTextField(name, { name=it; formError=null }, label={Text("Nombre")}, modifier=Modifier.fillMaxWidth(), isError=formError!=null&&name.isBlank())
                OutlinedTextField(specialty, { specialty=it; formError=null }, label={Text("Especialidad")}, modifier=Modifier.fillMaxWidth(), isError=formError!=null&&specialty.isBlank())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)){
                    OutlinedTextField(phone, { phone=it.filter{c->c.isDigit()}.take(15); formError=null }, label={Text("Teléfono")}, modifier=Modifier.weight(1f), isError=formError!=null&&phone.isBlank())
                    OutlinedTextField(hourlyRate, { hourlyRate=it.filter{c->c.isDigit()||c=='.'}.take(6); formError=null }, label={Text("Tarifa/€")}, modifier=Modifier.weight(1f), isError=formError!=null&&(hourlyRate.isBlank()||hourlyRate.toDoubleOrNull()==null))
                }
                OutlinedTextField(location, { location=it; formError=null }, label={Text("Ubicación")}, modifier=Modifier.fillMaxWidth(), isError=formError!=null&&location.isBlank())
                if (formError != null) Text(formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        },
        confirmButton = { Button(onClick = {
            val rateDouble = hourlyRate.toDoubleOrNull()
            if (name.isNotBlank() && specialty.isNotBlank() && phone.isNotBlank() && location.isNotBlank() && rateDouble != null && rateDouble > 0) {
                onAddWorkshop(Workshop(System.currentTimeMillis().toString(), name.trim(), specialty.trim(), phone.trim(), location.trim(), rateDouble)); onDismiss()
            } else formError = "Por favor, rellena todos los campos correctamente."
        }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)) { Text("Añadir", color = MaterialTheme.colorScheme.onPrimary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = MaterialTheme.colorScheme.secondary) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceDialog(carId: String, currentKm: Int, workshops: List<Workshop>, onDismiss: () -> Unit, onAddMaintenance: (Maintenance) -> Unit) {
    var selectedWorkshopId by remember { mutableStateOf(workshops.firstOrNull()?.id ?: "") }
    var date by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var cost by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Preventivo") }; var km by remember { mutableStateOf(currentKm.toString()) }
    var formError by remember { mutableStateOf<String?>(null) }
    var expandedWorkshop by remember { mutableStateOf(false) }; var expandedType by remember { mutableStateOf(false) }
    val maintenanceTypes = listOf("Preventivo", "Correctivo", "Mejora", "ITV", "Diagnosis")

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface, textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("Registrar Mantenimiento", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(min=450.dp, max=600.dp).padding(vertical=8.dp)) {
                OutlinedTextField(description, {description=it;formError=null}, label={Text("Descripción del Servicio")}, modifier=Modifier.fillMaxWidth(), isError=formError!=null&&description.isBlank())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically){
                    OutlinedTextField(date, {date=it.filter{c->c.isDigit()||c=='-'}.take(10);formError=null}, label={Text("Fecha (YYYY-MM-DD)")}, placeholder={Text("YYYY-MM-DD")}, modifier=Modifier.weight(1f), isError=formError!=null&&(date.isBlank()||!date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))))
                    ExposedDropdownMenuBox(expandedType, {expandedType = !expandedType}, Modifier.weight(1f)) {
                        OutlinedTextField(value = type, onValueChange = {}, label={Text("Tipo")}, readOnly=true, trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expandedType)}, modifier=Modifier.menuAnchor().fillMaxWidth(), isError=formError!=null&&type.isBlank())
                        ExposedDropdownMenu(expandedType, {expandedType=false}) { maintenanceTypes.forEach { opt -> DropdownMenuItem(text={Text(opt)}, onClick={type=opt;expandedType=false;formError=null})}}
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)){
                    OutlinedTextField(cost, {cost=it.filter{c->c.isDigit()||c=='.'}.take(8);formError=null}, label={Text("Coste (€)")}, modifier=Modifier.weight(1f), isError=formError!=null&&(cost.isBlank()||cost.toDoubleOrNull()==null||cost.toDouble()!!<=0))
                    OutlinedTextField(km, {km=it.filter{c->c.isDigit()};formError=null}, label={Text("Kilómetros")}, modifier=Modifier.weight(1f), isError=formError!=null&&(km.isBlank()||km.toIntOrNull()==null||km.toInt()!!<=0||(km.toIntOrNull() ?: currentKm) < currentKm && type != "ITV"))
                }
                if (workshops.isNotEmpty()) {
                    ExposedDropdownMenuBox(expandedWorkshop, {expandedWorkshop = !expandedWorkshop}) {
                        OutlinedTextField(value = workshops.find{it.id==selectedWorkshopId}?.name ?: "Seleccionar taller", onValueChange = {},label={Text("Taller")}, readOnly=true, trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expandedWorkshop)}, modifier=Modifier.menuAnchor().fillMaxWidth(), isError=formError!=null&&selectedWorkshopId.isBlank())
                        ExposedDropdownMenu(expandedWorkshop, {expandedWorkshop=false}) { workshops.forEach { opt -> DropdownMenuItem(text={Text(opt.name)}, onClick={selectedWorkshopId=opt.id;expandedWorkshop=false;formError=null})}}
                    }
                } else { Text("No hay talleres. Añade un taller primero.", color = MaterialTheme.colorScheme.error) }
                if (formError != null) Text(formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        },
        confirmButton = { Button(onClick = {
            val costDouble = cost.toDoubleOrNull(); val kmInt = km.toIntOrNull(); val isDateValid = date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
            if (kmInt != null && kmInt < currentKm && type != "ITV") {
                formError = "Los KM del mantenimiento no deben ser menores a los actuales ($currentKm km), excepto para ITV."
            } else if (description.isNotBlank() && isDateValid && costDouble != null && costDouble > 0 && kmInt != null && kmInt >= 0 && selectedWorkshopId.isNotBlank()) {
                onAddMaintenance(Maintenance(System.currentTimeMillis().toString(), carId, selectedWorkshopId, date, description.trim(), costDouble, type, kmInt)); onDismiss()
            } else {
                formError="Rellena los campos correctamente.";
                if(selectedWorkshopId.isBlank() && workshops.isNotEmpty()) formError += " Selecciona un taller.";
                if(!isDateValid && date.isNotBlank()) formError += " Fecha YYYY-MM-DD.";
                if (kmInt != null && kmInt < 0) formError += " KM no pueden ser negativos."
            }
        }, enabled = workshops.isNotEmpty(), colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)) { Text("Registrar", color = MaterialTheme.colorScheme.onPrimary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = MaterialTheme.colorScheme.secondary) } }
    )
}

@Composable
fun AddExpenseDialog(carId: String, onDismiss: () -> Unit, onAddExpense: (ExpenseItem) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = {Text("Registrar Nuevo Gasto")}, text = {Text("Formulario para registrar un nuevo gasto para el coche con ID: $carId (TODO)")}, confirmButton = {Button(onClick={ onDismiss() }){Text("Guardar")}}, dismissButton = {TextButton(onClick = onDismiss){Text("Cancelar")}})
}

@Composable
fun EditCarDialog(car: Car, onDismiss: () -> Unit, onEditCar: (Car) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = {Text("Editar Vehículo: ${car.brand} ${car.model}".trim())}, text = {Text("Formulario para editar los detalles del vehículo ID: ${car.id} (TODO)")}, confirmButton = {Button(onClick={ onDismiss() }){Text("Guardar Cambios")}}, dismissButton = {TextButton(onClick = onDismiss){Text("Cancelar")}})
}

@Composable
fun AddReminderDialog(carId: String, onDismiss: () -> Unit, onAddReminder: (Reminder) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = {Text("Añadir Nuevo Recordatorio")}, text = {Text("Formulario para añadir un nuevo recordatorio para el coche con ID: $carId (TODO)")}, confirmButton = {Button(onClick={ onDismiss() }){Text("Añadir")}}, dismissButton = {TextButton(onClick = onDismiss){Text("Cancelar")}})
}

// --- CarDetailView y sus componentes internos ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailView(
    car: Car,
    maintenances: List<Maintenance>,
    workshops: List<Workshop>,
    expenses: List<ExpenseItem>,
    reminders: List<Reminder>,
    onBack: () -> Unit,
    onShowAddMaintenanceDialog: () -> Unit,
    onShowAddExpenseDialog: () -> Unit,
    onShowEditCarDialog: () -> Unit,
    onShowAddReminderDialog: () -> Unit
) {
    var selectedDetailTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Información", "Mantenimiento", "Gastos")

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onSurface) }
            Spacer(Modifier.width(16.dp))
            Text("${car.brand} ${car.model}".trim(), style = MaterialTheme.typography.titleLarge)
        }
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        Row(Modifier.fillMaxSize().padding(24.dp)) {
            Column(modifier = Modifier.weight(0.65f).padding(end = 24.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(250.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val painter = asyncPainterResource(data = car.imageUrl ?: "https://placehold.co/800x400/EFEFEF/CCC?text=+)&font=raleway")
                    KamelImage(resource = painter, contentDescription = car.brand, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                        onLoading = { CircularProgressIndicator(Modifier.size(48.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary) },
                        onFailure = { Icon(Icons.Filled.PhotoCamera, "Sin imagen", Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("${car.brand} ${car.model}".trim(), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp))
                        Text(car.year.toString(), style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                        DetailInfoRow(Icons.Filled.ConfirmationNumber, "Matrícula:", car.plate)
                        DetailInfoRow(Icons.Filled.Speed, "Kilometraje:", "${String.format("%,d", car.km)} km")
                        DetailInfoRow(Icons.Filled.CalendarMonth, "F. Compra:", car.purchaseDate ?: "N/A")
                        DetailInfoRow(Icons.Filled.ColorLens, "Color:", car.color)
                        DetailInfoRow(Icons.Filled.SettingsInputComponent, "Transmisión:", car.transmission)
                        DetailInfoRow(Icons.Filled.LocalGasStation, "Combustible:", car.fuelType)
                    }
                }
                Spacer(Modifier.height(24.dp))
                TabRow(
                    selectedTabIndex = selectedDetailTab, // Tu variable de estado para la pestaña seleccionada
                    containerColor = MaterialTheme.colorScheme.surface,
                    indicator = { tabPositions -> // tabPositions es de tipo List<TabPosition>
                        // Es buena práctica verificar que selectedDetailTab esté dentro de los límites de tabPositions
                        if (selectedDetailTab < tabPositions.size) {
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedDetailTab]), // Así se usa
                                color = MaterialTheme.colorScheme.primary // Color del indicador
                            )
                        }
                    }
                ) {
                    // Aquí van tus Tabs individuales (Tab(...))
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedDetailTab == index,
                            onClick = { selectedDetailTab = index },
                            text = { Text(title, style = if (selectedDetailTab == index) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodyMedium) },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    when (selectedDetailTab) {
                        0 -> Text("Pestaña de Información General Adicional (TODO)", style = MaterialTheme.typography.bodyLarge)
                        1 -> MaintenanceListForDetail(maintenances, workshops)
                        2 -> ExpensesListForDetail(expenses)
                    }
                }
            }

            Column(modifier = Modifier.weight(0.35f).fillMaxHeight()) {
                Text("Acciones rápidas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onShowAddMaintenanceDialog, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Registrar mantenimiento", color = MaterialTheme.colorScheme.onPrimary) }
                        OutlinedButton(onClick = onShowAddExpenseDialog, Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) { Text("Registrar gasto") }
                        OutlinedButton(onClick = onShowEditCarDialog, Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) { Text("Editar vehículo") }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("Recordatorios", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        if (reminders.isEmpty()) { Text("No hay recordatorios.", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))) }
                        else { reminders.forEach { reminder -> ReminderItemRow(reminder); if (reminder != reminders.last()) Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) } }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = onShowAddReminderDialog, Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) { Text("Añadir recordatorio") }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
        Spacer(Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
@Composable
fun MaintenanceListForDetail(maintenances: List<Maintenance>, workshops: List<Workshop>) {
    if (maintenances.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(top = 20.dp), contentAlignment = Alignment.TopCenter) {
            Text("No hay mantenimientos registrados para este vehículo.", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)))
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(maintenances.sortedByDescending { it.date }) { maintenance ->
                MaintenanceCard(maintenance = maintenance, workshop = workshops.find { it.id == maintenance.workshopId })
            }
        }
    }
}
@Composable
fun ExpensesListForDetail(expenses: List<ExpenseItem>) {
    Column {
        // El título "Gastos del vehículo" ya está en la pestaña
        if (expenses.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(top = 20.dp), contentAlignment = Alignment.TopCenter) {
                Text("No hay gastos registrados para este vehículo.", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(expenses) { expense ->
                    ExpenseRow(expense)
                    if (expense != expenses.last()) Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { /* TODO: Ver todos los gastos */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Ver todos los gastos", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
@Composable
fun ExpenseRow(expense: ExpenseItem) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(expense.icon, contentDescription = expense.description, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(expense.description, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(expense.date, style = MaterialTheme.typography.bodySmall)
        }
        Text("€${String.format("%.2f", expense.amount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}
@Composable
fun ReminderItemRow(reminder: Reminder) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(reminder.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(reminder.subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// --- DashboardCard (el que se usa en CarDetailView, diferente a StatDisplayCard) ---
@Composable
fun DashboardCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(110.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), Arrangement.Center, Alignment.CenterHorizontally ) {
            Icon(icon, contentDescription = title, Modifier.size(28.dp), tint = color)
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 1)
        }
    }
}

// --- Pestañas Globales y Tarjetas de Items ---
val preventiveColorBW = Color(0xFF5C5C5C)
val correctiveColorBW = Color.Black
val paidColorBW = Color(0xFF5C5C5C)
val pendingColorBW = Color.Black

@Composable
fun MaintenanceTab(maintenances: List<Maintenance>, workshops: List<Workshop>, onAddMaintenance: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            // El título de la pestaña ya está en el TabRow de CarDetailView
            // Text("Historial de Mantenimiento del Vehículo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f)) // Empuja el botón a la derecha si no hay título
            Button(onAddMaintenance, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Default.Add, "Nuevo Mantenimiento", tint = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(8.dp)); Text("Nuevo", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (maintenances.isEmpty()) Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Build, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp)); Text("No hay mantenimientos para este vehículo", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 16.sp)
                }
            }
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(maintenances.sortedByDescending { it.date }) { maintenance ->
                MaintenanceCard(maintenance = maintenance, workshop = workshops.find { it.id == maintenance.workshopId })
            }
        }
    }
}

@Composable
fun MaintenanceCard(maintenance: Maintenance, workshop: Workshop?, carBrandModel: String? = null) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(1.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if(maintenance.type == "Preventivo") Icons.Filled.Shield else Icons.Filled.ReportProblem,
                    contentDescription = maintenance.type,
                    tint = if(maintenance.type == "Preventivo") preventiveColorBW else correctiveColorBW,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(maintenance.description, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val subtitlePrefix = carBrandModel?.let { "$it • " } ?: ""
                    Text("$subtitlePrefix${workshop?.name?.take(15) ?: "Taller desc."}... • ${String.format("%,d", maintenance.km)} km", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = if (maintenance.type == "Preventivo") preventiveColorBW else correctiveColorBW, shape = RoundedCornerShape(6.dp)) {
                            Text(maintenance.type, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                        Text(formatDateFromYYYYMMDDToDDMMYYYY(maintenance.date) ?: maintenance.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
            Text("€${String.format("%.2f", maintenance.cost)}", fontSize = 15.sp, color = if(maintenance.type == "Preventivo") preventiveColorBW else correctiveColorBW, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InvoicesTab(maintenances: List<Maintenance>, workshops: List<Workshop>, invoices: List<Invoice>, onToggleInvoiceStatus: (String) -> Unit) {
    Column {
        // Text("Facturas del Vehículo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp)) // Título ya en la Tab
        val carMaintenanceIds = maintenances.map { it.id }.toSet()
        val relevantInvoices = invoices.filter { it.maintenanceId in carMaintenanceIds }

        if (relevantInvoices.isEmpty()) Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ReceiptLong, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp)); Text("No hay facturas para este vehículo", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 16.sp)
                }
            }
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(relevantInvoices.sortedByDescending { it.date }) { invoice ->
                val maintenanceItem = maintenances.find { it.id == invoice.maintenanceId }
                if (maintenanceItem != null) InvoiceCard(invoice = invoice, maintenance = maintenanceItem, workshop = workshops.find { it.id == maintenanceItem.workshopId }, onToggleStatus = { onToggleInvoiceStatus(invoice.id) })
            }
        }
    }
}

@Composable
fun InvoiceCard(invoice: Invoice, maintenance: Maintenance, workshop: Workshop?, onToggleStatus: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(1.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, "Factura", tint = if(invoice.status == "Pagada") paidColorBW else pendingColorBW, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Factura #${invoice.id.take(6)}...", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${maintenance.description.take(20)}... • ${workshop?.name?.take(15) ?: "Taller desc."}...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = if (invoice.status == "Pagada") paidColorBW else pendingColorBW, shape = RoundedCornerShape(6.dp)) {
                            Text(invoice.status, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                        Text(formatDateFromYYYYMMDDToDDMMYYYY(invoice.date) ?: invoice.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("€${String.format("%.2f", invoice.total)}", fontSize = 15.sp, color = if(invoice.status == "Pagada") paidColorBW else pendingColorBW, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = { /* TODO: Ver PDF */ }, modifier = Modifier.height(32.dp), enabled = false, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), contentPadding = PaddingValues(horizontal = 8.dp)) { Text("PDF", fontSize = 10.sp) }
                    if (invoice.status == "Pendiente") Button(onToggleStatus, modifier = Modifier.height(32.dp), colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary), contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Pagar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondary) }
                }
            }
        }
    }
}

@Composable
fun AllMaintenancesTab(maintenances: List<Maintenance>, cars: List<Car>, workshops: List<Workshop>) {
    Column {
        Text("Todos los Mantenimientos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
        if (maintenances.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay mantenimientos registrados.", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(maintenances.sortedByDescending { it.date }) { maintenance ->
                    val car = cars.find { it.id == maintenance.carId }
                    val workshop = workshops.find { it.id == maintenance.workshopId }
                    MaintenanceCard(maintenance = maintenance, workshop = workshop, carBrandModel = car?.let { "${it.brand} ${it.model}".trim() } ?: "Vehículo desconocido")
                }
            }
        }
    }
}

@Composable
fun GlobalExpensesTab(maintenances: List<Maintenance>, expenses: List<ExpenseItem>, cars: List<Car>) { // Renombrado
    Column {
        Text("Resumen Global de Gastos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
        val totalMaintenanceCost = maintenances.sumOf { it.cost }
        val totalOtherExpenses = expenses.sumOf { it.amount }
        Text("Gastos de Mantenimiento Totales: €${String.format("%.2f", totalMaintenanceCost)}", style = MaterialTheme.typography.bodyLarge)
        Text("Otros Gastos Registrados Totales: €${String.format("%.2f", totalOtherExpenses)}", style = MaterialTheme.typography.bodyLarge)
        Text("Gasto Combinado Total: €${String.format("%.2f", totalMaintenanceCost + totalOtherExpenses)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(20.dp))
        Text("(Aquí podrían ir gráficos globales de gastos)", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)))
    }
}


// Datos de ejemplo
val workshopsSampleData = listOf(
    Workshop("w1", "Taller Veloz", "Mecánica General", "555-0101", "Calle Falsa 123, Springfield", 50.0),
    Workshop("w2", "Boxes Auto", "Chapa y Pintura", "555-0202", "Avenida Siempreviva 742", 65.0)
)
val maintenancesSampleData = listOf(
    Maintenance("m1", "1", "w1", "2024-03-15", "Cambio de aceite y filtros", 85.0, "Preventivo", 45000),
    Maintenance("m2", "2", "w1", "2024-02-02", "Revisión de frenos", 120.0, "Correctivo", 78500),
    Maintenance("m3", "1", "w2", "2023-10-10", "Pintura puerta conductor", 250.0, "Correctivo", 40000),
    Maintenance("m4", "3", "w1", "2024-05-20", "ITV y pre-revisión", 95.0, "ITV", 21500),
)
val invoicesSampleData = listOf(
    Invoice("i1", "m1", "2024-03-15", 85.0, "Pagada"),
    Invoice("i2", "m2", "2024-02-02", 120.0, "Pendiente"),
    Invoice("i3", "m3", "2023-10-10", 250.0, "Pagada"),
    Invoice("i4", "m4", "2024-05-20", 95.0, "Pagada")
)
val sampleExpenses = listOf(
    ExpenseItem("e1", "1", "Combustible Super", "10/07/2025", 65.0, Icons.Filled.LocalGasStation),
    ExpenseItem("e2", "1", "Seguro Trimestral", "01/07/2025", 210.0, Icons.Filled.Shield),
    ExpenseItem("e3", "2", "Limpieza Completa", "12/07/2025", 20.0, Icons.Filled.Wash),
)
val sampleReminders = listOf(
    Reminder("r1", "1", "Cambio de aceite", "En 5 días - 15/08/2025"),
    Reminder("r2", "1", "Pasar ITV", "En 45 días - 30/09/2025"),
    Reminder("r3", "2", "Revisión neumáticos", "En 12 días - 22/08/2025"),
)

@Preview
@Composable
fun AppPreview() {
    CarMaintenanceTheme {
        CarMaintenanceApp()
    }
}
// FIN DEL CÓDIGO Main.kt COMPLETO