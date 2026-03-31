package com.danielrosehill.familypager.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danielrosehill.familypager.data.SettingsRepository
import com.danielrosehill.familypager.network.PushoverApi
import com.danielrosehill.familypager.ui.theme.EmergencyRed
import com.danielrosehill.familypager.ui.theme.EmergencyRedDark
import com.danielrosehill.familypager.ui.theme.UrgentAmber
import com.danielrosehill.familypager.ui.theme.UrgentAmberDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagerScreen(
    settings: SettingsRepository,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { PushoverApi() }
    var isSending by remember { mutableStateOf(false) }

    fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Vibrator::class.java)
            v?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun sendPage(isEmergency: Boolean) {
        if (!settings.isConfigured) {
            Toast.makeText(context, "Please configure settings first", Toast.LENGTH_LONG).show()
            onNavigateToSettings()
            return
        }
        if (isSending) return

        vibrate()
        isSending = true

        val message = if (isEmergency) {
            "Family Pager: ${settings.yourName} — EMERGENCY / SOS!"
        } else {
            "Family Pager: ${settings.yourName} — Call me ASAP"
        }

        scope.launch {
            val result = api.sendMessage(
                token = settings.appApiToken,
                userKey = settings.spouseUserKey,
                message = message,
                title = "Family Pager",
                priority = if (isEmergency) 2 else 1,
                sound = if (isEmergency) "siren" else "persistent",
                retry = if (isEmergency) 60 else null,
                expire = if (isEmergency) 3600 else null
            )
            isSending = false
            Toast.makeText(
                context,
                if (result.success) "Page sent!" else "Failed: ${result.message}",
                if (result.success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (settings.spouseName.isNotBlank())
                            "Page ${settings.spouseName}"
                        else
                            "Family Pager",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Emergency button — top half
            Button(
                onClick = { sendPage(isEmergency = true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmergencyRed,
                    disabledContainerColor = EmergencyRedDark
                ),
                enabled = !isSending
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "EMERGENCY",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Repeats until acknowledged",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Call Me ASAP button — bottom half
            Button(
                onClick = { sendPage(isEmergency = false) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = UrgentAmber,
                    disabledContainerColor = UrgentAmberDark
                ),
                enabled = !isSending
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "CALL ME ASAP",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bypasses quiet hours",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (isSending) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
