package com.danielrosehill.familypager.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danielrosehill.familypager.data.SettingsRepository
import com.danielrosehill.familypager.network.PushoverApi
import com.danielrosehill.familypager.service.PushoverListenerService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { PushoverApi() }

    var yourName by remember { mutableStateOf(settings.yourName) }
    var spouseName by remember { mutableStateOf(settings.spouseName) }
    var appApiToken by remember { mutableStateOf(settings.appApiToken) }
    var spouseUserKey by remember { mutableStateOf(settings.spouseUserKey) }
    var yourUserKey by remember { mutableStateOf(settings.yourUserKey) }
    var pushoverEmail by remember { mutableStateOf(settings.pushoverEmail) }
    var pushoverPassword by remember { mutableStateOf(settings.pushoverPassword) }
    var receivePages by remember { mutableStateOf(settings.receivePages) }
    var isValidating by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }

    fun save() {
        settings.yourName = yourName.trim()
        settings.spouseName = spouseName.trim()
        settings.appApiToken = appApiToken.trim()
        settings.spouseUserKey = spouseUserKey.trim()
        settings.yourUserKey = yourUserKey.trim()
        settings.pushoverEmail = pushoverEmail.trim()
        settings.pushoverPassword = pushoverPassword
        settings.receivePages = receivePages

        Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()

        // Start or stop listener service based on toggle
        if (receivePages && settings.isReceiveConfigured) {
            startListenerService(context)
        } else {
            stopListenerService(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // --- Names section ---
            Text("People", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp))

            OutlinedTextField(
                value = yourName,
                onValueChange = { yourName = it },
                label = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = spouseName,
                onValueChange = { spouseName = it },
                label = { Text("Spouse's name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Pushover section ---
            Text("Pushover API", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = appApiToken,
                onValueChange = { appApiToken = it },
                label = { Text("App API Token") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
            )

            OutlinedTextField(
                value = spouseUserKey,
                onValueChange = { spouseUserKey = it },
                label = { Text("Spouse's User Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
            )

            // Validate button
            Button(
                onClick = {
                    isValidating = true
                    scope.launch {
                        val result = api.validateUser(appApiToken.trim(), spouseUserKey.trim())
                        isValidating = false
                        Toast.makeText(
                            context,
                            if (result.success) "Configuration valid!" else "Invalid: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                enabled = appApiToken.isNotBlank() && spouseUserKey.isNotBlank() && !isValidating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Test Configuration")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Receive pages section ---
            Text("Receive Pages (Optional)", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Enable to receive pages as Android notifications via Pushover's Open Client API.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Receive pages on this device", modifier = Modifier.weight(1f))
                Switch(
                    checked = receivePages,
                    onCheckedChange = { receivePages = it }
                )
            }

            if (receivePages) {
                OutlinedTextField(
                    value = yourUserKey,
                    onValueChange = { yourUserKey = it },
                    label = { Text("Your User Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pushoverEmail,
                    onValueChange = { pushoverEmail = it },
                    label = { Text("Pushover Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                OutlinedTextField(
                    value = pushoverPassword,
                    onValueChange = { pushoverPassword = it },
                    label = { Text("Pushover Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                val deviceRegistered = settings.sessionSecret.isNotBlank() && settings.deviceId.isNotBlank()

                Button(
                    onClick = {
                        isLoggingIn = true
                        scope.launch {
                            val loginResult = api.login(pushoverEmail.trim(), pushoverPassword)
                            loginResult.onSuccess { login ->
                                if (login.needs2fa) {
                                    Toast.makeText(context, "2FA required — not yet supported", Toast.LENGTH_LONG).show()
                                    isLoggingIn = false
                                    return@launch
                                }
                                settings.sessionSecret = login.secret
                                val deviceName = "familypager_${android.os.Build.MODEL.replace(" ", "_").take(20)}"
                                    .lowercase().replace(Regex("[^a-z0-9_-]"), "")
                                val regResult = api.registerDevice(login.secret, deviceName)
                                regResult.onSuccess { deviceId ->
                                    settings.deviceId = deviceId
                                    settings.pushoverEmail = pushoverEmail.trim()
                                    settings.pushoverPassword = pushoverPassword
                                    Toast.makeText(context, "Device registered!", Toast.LENGTH_SHORT).show()
                                    startListenerService(context)
                                }.onFailure { e ->
                                    Toast.makeText(context, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                                isLoggingIn = false
                            }.onFailure { e ->
                                Toast.makeText(context, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                                isLoggingIn = false
                            }
                        }
                    },
                    enabled = pushoverEmail.isNotBlank() && pushoverPassword.isNotBlank() && !isLoggingIn,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoggingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (deviceRegistered) "Re-register Device" else "Login & Register Device")
                }

                if (deviceRegistered) {
                    Text(
                        "Device registered and listening.",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Save button
            Button(
                onClick = {
                    save()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                enabled = yourName.isNotBlank() && spouseName.isNotBlank() &&
                        appApiToken.isNotBlank() && spouseUserKey.isNotBlank()
            ) {
                Text("Save Settings", fontSize = 16.sp)
            }
        }
    }
}

private fun startListenerService(context: Context) {
    val intent = Intent(context, PushoverListenerService::class.java)
    context.startForegroundService(intent)
}

private fun stopListenerService(context: Context) {
    val intent = Intent(context, PushoverListenerService::class.java)
    context.stopService(intent)
}
