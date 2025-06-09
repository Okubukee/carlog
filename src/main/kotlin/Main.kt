import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import db.DatabaseManager
import db.repository.*
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit


data class Car(
    val id: String,
    val brand: String,
    val model: String,
    val year: Int,
    val plate: String,
    val km: Int,
    val imageUrl: String?,
    val nextServiceDate: String?,
    val color: String,
    val transmission: String,
    val fuelType: String,
    val purchaseDate: String?
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
    val workshopId: String?,
    val date: String,
    val description: String,
    val cost: Double,
    val type: String,
    val km: Int
)

data class Invoice(
    val id: String,
    val maintenanceId: String,
    val date: String,
    val total: Double,
    val status: String
)

data class ExpenseItem(
    val id: String,
    val carId: String,
    val description: String,
    val date: String,
    val amount: Double,
    val icon: ImageVector
)

data class Reminder(
    val id: String,
    val carId: String,
    val title: String,
    val subtitle: String
)
data class IconOption(val name: String, val icon: ImageVector)

val expenseIconOptions = listOf(
    IconOption("Combustible", Icons.Filled.LocalGasStation),
    IconOption("Seguro", Icons.Filled.Shield),
    IconOption("Lavado", Icons.Filled.Wash),
    IconOption("Mantenimiento/Reparación", Icons.Filled.Build),
    IconOption("Peaje/Parking", Icons.Filled.CreditCard),
    IconOption("Impuestos/Otros", Icons.Filled.Receipt),
    IconOption("Compra Accesorio", Icons.Filled.ShoppingCart)

)

fun main() = application {

    var databaseInitializedSuccessfully = false

    try {
        DatabaseManager.init()
        databaseInitializedSuccessfully = true
        println("DatabaseManager.init() llamado y completado exitosamente desde main().")
    } catch (e: Exception) {

        System.err.println("FALLO CRÍTICO AL INICIALIZAR LA BASE DE DATOS desde main(): ${e.message}")
        e.printStackTrace()
    }

    if (databaseInitializedSuccessfully) {

        var isDarkMode by remember { mutableStateOf(false) }
        var currentUser by remember { mutableStateOf<User?>(null) }

        val windowState = rememberWindowState(width = 1450.dp, height = 950.dp)
        Window(
            onCloseRequest = ::exitApplication,
            title = if (currentUser == null) "CarLog - Login" else "CarLog",
            state = windowState
        ) {
            CarMaintenanceTheme(isDarkMode = isDarkMode) {
                if (currentUser == null) {
                    AuthScreen(onLoginSuccess = { user ->
                        currentUser = user
                    })
                } else {
                    // Pasamos el usuario (y su ID) a la app principal
                    CarMaintenanceApp(
                        currentUser = currentUser!!,
                        onLogout = { currentUser = null }, // <-- Función de logout
                        isDarkMode = isDarkMode,
                        onToggleTheme = { isDarkMode = !isDarkMode }
                    )
                }
            }
        }
    } else {

        Window(
            onCloseRequest = ::exitApplication,
            title = "Error de Aplicación - CarLog",
            state = rememberWindowState(width = 600.dp, height = 300.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error crítico: No se pudo inicializar la base de datos.\n" +
                            "La aplicación no puede continuar.\n" +
                            "Por favor, revisa la consola para más detalles del error.",
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    color = Color.Red
                )
            }
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
    outline = Color(0xFFDEE2E6),

    primaryContainer = Color(0xFF303030),
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFFD0D0D0),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF00695C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB2DFDB),
    onTertiaryContainer = Color(0xFF004D40),
    errorContainer = Color(0xFFFDECEA),
    onErrorContainer = Color(0xFFB71C1C)
)

private val AppDarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9), // Light Blue
    onPrimary = Color(0xFF003366), // Dark Blue for text on primary
    primaryContainer = Color(0xFF004C99), // Darker Blue
    onPrimaryContainer = Color(0xFFD6E4FF), // Light text for onPrimaryContainer

    secondary = Color(0xFF80CBC4), // Teal
    onSecondary = Color(0xFF003737),
    secondaryContainer = Color(0xFF004F50),
    onSecondaryContainer = Color(0xFFB2DFDB),

    tertiary = Color(0xFFFFB74D), // Orange for "Pending" status
    onTertiary = Color(0xFF4E3500),
    tertiaryContainer = Color(0xFF755000),
    onTertiaryContainer = Color(0xFFFFDDB8),

    background = Color(0xFF121212), // Standard dark background
    onBackground = Color(0xFFE0E0E0), // Light gray text
    surface = Color(0xFF1E1E1E),   // Slightly lighter surface for cards, dialogs
    onSurface = Color(0xFFE0E0E0), // Light gray text on surface

    error = Color(0xFFCF6679), // Material dark error
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFC4C7C5),
    outline = Color(0xFF8E908E)
)

@Composable
fun CarMaintenanceTheme(isDarkMode: Boolean = false, content: @Composable () -> Unit) {
    val colorScheme = if (isDarkMode) AppDarkColorScheme else AppLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = colorScheme.onSurface),
            titleMedium = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = colorScheme.onSurface),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, color = colorScheme.onSurface),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = colorScheme.onSurface),
            bodySmall = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.7f)),
            labelSmall = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = colorScheme.onSurface.copy(alpha = 0.6f)),
            headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontSize=24.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
        ),
        shapes = Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(12.dp)
        ),
        content = content
    )
}


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
fun formatDateFromYYYYMMDDToDDMMYYYY(dateStrYYYYMMDD: String?): String? = parseDate(dateStrYYYYMMDD)?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))


// --- Composable Principal de la App ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarMaintenanceApp(currentUser: User,
                      onLogout: () -> Unit,
                      isDarkMode: Boolean,
                      onToggleTheme: () -> Unit) {

    val scope = rememberCoroutineScope()

    var cars by remember { mutableStateOf<List<Car>>(emptyList()) }
    var workshops by remember { mutableStateOf<List<Workshop>>(emptyList()) }
    var maintenances by remember { mutableStateOf<List<Maintenance>>(emptyList()) }
    var invoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var expenses by remember { mutableStateOf<List<ExpenseItem>>(emptyList()) }
    var reminders by remember { mutableStateOf<List<Reminder>>(emptyList()) }
    var selectedCarId by remember { mutableStateOf<String?>(null) }
    var selectedTabInMain by remember { mutableStateOf(0) }
    var showAddCarDialog by remember { mutableStateOf(false) }
    var showAddMaintenanceDialogForCarId by remember { mutableStateOf<String?>(null) }
    var showAddExpenseDialogForCarId by remember { mutableStateOf<String?>(null) }
    var showEditCarDialogForCarId by remember { mutableStateOf<String?>(null) }
    var showAddReminderDialogForCarId by remember { mutableStateOf<String?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAddWorkshopDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteCarDialog by remember { mutableStateOf(false) }
    var carIdToDelete by remember { mutableStateOf<String?>(null) }
    var showConfirmDeleteMaintenanceDialog by remember { mutableStateOf(false) }
    var maintenanceIdToDelete by remember { mutableStateOf<String?>(null) }
    var showEditMaintenanceDialog by remember { mutableStateOf(false) }
    var maintenanceToEdit by remember { mutableStateOf<Maintenance?>(null) }
    var showConfirmDeleteInvoiceDialog by remember { mutableStateOf(false) }
    var invoiceIdToDelete by remember { mutableStateOf<String?>(null) }
    var showEditInvoiceDialog by remember { mutableStateOf(false) }
    var invoiceToEdit by remember { mutableStateOf<Invoice?>(null) }
    var showConfirmDeleteExpenseDialog by remember { mutableStateOf(false) }
    var expenseIdToDelete by remember { mutableStateOf<String?>(null) }
    var showEditExpenseDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<ExpenseItem?>(null) }
    var showConfirmDeleteReminderDialog by remember { mutableStateOf(false) }
    var reminderIdToDelete by remember { mutableStateOf<String?>(null) }
    var showEditReminderDialog by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<Reminder?>(null) }
    var showConfirmDeleteWorkshopDialog by remember { mutableStateOf(false) }
    var workshopIdToDelete by remember { mutableStateOf<String?>(null) }
    var showEditWorkshopDialog by remember { mutableStateOf(false) }
    var workshopToEdit by remember { mutableStateOf<Workshop?>(null) }

    fun handleDeleteReminderRequest(id: String) {
        reminderIdToDelete = id
        showConfirmDeleteReminderDialog = true
    }
    fun handleDeleteWorkshopRequest(id: String) {
        workshopIdToDelete = id
        showConfirmDeleteWorkshopDialog = true
    }

    fun confirmDeleteWorkshop() {
        workshopIdToDelete?.let { id ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {

                        WorkshopRepository.deleteWorkshop(id)
                        workshops = WorkshopRepository.getAllWorkshops()
                        maintenances = MaintenanceRepository.getAllMaintenances()
                    } catch (e: Exception) {
                        System.err.println("Error eliminando taller: ${e.localizedMessage}")
                    }
                }
                workshopIdToDelete = null
            }
        }
    }

    fun handleEditWorkshopRequest(workshop: Workshop) {
        workshopToEdit = workshop
        showEditWorkshopDialog = true
    }

    fun confirmEditWorkshop(updatedWorkshop: Workshop) {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    WorkshopRepository.updateWorkshop(updatedWorkshop)
                    workshops = WorkshopRepository.getAllWorkshops()
                    maintenances = MaintenanceRepository.getAllMaintenances()
                } catch (e: Exception) {
                    System.err.println("Error actualizando taller: ${e.localizedMessage}")
                }
            }
            workshopToEdit = null
            showEditWorkshopDialog = false
        }
    }

    fun confirmDeleteReminder() {
        reminderIdToDelete?.let { id ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        ReminderRepository.deleteReminder(id)
                        reminders = ReminderRepository.getAllReminders()
                    } catch (e: Exception) {
                        System.err.println("Error eliminando recordatorio: ${e.localizedMessage}")
                    }
                }
                reminderIdToDelete = null
            }
        }
    }
    fun handleEditReminderRequest(reminder: Reminder) {
        reminderToEdit = reminder
        showEditReminderDialog = true
    }

    fun confirmEditReminder(newTitle: String, newSubtitle: String) {
        reminderToEdit?.let { originalReminder ->
            val updatedReminder = originalReminder.copy(title = newTitle, subtitle = newSubtitle)
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        ReminderRepository.updateReminder(updatedReminder)
                        reminders = ReminderRepository.getAllReminders()
                    } catch (e: Exception) {
                        System.err.println("Error actualizando recordatorio: ${e.localizedMessage}")
                    }
                }
                reminderToEdit = null // Limpiar
                showEditReminderDialog = false // Cerrar diálogo
            }
        }
    }
    fun handleEditExpenseRequest(expense: ExpenseItem) {
        expenseToEdit = expense
        showEditExpenseDialog = true
    }

    fun confirmEditExpense(updatedExpense: ExpenseItem) {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    ExpenseItemRepository.updateExpenseItem(updatedExpense) // ACTUALIZAR EN BD
                    expenses = ExpenseItemRepository.getAllExpenseItems() // RECARGAR LISTA
                } catch (e: Exception) {
                    System.err.println("Error actualizando gasto: ${e.localizedMessage}")
                }
            }
            expenseToEdit = null
            showEditExpenseDialog = false
        }
    }

    fun handleDeleteExpenseRequest(id: String) {
        expenseIdToDelete = id
        showConfirmDeleteExpenseDialog = true
    }

    fun confirmDeleteExpense() {
        expenseIdToDelete?.let { id ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        ExpenseItemRepository.deleteExpenseItem(id) // ELIMINAR DE LA BD
                        expenses = ExpenseItemRepository.getAllExpenseItems() // RECARGAR LISTA
                    } catch (e: Exception) {
                        System.err.println("Error eliminando gasto: ${e.localizedMessage}")
                    }
                }
                expenseIdToDelete = null
            }
        }
    }

    fun handleDeleteInvoiceRequest(id: String) {
        invoiceIdToDelete = id
        showConfirmDeleteInvoiceDialog = true
    }

    fun confirmDeleteInvoice() {
        invoiceIdToDelete?.let { id ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        InvoiceRepository.deleteInvoice(id) // ELIMINAR DE LA BD
                        invoices = InvoiceRepository.getAllInvoices() // RECARGAR LISTA
                    } catch (e: Exception) {
                        System.err.println("Error eliminando factura: ${e.localizedMessage}")
                    }
                }
                invoiceIdToDelete = null
            }
        }
    }

    fun handleEditInvoiceRequest(invoice: Invoice) {
        invoiceToEdit = invoice
        showEditInvoiceDialog = true
    }

    fun confirmEditInvoice(invoiceId: String, newStatus: String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    InvoiceRepository.updateInvoiceStatus(invoiceId, newStatus) // ACTUALIZAR EN BD
                    invoices = InvoiceRepository.getAllInvoices() // RECARGAR LISTA
                } catch (e: Exception) {
                    System.err.println("Error actualizando estado de factura: ${e.localizedMessage}")
                }
            }
            invoiceToEdit = null // Limpiar
            showEditInvoiceDialog = false
        }
    }

    fun toggleInvoiceStatus(invoiceId: String, currentStatus: String) {
        val newStatus = if (currentStatus == "Pagada") "Pendiente" else "Pagada"
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    InvoiceRepository.updateInvoiceStatus(invoiceId, newStatus)
                    invoices = InvoiceRepository.getAllInvoices()
                } catch (e: Exception) {
                    System.err.println("Error actualizando estado de factura (toggle): ${e.localizedMessage}")
                }
            }
        }
    }

    fun handleEditMaintenanceRequest(maintenance: Maintenance) {
        maintenanceToEdit = maintenance
        showEditMaintenanceDialog = true
    }

    fun confirmEditMaintenance(updatedMaintenance: Maintenance) {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    MaintenanceRepository.updateMaintenance(updatedMaintenance) // ACTUALIZAR EN BD
                    maintenances = MaintenanceRepository.getAllMaintenances() // RECARGAR LISTA

                    // Opcional: Actualizar KM del coche si el KM del mantenimiento es mayor
                    // y si este mantenimiento es el más reciente para ese coche en KM.
                    val carOfMaintenance = cars.find { it.id == updatedMaintenance.carId }
                    if (carOfMaintenance != null && updatedMaintenance.km > carOfMaintenance.km) {
                        val carMaintenances = MaintenanceRepository.getMaintenancesByCarId(carOfMaintenance.id) // Podrías optimizar no volviendo a llamar
                        if (carMaintenances.all { updatedMaintenance.km >= it.km } || carMaintenances.filter { it.id != updatedMaintenance.id }.all { updatedMaintenance.km >= it.km }) {
                            val updatedCar = carOfMaintenance.copy(km = updatedMaintenance.km)
                            CarRepository.updateCar(updatedCar)
                            cars = CarRepository.getAllCars(currentUser.id) // Recargar coches
                        }
                    }


                } catch (e: Exception) {
                    System.err.println("Error actualizando mantenimiento: ${e.localizedMessage}")
                    // Mostrar error en UI
                }
            }
            maintenanceToEdit = null // Limpiar
            showEditMaintenanceDialog = false
        }
    }

    fun handleDeleteMaintenanceRequest(id: String) {
        maintenanceIdToDelete = id
        showConfirmDeleteMaintenanceDialog = true
    }

    fun confirmDeleteMaintenance() {
        maintenanceIdToDelete?.let { id ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        MaintenanceRepository.deleteMaintenance(id) // ELIMINAR DE LA BD
                        maintenances = MaintenanceRepository.getAllMaintenances() // RECARGAR LISTA
                        invoices = InvoiceRepository.getAllInvoices() // Recargar facturas por si acaso
                    } catch (e: Exception) {
                        System.err.println("Error eliminando mantenimiento: ${e.localizedMessage}")
                    }
                }
                maintenanceIdToDelete = null // Limpiar
            }
        }
    }
    fun handleDeleteCarRequest(carId: String) {
        carIdToDelete = carId
        showConfirmDeleteCarDialog = true
    }

    fun confirmDeleteCar() {
        carIdToDelete?.let { id ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        CarRepository.deleteCar(id) // <--- ELIMINAR DE LA BD
                        cars = CarRepository.getAllCars(currentUser.id) // <--- RECARGAR LISTA DE COCHES

                        if (selectedCarId == id) {
                            selectedCarId = null
                        }
                    } catch (e: Exception) {
                        System.err.println("Error eliminando coche: ${e.localizedMessage}")
                    }
                }
                carIdToDelete = null // Limpiar el ID
            }
        }
    }

    LaunchedEffect(currentUser) {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    workshops = WorkshopRepository.getAllWorkshops()
                    cars = CarRepository.getAllCars(currentUser.id)
                    maintenances = MaintenanceRepository.getAllMaintenances()
                    invoices = InvoiceRepository.getAllInvoices()
                    expenses = ExpenseItemRepository.getAllExpenseItems()
                    reminders = ReminderRepository.getAllReminders()
                    println("Facturas cargadas desde la BD. Total: ${invoices.size}")
                    println("Simulando carga de coches desde CarRepository.getAllCars()")
                } catch (e: Exception) {
                    System.err.println("Error cargando coches: ${e.localizedMessage}")
                }
            }

            withContext(Dispatchers.IO) {
                try {
                    workshops = WorkshopRepository.getAllWorkshops() // <--- LLAMADA AL REPOSITORIO
                    println("Simulando carga de talleres desde WorkshopRepository.getAllWorkshops()")
                } catch (e: Exception) { System.err.println("Error cargando talleres: ${e.localizedMessage}") }
            }

            withContext(Dispatchers.IO) {
                try {
                    maintenances = MaintenanceRepository.getAllMaintenances() // <--- LLAMADA AL REPOSITORIO
                    println("Simulando carga de mantenimientos desde MaintenanceRepository.getAllMaintenances()")
                } catch (e: Exception) { System.err.println("Error cargando mantenimientos: ${e.localizedMessage}") }
            }

        }
    }
    val upcomingTotalMaintenancesCount = maintenances.count { maintenance ->
        parseDate(maintenance.date)?.let { serviceDate -> // parseDate es tu función de ayuda
            !serviceDate.isBefore(LocalDate.now()) // Que no sea anterior a hoy
        } ?: false
    }
    println("Total de tareas de mantenimiento próximas calculadas: $upcomingTotalMaintenancesCount")

    fun getCarById(id: String): Car? = cars.find { it.id == id }
    fun getMaintenancesByCarId(carId: String): List<Maintenance> = maintenances.filter { it.carId == carId }
    fun getLastServiceDateForCar(carId: String): String? = maintenances.filter { it.carId == carId }.maxByOrNull { it.date }?.date?.let { formatDateFromYYYYMMDDToDDMMYYYY(it) }

    val totalCars = cars.size

    val totalExpensesAllCars = invoices.filter { it.status == "Pagada" }.sumOf { it.total } + expenses.sumOf { it.amount } + maintenances.sumOf { it.cost }

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
                    invoices = invoices,
                    onBack = { selectedCarId = null },
                    onShowAddMaintenanceDialog = { showAddMaintenanceDialogForCarId = car.id },
                    onShowAddExpenseDialog = { showAddExpenseDialogForCarId = car.id },
                    onShowEditCarDialog = { showEditCarDialogForCarId = car.id },
                    onShowAddReminderDialog = { showAddReminderDialogForCarId = car.id },
                    onDeleteCarClick = { handleDeleteCarRequest(car.id) },
                    onDeleteMaintenanceRequest = { maintenanceId -> handleDeleteMaintenanceRequest(maintenanceId) },
                    onEditMaintenanceRequest = { maintenance -> handleEditMaintenanceRequest(maintenance) },
                    onDeleteInvoiceRequest = { invoiceId -> handleDeleteInvoiceRequest(invoiceId) },
                    onEditInvoiceRequest = { invoice -> handleEditInvoiceRequest(invoice) },
                    onToggleInvoiceStatus = { invoiceId, currentStatus -> toggleInvoiceStatus(invoiceId, currentStatus) },
                    onDeleteExpenseRequest = { expenseId -> handleDeleteExpenseRequest(expenseId) },
                    onEditExpenseRequest = { expense -> handleEditExpenseRequest(expense) },
                    onDeleteReminderRequest = { reminderId -> handleDeleteReminderRequest(reminderId) },
                    onEditReminderRequest = { reminder -> handleEditReminderRequest(reminder) }

                )
            } else { selectedCarId = null }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                AppHeader(
                    onSettingsClick = { showSettingsDialog = true },
                    onAddWorkshopClick = { showAddWorkshopDialog = true }
                )
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    TopDashboardCardsRow(
                        totalCars = totalCars,
                        upcomingMaintenancesCount = upcomingTotalMaintenancesCount , // <--- VALOR CAMBIADO
                        nextMaintenanceDays = soonestNextService?.third,
                        nextMaintenanceCarInfo = soonestNextService?.first?.let { "${it.brand} ${it.model}".trim().take(15) + "... (${it.nextServiceDate})" } ?: "N/A",
                        totalExpenses = totalExpensesAllCars // Asegúrate que el nombre del parámetro coincida en la definición de TopDashboardCardsRow
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    TabRow(selectedTabIndex = selectedTabInMain, containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.primary, indicator = {}, divider = {}) {
                        val tabs = listOf("Mis Vehículos", "Mantenimiento", "Gastos", "Talleres")
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
                        0 -> CarsTab(
                            cars = cars, // Pasa la lista de coches actualizada
                            onCarSelected = { selectedCarId = it },
                            getLastServiceDate = ::getLastServiceDateForCar,
                            onShowAddCarDialog = { showAddCarDialog = true },
                            onShowAddMaintenanceDialog = { carId -> showAddMaintenanceDialogForCarId = carId },
                            onDeleteCarRequest = { carId -> handleDeleteCarRequest(carId) } // <--- NUEVA LAMBDA PASADA A CarsTab
                        )
                        1 -> AllMaintenancesTab(maintenances, cars, workshops,
                            onDeleteMaintenanceRequest = { maintenanceId -> handleDeleteMaintenanceRequest(maintenanceId) }, // <--- PASAR NUEVA LAMBDA
                            onEditMaintenanceRequest = { maintenance -> handleEditMaintenanceRequest(maintenance) } // <--- PASAR NUEVA LAMBDA (para el siguiente paso)
                        )
                        2 -> GlobalExpensesTab(maintenances, invoices, expenses)
                        3 -> AllWorkshopsTab(
                            workshops = workshops,
                            onAddWorkshopClick = { showAddWorkshopDialog = true },
                            onEditWorkshopClick = { workshop -> handleEditWorkshopRequest(workshop) }, // <--- PASAR LAMBDA
                            onDeleteWorkshopConfirm = { workshopId -> handleDeleteWorkshopRequest(workshopId) } // <--- PASAR LAMBDA
                        )
                    }
                }
            }
        }
    }

    if (showEditExpenseDialog && expenseToEdit != null) {
        EditExpenseDialog(
            expenseToEdit = expenseToEdit!!,
            onDismiss = { showEditExpenseDialog = false; expenseToEdit = null },
            onConfirmEdit = { updatedExpense ->
                confirmEditExpense(updatedExpense)
                // El cierre y limpieza se manejan en confirmEditExpense
            }
        )
    }
    if (showConfirmDeleteWorkshopDialog) {
        val workshopName = workshopIdToDelete?.let { id -> workshops.find { it.id == id }?.name ?: "este taller" } ?: "este taller"
        ConfirmDeleteDialog(
            title = "Confirmar Eliminación",
            message = "Si eliminas '$workshopName', los mantenimientos asociados no se borrarán, pero perderán la referencia a este taller. ¿Estás seguro?",
            onConfirm = {
                confirmDeleteWorkshop()
                showConfirmDeleteWorkshopDialog = false
            },
            onDismiss = { showConfirmDeleteWorkshopDialog = false }
        )
    }

    if (showEditWorkshopDialog && workshopToEdit != null) {
        EditWorkshopDialog(
            workshopToEdit = workshopToEdit!!,
            onDismiss = {
                showEditWorkshopDialog = false
                workshopToEdit = null
            },
            onConfirmEdit = { updatedWorkshop ->
                confirmEditWorkshop(updatedWorkshop)
            }
        )
    }
    if (showConfirmDeleteExpenseDialog) {
        val expenseDescription = expenseIdToDelete?.let { id -> expenses.find { it.id == id }?.description ?: "este gasto" } ?: "este gasto"
        ConfirmDeleteDialog(
            title = "Confirmar Eliminación",
            message = "¿Estás seguro de que quieres eliminar el gasto \"${expenseDescription.take(30)}...\"?",
            onConfirm = {
                confirmDeleteExpense()
                showConfirmDeleteExpenseDialog = false
            },
            onDismiss = { showConfirmDeleteExpenseDialog = false }
        )
    }
    if (showConfirmDeleteReminderDialog) {
        val reminderTitle = reminderIdToDelete?.let { id -> reminders.find { it.id == id }?.title ?: "este recordatorio" } ?: "este recordatorio"
        ConfirmDeleteDialog( // Usamos el diálogo genérico que ya existe
            title = "Confirmar Eliminación",
            message = "¿Estás seguro de que quieres eliminar el recordatorio \"${reminderTitle.take(30)}...\"?",
            onConfirm = {
                confirmDeleteReminder()
                showConfirmDeleteReminderDialog = false // Cerrar el diálogo
            },
            onDismiss = { showConfirmDeleteReminderDialog = false }
        )
    }


    if (showEditReminderDialog && reminderToEdit != null) {
        EditReminderDialog(
            reminderToEdit = reminderToEdit!!,
            onDismiss = {
                showEditReminderDialog = false
                reminderToEdit = null // Limpiar al descartar
            },
            onConfirmEdit = { title, subtitle ->
                confirmEditReminder(title, subtitle)
            }
        )
    }

    if (showAddCarDialog) {
        AddCarDialog(
            onDismiss = { showAddCarDialog = false },
            onAddCar = { carDataFromDialog ->
                scope.launch { // Usar el CoroutineScope
                    val newCarId = System.currentTimeMillis().toString() // O genera un UUID
                    val carToAdd = carDataFromDialog.copy(id = newCarId) // Asignar ID

                    withContext(Dispatchers.IO) { // Operación de BD en hilo de IO
                        try {
                            CarRepository.addCar(carToAdd, currentUser.id)
                            cars = CarRepository.getAllCars(currentUser.id) // Recargar con filtro
                        } catch (e: Exception) {
                            System.err.println("Error añadiendo coche: ${e.localizedMessage}")
                        }
                    }
                    showAddCarDialog = false
                }
            }
        )
    }
    if (showSettingsDialog) {
        SettingsDialog(
            isDarkMode = isDarkMode,
            onDismiss = { showSettingsDialog = false },
            onToggleTheme = onToggleTheme,
            onLogout = {
                showSettingsDialog = false
                onLogout() // Llamar a la función de logout
            }
        )
    }

    showAddMaintenanceDialogForCarId?.let { carIdForMaintenance ->
        val car = getCarById(carIdForMaintenance) // Asume que getCarById() obtiene el coche de tu estado 'cars'

        println("VERIFICACIÓN PRE-DIÁLOGO (onAddMaintenance): carId='$carIdForMaintenance', cocheObtenidoPorGetById=$car")

        if (car != null) {
            AddMaintenanceDialog(
                carId = car.id,
                currentKm = car.km,
                workshops = workshops, // La lista de talleres cargada desde WorkshopRepository
                onDismiss = { showAddMaintenanceDialogForCarId = null },
                onAddMaintenance = { maintenanceDataFromDialog ->
                    scope.launch {

                        if (car == null) {
                            System.err.println("ERROR CRÍTICO: El objeto 'car' es null dentro de la coroutina onAddMaintenance.")
                            return@launch // Salir de la coroutina
                        }

                        val newMaintenanceId = System.currentTimeMillis().toString() // O un UUID
                        val maintenanceToAdd = maintenanceDataFromDialog.copy(id = newMaintenanceId)

                        withContext(Dispatchers.IO) {
                            try {
                                MaintenanceRepository.addMaintenance(maintenanceToAdd)
                                println("Simulando MaintenanceRepository.addMaintenance para: ${maintenanceToAdd.description}")
                                maintenances = MaintenanceRepository.getMaintenancesByCarId(maintenanceToAdd.carId) // Recargar mantenimientos

                                if (maintenanceToAdd.km > car.km) { // <- ¿Podría ser esta la línea 537?
                                    val updatedCar = car.copy(km = maintenanceToAdd.km) // <- ¿O esta?
                                    println("Simulando CarRepository.updateCar KM para ${updatedCar.brand} a ${updatedCar.km}km")
                                    cars = cars.map { if (it.id == updatedCar.id) updatedCar else it }
                                }

                                val newInvoice = Invoice(
                                    id = "inv_${newMaintenanceId}", // ID único para la factura
                                    maintenanceId = newMaintenanceId,
                                    date = maintenanceToAdd.date, // O la fecha actual si prefieres
                                    total = maintenanceToAdd.cost,
                                    status = "Pendiente" // Estado inicial por defecto
                                )
                                InvoiceRepository.addInvoice(newInvoice)
                                println("Simulando InvoiceRepository.addInvoice para mantenimiento ID: $newMaintenanceId")
                                maintenances = MaintenanceRepository.getAllMaintenances()
                                cars = CarRepository.getAllCars(currentUser.id)
                                invoices = InvoiceRepository.getAllInvoices()
                                invoices.forEach { println("Factura ID: ${it.id}, Estado: ${it.status}") }
                            } catch (e: Exception) {
                                System.err.println("Error añadiendo mantenimiento o factura: ${e.localizedMessage}")
                                e.printStackTrace()
                            }
                        }
                        showAddMaintenanceDialogForCarId = null // Cerrar el diálogo
                    }
                }
            )
        } else {
            println("ADVERTENCIA: No se encontró el coche con ID '$carIdForMaintenance'. No se muestra el diálogo de mantenimiento.")
        }
    }
    if (showConfirmDeleteInvoiceDialog) {
        val invoiceNumber = invoiceIdToDelete?.let { id -> invoices.find { it.id == id }?.id?.take(6) ?: "esta factura" } ?: "esta factura"
        ConfirmDeleteDialog(
            title = "Confirmar Eliminación",
            message = "¿Estás seguro de que quieres eliminar la factura #${invoiceNumber}...? Esta acción no se puede deshacer.",
            onConfirm = {
                confirmDeleteInvoice()
                showConfirmDeleteInvoiceDialog = false
            },
            onDismiss = { showConfirmDeleteInvoiceDialog = false }
        )
    }
    if (showEditInvoiceDialog && invoiceToEdit != null) {
        EditInvoiceDialog(
            invoice = invoiceToEdit!!,
            onDismiss = { showEditInvoiceDialog = false; invoiceToEdit = null },
            onConfirmEdit = { updatedStatus ->
                confirmEditInvoice(invoiceToEdit!!.id, updatedStatus)
            }
        )
    }
    if (showAddWorkshopDialog) {
        AddWorkshopDialog(
            onDismiss = { showAddWorkshopDialog = false },
            onAddWorkshop = { workshopDataFromDialog ->
                scope.launch {
                    val newWorkshopId = System.currentTimeMillis().toString() // O un UUID
                    val workshopToAdd = workshopDataFromDialog.copy(id = newWorkshopId)

                    withContext(Dispatchers.IO) { // Operación de BD en hilo de IO
                        try {
                            WorkshopRepository.addWorkshop(workshopToAdd)
                            println("Taller '${workshopToAdd.name}' insertado en la BD.")
                            workshops = WorkshopRepository.getAllWorkshops() // <--- ASEGÚRATE DE QUE ESTÉ DESCOMENTADA
                            println("Lista de talleres recargada desde la BD. Total: ${workshops.size}")
                        } catch (e: Exception) {
                            System.err.println("Error añadiendo taller a la BD: ${e.localizedMessage}")
                            e.printStackTrace()
                        }
                    }
                    showAddWorkshopDialog = false // Cerrar el diálogo
                }
            }
        )
    }
    val carIdForDialog = showAddExpenseDialogForCarId // Captura el valor ANTES del if

    if (showEditMaintenanceDialog && maintenanceToEdit != null) {
        val carForMaintenanceKm = cars.find { it.id == maintenanceToEdit!!.carId }?.km ?: maintenanceToEdit!!.km
        EditMaintenanceDialog(
            maintenanceToEdit = maintenanceToEdit!!,
            carCurrentKm = carForMaintenanceKm, // Pasar los KM actuales del coche asociado
            workshops = workshops,
            onDismiss = { showEditMaintenanceDialog = false; maintenanceToEdit = null },
            onEditMaintenance = { updatedMaintenance ->
                confirmEditMaintenance(updatedMaintenance)
            }
        )
    }
    if (carIdForDialog != null) {
        AddExpenseDialog( // Asegúrate de que tu AddExpenseDialog esté completamente implementado
            carId = carIdForDialog, // Pasa el valor capturado
            onDismiss = { showAddExpenseDialogForCarId = null },
            onAddExpense = { expenseItemDataFromDialog -> // expenseItemDataFromDialog debe ser un ExpenseItem (sin id)
                scope.launch {
                    if (carIdForDialog == null) { // Doble chequeo, aunque no debería ser estrictamente necesario aquí.
                        System.err.println("Error: carIdForDialog se volvió null DENTRO de la coroutina, esto es inesperado.")
                        return@launch
                    }
                    val newExpenseId = System.currentTimeMillis().toString()
                    val expenseToAdd = expenseItemDataFromDialog.copy(id = newExpenseId)

                    withContext(Dispatchers.IO) {
                        try {
                            ExpenseItemRepository.addExpenseItem(expenseToAdd)
                            println("Simulando ExpenseItemRepository.addExpenseItem para: ${expenseToAdd.description}")
                            expenses = ExpenseItemRepository.getAllExpenseItems()
                            println("Lista de expenses actualizada (simulación local). Nuevo total: ${expenses.size}")

                        } catch (e: Exception) {
                            System.err.println("Error añadiendo gasto a la BD: ${e.localizedMessage}")
                            e.printStackTrace()
                        }
                    }
                    showAddExpenseDialogForCarId = null // Cierra el diálogo
                }
            }
        )
    }
    if (showAddExpenseDialogForCarId != null) {
        val carIdForDialog = showAddExpenseDialogForCarId!!
        AddExpenseDialog(
            carId = carIdForDialog,
            onDismiss = { showAddExpenseDialogForCarId = null },
            onAddExpense = { expenseItemDataFromDialog -> // expenseItemDataFromDialog no tiene ID aún
                scope.launch {
                    val newExpenseId = System.currentTimeMillis().toString() // O UUID

                    val expenseToAdd = expenseItemDataFromDialog.copy(
                        id = newExpenseId,
                        carId = carIdForDialog // Asegurar que el carId se establece aquí
                    )

                    withContext(Dispatchers.IO) {
                        try {
                            ExpenseItemRepository.addExpenseItem(expenseToAdd) // <--- GUARDAR EN BD

                            expenses = ExpenseItemRepository.getAllExpenseItems() // <--- RECARGAR LISTA
                            println("Gasto '${expenseToAdd.description}' añadido para el coche ID: ${expenseToAdd.carId}")
                        } catch (e: Exception) {
                            System.err.println("Error añadiendo gasto: ${e.localizedMessage}")
                            // Considera mostrar un mensaje de error en la UI
                        }
                    }
                    showAddExpenseDialogForCarId = null // Cerrar el diálogo
                }
            }
        )
    }
    if (showConfirmDeleteCarDialog) {
        val carNameToDelete = carIdToDelete?.let { id -> cars.find { it.id == id }?.let { "${it.brand} ${it.model}".trim() } ?: "este vehículo" } ?: "este vehículo"
        ConfirmDeleteDialog(
            title = "Confirmar Eliminación",
            message = "¿Estás seguro de que quieres eliminar ${carNameToDelete}? Esta acción no se puede deshacer.",
            onConfirm = {
                confirmDeleteCar()
                showConfirmDeleteCarDialog = false // Cierra el diálogo después de la confirmación (o dentro de confirmDeleteCar)
            },
            onDismiss = { showConfirmDeleteCarDialog = false }
        )
    }
    if (showEditCarDialogForCarId != null) {
        val carToEdit = cars.find { it.id == showEditCarDialogForCarId } // Obtener de la lista actual
        if (carToEdit != null) {
            EditCarDialog( // Tu EditCarDialog necesitará campos para editar los datos del coche
                car = carToEdit,
                onDismiss = { showEditCarDialogForCarId = null },
                onEditCar = { updatedCarFromDialog  -> // 'updatedCar' vendría del formulario de EditCarDialog
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                // 1. ACTUALIZA EL COCHE EN LA BASE DE DATOS
                                CarRepository.updateCar(updatedCarFromDialog) // <--- DESCOMENTA ESTO
                                println("Coche '${updatedCarFromDialog.brand} ${updatedCarFromDialog.model}' actualizado en la BD.")

                                // 2. RECARGA LA LISTA DE COCHES DESDE LA BD
                                cars = CarRepository.getAllCars(currentUser.id) // <--- DESCOMENTA ESTO
                                println("Lista de coches recargada desde la BD. Total: ${cars.size}")

                            } catch (e: Exception) {
                                System.err.println("Error actualizando coche en la BD: ${e.localizedMessage}")
                                e.printStackTrace()
                                // Aquí podrías querer informar al usuario del error.
                            }
                        }
                        showEditCarDialogForCarId = null
                    }
                }
            )
        }
    } else {
        showEditCarDialogForCarId = null
    }
    if (showConfirmDeleteMaintenanceDialog) {
        val maintenanceDescription = maintenanceIdToDelete?.let { id -> maintenances.find { it.id == id }?.description ?: "este mantenimiento" } ?: "este mantenimiento"
        ConfirmDeleteDialog(
            title = "Confirmar Eliminación",
            message = "¿Estás seguro de que quieres eliminar el mantenimiento \"${maintenanceDescription.take(30)}...\"? Las facturas asociadas también se eliminarán.",
            onConfirm = {
                confirmDeleteMaintenance()
                showConfirmDeleteMaintenanceDialog = false
            },
            onDismiss = { showConfirmDeleteMaintenanceDialog = false }
        )
    }
    if (showAddReminderDialogForCarId != null) {
        val carIdForReminder = showAddReminderDialogForCarId!! // Protegido por el if
        AddReminderDialog( // Llama a tu nuevo AddReminderDialog
            carId = carIdForReminder,
            onDismiss = { showAddReminderDialogForCarId = null },
            onAddReminder = { reminderFromDialog ->
                scope.launch {
                    val newReminderId = System.currentTimeMillis().toString() // O UUID
                    val reminderToAdd = reminderFromDialog.copy(id = newReminderId)

                    println("--- DEBUG onAddReminder ---")
                    println("carIdForReminder: $carIdForReminder")
                    println("reminderFromDialog: $reminderFromDialog")
                    println("reminderToAdd: $reminderToAdd")

                    withContext(Dispatchers.IO) {
                        try {
                            ReminderRepository.addReminder(reminderToAdd) // <--- DESCOMENTA ESTO
                            println("Simulando ReminderRepository.addReminder para: ${reminderToAdd.title}")
                            reminders = ReminderRepository.getAllReminders()
                            println("Simulando recarga de recordatorios.")
                        } catch (e: Exception) {
                            System.err.println("Error añadiendo recordatorio a la BD: ${e.localizedMessage}")
                            e.printStackTrace()
                        }
                    }
                    showAddReminderDialogForCarId = null // Cerrar el diálogo
                }
            }
        )
    }
    if (showSettingsDialog) {
        SettingsDialog(
            isDarkMode = isDarkMode,
            onDismiss = { showSettingsDialog = false },
            onToggleTheme = onToggleTheme,
            // --- AÑADE ESTE PARÁMETRO QUE FALTA ---
            onLogout = {
                showSettingsDialog = false // Opcional: cierra el diálogo primero
                onLogout() // Llama a la función de logout que viene de CarMaintenanceApp
            }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseDialog(
    expenseToEdit: ExpenseItem, // Gasto a editar
    onDismiss: () -> Unit,
    onConfirmEdit: (ExpenseItem) -> Unit // Devuelve el gasto actualizado
) {
    var description by remember { mutableStateOf(expenseToEdit.description) }
    var date by remember { mutableStateOf(expenseToEdit.date) } // Asume formato DD/MM/YYYY
    var amount by remember { mutableStateOf(expenseToEdit.amount.toString()) }

    val initialIconOption = remember(expenseToEdit.icon) {
        expenseIconOptions.find { it.icon == expenseToEdit.icon } ?: expenseIconOptions.first()
    }
    var selectedIconOption by remember { mutableStateOf(initialIconOption) }
    var iconDropdownExpanded by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Gasto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(min = 400.dp).padding(vertical = 8.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; formError = null },
                    label = { Text("Descripción del Gasto") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = formError != null && description.isBlank()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it.filter { c -> c.isDigit() || c == '/' }.take(10); formError = null },
                        label = { Text("Fecha (DD/MM/YYYY)") },
                        placeholder = { Text("DD/MM/YYYY") },
                        modifier = Modifier.weight(1f),
                        isError = formError != null && (date.isBlank() || parseDate(date) == null)
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }.take(8); formError = null },
                        label = { Text("Monto (€)") },
                        modifier = Modifier.weight(1f),
                        isError = formError != null && (amount.isBlank() || amount.toDoubleOrNull() == null || amount.toDouble()!! <= 0)
                    )
                }
                ExposedDropdownMenuBox(
                    expanded = iconDropdownExpanded,
                    onExpandedChange = { iconDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedIconOption.name,
                        onValueChange = {},
                        label = { Text("Tipo de Gasto (Icono)") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = iconDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = iconDropdownExpanded, onDismissRequest = { iconDropdownExpanded = false }) {
                        expenseIconOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(option.icon, contentDescription = option.name, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(option.name)
                                }},
                                onClick = { selectedIconOption = option; iconDropdownExpanded = false; formError = null }
                            )
                        }
                    }
                }
                if (formError != null) {
                    Text(formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amountDouble = amount.toDoubleOrNull()
                val parsedDate = parseDate(date)
                if (description.isNotBlank() && parsedDate != null && amountDouble != null && amountDouble > 0) {
                    onConfirmEdit(
                        expenseToEdit.copy( // Copia el original para mantener ID y carId
                            description = description.trim(),
                            date = date,
                            amount = amountDouble,
                            icon = selectedIconOption.icon
                        )
                    )
                    onDismiss()
                } else {
                    formError = "Por favor, rellena todos los campos correctamente."; if (parsedDate == null && date.isNotBlank()) formError += " Formato de fecha inválido."
                }
            }) { Text("Guardar Cambios") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun AppHeader(
    onSettingsClick: () -> Unit,
    onAddWorkshopClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), // Padding vertical ajustado
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CarLog", style = MaterialTheme.typography.headlineSmall)
            Row(verticalAlignment = Alignment.CenterVertically) { // Fila para agrupar los botones de acción

                TextButton(onClick = onAddWorkshopClick) { // Este es el botón que debería aparecer
                    Icon(
                        imageVector = Icons.Filled.AddBusiness, // Ejemplo de icono
                        contentDescription = "Añadir Taller",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir Taller", color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.width(8.dp)) // Espacio entre el nuevo botón y el de configuración
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Configuración",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    }
}

@Composable
fun TopDashboardCardsRow(totalCars: Int, upcomingMaintenancesCount: Int, nextMaintenanceDays: Long?, nextMaintenanceCarInfo: String?, totalExpenses: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatDisplayCard("Vehículos", totalCars.toString(), Icons.Filled.DirectionsCar, Modifier.weight(1f))
        StatDisplayCard(
            "Mantenim. Pendientes",
            upcomingMaintenancesCount.toString(),
            Icons.Filled.Build,
            Modifier.weight(1f))
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
fun CarsTab(
    cars: List<Car>,
    onCarSelected: (String) -> Unit,
    getLastServiceDate: (String) -> String?,
    onShowAddCarDialog: () -> Unit,
    onShowAddMaintenanceDialog: (String) -> Unit,
    onDeleteCarRequest: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)) {
        items(cars) { car ->
            CarCard(
                car = car,
                lastServiceDate = getLastServiceDate(car.id),
                onDetailsClick = { onCarSelected(car.id) },
                onMaintenanceClick = { onShowAddMaintenanceDialog(car.id) },
                onDeleteClick = { onDeleteCarRequest(car.id)
                }
            )
        }
        item { AddVehicleCard(onClick = onShowAddCarDialog) }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInvoiceDialog(
    invoice: Invoice,
    onDismiss: () -> Unit,
    onConfirmEdit: (newStatus: String) -> Unit
) {
    var status by remember { mutableStateOf(invoice.status) }
    val possibleStatus = listOf("Pagada", "Pendiente") // Puedes añadir más si es necesario
    var expandedStatus by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Factura #${invoice.id.take(6)}...") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Mantenimiento ID: ${invoice.maintenanceId}")
                Text("Fecha: ${formatDateFromYYYYMMDDToDDMMYYYY(invoice.date) ?: invoice.date}")
                Text("Total: €${String.format("%.2f", invoice.total)}")

                Spacer(Modifier.height(8.dp))
                Text("Estado de la Factura:", style = MaterialTheme.typography.bodyLarge)
                ExposedDropdownMenuBox(
                    expanded = expandedStatus,
                    onExpandedChange = { expandedStatus = !expandedStatus }
                ) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {}, // No se cambia directamente aquí
                        label = { Text("Estado") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStatus) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedStatus,
                        onDismissRequest = { expandedStatus = false }
                    ) {
                        possibleStatus.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    status = selectionOption
                                    expandedStatus = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmEdit(status) }) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarCard(car: Car, lastServiceDate: String?, onDetailsClick: () -> Unit, onMaintenanceClick: () -> Unit,  onDeleteClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.width(280.dp).height(380.dp), shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, hoveredElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                val painter = asyncPainterResource(data = car.imageUrl ?: "https://placehold.co/600x400/EFEFEF/CCC?text=+)&font=raleway")
                KamelImage(
                    resource = painter,
                    contentDescription = car.brand,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onLoading = { CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary) },
                    onFailure = { Icon(Icons.Filled.PhotoCamera, "Sin imagen", Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                )
                IconButton(
                    onClick = onDeleteClick, // Llama a la nueva lambda
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), CircleShape) // Fondo semitransparente
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline, // Icono de eliminar
                        contentDescription = "Eliminar Vehículo",
                        tint = MaterialTheme.colorScheme.error // Color rojo para indicar peligro
                    )
                }
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
    var purchaseDate by remember { mutableStateOf("") }
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
                    ExposedDropdownMenuBox(
                        expanded = transmissionDropdownExpanded,
                        onExpandedChange = { transmissionDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = transmission,
                            onValueChange = {},
                            label = { Text("Transmisión") },
                            placeholder = { Text("Seleccionar...") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transmissionDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            isError = formError != null && transmission.isBlank()
                        )
                        ExposedDropdownMenu(
                            expanded = transmissionDropdownExpanded,
                            onDismissRequest = { transmissionDropdownExpanded = false }
                        ) {
                            transmissionTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        transmission = type
                                        transmissionDropdownExpanded = false
                                        formError = null
                                    }
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = fuelDropdownExpanded,
                        onExpandedChange = { fuelDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = fuelType,
                            onValueChange = {},
                            label = { Text("Combustible") },
                            placeholder = { Text("Seleccionar...") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            isError = formError != null && fuelType.isBlank()
                        )
                        ExposedDropdownMenu(
                            expanded = fuelDropdownExpanded,
                            onDismissRequest = { fuelDropdownExpanded = false }
                        ) {
                            fuelTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        fuelType = type
                                        fuelDropdownExpanded = false
                                        formError = null
                                    }
                                )
                            }
                        }
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
@OptIn(ExperimentalMaterial3Api::class)
fun AddExpenseDialog(
    carId: String, // Para asociar el gasto al coche
    onDismiss: () -> Unit,
    onAddExpense: (ExpenseItem) -> Unit // Devuelve el ExpenseItem completo (sin ID de BD)
) {
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
    var amount by remember { mutableStateOf("") }
    var selectedIconOption by remember { mutableStateOf(expenseIconOptions.first()) }
    var iconDropdownExpanded by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Nuevo Gasto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(min = 400.dp).padding(vertical = 8.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; formError = null },
                    label = { Text("Descripción del Gasto") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = formError != null && description.isBlank()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it.filter { c -> c.isDigit() || c == '/' }.take(10); formError = null },
                        label = { Text("Fecha (DD/MM/YYYY)") },
                        placeholder = { Text("DD/MM/YYYY") },
                        modifier = Modifier.weight(1f),
                        isError = formError != null && (date.isBlank() || parseDate(date) == null)
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }.take(8); formError = null },
                        label = { Text("Monto (€)") },
                        modifier = Modifier.weight(1f),
                        isError = formError != null && (amount.isBlank() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0)
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = iconDropdownExpanded,
                    onExpandedChange = { iconDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedIconOption.name,
                        onValueChange = {},
                        label = { Text("Tipo de Gasto (Icono)") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = iconDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = iconDropdownExpanded,
                        onDismissRequest = { iconDropdownExpanded = false }
                    ) {
                        expenseIconOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(option.icon, contentDescription = option.name, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(option.name)
                                    }
                                },
                                onClick = {
                                    selectedIconOption = option
                                    iconDropdownExpanded = false
                                    formError = null
                                }
                            )
                        }
                    }
                }

                if (formError != null) {
                    Text(formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amountDouble = amount.toDoubleOrNull()
                val parsedDate = parseDate(date) // Usa tu función parseDate
                if (description.isNotBlank() && parsedDate != null && amountDouble != null && amountDouble > 0) {
                    onAddExpense(
                        ExpenseItem(
                            id = "", // El ID se asignará en CarMaintenanceApp antes de la BD
                            carId = carId, // Ya lo tenemos
                            description = description.trim(),
                            date = date, // Guardamos el string formateado
                            amount = amountDouble,
                            icon = selectedIconOption.icon
                        )
                    )
                    onDismiss()
                } else {
                    formError = "Por favor, rellena todos los campos correctamente."
                    if (parsedDate == null && date.isNotBlank()) formError += " Formato de fecha inválido."
                }
            }) { Text("Guardar Gasto") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCarDialog(
    car: Car, // El coche a editar
    onDismiss: () -> Unit,
    onEditCar: (Car) -> Unit // Lambda que se llama con el coche actualizado
) {
    // Estados para los campos del formulario, inicializados con los datos del coche
    var brandAndModel by remember { mutableStateOf("${car.brand} ${car.model}".trim()) }
    var year by remember { mutableStateOf(car.year.toString()) }
    var plate by remember { mutableStateOf(car.plate) }
    var km by remember { mutableStateOf(car.km.toString()) }
    var imageUrl by remember { mutableStateOf(car.imageUrl ?: "") }
    var nextServiceDate by remember { mutableStateOf(car.nextServiceDate ?: "") }
    var color by remember { mutableStateOf(car.color) }
    var transmission by remember { mutableStateOf(car.transmission) }
    var fuelType by remember { mutableStateOf(car.fuelType) }
    var purchaseDate by remember { mutableStateOf(car.purchaseDate ?: "") }
    var formError by remember { mutableStateOf<String?>(null) }
    val transmissionTypes = listOf("Manual", "Automática", "CVT", "Secuencial", "Otro")
    var transmissionDropdownExpanded by remember { mutableStateOf(false) }
    val fuelTypes = listOf("Gasolina", "Diesel", "Híbrido", "Eléctrico", "GLP", "GNC", "Otro")
    var fuelDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Editar Vehículo: ${car.brand} ${car.model}".trim(), style = MaterialTheme.typography.titleLarge) },
        text = {
            LazyColumn(modifier = Modifier.widthIn(min = 480.dp, max = 600.dp).padding(vertical = 8.dp)) {
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    OutlinedTextField(
                        value = brandAndModel,
                        onValueChange = { brandAndModel = it; formError = null },
                        label = { Text("Marca y Modelo") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = formError != null && brandAndModel.isBlank()
                    )
                }
                item { Spacer(Modifier.height(10.dp)) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it.filter { c -> c.isDigit() }.take(4); formError = null },
                            label = { Text("Año") },
                            modifier = Modifier.weight(1f),
                            isError = formError != null && (year.isBlank() || year.toIntOrNull() == null || year.length != 4)
                        )
                        OutlinedTextField(
                            value = plate.uppercase(),
                            onValueChange = { plate = it.take(10); formError = null },
                            label = { Text("Matrícula") },
                            modifier = Modifier.weight(1f),
                            isError = formError != null && plate.isBlank(),
                        )
                    }
                }
                item { Spacer(Modifier.height(10.dp)) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = km,
                            onValueChange = { km = it.filter { c -> c.isDigit() }; formError = null },
                            label = { Text("Kilómetros") },
                            modifier = Modifier.weight(1f),
                            isError = formError != null && (km.isBlank() || km.toIntOrNull() == null)
                        )
                        OutlinedTextField(
                            value = color,
                            onValueChange = { color = it; formError = null },
                            label = { Text("Color") },
                            modifier = Modifier.weight(1f),
                            isError = formError != null && color.isBlank()
                        )
                    }
                }
                item { Spacer(Modifier.height(10.dp)) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ExposedDropdownMenuBox(transmissionDropdownExpanded, { transmissionDropdownExpanded = it }, Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = transmission,
                                onValueChange = {},
                                label = { Text("Transmisión") },
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transmissionDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                isError = formError != null && transmission.isBlank()
                            )
                            ExposedDropdownMenu(transmissionDropdownExpanded, { transmissionDropdownExpanded = false }) {
                                transmissionTypes.forEach { type ->
                                    DropdownMenuItem(text = { Text(type) }, onClick = {
                                        transmission = type
                                        transmissionDropdownExpanded = false
                                        formError = null
                                    })
                                }
                            }
                        }
                        ExposedDropdownMenuBox(fuelDropdownExpanded, { fuelDropdownExpanded = it }, Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = fuelType,
                                onValueChange = {},
                                label = { Text("Combustible") },
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                isError = formError != null && fuelType.isBlank()
                            )
                            ExposedDropdownMenu(fuelDropdownExpanded, { fuelDropdownExpanded = false }) {
                                fuelTypes.forEach { type ->
                                    DropdownMenuItem(text = { Text(type) }, onClick = {
                                        fuelType = type
                                        fuelDropdownExpanded = false
                                        formError = null
                                    })
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(10.dp)) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = purchaseDate,
                            onValueChange = { purchaseDate = it.filter { c -> c.isDigit() || c == '/' }.take(10); formError = null },
                            label = { Text("Fecha Compra (DD/MM/YYYY)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = nextServiceDate,
                            onValueChange = { nextServiceDate = it.filter { c -> c.isDigit() || c == '/' }.take(10); formError = null },
                            label = { Text("Próximo Serv. (DD/MM/YYYY)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item { Spacer(Modifier.height(10.dp)) }
                item {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it; formError = null },
                        label = { Text("URL de Imagen (Opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item { Spacer(Modifier.height(10.dp)) }
                if (formError != null) {
                    item { Text(formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val yearInt = year.toIntOrNull()
                val kmInt = km.toIntOrNull()
                if (brandAndModel.isNotBlank() && plate.isNotBlank() && yearInt != null && year.length == 4 && kmInt != null && color.isNotBlank() && transmission.isNotBlank() && fuelType.isNotBlank()) {
                    val parts = brandAndModel.split(" ", limit = 2)
                    val brandValue = parts.getOrElse(0) { "" }
                    val modelValue = parts.getOrElse(1) { "" }

                    val updatedCar = car.copy( // Copia el coche original para mantener el ID
                        brand = brandValue,
                        model = modelValue,
                        year = yearInt,
                        plate = plate.trim().uppercase(),
                        km = kmInt,
                        imageUrl = imageUrl.ifBlank { null },
                        nextServiceDate = nextServiceDate.ifBlank { null },
                        color = color.trim(),
                        transmission = transmission.trim(),
                        fuelType = fuelType.trim(),
                        purchaseDate = purchaseDate.ifBlank { null }
                    )
                    onEditCar(updatedCar) // Llama a la lambda con el coche actualizado
                    onDismiss()
                } else {
                    formError = "Rellena todos los campos obligatorios correctamente."
                }
            }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)) {
                Text("Guardar Cambios", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = MaterialTheme.colorScheme.secondary) } }
    )
}

@Composable
fun AddReminderDialog(
    carId: String, // Para asociar el recordatorio al coche
    onDismiss: () -> Unit,
    onAddReminder: (Reminder) -> Unit // Devuelve el Reminder completo (sin ID de BD)
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") } // El subtítulo puede incluir la fecha o detalles
    var formError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Nuevo Recordatorio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(min = 400.dp).padding(vertical = 8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; formError = null },
                    label = { Text("Título del Recordatorio") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = formError != null && title.isBlank(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it; formError = null },
                    label = { Text("Detalles / Fecha (ej: Próximo 15/08/2025)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = formError != null && subtitle.isBlank(),
                    maxLines = 3
                )
                if (formError != null) {
                    Text(formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank() && subtitle.isNotBlank()) {
                    onAddReminder(
                        Reminder(
                            id = "", // El ID se asignará en CarMaintenanceApp antes de la BD
                            carId = carId, // Ya lo tenemos
                            title = title.trim(),
                            subtitle = subtitle.trim()
                        )
                    )
                    onDismiss()
                } else {
                    formError = "Por favor, rellena todos los campos."
                }
            }) { Text("Guardar Recordatorio") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun SettingsDialog(isDarkMode: Boolean, onDismiss: () -> Unit, onToggleTheme: () -> Unit, onLogout: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuración", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Modo Oscuro", color = MaterialTheme.colorScheme.onSurface)
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onToggleTheme() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
            Button(
                onClick = onLogout,
                 colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
            Text("Cerrar Sesión", color = MaterialTheme.colorScheme.onError)
        }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Cerrar", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailView(
    car: Car,
    maintenances: List<Maintenance>,
    workshops: List<Workshop>,
    expenses: List<ExpenseItem>,
    reminders: List<Reminder>,
    invoices: List<Invoice>,
    onBack: () -> Unit,
    onShowAddMaintenanceDialog: () -> Unit,
    onShowAddExpenseDialog: () -> Unit,
    onShowEditCarDialog: () -> Unit,
    onShowAddReminderDialog: () -> Unit,
    onDeleteCarClick: () -> Unit = {},
    onDeleteMaintenanceRequest: (String) -> Unit = {},
    onEditMaintenanceRequest: (Maintenance) -> Unit = {},
    onDeleteInvoiceRequest: (String) -> Unit = {},
    onEditInvoiceRequest: (Invoice) -> Unit = {},
    onDeleteExpenseRequest: (String) -> Unit = {},
    onEditExpenseRequest: (ExpenseItem) -> Unit = {},
    onDeleteReminderRequest: (String) -> Unit = {},
    onEditReminderRequest: (Reminder) -> Unit = {},
    onToggleInvoiceStatus: (invoiceId: String, currentStatus: String) -> Unit = { _, _ -> }

) {
    var selectedDetailTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Información", "Mantenimiento", "Gastos", "Facturas")
    val carMaintenanceIds = maintenances.map { it.id }.toSet()
    val relevantInvoices = invoices.filter { it.maintenanceId in carMaintenanceIds }

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
                    selectedTabIndex = selectedDetailTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    indicator = { tabPositions ->
                        if (selectedDetailTab < tabPositions.size) {
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedDetailTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
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
                        0 -> CarInfoTab(car = car, maintenances = maintenances, expenses = expenses, invoices = relevantInvoices)
                        1 -> MaintenanceListForDetail(
                            maintenances = maintenances, // 'maintenances' es un parámetro de CarDetailView
                            workshops = workshops,       // 'workshops' es un parámetro de CarDetailView
                            onDeleteMaintenanceRequest = onDeleteMaintenanceRequest, // <--- PASAR LA LAMBDA
                            onEditMaintenanceRequest = onEditMaintenanceRequest    // <--- PASAR LA LAMBDA (para después)
                        )
                        2 -> ExpensesListForDetail(
                            expenses = expenses, // 'expenses' es un parámetro de CarDetailView
                            onDeleteExpenseRequest = onDeleteExpenseRequest, // <--- PASAR LAMBDA
                            onEditExpenseRequest = onEditExpenseRequest    // <--- PARA EL SIGUIENTE PASO
                        )
                        3 -> {
                            // Filtramos las facturas que pertenecen a los mantenimientos de ESTE coche
                            val carMaintenanceIds = maintenances.map { it.id }.toSet()
                            val relevantInvoices = invoices.filter { it.maintenanceId in carMaintenanceIds }
                            InvoicesTab(
                                maintenances = maintenances, // Mantenimientos de este coche
                                workshops = workshops,       // Talleres globales
                                invoices = relevantInvoices, // Facturas filtradas para este coche
                                onToggleInvoiceStatus = onToggleInvoiceStatus,
                                onEditInvoiceRequest = onEditInvoiceRequest,
                                onDeleteInvoiceRequest = onDeleteInvoiceRequest
                            )
                        }
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
                        OutlinedButton(
                            onClick = onDeleteCarClick, // Llama a la nueva lambda
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = "Eliminar Vehículo", tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("Eliminar Vehículo")
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("Recordatorios", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        if (reminders.isEmpty()) { Text("No hay recordatorios.", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))) }
                        else { reminders.forEach { reminder -> ReminderItemRow(reminder,
                            onEditClick = { onEditReminderRequest(reminder) },     // <--- PASAR LAMBDA
                            onDeleteClick = { onDeleteReminderRequest(reminder.id) }); if (reminder != reminders.last()) Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) } }
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
fun MaintenanceListForDetail(
    maintenances: List<Maintenance>,
    workshops: List<Workshop>,
    onDeleteMaintenanceRequest: (String) -> Unit, // <--- NUEVO PARÁMETRO
    onEditMaintenanceRequest: (Maintenance) -> Unit // <--- NUEVO PARÁMETRO (para después)
) {
    if (maintenances.isEmpty()) {
        // ... (mensaje de no hay mantenimientos)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(maintenances.sortedByDescending { it.date }) { maintenance ->
                MaintenanceCard(
                    maintenance = maintenance,
                    workshop = workshops.find { it.id == maintenance.workshopId },
                    onDeleteClick = { onDeleteMaintenanceRequest(maintenance.id) }, // <--- PASAR LA LAMBDA
                    onEditClick = { onEditMaintenanceRequest(maintenance) } // <--- PASAR LA LAMBDA (para después)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMaintenanceDialog(
    maintenanceToEdit: Maintenance, // El mantenimiento a editar
    carCurrentKm: Int, // KM actuales del coche para validación
    workshops: List<Workshop>,
    onDismiss: () -> Unit,
    onEditMaintenance: (Maintenance) -> Unit // Devuelve el mantenimiento actualizado
) {
    // Estados para los campos del formulario, inicializados con maintenanceToEdit
    var selectedWorkshopId by remember { mutableStateOf(maintenanceToEdit.workshopId ?: workshops.firstOrNull()?.id ?: "") }
    var date by remember { mutableStateOf(maintenanceToEdit.date) }
    var description by remember { mutableStateOf(maintenanceToEdit.description) }
    var cost by remember { mutableStateOf(maintenanceToEdit.cost.toString()) }
    var type by remember { mutableStateOf(maintenanceToEdit.type) }
    var km by remember { mutableStateOf(maintenanceToEdit.km.toString()) }

    var formError by remember { mutableStateOf<String?>(null) }
    var expandedWorkshop by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }
    val maintenanceTypes = listOf("Preventivo", "Correctivo", "Mejora", "ITV", "Diagnosis")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("Editar Mantenimiento", style = MaterialTheme.typography.titleLarge) },
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
                    OutlinedTextField(km, {km=it.filter{c->c.isDigit()};formError=null}, label={Text("Kilómetros")}, modifier=Modifier.weight(1f), isError=formError!=null&&(km.isBlank()||km.toIntOrNull()==null||km.toInt()!!<0 /* Permitir KM menores si se edita un mantenimiento antiguo, pero no menor que los KM del coche si no es ITV */))
                }
                if (workshops.isNotEmpty()) {
                    ExposedDropdownMenuBox(expandedWorkshop, {expandedWorkshop = !expandedWorkshop}) {
                        OutlinedTextField(
                            value = workshops.find { it.id == selectedWorkshopId }?.name ?: (if (selectedWorkshopId.isBlank() && maintenanceToEdit.workshopId == null) "Sin Taller (Opcional)" else workshops.firstOrNull{it.id == maintenanceToEdit.workshopId}?.name ?: "Seleccionar taller"),
                            onValueChange = {},
                            label = { Text("Taller (Opcional)") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedWorkshop) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            isError = formError != null && selectedWorkshopId.isBlank() && maintenanceToEdit.workshopId != null // Error si antes tenia taller y ahora no se selecciona uno, aunque sea opcional
                        )
                        ExposedDropdownMenu(expandedWorkshop, { expandedWorkshop = false }) {
                            // Opción para "Sin Taller"
                            DropdownMenuItem(text = { Text("Sin Taller (Mantener Opcional)") }, onClick = {
                                selectedWorkshopId = "" // Usar un string vacío o null para indicar "Sin Taller"
                                expandedWorkshop = false
                                formError = null
                            })
                            workshops.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.name) },
                                    onClick = {
                                        selectedWorkshopId = opt.id
                                        expandedWorkshop = false
                                        formError = null
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text("No hay talleres disponibles. Puedes añadir uno o continuar sin seleccionar taller.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    // Permitir selectedWorkshopId = null o "" si no hay talleres.
                    LaunchedEffect(Unit) { selectedWorkshopId = maintenanceToEdit.workshopId ?: "" } // Mantener el workshopId original si es null
                }
                if (formError != null) Text(formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        },
        confirmButton = {
            Button(onClick = {
                val costDouble = cost.toDoubleOrNull()
                val kmInt = km.toIntOrNull()
                val isDateValid = date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
                if (kmInt != null && kmInt < carCurrentKm && type != "ITV" && kmInt < maintenanceToEdit.km) { // Comparamos con KM originales del mantenimiento si es menor que el actual del coche
                    formError = "Los KM del mantenimiento no pueden ser menores a los KM actuales del coche ($carCurrentKm km) a menos que sea ITV o se esté corrigiendo un registro anterior con KM menores que los del coche pero mayores o iguales a los KM originales del mantenimiento."
                } else if (description.isNotBlank() && isDateValid && costDouble != null && costDouble > 0 && kmInt != null && kmInt >= 0) {
                    val updatedMaintenance = maintenanceToEdit.copy(
                        workshopId = if (selectedWorkshopId.isBlank()) null else selectedWorkshopId, // Manejar "Sin Taller"
                        date = date,
                        description = description.trim(),
                        cost = costDouble,
                        type = type,
                        km = kmInt
                    )
                    onEditMaintenance(updatedMaintenance)
                    onDismiss()
                } else {
                    formError = "Rellena los campos correctamente."
                    if (!isDateValid && date.isNotBlank()) formError += " Fecha YYYY-MM-DD."
                    if (kmInt != null && kmInt < 0) formError += " KM no pueden ser negativos."
                }
            }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)) { Text("Guardar Cambios", color = MaterialTheme.colorScheme.onPrimary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = MaterialTheme.colorScheme.secondary) } }
    )
}
@Composable
fun ExpensesListForDetail(expenses: List<ExpenseItem>,
                          onEditExpenseRequest: (ExpenseItem) -> Unit = {}, // <--- Para el siguiente paso
                          onDeleteExpenseRequest: (String) -> Unit = {} ) {
    Column {
        if (expenses.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(top = 20.dp), contentAlignment = Alignment.TopCenter) {
                Text("No hay gastos registrados para este vehículo.", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(expenses) { expense -> // expenses ya viene ordenada por fecha descendente del repositorio
                    ExpenseRow(
                        expense = expense,
                        onEditClick = { onEditExpenseRequest(expense) }, // <--- Para el siguiente paso
                        onDeleteClick = { onDeleteExpenseRequest(expense.id) } // <--- PASAR LAMBDA
                    )
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
fun ExpenseRow(expense: ExpenseItem, onEditClick: () -> Unit = {}, onDeleteClick: () -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(expense.icon, contentDescription = expense.description, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(expense.description, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(expense.date, style = MaterialTheme.typography.bodySmall)
        }
        Text("€${String.format("%.2f", expense.amount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.width(8.dp)) // Space before buttons
        IconButton(onClick = onEditClick, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Edit, "Editar Gasto", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        IconButton(onClick = onDeleteClick, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Delete, "Eliminar Gasto", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
        }
    }
}
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Eliminar", color = MaterialTheme.colorScheme.onError)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.secondary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderDialog(
    reminderToEdit: Reminder, // Recordatorio actual para pre-rellenar
    onDismiss: () -> Unit,
    onConfirmEdit: (title: String, subtitle: String) -> Unit // Devuelve los nuevos valores
) {
    var title by remember { mutableStateOf(reminderToEdit.title) }
    var subtitle by remember { mutableStateOf(reminderToEdit.subtitle) }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Recordatorio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = it.isBlank() },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = titleError,
                    singleLine = true
                )
                if (titleError) {
                    Text("El título no puede estar vacío.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtítulo / Descripción Adicional") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirmEdit(title.trim(), subtitle.trim())
                        onDismiss() // Cierra el diálogo
                    } else {
                        titleError = true
                    }
                }
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
@Composable
fun ReminderItemRow(reminder: Reminder ,  onEditClick: () -> Unit = {},
                    onDeleteClick: () -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(reminder.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(reminder.subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(8.dp)) // Space before buttons
        IconButton(onClick = onEditClick, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Edit, "Editar Recordatorio", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        IconButton(onClick = onDeleteClick, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Delete, "Eliminar Recordatorio", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
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
@Composable
fun WorkshopListItem(
    workshop: Workshop,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(workshop.name, style = MaterialTheme.typography.titleMedium)
                Text(workshop.specialty, style = MaterialTheme.typography.bodySmall)
                Text(workshop.location, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)))
            }
            Row {
                IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) { // Llamada a onEditClick
                    Icon(Icons.Filled.Edit, "Editar Taller", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) { // Llamada a onDeleteClick
                    Icon(Icons.Filled.Delete, "Eliminar Taller", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun AllWorkshopsTab(
    workshops: List<Workshop>,
    onAddWorkshopClick: () -> Unit,
    onEditWorkshopClick: (Workshop) -> Unit = {},
    onDeleteWorkshopConfirm: (String) -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gestión de Talleres", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onAddWorkshopClick) {
                Icon(Icons.Filled.AddBusiness, contentDescription = "Añadir Taller")
                Spacer(Modifier.width(8.dp))
                Text("Añadir Taller")
            }
        }

        if (workshops.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay talleres registrados.", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(workshops) { workshop ->
                    WorkshopListItem(
                        workshop = workshop,
                        onEditClick = { onEditWorkshopClick(workshop) }, // Implement actual navigation/dialog later
                        onDeleteClick = { onDeleteWorkshopConfirm(workshop.id) } // Implement actual confirmation later
                    )
                }
            }
        }
    }
}

@Composable
fun MaintenanceCard(maintenance: Maintenance, workshop: Workshop?, carBrandModel: String? = null, onEditClick: () -> Unit = {}, onDeleteClick: () -> Unit = {}) {
    val isPreventive = maintenance.type == "Preventivo"
    val iconTint = if (isPreventive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val costColor = iconTint
    val badgeBackgroundColor = if (isPreventive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val badgeContentColor = if (isPreventive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer

    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(1.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPreventive) Icons.Filled.Shield else Icons.Filled.ReportProblem,
                    contentDescription = maintenance.type,
                    tint = iconTint,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) { // Ensure column takes available width before buttons
                    Text(maintenance.description, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val subtitlePrefix = carBrandModel?.let { "$it • " } ?: ""
                    Text("$subtitlePrefix${workshop?.name?.take(15) ?: "Taller desc."}... • ${String.format("%,d", maintenance.km)} km", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = badgeBackgroundColor, shape = RoundedCornerShape(6.dp)) {
                            Text(maintenance.type, color = badgeContentColor, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                        Text(formatDateFromYYYYMMDDToDDMMYYYY(maintenance.date) ?: maintenance.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
            // Column for cost and action buttons
            Column(horizontalAlignment = Alignment.End) {
                Text("€${String.format("%.2f", maintenance.cost)}", fontSize = 15.sp, color = costColor, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp)) // Add some space
                Row {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.Edit, "Editar Mantenimiento", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.Delete, "Eliminar Mantenimiento", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun InvoicesTab(
    maintenances: List<Maintenance>, // Lista de mantenimientos del coche actual
    workshops: List<Workshop>,
    invoices: List<Invoice>, // Lista de todas las facturas (ya filtrada o se filtra aquí)
    onToggleInvoiceStatus: (invoiceId: String, currentStatus: String) -> Unit,
    onEditInvoiceRequest: (Invoice) -> Unit,
    onDeleteInvoiceRequest: (String) -> Unit
) {
    Column {
        val carMaintenanceIds = maintenances.map { it.id }.toSet()
        val relevantInvoices = invoices.filter { it.maintenanceId in carMaintenanceIds }

        if (relevantInvoices.isEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ReceiptLong, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(Modifier.height(16.dp)); Text("No hay facturas para este vehículo", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 16.sp)
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(relevantInvoices.sortedByDescending { it.date }) { invoice ->
                    val maintenanceItem = maintenances.find { it.id == invoice.maintenanceId }
                    if (maintenanceItem != null) {
                        InvoiceCard(
                            invoice = invoice,
                            maintenance = maintenanceItem,
                            workshop = workshops.find { it.id == maintenanceItem.workshopId },
                            onToggleStatus = { onToggleInvoiceStatus(invoice.id, invoice.status) },
                            onEditClick = { onEditInvoiceRequest(invoice) },       // <--- PASAR LA LAMBDA
                            onDeleteClick = { onDeleteInvoiceRequest(invoice.id) } // <--- PASAR LA LAMBDA
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceCard(invoice: Invoice, maintenance: Maintenance, workshop: Workshop?, onToggleStatus: () -> Unit, // Lambda existente
                onEditClick: () -> Unit = {},    // <--- NUEVA LAMBDA
                onDeleteClick: () -> Unit = {} ) {
    val isPaid = invoice.status == "Pagada"
    val iconTint = if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val totalColor = iconTint
    val badgeBackgroundColor = if (isPaid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
    val badgeContentColor = if (isPaid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer

    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(1.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, "Factura", tint = iconTint, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Factura #${invoice.id.take(6)}...", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${maintenance.description.take(20)}... • ${workshop?.name?.take(15) ?: "Taller desc."}...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = badgeBackgroundColor, shape = RoundedCornerShape(6.dp)) {
                            Text(invoice.status, color = badgeContentColor, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                        Text(formatDateFromYYYYMMDDToDDMMYYYY(invoice.date) ?: invoice.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("€${String.format("%.2f", invoice.total)}", fontSize = 15.sp, color = totalColor, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Botón de Editar
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Edit, "Editar Factura", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    // Botón de Eliminar
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Delete, "Eliminar Factura", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    }
                    // Botón Pagar existente (o puedes quitarlo si "Editar" lo cubre)
                    if (invoice.status == "Pendiente") {
                        Button(
                            onClick = onToggleStatus,
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Pagar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondary)
                        }
                    } else { // Si está pagada, quizás un botón para marcar como pendiente
                        OutlinedButton(
                            onClick = onToggleStatus,
                            modifier = Modifier.height(32.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Anular Pago", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun CarInfoTab(car: Car, maintenances: List<Maintenance>, expenses: List<ExpenseItem>, invoices: List<Invoice>) {
    // --- Cálculos No Financieros ---
    val lastMaintenance = maintenances.maxByOrNull { it.date }
    val nextServiceLocalDate = parseDate(car.nextServiceDate)
    val daysToNextService = daysUntil(nextServiceLocalDate)
    val avgKmBetweenServices = if (maintenances.size < 2) {
        "N/A (se necesitan al menos 2 mantenimientos)"
    } else {
        val kmIntervals = maintenances.sortedBy { it.km }
            .zipWithNext { a, b -> b.km - a.km }
            .filter { it > 0 }
        if (kmIntervals.isNotEmpty()) {
            String.format("%,d km", kmIntervals.average().toInt())
        } else {
            "N/A"
        }
    }

    // --- Cálculos Financieros ---
    val totalMaintenanceCost = maintenances.sumOf { it.cost }
    val totalOtherExpenses = expenses.sumOf { it.amount }
    val grandTotal = totalMaintenanceCost + totalOtherExpenses

    // --- NUEVO: Cálculos de Facturas ---
    val paidInvoicesCount = invoices.count { it.status == "Pagada" }
    val pendingInvoicesCount = invoices.count { it.status == "Pendiente" }


    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // --- Tarjeta de Resumen Financiero ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Resumen Financiero", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    InfoRowForExpenses("Coste en Mantenimientos:", totalMaintenanceCost)
                    InfoRowForExpenses("Total en Otros Gastos:", totalOtherExpenses)
                    Divider(Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gasto Total:", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Text(
                            "€${String.format("%.2f", grandTotal)}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Resumen de Facturas", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    DetailInfoRow(Icons.Filled.ReceiptLong, "Facturas Totales:", "${invoices.size}")
                    DetailInfoRow(Icons.Filled.CheckCircle, "Pagadas:", "$paidInvoicesCount")
                    DetailInfoRow(Icons.Filled.Error, "Pendientes:", "$pendingInvoicesCount")
                }
            }
        }
        if (lastMaintenance != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Último Mantenimiento", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        DetailInfoRow(Icons.Filled.Event, "Fecha:", formatDateFromYYYYMMDDToDDMMYYYY(lastMaintenance.date) ?: lastMaintenance.date)
                        DetailInfoRow(Icons.Filled.Build, "Descripción:", lastMaintenance.description)
                        DetailInfoRow(Icons.Filled.Speed, "Kilometraje:", "${String.format("%,d", lastMaintenance.km)} km")
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Planificación y Uso", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    DetailInfoRow(
                        Icons.Filled.EventBusy,
                        "Próximo Servicio:",
                        car.nextServiceDate?.let { "$it (${if (daysToNextService != null && daysToNextService >= 0) "en $daysToNextService días" else "revisar fecha"})" } ?: "No programado"
                    )
                    Divider(Modifier.padding(vertical = 8.dp))
                    DetailInfoRow(Icons.Filled.TrendingUp, "Promedio entre servicios:", avgKmBetweenServices)
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkshopDialog(
    workshopToEdit: Workshop, // El taller a editar
    onDismiss: () -> Unit,
    onConfirmEdit: (Workshop) -> Unit // Devuelve el taller actualizado
) {
    // Estados para los campos, inicializados con los datos del taller
    var name by remember { mutableStateOf(workshopToEdit.name) }
    var specialty by remember { mutableStateOf(workshopToEdit.specialty) }
    var phone by remember { mutableStateOf(workshopToEdit.phone) }
    var location by remember { mutableStateOf(workshopToEdit.location) }
    var hourlyRate by remember { mutableStateOf(workshopToEdit.hourlyRate.toString()) }
    var formError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("Editar Taller", style = MaterialTheme.typography.titleLarge) },
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
        confirmButton = {
            Button(onClick = {
                val rateDouble = hourlyRate.toDoubleOrNull()
                if (name.isNotBlank() && specialty.isNotBlank() && phone.isNotBlank() && location.isNotBlank() && rateDouble != null && rateDouble > 0) {
                    val updatedWorkshop = workshopToEdit.copy( // Copia el original para mantener el ID
                        name = name.trim(),
                        specialty = specialty.trim(),
                        phone = phone.trim(),
                        location = location.trim(),
                        hourlyRate = rateDouble
                    )
                    onConfirmEdit(updatedWorkshop)
                    onDismiss()
                } else {
                    formError = "Por favor, rellena todos los campos correctamente."
                }
            }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)) { Text("Guardar Cambios", color = MaterialTheme.colorScheme.onPrimary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = MaterialTheme.colorScheme.secondary) } }
    )
}
@Composable
fun AllMaintenancesTab(maintenances: List<Maintenance>, cars: List<Car>, workshops: List<Workshop>,
                       onDeleteMaintenanceRequest: (String) -> Unit,
                       onEditMaintenanceRequest: (Maintenance) -> Unit) {
    Column {
        Text("Todos los Mantenimientos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
        if (maintenances.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay mantenimientos registrados.", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(maintenances.sortedByDescending { it.date }) { maintenance ->
                    val car = cars.find { it.id == maintenance.carId }
                    val workshop = workshops.find { it.id == maintenance.workshopId }
                    MaintenanceCard(maintenance = maintenance, workshop = workshop, carBrandModel = car?.let { "${it.brand} ${it.model}".trim() } ?: "Vehículo desconocido",
                        onDeleteClick = { onDeleteMaintenanceRequest(maintenance.id) }, // <--- PASAR LA LAMBDA
                        onEditClick = { onEditMaintenanceRequest(maintenance) } // <--- PASAR LA LAMBDA (para después)
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalExpensesTab(maintenances: List<Maintenance>, invoices: List<Invoice>, expenses: List<ExpenseItem>) {
    Column {
        Text("Resumen Financiero Global", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 24.dp))

        val totalMaintenanceCost = maintenances.sumOf { it.cost }
        val totalPaidInvoices = invoices.filter { it.status == "Pagada" }.sumOf { it.total }
        val totalPendingInvoices = invoices.filter { it.status == "Pendiente" }.sumOf { it.total }
        val totalOtherExpenses = expenses.sumOf { it.amount }
        val grandTotalPaid = totalPaidInvoices + totalOtherExpenses

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Mantenimientos", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                InfoRowForExpenses("Coste Total de Mantenimientos:", totalMaintenanceCost)

                Divider(Modifier.padding(vertical = 8.dp))
                Text("Estado de Facturas", style = MaterialTheme.typography.labelSmall)
                InfoRowForExpenses("Pagado:", totalPaidInvoices, isPositive = true)
                InfoRowForExpenses("Pendiente de Pago:", totalPendingInvoices, isWarning = true)
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Otros Gastos Varios (Expenses)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                InfoRowForExpenses("Total de Gastos Varios:", totalOtherExpenses)
            }
        }

        Spacer(Modifier.weight(1f)) // Empuja el total final hacia abajo

        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gasto Real Total (Pagado):", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(
                "€${String.format("%.2f", grandTotalPaid)}",
                style = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun InfoRowForExpenses(label: String, amount: Double, isPositive: Boolean = false, isWarning: Boolean = false) {
    val amountColor = when {
        isPositive && amount > 0 -> MaterialTheme.colorScheme.primary
        isWarning && amount > 0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            "€${String.format("%.2f", amount)}",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = amountColor)
        )
    }
}

@Preview
@Composable
fun AppPreview() {
    var isPreviewingDarkMode by remember { mutableStateOf(false) }

    // --- CAMBIOS PARA LA PREVISUALIZACIÓN ---
    // 1. Creamos un usuario falso solo para que la preview funcione.
    val previewUser = User(id = "preview_user_id", email = "preview@carlog.com")

    CarMaintenanceTheme(isDarkMode = isPreviewingDarkMode) {
        // 2. Pasamos los nuevos parámetros que ahora son obligatorios.
        CarMaintenanceApp(
            currentUser = previewUser,
            onLogout = {}, // Pasamos una función vacía, ya que en la preview no se puede hacer logout.
            isDarkMode = isPreviewingDarkMode,
            onToggleTheme = { isPreviewingDarkMode = !isPreviewingDarkMode }
        )
    }
}