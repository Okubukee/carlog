// COMIENZO DEL CÓDIGO Main.kt COMPLETO
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
    val workshopId: String?,
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
data class IconOption(val name: String, val icon: ImageVector)

val expenseIconOptions = listOf(
    IconOption("Combustible", Icons.Filled.LocalGasStation),
    IconOption("Seguro", Icons.Filled.Shield),
    IconOption("Lavado", Icons.Filled.Wash),
    IconOption("Mantenimiento/Reparación", Icons.Filled.Build),
    IconOption("Peaje/Parking", Icons.Filled.CreditCard),
    IconOption("Impuestos/Otros", Icons.Filled.Receipt),
    IconOption("Compra Accesorio", Icons.Filled.ShoppingCart)
    // Puedes añadir más opciones aquí si lo necesitas
)

fun main() = application {
    // Variable para rastrear si la BD se inicializó correctamente
    var databaseInitializedSuccessfully = false

    // 1. Intentar inicializar la base de datos
    try {
        // Aquí es donde llamas a la inicialización de tu base de datos.
        // Asegúrate de que DatabaseManager y su método init() estén definidos
        // y que DatabaseManager esté importado correctamente.
        DatabaseManager.init() // <--- PUNTO CLAVE DE INTEGRACIÓN
        databaseInitializedSuccessfully = true
        println("DatabaseManager.init() llamado y completado exitosamente desde main().")
    } catch (e: Exception) {
        // Si init() lanza una excepción, se captura aquí.
        System.err.println("FALLO CRÍTICO AL INICIALIZAR LA BASE DE DATOS desde main(): ${e.message}")
        e.printStackTrace() // Imprime el stack trace completo para ver detalles del error.
        // databaseInitializedSuccessfully permanece false
    }

    // 2. Solo mostrar la UI principal si la base de datos se inicializó bien
    if (databaseInitializedSuccessfully) {
        // Tu código existente para el tema y la ventana principal de la aplicación
        var isDarkMode by remember { mutableStateOf(false) } // State for theme

        val windowState = rememberWindowState(width = 1450.dp, height = 950.dp)
        Window(
            onCloseRequest = ::exitApplication,
            title = "AutoTracker v1.3",
            state = windowState
        ) {
            CarMaintenanceTheme(isDarkMode = isDarkMode) { // Pass theme state
                CarMaintenanceApp(
                    isDarkMode = isDarkMode, // Pass current theme mode
                    onToggleTheme = { isDarkMode = !isDarkMode } // Pass lambda to toggle theme
                )
            }
        }
    } else {
        // Si la inicialización de la BD falló, muestra una ventana de error simple.
        // Esto evita que la aplicación intente funcionar sin una base de datos funcional,
        // lo que probablemente causaría más errores.
        Window(
            onCloseRequest = ::exitApplication,
            title = "Error de Aplicación - AutoTracker v1.3",
            state = rememberWindowState(width = 600.dp, height = 300.dp) // Un tamaño menor para el error
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
                    color = Color.Red // O usa MaterialTheme.colorScheme.error
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
    // M3 also uses container colors, ensure they are well-defined or derived
    primaryContainer = Color(0xFF303030), // Example, adjust as needed
    onPrimaryContainer = Color.White,      // Example
    secondaryContainer = Color(0xFFD0D0D0),// Example
    onSecondaryContainer = Color.Black,    // Example
    tertiary = Color(0xFF00695C),          // Example for "Pending" status - Teal
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB2DFDB), // Example
    onTertiaryContainer = Color(0xFF004D40),// Example
    errorContainer = Color(0xFFFDECEA),    // Example
    onErrorContainer = Color(0xFFB71C1C)   // Example
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

    surfaceVariant = Color(0xFF2C2C2C), // For elements like TabRow background
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
fun CarMaintenanceApp(isDarkMode: Boolean, onToggleTheme: () -> Unit) {
    // 1. Obtener un CoroutineScope para lanzar operaciones de BD
    val scope = rememberCoroutineScope()

    // 2. Inicializar el estado de 'cars' como una lista vacía.
    //    Ya no se usan los datos de ejemplo directamente aquí para 'cars'.
    var cars by remember { mutableStateOf<List<Car>>(emptyList()) }
    var workshops by remember { mutableStateOf<List<Workshop>>(emptyList()) }
    var maintenances by remember { mutableStateOf<List<Maintenance>>(emptyList()) }
    var invoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var expenses by remember { mutableStateOf<List<ExpenseItem>>(emptyList()) }
    var reminders by remember { mutableStateOf<List<Reminder>>(emptyList()) }

   // var workshops by remember { mutableStateOf(workshopsSampleData) }
   // var maintenances by remember { mutableStateOf(maintenancesSampleData) }

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
                            cars = CarRepository.getAllCars() // Recargar coches
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
                        // También podrías necesitar actualizar facturas asociadas si la lógica de cascada no lo hace
                        // o si quieres eliminar facturas manualmente aquí (aunque onDelete = ReferenceOption.CASCADE en InvoicesTable debería manejarlo)
                        invoices = InvoiceRepository.getAllInvoices() // Recargar facturas por si acaso
                    } catch (e: Exception) {
                        System.err.println("Error eliminando mantenimiento: ${e.localizedMessage}")
                        // Mostrar error en UI
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
                        cars = CarRepository.getAllCars() // <--- RECARGAR LISTA DE COCHES

                        // Si el coche eliminado era el seleccionado, deselecciónalo
                        if (selectedCarId == id) {
                            selectedCarId = null
                        }
                    } catch (e: Exception) {
                        System.err.println("Error eliminando coche: ${e.localizedMessage}")
                        // Aquí podrías mostrar un mensaje de error en la UI
                    }
                }
                carIdToDelete = null // Limpiar el ID
            }
        }
    }

    // 3. Cargar los coches desde la base de datos al iniciar CarMaintenanceApp
    LaunchedEffect(Unit) { // El 'Unit' hace que se ejecute solo una vez al inicio
        scope.launch {
            withContext(Dispatchers.IO) { // Ejecutar la llamada de BD en un hilo de IO
                try {
                    workshops = WorkshopRepository.getAllWorkshops()
                    cars = CarRepository.getAllCars() // <--- LLAMADA AL REPOSITORIO
                    maintenances = MaintenanceRepository.getAllMaintenances()
                    invoices = InvoiceRepository.getAllInvoices()
                    expenses = ExpenseItemRepository.getAllExpenseItems()
                    reminders = ReminderRepository.getAllReminders()
                    println("Facturas cargadas desde la BD. Total: ${invoices.size}")
                    println("Simulando carga de coches desde CarRepository.getAllCars()") // Comenta/descomenta
                    // Para probar sin BD completa:
                    //cars = listOf(
                  //       Car("1_db", "Seat León DB", "ST 1.5 TSI", 2019, "1234 JKL", 45000, "https://placehold.co/800x400/E1E1E1/31343C?text=Seat+Le%C3%B3n&font=raleway", "15/07/2025", "Blanco Nieve", "Manual", "Gasolina", "10/01/2019")
                   //  )
                } catch (e: Exception) {
                    System.err.println("Error cargando coches: ${e.localizedMessage}")
                    // Aquí podrías mostrar un mensaje de error en la UI
                }
            }

            // --- INICIO DE CARGA PARA WORKSHOPS Y MAINTENANCES ---
            // Cargar Talleres
            withContext(Dispatchers.IO) {
                try {
                    workshops = WorkshopRepository.getAllWorkshops() // <--- LLAMADA AL REPOSITORIO
                    println("Simulando carga de talleres desde WorkshopRepository.getAllWorkshops()")
                    // Datos de prueba temporales para workshops:
                    //workshops = listOf(
                      //  Workshop("w1_db", "Taller Veloz DB", "Mecánica General", "555-0101", "Calle Falsa 123", 55.0),
                        //Workshop("w2_db", "Boxes Auto DB", "Chapa y Pintura", "555-0202", "Avenida Siempreviva 742", 70.0)
                    //)
                } catch (e: Exception) { System.err.println("Error cargando talleres: ${e.localizedMessage}") }
            }

            // Cargar Mantenimientos (puedes cargar todos o solo los del coche seleccionado si aplica al inicio)
            // Por ahora, cargaremos todos como ejemplo.
            withContext(Dispatchers.IO) {
                try {
                    maintenances = MaintenanceRepository.getAllMaintenances() // <--- LLAMADA AL REPOSITORIO
                    println("Simulando carga de mantenimientos desde MaintenanceRepository.getAllMaintenances()")
                  //  maintenances = listOf( // Datos de prueba temporales para maintenances
                  //      Maintenance("m1_db", "1_db", "w1_db", "2024-04-15", "Revisión Completa DB", 150.0, "Preventivo", 50000),
                   //     Maintenance("m2_db", "1_db", "w2_db", "2024-05-20", "Cambio pastillas freno DB", 90.0, "Correctivo", 52000)
                  //  )
                } catch (e: Exception) { System.err.println("Error cargando mantenimientos: ${e.localizedMessage}") }
            }

        }
    }
    val upcomingTotalMaintenancesCount = maintenances.count { maintenance ->
        // Asumiendo que Maintenance.date está en formato "YYYY-MM-DD" como se definió
        // para facilitar la ordenación y el parseo.
        // Si está en "DD/MM/YYYY", tu función parseDate también debería manejarlo para esta lógica.
        // Aquí usaré parseDate asumiendo que puede manejar el formato de Maintenance.date.
        parseDate(maintenance.date)?.let { serviceDate -> // parseDate es tu función de ayuda
            !serviceDate.isBefore(LocalDate.now()) // Que no sea anterior a hoy
        } ?: false // Si no hay fecha o no se puede parsear, no cuenta
    }
    println("Total de tareas de mantenimiento próximas calculadas: $upcomingTotalMaintenancesCount")



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
                    onShowAddReminderDialog = { showAddReminderDialogForCarId = car.id },
                    onDeleteCarClick = { handleDeleteCarRequest(car.id) },
                    onDeleteMaintenanceRequest = { maintenanceId -> handleDeleteMaintenanceRequest(maintenanceId) }, // <--- PASAR NUEVA LAMBDA
                    onEditMaintenanceRequest = { maintenance -> handleEditMaintenanceRequest(maintenance) }, // <--- PASAR NUEVA LAMBDA (para el siguiente paso)onDeleteInvoiceRequest = { invoiceId -> handleDeleteInvoiceRequest(invoiceId) }, // <--- PASAR NUEVA LAMBDA
                    onEditInvoiceRequest = { invoice -> handleEditInvoiceRequest(invoice) },         // <--- PASAR NUEVA LAMBDA
                    onToggleInvoiceStatus = { invoiceId, currentStatus -> toggleInvoiceStatus(invoiceId, currentStatus) }
                )
            } else { selectedCarId = null }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                AppHeader(
                    onSettingsClick = { showSettingsDialog = true },
                    onAddWorkshopClick = { showAddWorkshopDialog = true } // <--- PARÁMETRO NECESARIO
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
                        2 -> GlobalExpensesTab(maintenances, expenses, cars)
                        3 -> AllWorkshopsTab(workshops, onAddWorkshopClick = { showAddWorkshopDialog = true })
                    }
                }
            }
        }
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
                            CarRepository.addCar(carToAdd) // <--- AÑADIR A LA BD
                            cars = CarRepository.getAllCars() // <--- RECARGAR LISTA DE COCHES
                            println("Simulando CarRepository.addCar y recarga de coches para: ${carToAdd.brand}")
                            // Para probar sin BD completa:

                        } catch (e: Exception) {
                            System.err.println("Error añadiendo coche: ${e.localizedMessage}")
                            // Aquí podrías mostrar un mensaje de error en la UI del diálogo
                        }
                    }
                    showAddCarDialog = false
                }
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
                    // 'maintenanceDataFromDialog' es el objeto Maintenance con los datos del formulario
                    // pero sin un 'id' de base de datos todavía.
                    scope.launch { // Iniciar una coroutine para la operación de BD

                        println("Dentro de onAddMaintenance: car = $car, maintenanceData = $maintenanceDataFromDialog") // Log

                        // Si la línea 537 involucra 'car', por ejemplo:
                        // if (maintenanceToAdd.km > car.km) { ... }
                        // O
                        // val updatedCar = car.copy(km = maintenanceToAdd.km)
                        // --- LOGS DE DEPURACIÓN URGENTES ---
                        println("INICIO COROUTINA onAddMaintenance:")
                        println("   - car (capturado del ámbito exterior): $car") // 'car' es el objeto de la función externa
                        println("   - maintenanceDataFromDialog (del diálogo): $maintenanceDataFromDialog")
                        // --- FIN LOGS ---
                        // AÑADE UNA COMPROBACIÓN DE NULIDAD PARA 'car' AQUÍ SI LA LÍNEA 537 LO USA DIRECTAMENTE
                        if (car == null) {
                            System.err.println("ERROR CRÍTICO: El objeto 'car' es null dentro de la coroutina onAddMaintenance.")
                            // Podrías retornar o manejar el error para evitar el NPE
                            return@launch // Salir de la coroutina
                        }

                        val newMaintenanceId = System.currentTimeMillis().toString() // O un UUID
                        val maintenanceToAdd = maintenanceDataFromDialog.copy(id = newMaintenanceId)

                        withContext(Dispatchers.IO) { // Ejecutar en un hilo de E/S
                            try {
                                MaintenanceRepository.addMaintenance(maintenanceToAdd)
                                println("Simulando MaintenanceRepository.addMaintenance para: ${maintenanceToAdd.description}")
                                // Simulación local
                                maintenances = MaintenanceRepository.getMaintenancesByCarId(maintenanceToAdd.carId) // Recargar mantenimientos

                                if (maintenanceToAdd.km > car.km) { // <- ¿Podría ser esta la línea 537?
                                    val updatedCar = car.copy(km = maintenanceToAdd.km) // <- ¿O esta?
                                    // ... (actualizar coche en repositorio)
                                    println("Simulando CarRepository.updateCar KM para ${updatedCar.brand} a ${updatedCar.km}km")
                                    cars = cars.map { if (it.id == updatedCar.id) updatedCar else it }
                                }
                                // --- AÑADIR CREACIÓN DE INVOICE ---
                                val newInvoice = Invoice(
                                    id = "inv_${newMaintenanceId}", // ID único para la factura
                                    maintenanceId = newMaintenanceId,
                                    date = maintenanceToAdd.date, // O la fecha actual si prefieres
                                    total = maintenanceToAdd.cost,
                                    status = "Pendiente" // Estado inicial por defecto
                                )
                                InvoiceRepository.addInvoice(newInvoice)
                                println("Simulando InvoiceRepository.addInvoice para mantenimiento ID: $newMaintenanceId")
                                invoices = InvoiceRepository.getAllInvoices()
                                // Simulación local
                                // --- FIN CREACIÓN DE INVOICE ---

                                // ... (actualizar KM del coche, recargar maintenances, como antes) ...
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
            // Considera resetear showAddMaintenanceDialogForCarId aquí para evitar estados extraños
            // showAddMaintenanceDialogForCarId = null
        }
    }
// --- DIÁLOGO DE CONFIRMACIÓN DE ELIMINACIÓN DE FACTURA ---
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

    // --- DIÁLOGO DE EDICIÓN DE FACTURA ---
    if (showEditInvoiceDialog && invoiceToEdit != null) {
        EditInvoiceDialog(
            invoice = invoiceToEdit!!,
            onDismiss = { showEditInvoiceDialog = false; invoiceToEdit = null },
            onConfirmEdit = { updatedStatus ->
                confirmEditInvoice(invoiceToEdit!!.id, updatedStatus)
                // El cierre del diálogo y limpieza se hace en confirmEditInvoice
            }
        )
    }
    if (showAddWorkshopDialog) {
        AddWorkshopDialog(
            onDismiss = { showAddWorkshopDialog = false },
            onAddWorkshop = { workshopDataFromDialog ->
                // 'workshopDataFromDialog' es el objeto Workshop con los datos del formulario.
                scope.launch {
                    val newWorkshopId = System.currentTimeMillis().toString() // O un UUID
                    val workshopToAdd = workshopDataFromDialog.copy(id = newWorkshopId)

                    withContext(Dispatchers.IO) { // Operación de BD en hilo de IO
                        try {
                            // 1. ESTA ES LA LÍNEA QUE INSERTA EN LA BASE DE DATOS
                            WorkshopRepository.addWorkshop(workshopToAdd) // <--- ASEGÚRATE DE QUE ESTÉ DESCOMENTADA
                            println("Taller '${workshopToAdd.name}' insertado en la BD.")

                            // 2. DESPUÉS DE INSERTAR, RECARGA LA LISTA DE TALLERES DESDE LA BD
                            workshops = WorkshopRepository.getAllWorkshops() // <--- ASEGÚRATE DE QUE ESTÉ DESCOMENTADA
                            println("Lista de talleres recargada desde la BD. Total: ${workshops.size}")

                        } catch (e: Exception) {
                            System.err.println("Error añadiendo taller a la BD: ${e.localizedMessage}")
                            e.printStackTrace()
                            // Aquí podrías querer informar al usuario del error a través de la UI.
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
                // El cierre del diálogo y limpieza de maintenanceToEdit se hace en confirmEditMaintenance
            }
        )
    }
    if (carIdForDialog != null) {
        AddExpenseDialog( // Asegúrate de que tu AddExpenseDialog esté completamente implementado
            carId = carIdForDialog, // Pasa el valor capturado
            onDismiss = { showAddExpenseDialogForCarId = null },
            onAddExpense = { expenseItemDataFromDialog -> // expenseItemDataFromDialog debe ser un ExpenseItem (sin id)
                scope.launch {
                    // 'carIdForDialog' aquí fue capturado cuando no era null
                    // y no la variable de estado 'showAddExpenseDialogForCarId' directamente
                    // porque esta última podría haber cambiado.
                    // La siguiente comprobación es una doble seguridad, aunque si entramos al if (carIdForDialog != null)
                    // y la lambda se ejecuta inmediatamente después, no debería ser null.
                    // Sin embargo, si hay un delay o un cambio de estado concurrente, es una buena práctica.
                    if (carIdForDialog == null) { // Doble chequeo, aunque no debería ser estrictamente necesario aquí.
                        System.err.println("Error: carIdForDialog se volvió null DENTRO de la coroutina, esto es inesperado.")
                        return@launch
                    }

                    val newExpenseId = System.currentTimeMillis().toString()
                    // Asumiendo que expenseItemDataFromDialog es un ExpenseItem (sin id, pero con carId ya igual a carIdForDialog)
                    // que proviene de un AddExpenseDialog bien implementado.
                    val expenseToAdd = expenseItemDataFromDialog.copy(id = newExpenseId)
                    // Si expenseItemDataFromDialog NO tiene carId, entonces necesitarías:
                    // val expenseToAdd = expenseItemDataFromDialog.copy(id = newExpenseId, carId = carIdForDialog)


                    println("--- DEBUG onAddExpense ---")
                    println("carIdForDialog: $carIdForDialog")
                    println("expenseItemDataFromDialog: $expenseItemDataFromDialog")
                    println("expenseToAdd: $expenseToAdd")


                    withContext(Dispatchers.IO) {
                        try {
                            // Descomenta para la interacción real con la base de datos:
                            ExpenseItemRepository.addExpenseItem(expenseToAdd)
                            println("Simulando ExpenseItemRepository.addExpenseItem para: ${expenseToAdd.description}")

                            // Descomenta para recargar desde la base de datos:
                            // expenses = ExpenseItemRepository.getExpenseItemsByCarId(carIdForDialog)
                            expenses = ExpenseItemRepository.getAllExpenseItems()
                            // Simulación local:

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
                                cars = CarRepository.getAllCars() // <--- DESCOMENTA ESTO
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
        // El coche no se encontró en la lista, algo fue mal (ej. ID incorrecto o lista no actualizada)
        // Simplemente cierra el diálogo en este caso o muestra un error.
        println("Error: No se encontró el coche con ID $showEditCarDialogForCarId para editar.")
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
                // 'reminderFromDialog' viene del diálogo, aún sin ID de BD.
                // 'carId' ya está en reminderFromDialog.
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

                            // Recargar recordatorios para el coche actual o todos
                            // reminders = ReminderRepository.getRemindersByCarId(carIdForReminder)
                            // o si manejas una lista global de todos los recordatorios:
                            reminders = ReminderRepository.getAllReminders()
                            println("Simulando recarga de recordatorios.")
                            // Simulación local:


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
            onToggleTheme = onToggleTheme
        )
    }
}

// --- COMPOSABLES DE UI ---
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
            Text("AutoTracker", style = MaterialTheme.typography.headlineSmall)
            Row(verticalAlignment = Alignment.CenterVertically) { // Fila para agrupar los botones de acción
                // --- VERIFICA ESTA SECCIÓN CUIDADOSAMENTE ---
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
                // --- FIN DE LA SECCIÓN A VERIFICAR ---
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
                        // CORRECTED: Set to the new state directly
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
                        // CORRECTED: Set to the new state directly
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
                    // El ID se generará antes de insertar en la BD en CarMaintenanceApp
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

    // Estas listas deben estar accesibles (pueden ser las mismas que usa AddCarDialog)
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
                            // La matrícula y el ID del coche generalmente no se editan, pero aquí permitimos editar la matrícula
                            // Si el ID es la matrícula, considera si esto debe ser editable.
                            // El ID del objeto Car (car.id) no lo estamos cambiando.
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
fun SettingsDialog(isDarkMode: Boolean, onDismiss: () -> Unit, onToggleTheme: () -> Unit) {
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
    onShowAddReminderDialog: () -> Unit,       // <-- Ensure this line is present and correct
    onDeleteCarClick: () -> Unit = {},
    onDeleteMaintenanceRequest: (String) -> Unit = {}, // <-- Ensure this line is present
    onEditMaintenanceRequest: (Maintenance) -> Unit = {},
    onDeleteInvoiceRequest: (String) -> Unit = {},     // <-- NUEVO PARÁMETRO
    onEditInvoiceRequest: (Invoice) -> Unit = {},       // <-- NUEVO PARÁMETRO
    onToggleInvoiceStatus: (invoiceId: String, currentStatus: String) -> Unit = { _, _ -> } // <-- PARÁMETRO EXISTENTE

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
                        0 -> Text("Pestaña de Información General Adicional (TODO)", style = MaterialTheme.typography.bodyLarge)
                        1 -> MaintenanceListForDetail(
                            maintenances = maintenances, // 'maintenances' es un parámetro de CarDetailView
                            workshops = workshops,       // 'workshops' es un parámetro de CarDetailView
                            onDeleteMaintenanceRequest = onDeleteMaintenanceRequest, // <--- PASAR LA LAMBDA
                            onEditMaintenanceRequest = onEditMaintenanceRequest    // <--- PASAR LA LAMBDA (para después)
                        )
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
                // Validar que los KM no sean menores que los del coche, excepto para ITV o si se está editando un mantenimiento anterior a los KM actuales del coche.
                // Esta validación puede ser compleja aquí, ya que no tenemos los KM actuales del coche directamente a menos que se pasen.
                // Por ahora, una validación simple:
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
                        // carId no cambia al editar un mantenimiento
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
fun ExpensesListForDetail(expenses: List<ExpenseItem>) {
    Column {
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
@Composable
fun ReminderItemRow(reminder: Reminder ,  onEditClick: () -> Unit = {},
                    onDeleteClick: () -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
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
                IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Edit, "Editar Taller", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
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
    // Placeholder for edit/delete dialogs or navigation
    onEditWorkshopClick: (Workshop) -> Unit = {},
    onDeleteWorkshopConfirm: (Workshop) -> Unit = {}
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
                        onDeleteClick = { onDeleteWorkshopConfirm(workshop) } // Implement actual confirmation later
                    )
                }
            }
        }
    }
}


// --- Pestañas Globales y Tarjetas de Items ---
// Removed global color vars: preventiveColorBW, correctiveColorBW, paidColorBW, pendingColorBW

@Composable
fun MaintenanceTab(maintenances: List<Maintenance>, workshops: List<Workshop>, onAddMaintenance: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
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
    onEditInvoiceRequest: (Invoice) -> Unit,    // <--- NUEVO PARÁMETRO
    onDeleteInvoiceRequest: (String) -> Unit  // <--- NUEVO PARÁMETRO
) {
    Column {
        val carMaintenanceIds = maintenances.map { it.id }.toSet()
        // Filtra las facturas que pertenecen a los mantenimientos del coche actual
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
fun GlobalExpensesTab(maintenances: List<Maintenance>, expenses: List<ExpenseItem>, cars: List<Car>) {
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
    // Corrected variable declaration
    var isPreviewingDarkMode by remember { mutableStateOf(false) }
    CarMaintenanceTheme(isDarkMode = isPreviewingDarkMode) {
        CarMaintenanceApp(
            isDarkMode = isPreviewingDarkMode,
            onToggleTheme = { isPreviewingDarkMode = !isPreviewingDarkMode }
        )
    }
}
// FIN DEL CÓDIGO Main.kt COMPLETO