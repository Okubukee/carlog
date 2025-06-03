import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.text.SimpleDateFormat
import java.util.*

// Data Classes
data class Car(
    val id: String,
    val brand: String,
    val model: String,
    val year: Int,
    val plate: String,
    val km: Int
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

fun main() = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Gestión de Mantenimiento de Coches",
        state = windowState
    ) {
        CarMaintenanceTheme {
            CarMaintenanceApp()
        }
    }
}

@Composable
fun CarMaintenanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF1976D2),
            secondary = Color(0xFF388E3C),
            background = Color(0xFFF5F5F5),
            surface = Color.White
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarMaintenanceApp() {
    // Sample Data
    var cars by remember {
        mutableStateOf(
            listOf(
                Car("1", "Toyota", "Corolla", 2020, "ABC-1234", 45000),
                Car("2", "BMW", "320i", 2019, "XYZ-5678", 62000),
                Car("3", "Audi", "A4", 2021, "DEF-9012", 28000)
            )
        )
    }

    var workshops by remember {
        mutableStateOf(
            listOf(
                Workshop("1", "Taller Pérez", "Mecánica General", "666-123-456", "Calle Mayor 123, Madrid", 45.0),
                Workshop("2", "AutoElectric García", "Electricidad del Automóvil", "666-789-012", "Av. Libertad 45, Barcelona", 50.0),
                Workshop("3", "Chapa y Pintura López", "Carrocería y Pintura", "666-345-678", "Polígono Industrial 67, Valencia", 40.0)
            )
        )
    }

    var maintenances by remember {
        mutableStateOf(
            listOf(
                Maintenance("1", "1", "1", "2024-01-15", "Cambio de aceite y filtros", 85.0, "Preventivo", 44500),
                Maintenance("2", "1", "2", "2024-02-20", "Revisión sistema eléctrico", 120.0, "Correctivo", 44800),
                Maintenance("3", "2", "1", "2024-01-10", "Cambio de pastillas de freno", 150.0, "Preventivo", 61500),
                Maintenance("4", "2", "3", "2024-03-05", "Reparación de chapa lateral", 280.0, "Correctivo", 62000),
                Maintenance("5", "3", "1", "2024-02-28", "Revisión general", 95.0, "Preventivo", 27800)
            )
        )
    }

    var invoices by remember {
        mutableStateOf(
            listOf(
                Invoice("1", "1", "2024-01-15", 85.0, "Pagada"),
                Invoice("2", "2", "2024-02-20", 120.0, "Pendiente"),
                Invoice("3", "3", "2024-01-10", 150.0, "Pagada"),
                Invoice("4", "4", "2024-03-05", 280.0, "Pendiente"),
                Invoice("5", "5", "2024-02-28", 95.0, "Pagada")
            )
        )
    }

    var selectedCar by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    // Helper functions
    fun getCarById(id: String) = cars.find { it.id == id }
    fun getWorkshopById(id: String) = workshops.find { it.id == id }
    fun getMaintenancesByCarId(carId: String) = maintenances.filter { it.carId == carId }
    fun getInvoiceByMaintenanceId(maintenanceId: String) = invoices.find { it.maintenanceId == maintenanceId }

    val totalExpenses = maintenances.sumOf { it.cost }
    val pendingInvoices = invoices.count { it.status == "Pendiente" }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (selectedCar != null) {
            CarDetailView(
                car = getCarById(selectedCar!!)!!,
                maintenances = getMaintenancesByCarId(selectedCar!!),
                workshops = workshops,
                invoices = invoices,
                onBack = { selectedCar = null },
                onAddMaintenance = { maintenance ->
                    maintenances = maintenances + maintenance
                    val invoice = Invoice(
                        id = System.currentTimeMillis().toString(),
                        maintenanceId = maintenance.id,
                        date = maintenance.date,
                        total = maintenance.cost,
                        status = "Pendiente"
                    )
                    invoices = invoices + invoice
                },
                onToggleInvoiceStatus = { invoiceId ->
                    invoices = invoices.map { invoice ->
                        if (invoice.id == invoiceId) {
                            invoice.copy(status = if (invoice.status == "Pendiente") "Pagada" else "Pendiente")
                        } else invoice
                    }
                }
            )
        } else {
            MainView(
                cars = cars,
                workshops = workshops,
                totalExpenses = totalExpenses,
                pendingInvoices = pendingInvoices,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onCarSelected = { selectedCar = it },
                onAddCar = { car -> cars = cars + car },
                onAddWorkshop = { workshop -> workshops = workshops + workshop },
                getMaintenancesByCarId = ::getMaintenancesByCarId
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainView(
    cars: List<Car>,
    workshops: List<Workshop>,
    totalExpenses: Double,
    pendingInvoices: Int,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onCarSelected: (String) -> Unit,
    onAddCar: (Car) -> Unit,
    onAddWorkshop: (Workshop) -> Unit,
    getMaintenancesByCarId: (String) -> List<Maintenance>
) {
    var showAddCarDialog by remember { mutableStateOf(false) }
    var showAddWorkshopDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Gestión de Mantenimiento de Coches",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Controla tus vehículos y talleres de confianza",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }

            // Quick actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddCarDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo Coche")
                }
                Button(
                    onClick = { showAddWorkshopDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo Taller")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dashboard Cards - Desktop layout with 4 columns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardCard(
                title = "Total Coches",
                value = cars.size.toString(),
                icon = Icons.Default.DirectionsCar,
                color = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )
            DashboardCard(
                title = "Talleres",
                value = workshops.size.toString(),
                icon = Icons.Default.Build,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            DashboardCard(
                title = "Gastos Totales",
                value = "€${String.format("%.2f", totalExpenses)}",
                icon = Icons.Default.Euro,
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
            DashboardCard(
                title = "Facturas Pendientes",
                value = pendingInvoices.toString(),
                icon = Icons.Default.Receipt,
                color = Color(0xFFF44336),
                modifier = Modifier.weight(1f)
            )
        }

        // Main content area
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column {
                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mis Coches (${cars.size})")
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Talleres (${workshops.size})")
                            }
                        }
                    )
                }

                // Tab content
                Box(modifier = Modifier.padding(24.dp)) {
                    when (selectedTab) {
                        0 -> CarsTab(
                            cars = cars,
                            onCarSelected = onCarSelected,
                            getMaintenancesByCarId = getMaintenancesByCarId
                        )
                        1 -> WorkshopsTab(workshops = workshops)
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddCarDialog) {
        AddCarDialog(
            onDismiss = { showAddCarDialog = false },
            onAddCar = { car ->
                onAddCar(car)
                showAddCarDialog = false
            }
        )
    }

    if (showAddWorkshopDialog) {
        AddWorkshopDialog(
            onDismiss = { showAddWorkshopDialog = false },
            onAddWorkshop = { workshop ->
                onAddWorkshop(workshop)
                showAddWorkshopDialog = false
            }
        )
    }
}

@Composable
fun DashboardCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun CarsTab(
    cars: List<Car>,
    onCarSelected: (String) -> Unit,
    getMaintenancesByCarId: (String) -> List<Maintenance>
) {
    Column {
        Text(
            text = "Mis Vehículos",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Grid layout for desktop
        val chunkedCars = cars.chunked(2)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chunkedCars) { carPair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    carPair.forEach { car ->
                        val carMaintenances = getMaintenancesByCarId(car.id)
                        val carExpenses = carMaintenances.sumOf { it.cost }

                        CarCard(
                            car = car,
                            maintenanceCount = carMaintenances.size,
                            totalExpenses = carExpenses,
                            onClick = { onCarSelected(car.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining space if odd number of cars
                    if (carPair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarCard(
    car: Car,
    maintenanceCount: Int,
    totalExpenses: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${car.brand} ${car.model}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${car.plate} • ${car.year}",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${String.format("%,d", car.km)} km",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "$maintenanceCount servicios",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = "€${String.format("%.2f", totalExpenses)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun WorkshopsTab(workshops: List<Workshop>) {
    Column {
        Text(
            text = "Talleres de Confianza",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(workshops) { workshop ->
                WorkshopCard(workshop = workshop)
            }
        }
    }
}

@Composable
fun WorkshopCard(workshop: Workshop) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workshop.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = workshop.specialty,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = workshop.phone,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = workshop.location,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "€${String.format("%.2f", workshop.hourlyRate)}/hora",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { /* Implementar llamada */ },
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Llamar",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Llamar", fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailView(
    car: Car,
    maintenances: List<Maintenance>,
    workshops: List<Workshop>,
    invoices: List<Invoice>,
    onBack: () -> Unit,
    onAddMaintenance: (Maintenance) -> Unit,
    onToggleInvoiceStatus: (String) -> Unit
) {
    var selectedDetailTab by remember { mutableStateOf(0) }
    var showAddMaintenanceDialog by remember { mutableStateOf(false) }

    val carExpenses = maintenances.sumOf { it.cost }
    val lastServiceDate = maintenances.maxByOrNull { it.date }?.date ?: "N/A"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header with back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Volver")
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text(
                    text = "${car.brand} ${car.model}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${car.plate} • ${car.year} • ${String.format("%,d", car.km)} km",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        }

        // Car stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardCard(
                title = "Mantenimientos",
                value = maintenances.size.toString(),
                icon = Icons.Default.Build,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            DashboardCard(
                title = "Gastos Totales",
                value = "€${String.format("%.2f", carExpenses)}",
                icon = Icons.Default.Euro,
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
            DashboardCard(
                title = "Último Servicio",
                value = if (lastServiceDate != "N/A") lastServiceDate.substring(5) else "N/A",
                icon = Icons.Default.CalendarToday,
                color = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )
        }

        // Main content card
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column {
                // Detail tabs
                TabRow(
                    selectedTabIndex = selectedDetailTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedDetailTab == 0,
                        onClick = { selectedDetailTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Historial de Mantenimiento")
                            }
                        }
                    )
                    Tab(
                        selected = selectedDetailTab == 1,
                        onClick = { selectedDetailTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Facturas")
                            }
                        }
                    )
                }

                Box(modifier = Modifier.padding(24.dp)) {
                    when (selectedDetailTab) {
                        0 -> MaintenanceTab(
                            maintenances = maintenances,
                            workshops = workshops,
                            onAddMaintenance = { showAddMaintenanceDialog = true }
                        )
                        1 -> InvoicesTab(
                            maintenances = maintenances,
                            workshops = workshops,
                            invoices = invoices,
                            onToggleInvoiceStatus = onToggleInvoiceStatus
                        )
                    }
                }
            }
        }
    }

    if (showAddMaintenanceDialog) {
        AddMaintenanceDialog(
            carId = car.id,
            workshops = workshops,
            onDismiss = { showAddMaintenanceDialog = false },
            onAddMaintenance = { maintenance ->
                onAddMaintenance(maintenance)
                showAddMaintenanceDialog = false
            }
        )
    }
}

@Composable
fun MaintenanceTab(
    maintenances: List<Maintenance>,
    workshops: List<Workshop>,
    onAddMaintenance: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Historial de Mantenimiento",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddMaintenance,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nuevo Mantenimiento")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (maintenances.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay mantenimientos registrados",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(maintenances.sortedByDescending { it.date }) { maintenance ->
                    val workshop = workshops.find { it.id == maintenance.workshopId }
                    MaintenanceCard(maintenance = maintenance, workshop = workshop)
                }
            }
        }
    }
}

@Composable
fun MaintenanceCard(
    maintenance: Maintenance,
    workshop: Workshop?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = maintenance.description,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${workshop?.name ?: "Taller desconocido"} • ${String.format("%,d", maintenance.km)} km",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = if (maintenance.type == "Preventivo") MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = maintenance.type,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = maintenance.date,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
            Text(
                text = "€${String.format("%.2f", maintenance.cost)}",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun InvoicesTab(
    maintenances: List<Maintenance>,
    workshops: List<Workshop>,
    invoices: List<Invoice>,
    onToggleInvoiceStatus: (String) -> Unit
) {
    Column {
        Text(
            text = "Facturas",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (maintenances.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay facturas para este vehículo",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(maintenances.sortedByDescending { it.date }) { maintenance ->
                    val invoice = invoices.find { it.maintenanceId == maintenance.id }
                    val workshop = workshops.find { it.id == maintenance.workshopId }
                    if (invoice != null) {
                        InvoiceCard(
                            invoice = invoice,
                            maintenance = maintenance,
                            workshop = workshop,
                            onToggleStatus = { onToggleInvoiceStatus(invoice.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceCard(
    invoice: Invoice,
    maintenance: Maintenance,
    workshop: Workshop?,
    onToggleStatus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = Color(0xFF9C27B0),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Factura #${invoice.id}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${maintenance.description} • ${workshop?.name ?: "Taller desconocido"}",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = if (invoice.status == "Pagada") MaterialTheme.colorScheme.secondary else Color.Red,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = invoice.status,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = invoice.date,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "€${String.format("%.2f", invoice.total)}",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* Ver PDF */ },
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Ver PDF", fontSize = 12.sp)
                    }
                    if (invoice.status == "Pendiente") {
                        Button(
                            onClick = onToggleStatus,
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Marcar Pagada", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// Dialog Components for Desktop
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarDialog(
    onDismiss: () -> Unit,
    onAddCar: (Car) -> Unit
) {
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var km by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Añadir Nuevo Coche",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.width(400.dp)
            ) {
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Marca") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Modelo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Año") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = plate,
                        onValueChange = { plate = it },
                        label = { Text("Matrícula") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = km,
                    onValueChange = { km = it },
                    label = { Text("Kilómetros") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (brand.isNotBlank() && model.isNotBlank() && year.isNotBlank() && plate.isNotBlank()) {
                        val car = Car(
                            id = System.currentTimeMillis().toString(),
                            brand = brand,
                            model = model,
                            year = year.toIntOrNull() ?: 0,
                            plate = plate,
                            km = km.toIntOrNull() ?: 0
                        )
                        onAddCar(car)
                    }
                }
            ) {
                Text("Añadir Coche")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkshopDialog(
    onDismiss: () -> Unit,
    onAddWorkshop: (Workshop) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var hourlyRate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Añadir Nuevo Taller",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.width(400.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Taller") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = { Text("Especialidad") },
                    placeholder = { Text("ej. Mecánica General, Electricidad, Chapa y Pintura") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hourlyRate,
                        onValueChange = { hourlyRate = it },
                        label = { Text("Tarifa/Hora (€)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ubicación") },
                    placeholder = { Text("Dirección completa") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && specialty.isNotBlank() && phone.isNotBlank() && location.isNotBlank()) {
                        val workshop = Workshop(
                            id = System.currentTimeMillis().toString(),
                            name = name,
                            specialty = specialty,
                            phone = phone,
                            location = location,
                            hourlyRate = hourlyRate.toDoubleOrNull() ?: 0.0
                        )
                        onAddWorkshop(workshop)
                    }
                }
            ) {
                Text("Añadir Taller")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceDialog(
    carId: String,
    workshops: List<Workshop>,
    onDismiss: () -> Unit,
    onAddMaintenance: (Maintenance) -> Unit
) {
    var selectedWorkshopId by remember { mutableStateOf(workshops.firstOrNull()?.id ?: "") }
    var date by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Preventivo") }
    var km by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Registrar Mantenimiento",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.width(400.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción del Servicio") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Fecha") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Tipo") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = cost,
                        onValueChange = { cost = it },
                        label = { Text("Coste (€)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = km,
                        onValueChange = { km = it },
                        label = { Text("Kilómetros") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (description.isNotBlank() && date.isNotBlank() && cost.isNotBlank()) {
                        val maintenance = Maintenance(
                            id = System.currentTimeMillis().toString(),
                            carId = carId,
                            workshopId = selectedWorkshopId,
                            date = date,
                            description = description,
                            cost = cost.toDoubleOrNull() ?: 0.0,
                            type = type,
                            km = km.toIntOrNull() ?: 0
                        )
                        onAddMaintenance(maintenance)
                    }
                }
            ) {
                Text("Registrar Mantenimiento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Preview
@Composable
fun AppPreview() {
    CarMaintenanceTheme {
        CarMaintenanceApp()
    }
}