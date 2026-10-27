package com.nova.app.feature.plants

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.*
import kotlinx.coroutines.launch

/** Contextual entry and destination only; no fifth root tab. Contains no invitation secret. */
object PlantupiaHandoverNavigation {
    var requestVersion by mutableIntStateOf(0)
        private set
    var target by mutableStateOf<Pair<Long, String>?>(null)
        private set
    fun open() { requestVersion++ }
    fun connected(account: Long, space: String) { target = account to space }
    fun consume() { target = null }
}

@Composable
internal fun PlantupiaHandoverScreen(
    accountId: Long?, accountName: String, onLogin: () -> Unit, onRegister: () -> Unit,
    onClose: () -> Unit, onConnected: (String) -> Unit,
) {
    val context = LocalContext.current
    val store = remember { PlantupiaHandoverStore(context) }
    val sessionStore = remember { NovaSessionStore(context) }
    val identity = remember { sessionStore.identity() }
    val repository = remember { PlantupiaRepository(context) }
    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf(true) }
    DisposableEffect(Unit) { onDispose { active = false } }
    fun current() = active && sessionStore.isCurrent(identity)
    var code by remember { mutableStateOf(store.read(accountId)?.token.orEmpty()) }
    var delivery by remember { mutableStateOf<PlantupiaDelivery?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        // Bind a pre-login intent to the first signed-in account, without claiming it.
        if (accountId != null && code.isNotBlank() && current() && !store.save(code, accountId)) {
            error = "The code couldn't be stored securely. Enter it again to continue."
            code = ""
        }
    }

    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (current() && result.contents != null) {
            val scanned = deliveryCode(result.contents)
            if (scanned == null) error = "This QR isn't a Plantupia delivery code. Enter the code supplied with your plants."
            else {
                code = scanned; delivery = null
                error = if (store.save(scanned, accountId)) null else "The code couldn't be stored securely. Please retry."
            }
        }
    }
    fun scan() {
        try {
            scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Scan the delivery code supplied with your plants")
                .setBeepEnabled(false).setBarcodeImageEnabled(false).setOrientationLocked(false))
        } catch (_: Exception) {
            error = "The camera couldn't open. You can still enter your delivery code below."
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (current()) {
            if (granted) scan() else error = "Camera access is off. Enter your delivery code below to continue."
        }
    }

    fun preserve(): Boolean {
        if (!current()) return false
        val token = deliveryCode(code)
        if (token == null) { error = "Enter the complete delivery code supplied with your plants."; return false }
        if (!store.save(token, accountId)) { error = "The code couldn't be stored securely. Please retry."; return false }
        code = token
        return true
    }

    fun request(confirm: Boolean) {
        if (busy || !current() || !preserve()) return
        val token = code
        busy = true
        error = null
        scope.launch {
            val result = repository.delivery(token, confirm)
            if (!current()) return@launch
            busy = false
            when (result) {
                is ApiResult.Success -> delivery = result.value
                is ApiResult.Failure -> { error = result.message; delivery = null }
            }
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(
        usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn,
    )) {
        Surface(color = PlantupiaColors.cream, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().imePadding().safeDrawingPadding()
                .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onClose) { Text("← Back") }
                Text("WELCOME HOME", style = PlantupiaText.sectionLabel)
                Text("Bring your plants home.", style = MaterialTheme.typography.headlineLarge, color = PlantupiaColors.forest)
                Text("Scan the QR from your Plantupia delivery.", style = PlantupiaText.body)
                if (accountId != null) Text("Account · $accountName", style = PlantupiaText.supporting)
                if (delivery == null) {
                    Button(enabled = !busy, onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) scan()
                        else permission.launch(Manifest.permission.CAMERA)
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest)) { Text("Scan QR code") }
                    OutlinedTextField(value = code, onValueChange = {
                        code = it; error = null
                        deliveryCode(it)?.let { token ->
                            if (current() && !store.save(token, accountId)) error = "The code couldn't be stored securely. Please retry."
                        }
                    },
                        enabled = !busy, label = { Text("Delivery code") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    if (accountId == null) {
                        Text("Use the account named on your delivery. New account? Ask the team for a new code.", style = PlantupiaText.supporting)
                        Button(onClick = { if (preserve()) onLogin() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Continue to log in") }
                        OutlinedButton(onClick = { if (preserve()) onRegister() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Create account and continue") }
                    } else {
                        Button(enabled = !busy, onClick = { request(false) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Review my delivery") }
                    }
                } else {
                    val summary = requireNotNull(delivery)
                    Text(if (summary.spaceId != null) "Your plants are connected." else "Your plants are waiting.", style = MaterialTheme.typography.titleLarge)
                    Text(summary.spaceLabel, style = MaterialTheme.typography.titleMedium)
                    Text("${summary.plantCount} plants prepared")
                    if (summary.spaceId != null) {
                        Button(onClick = { if (current()) { store.clear(); onConnected(requireNotNull(summary.spaceId)) } },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Open My Space") }
                    } else {
                        Text("Only confirm if this delivery belongs to you.", style = PlantupiaText.supporting)
                        Button(enabled = !busy, onClick = { request(true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(if (summary.claimed) "Confirm and reopen my plants" else "Confirm and connect my plants")
                        }
                    }
                }
                if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Confirming with Plantupia…") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (code.isNotBlank()) {
                    TextButton(enabled = !busy, onClick = { if (current()) { store.clear(); onClose() } }) {
                        Text("Not my delivery? Remove code")
                    }
                }
                Text("Your code is stored privately for up to 24 hours; it may expire sooner.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
