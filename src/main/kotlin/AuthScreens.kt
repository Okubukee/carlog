import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import db.repository.User
import db.repository.UserRepository
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onLoginSuccess: (User) -> Unit) {
    var showLogin by remember { mutableStateOf(true) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            if (showLogin) {
                LoginScreen(
                    onLoginSuccess = onLoginSuccess,
                    onSwitchToRegister = { showLogin = false }
                )
            } else {
                RegisterScreen(
                    onRegisterSuccess = { showLogin = true },
                    onSwitchToLogin = { showLogin = true }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (User) -> Unit, onSwitchToRegister: () -> Unit) {
    // --- CAMBIADO: La variable ahora es 'email' ---
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.width(350.dp).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Iniciar Sesión", style = MaterialTheme.typography.headlineMedium)
        // --- CAMBIADO: El campo de texto ahora pide 'Email' ---
        OutlinedTextField(email, { email = it.trim(); error = null }, label = { Text("Email") })
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation()
        )
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    scope.launch {
                        // --- CAMBIADO: Se usan las funciones de repositorio con 'email' ---
                        val user = UserRepository.findUserByEmail(email)
                        if (user != null && UserRepository.checkPassword(email, password)) {
                            onLoginSuccess(user)
                        } else {
                            error = "Email o contraseña incorrectos."
                        }
                    }
                } else {
                    error = "Ambos campos son obligatorios."
                }
            }) {
            Text("Login")
        }
        TextButton(onClick = onSwitchToRegister) {
            Text("¿No tienes cuenta? Regístrate")
        }
    }
}

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onSwitchToLogin: () -> Unit) {
    // --- CAMBIADO: La variable ahora es 'email' ---
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.width(350.dp).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Crear Cuenta", style = MaterialTheme.typography.headlineMedium)
        // --- CAMBIADO: El campo de texto ahora pide 'Email' ---
        OutlinedTextField(email, { email = it.trim(); error = null }, label = { Text("Email") })
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation()
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; error = null },
            label = { Text("Confirmar Contraseña") },
            visualTransformation = PasswordVisualTransformation()
        )
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    error = "El email y la contraseña no pueden estar vacíos."
                    return@Button
                }
                if (!email.contains("@") || !email.contains(".")) { // Validación simple de email
                    error = "Por favor, introduce un email válido."
                    return@Button
                }
                if (password != confirmPassword) {
                    error = "Las contraseñas no coinciden."
                    return@Button
                }
                scope.launch {
                    // --- CAMBIADO: Se usan las funciones de repositorio con 'email' ---
                    val existingUser = UserRepository.findUserByEmail(email)
                    if (existingUser != null) {
                        error = "El email ya está en uso."
                    } else {
                        val newUser = UserRepository.createUser(email, password)
                        if (newUser != null) {
                            onRegisterSuccess()
                        } else {
                            error = "No se pudo crear el usuario. Inténtalo de nuevo."
                        }
                    }
                }
            }) {
            Text("Registrarse")
        }
        TextButton(onClick = onSwitchToLogin) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}