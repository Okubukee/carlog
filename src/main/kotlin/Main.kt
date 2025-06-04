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
                AppHeader(
                    onSettingsClick = { showSettingsDialog = true },
                    onAddWorkshopClick = { showAddWorkshopDialog = true } // <--- PARÁMETRO NECESARIO
                )
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
                            cars = cars + carToAdd
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
                                maintenances = maintenances + maintenanceToAdd // Simulación local

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
                                // InvoiceRepository.addInvoice(newInvoice)
                                println("Simulando InvoiceRepository.addInvoice para mantenimiento ID: $newMaintenanceId")
                                invoices = invoices + newInvoice // Simulación local
                                // --- FIN CREACIÓN DE INVOICE ---

                                // ... (actualizar KM del coche, recargar maintenances, como antes) ...

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
    if (showAddExpenseDialogForCarId != null) {
        // El composable AddExpenseDialog necesita ser definido o completado si es solo un placeholder
        // Asumiré que tienes un AddExpenseDialog similar a los otros.
        // Temporalmente, usaré un AlertDialog simple para ilustrar la lógica.
        // Deberías reemplazar esto con tu AddExpenseDialog real.

        AddExpenseDialog( // Este es tu composable que actualmente es un TODO
            carId = showAddExpenseDialogForCarId!!,
            onDismiss = { showAddExpenseDialogForCarId = null },
            onAddExpense = { expenseItemDataFromDialog -> // expenseItemDataFromDialog no tiene ID aún
                scope.launch {
                    val newExpenseId = System.currentTimeMillis().toString()
                    val expenseToAdd = expenseItemDataFromDialog.copy(id = newExpenseId, carId = showAddExpenseDialogForCarId!!)

                    withContext(Dispatchers.IO) {
                        try {
                            // ExpenseItemRepository.addExpenseItem(expenseToAdd)
                            println("Simulando ExpenseItemRepository.addExpenseItem para: ${expenseToAdd.description}")
                            // expenses = ExpenseItemRepository.getExpenseItemsByCarId(showAddExpenseDialogForCarId!!)
                            // o recargar todos: expenses = ExpenseItemRepository.getAllExpenseItems()
                            expenses = expenses + expenseToAdd // Simulación local
                        } catch (e: Exception) {
                            System.err.println("Error añadiendo gasto: ${e.localizedMessage}")
                        }
                    }
                    showAddExpenseDialogForCarId = null
                }
            }
        )


    }
    if (showEditCarDialogForCarId != null) {
        val carToEdit = cars.find { it.id == showEditCarDialogForCarId } // Obtener de la lista actual
        if (carToEdit != null) {
            EditCarDialog( // Tu EditCarDialog necesitará campos para editar los datos del coche
                car = carToEdit,
                onDismiss = { showEditCarDialogForCarId = null },
                onEditCar = { updatedCar -> // 'updatedCar' vendría del formulario de EditCarDialog
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                // CarRepository.updateCar(updatedCar) // <--- ACTUALIZAR EN BD
                                // cars = CarRepository.getAllCars() // <--- RECARGAR LISTA
                                println("Simulando CarRepository.updateCar y recarga para: ${updatedCar.brand}")
                                // Para probar sin BD completa:
                                cars = cars.map { if (it.id == updatedCar.id) updatedCar else it }
                            } catch (e: Exception) {
                                System.err.println("Error actualizando coche: ${e.localizedMessage}")
                            }
                        }
                        showEditCarDialogForCarId = null
                    }
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

@Composable
fun EditCarDialog(car: Car, onDismiss: () -> Unit, onEditCar: (Car) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = {Text("Editar Vehículo: ${car.brand} ${car.model}".trim())}, text = {Text("Formulario para editar los detalles del vehículo ID: ${car.id} (TODO)")}, confirmButton = {Button(onClick={ onDismiss() }){Text("Guardar Cambios")}}, dismissButton = {TextButton(onClick = onDismiss){Text("Cancelar")}})
}

@Composable
fun AddReminderDialog(carId: String, onDismiss: () -> Unit, onAddReminder: (Reminder) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = {Text("Añadir Nuevo Recordatorio")}, text = {Text("Formulario para añadir un nuevo recordatorio para el coche con ID: $carId (TODO)")}, confirmButton = {Button(onClick={ onDismiss() }){Text("Añadir")}}, dismissButton = {TextButton(onClick = onDismiss){Text("Cancelar")}})
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
fun MaintenanceCard(maintenance: Maintenance, workshop: Workshop?, carBrandModel: String? = null) {
    val isPreventive = maintenance.type == "Preventivo"
    val iconTint = if (isPreventive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val costColor = iconTint
    val badgeBackgroundColor = if (isPreventive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val badgeContentColor = if (isPreventive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer

    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(1.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if(isPreventive) Icons.Filled.Shield else Icons.Filled.ReportProblem,
                    contentDescription = maintenance.type,
                    tint = iconTint,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
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
            Text("€${String.format("%.2f", maintenance.cost)}", fontSize = 15.sp, color = costColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InvoicesTab(maintenances: List<Maintenance>, workshops: List<Workshop>, invoices: List<Invoice>, onToggleInvoiceStatus: (String) -> Unit) {
    Column {
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