package com.nova.app.feature.plants

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    onOwnPlants: () -> Unit = {},
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
    var confirmsRecipient by remember { mutableStateOf(false) }
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
                code = scanned; delivery = null; confirmsRecipient = false
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
                is ApiResult.Success -> {
                    delivery = result.value
                    confirmsRecipient = false
                }
                is ApiResult.Failure -> { error = result.message; delivery = null }
            }
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(
        usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn,
    )) {
        Surface(color = PlantupiaColors.cream, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize()
                    .imePadding()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onClose) { Text("? Back", color = PlantupiaColors.forest) }
                    Spacer(Modifier.weight(1f))
                    PlantupiaBrandGlyph(Modifier.size(28.dp))
                    Text(
                        "PLANTUPIA",
                        modifier = Modifier.padding(start = 7.dp),
                        style = PlantupiaText.sectionLabel,
                    )
                }

                Text("DELIVERY / PRIVATE CONNECTION", style = PlantupiaText.sectionLabel)
                Text(
                    if (delivery == null) "Connect what arrived." else "Review before you connect.",
                    style = PlantupiaText.pageHeading,
                )
                Text(
                    if (delivery == null)
                        "Scan the delivery QR or enter its code. Nothing is added to your account until you review and confirm it."
                    else
                        "Check the space and plant count first. Confirmation is the moment these plants become part of your private Plantupia space.",
                    style = PlantupiaText.body,
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PlantupiaColors.softSage,
                    shape = PlantupiaShapes.inset,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HandoverStep("01", "SCAN", active = delivery == null, modifier = Modifier.weight(1f))
                        HandoverStep("02", "REVIEW", active = delivery != null && delivery?.spaceId == null, modifier = Modifier.weight(1f))
                        HandoverStep("03", "CONNECT", active = delivery?.spaceId != null, modifier = Modifier.weight(1f))
                    }
                }

                if (accountId != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = PlantupiaColors.paper,
                        shape = PlantupiaShapes.inset,
                        border = BorderStroke(1.dp, PlantupiaColors.border),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("SIGNED IN AS", style = PlantupiaText.sectionLabel)
                            Text(accountName, style = PlantupiaText.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold))
                        }
                    }
                }

                if (delivery == null) {
                    Button(
                        enabled = !busy,
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) scan()
                            else permission.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
                        shape = PlantupiaShapes.inset,
                    ) { Text("Scan delivery QR") }

                    Text("OR ENTER THE DELIVERY CODE", style = PlantupiaText.sectionLabel)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = PlantupiaColors.paper,
                        shape = PlantupiaShapes.card,
                        border = BorderStroke(1.dp, PlantupiaColors.border),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = code,
                                onValueChange = {
                                    code = it
                                    error = null
                                    confirmsRecipient = false
                                    deliveryCode(it)?.let { token ->
                                        if (current() && !store.save(token, accountId)) {
                                            error = "The code couldn't be stored securely. Please retry."
                                        }
                                    }
                                },
                                enabled = !busy,
                                label = { Text("Delivery code") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    autoCorrectEnabled = false,
                                ),
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                "The code stays private and is used only to look up this delivery.",
                                style = PlantupiaText.supporting,
                            )
                        }
                    }

                    if (accountId == null) {
                        Text(
                            "Sign in or create an account to keep this delivery code and continue the connection safely.",
                            style = PlantupiaText.supporting,
                        )
                        Button(
                            onClick = { if (preserve()) onLogin() },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
                        ) { Text("Continue to log in") }
                        OutlinedButton(
                            onClick = { if (preserve()) onRegister() },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        ) { Text("Create account and continue", color = PlantupiaColors.forest) }
                    } else {
                        Button(
                            enabled = !busy && deliveryCode(code) != null,
                            onClick = { request(false) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
                        ) { Text("Review delivery details") }
                    }
                } else {
                    val summary = requireNotNull(delivery)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = PlantupiaColors.paper,
                        shape = PlantupiaShapes.card,
                        border = BorderStroke(1.dp, PlantupiaColors.border),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                if (summary.spaceId != null) "CONNECTED" else "READY TO CONFIRM",
                                style = PlantupiaText.sectionLabel,
                            )
                            Text(summary.spaceLabel, style = PlantupiaText.editorialHeading)
                            Text("${summary.plantCount} individual plants prepared", style = PlantupiaText.body)
                        }
                    }

                    if (summary.spaceId != null) {
                        Text(
                            "Next, open a plant in My Plants and add its first real photo. Care and Living History begin only when you record something that actually happened.",
                            style = PlantupiaText.supporting,
                        )
                        Button(
                            onClick = {
                                if (current()) {
                                    store.clear()
                                    onConnected(requireNotNull(summary.spaceId))
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
                        ) { Text("Open My Plants ?") }
                    } else {
                        Text(
                            "Reviewing does not add anything yet. Confirm only after checking the account, space and plant count.",
                            style = PlantupiaText.supporting,
                        )
                        Surface(
                            color = PlantupiaColors.softSage,
                            shape = PlantupiaShapes.inset,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = confirmsRecipient,
                                    onCheckedChange = { confirmsRecipient = it },
                                    enabled = !busy,
                                )
                                Text(
                                    "This is my delivery and this is the right account.",
                                    modifier = Modifier.weight(1f),
                                    style = PlantupiaText.body,
                                )
                            }
                        }
                        Button(
                            enabled = !busy && confirmsRecipient,
                            onClick = { request(true) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
                        ) { Text("Confirm and connect my plants") }
                    }
                }

                if (accountId != null && delivery == null) {
                    TextButton(onClick = onOwnPlants, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("No delivery? Start with my own plants", color = PlantupiaColors.forest)
                    }
                }
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = PlantupiaColors.forest)
                    Text("Checking with Plantupia\u2026", style = PlantupiaText.supporting)
                }
                error?.let {
                    Surface(color = PlantupiaColors.paper, shape = PlantupiaShapes.inset) {
                        Text(
                            it,
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = PlantupiaText.supporting,
                        )
                    }
                }
                if (code.isNotBlank()) {
                    TextButton(enabled = !busy, onClick = { if (current()) { store.clear(); onClose() } }) {
                        Text("Not my delivery? Remove code", color = PlantupiaColors.forest)
                    }
                }
                Text(
                    "Your code is stored privately for up to 24 hours; it may expire sooner.",
                    style = PlantupiaText.supporting,
                )
            }
        }
    }
}

@Composable
private fun HandoverStep(
    number: String,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = if (active) PlantupiaColors.forest else PlantupiaColors.paper,
        shape = PlantupiaShapes.badge,
    ) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 8.dp)) {
            Text(
                number,
                style = PlantupiaText.sectionLabel.copy(
                    color = if (active) PlantupiaColors.sage else PlantupiaColors.secondaryText,
                ),
            )
            Text(
                label,
                style = PlantupiaText.sectionLabel.copy(
                    color = if (active) PlantupiaColors.paper else PlantupiaColors.forest,
                ),
            )
        }
    }
}
