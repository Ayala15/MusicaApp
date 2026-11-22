package com.example.musicapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.R
import com.example.musicapp.data.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun AuthScreen() {
    val scope = rememberCoroutineScope()
    val savedCredentials = remember { AuthRepository.getSavedCredentials() }

    var registering by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf(savedCredentials?.username ?: "") }
    var email by rememberSaveable { mutableStateOf(savedCredentials?.email ?: "") }
    var password by rememberSaveable { mutableStateOf(savedCredentials?.password ?: "") }
    var rememberMe by rememberSaveable { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val dark = LocalDark.current

    // Mostrar tarjeta de cuenta guardada si hay datos guardados
    var showQuickAccount by remember { mutableStateOf(savedCredentials != null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Cream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { ThemeSettings.set(!dark) }) {
                Icon(
                    if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Cambiar entre modo claro y oscuro",
                    tint = Palette.Ink
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Image(
            painter = painterResource(R.drawable.ic_launcher),
            contentDescription = "Logo",
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            if (registering) "Crea tu cuenta" else "Bienvenido",
            fontFamily = SerifFont,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = Palette.Ink
        )
        Text(
            if (registering) "Guarda tus canciones favoritas" else "Inicia sesión para escuchar",
            color = Palette.Muted,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(28.dp))

        if (showQuickAccount && savedCredentials != null && !registering) {
            // Tarjeta de inicio de sesión rápido
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Palette.Muted.copy(alpha = 0.1f))
                    .border(1.dp, Palette.Ink.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Cuenta guardada",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Palette.Muted
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Palette.Ink),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            savedCredentials.username.take(1).uppercase(),
                            color = Palette.Cream,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "@${savedCredentials.username}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Palette.Ink
                        )
                        if (savedCredentials.email.isNotEmpty()) {
                            Text(
                                savedCredentials.email,
                                fontSize = 13.sp,
                                color = Palette.Muted
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                Button(
                    enabled = !isLoading,
                    onClick = {
                        scope.launch {
                            isLoading = true
                            error = AuthRepository.login(savedCredentials.identifier, savedCredentials.password, true)
                            isLoading = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Palette.Ink,
                        contentColor = Palette.Cream
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Palette.Cream, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Continuar como @${savedCredentials.username}", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = {
                    showQuickAccount = false
                }) {
                    Text("Usar otra cuenta o cambiar contraseña", fontSize = 13.sp, color = Palette.Ink)
                }
            }
        } else {
            // Formulario normal de Login / Registro
            if (registering) {
                // Registro: Usuario, Correo y Contraseña
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Palette.Muted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = minimalFieldColors()
                )
                Spacer(Modifier.height(8.dp))

                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Palette.Muted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    colors = minimalFieldColors()
                )
                Spacer(Modifier.height(8.dp))
            } else {
                // Login: Usuario o Correo
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario o Correo") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Palette.Muted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = minimalFieldColors()
                )
                Spacer(Modifier.height(8.dp))
            }

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Palette.Muted) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = minimalFieldColors()
            )

            if (!registering) {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { rememberMe = !rememberMe }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Palette.Ink,
                            checkmarkColor = Palette.Cream
                        )
                    )
                    Text("Recordar mi usuario y contraseña", fontSize = 13.sp, color = Palette.Ink)
                }
            }
        }

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Palette.Accent, fontSize = 14.sp, textAlign = TextAlign.Center)
        }

        if (!showQuickAccount || registering) {
            Spacer(Modifier.height(24.dp))
            Button(
                enabled = !isLoading,
                onClick = {
                    scope.launch {
                        isLoading = true
                        error = if (registering) {
                            AuthRepository.register(username, email, password)
                        } else {
                            AuthRepository.login(username, password, rememberMe)
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Palette.Ink,
                    contentColor = Palette.Cream
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Palette.Cream, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (registering) "Crear cuenta" else "Entrar", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = {
            registering = !registering
            showQuickAccount = false
            error = null
        }) {
            Text(
                if (registering) "¿Ya tienes cuenta? Inicia sesión" else "¿Primera vez? Crea tu cuenta",
                color = Palette.Ink
            )
        }

        if (savedCredentials != null && !showQuickAccount && !registering) {
            TextButton(onClick = {
                showQuickAccount = true
                error = null
            }) {
                Text("Volver a la cuenta guardada (@${savedCredentials.username})", color = Palette.Muted, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Tu cuenta se guarda solo en este dispositivo.",
            color = Palette.Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        // Botón de información del grupo desarrollador y proyecto
        OutlinedButton(
            onClick = { showInfoDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Palette.Ink
            )
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = Palette.Ink,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Información del Grupo Desarrollador", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Cerrar", color = Palette.Accent, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Palette.Card,
            titleContentColor = Palette.Ink,
            textContentColor = Palette.Ink,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Palette.Accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Acerca del Proyecto", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Tema de la App:",
                        fontWeight = FontWeight.Bold,
                        color = Palette.Ink,
                        fontSize = 14.sp
                    )
                    Text(
                        "Reproducción y catálogo de música en streaming con Jetpack Compose y ExoPlayer.",
                        color = Palette.Muted,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Tecnologías usadas:",
                        fontWeight = FontWeight.Bold,
                        color = Palette.Ink,
                        fontSize = 14.sp
                    )
                    Text(
                        "• Kotlin + Jetpack Compose (UI)\n• Media3 ExoPlayer (Streaming Audio)\n• Coroutines & StateFlow (Asincronía)\n• Retrofit & Gson (Consumo API Deezer)\n• SharedPreferences (Persistencia)",
                        color = Palette.Muted,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Grupo desarrollador:",
                        fontWeight = FontWeight.Bold,
                        color = Palette.Ink,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    val integrantes = listOf(
                        "Gerardo David Ayala Serpas",
                        "Anderson Alberto Baños Escobar",
                        "Jason Ernesto Benítez López",
                        "Ángel Esturdo Campos Natividad",
                        "Raúl Josué Campos Rodríguez"
                    )
                    integrantes.forEach { integrante ->
                        Text(
                            "• $integrante",
                            color = Palette.Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        )
    }
}
