package com.keyserdsoze.cardreader.ui

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keyserdsoze.cardreader.GoogleAccountCoordinator
import com.keyserdsoze.cardreader.GoogleSignInResult
import com.keyserdsoze.cardreader.R
import com.keyserdsoze.cardreader.SyncState
import com.keyserdsoze.cardreader.WalletViewModel
import com.keyserdsoze.cardreader.data.GoogleAccountIdentity
import com.keyserdsoze.cardreader.model.CardEntry
import com.keyserdsoze.cardreader.model.CodeFormat
import kotlinx.coroutines.launch

private data class EditorSeed(val card: CardEntry? = null, val value: String = "", val format: CodeFormat = CodeFormat.QR_CODE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardReaderApp(viewModel: WalletViewModel = viewModel()) {
    val document by viewModel.document.collectAsState()
    var query by remember { mutableStateOf("") }
    var scannerVisible by remember { mutableStateOf(false) }
    var editorSeed by remember { mutableStateOf<EditorSeed?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var syncVisible by remember { mutableStateOf(false) }
    val selected = document.cards.firstOrNull { it.id == selectedId }
    val filtered = remember(document.cards, query) {
        document.cards.filter { card ->
            query.isBlank() || card.name.contains(query, ignoreCase = true) || card.value.contains(query, ignoreCase = true)
        }
    }

    if (scannerVisible) {
        ScannerScreen(
            onScanned = { value, format ->
                scannerVisible = false
                editorSeed = EditorSeed(value = value, format = format)
            },
            onClose = { scannerVisible = false },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CardReader") },
                actions = {
                    IconButton(onClick = { editorSeed = EditorSeed() }) { Icon(Icons.Default.Add, "Aggiungi manualmente") }
                    IconButton(onClick = { syncVisible = true }) { Icon(Icons.Default.Settings, "Impostazioni e sync") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { scannerVisible = true }) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scansiona carta")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (document.cards.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    label = { Text("Cerca") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
            }
            if (document.cards.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Il tuo portafoglio è vuoto", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
                    Text("Scansiona la prima tessera. Non serve internet.", modifier = Modifier.padding(top = 8.dp))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered, key = { it.id }) { card ->
                        CardRow(
                            card = card,
                            onOpen = { selectedId = card.id },
                            onFavorite = { viewModel.update(card.copy(favorite = !card.favorite)) },
                        )
                    }
                }
            }
        }
    }

    editorSeed?.let { seed ->
        CardEditorDialog(
            seed = seed,
            onDismiss = { editorSeed = null },
            onSave = { name, value, format, color, notes ->
                val existing = seed.card
                if (existing == null) viewModel.add(name, value, format, color, notes)
                else viewModel.update(existing.copy(name = name, value = value, format = format, colorArgb = color, notes = notes))
                editorSeed = null
            },
        )
    }

    selected?.let { card ->
        CardDetailDialog(
            card = card,
            onClose = { selectedId = null },
            onEdit = { selectedId = null; editorSeed = EditorSeed(card, card.value, card.format) },
            onDelete = { viewModel.delete(card.id); selectedId = null },
        )
    }

    if (syncVisible) SyncDialog(viewModel, onDismiss = { syncVisible = false })
}

@Composable
private fun CardRow(card: CardEntry, onOpen: () -> Unit, onFavorite: () -> Unit) {
    val background = Color(card.colorArgb)
    val foreground = contentColorFor(background)
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(background, RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(card.name, color = foreground, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(card.format.label, color = foreground.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onFavorite) {
            Icon(if (card.favorite) Icons.Default.Star else Icons.Outlined.StarBorder, "Preferita", tint = foreground)
        }
    }
}

@Composable
private fun CardEditorDialog(
    seed: EditorSeed,
    onDismiss: () -> Unit,
    onSave: (String, String, CodeFormat, Int, String) -> Unit,
) {
    var name by remember(seed) { mutableStateOf(seed.card?.name.orEmpty()) }
    var value by remember(seed) { mutableStateOf(seed.card?.value ?: seed.value) }
    var format by remember(seed) { mutableStateOf(seed.card?.format ?: seed.format) }
    var notes by remember(seed) { mutableStateOf(seed.card?.notes.orEmpty()) }
    var color by remember(seed) { mutableStateOf(seed.card?.colorArgb ?: CARD_COLORS.first()) }
    var formatExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (seed.card == null) "Nuova carta" else "Modifica carta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value, { value = it }, label = { Text("Codice") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedButton(onClick = { formatExpanded = true }) { Text(format.label) }
                    DropdownMenu(expanded = formatExpanded, onDismissRequest = { formatExpanded = false }) {
                        CodeFormat.entries.forEach { option ->
                            DropdownMenuItem(text = { Text(option.label) }, onClick = { format = option; formatExpanded = false })
                        }
                    }
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Note (facoltative)") }, modifier = Modifier.fillMaxWidth())
                Text("Colore")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CARD_COLORS.forEach { option ->
                        Box(
                            Modifier.size(if (color == option) 34.dp else 28.dp)
                                .background(Color(option), RoundedCornerShape(50))
                                .clickable { color = option },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name.trim(), value, format, color, notes.trim()) }, enabled = name.isNotBlank() && value.isNotBlank()) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
private fun CardDetailDialog(card: CardEntry, onClose: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    var confirmDelete by remember { mutableStateOf(false) }
    DisposableEffect(activity) {
        val window = activity?.window
        val previous = window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window?.attributes = window?.attributes?.apply { screenBrightness = 1f }
        onDispose { window?.attributes = window?.attributes?.apply { screenBrightness = previous } }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color(card.colorArgb)) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Chiudi", tint = Color.White) }
                    Text(card.name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Modifica", tint = Color.White) }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Elimina", tint = Color.White) }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BarcodeImage(card.value, card.format)
                    Text(card.value, color = Color.White, modifier = Modifier.padding(top = 18.dp), style = MaterialTheme.typography.titleMedium)
                    if (card.notes.isNotBlank()) Text(card.notes, color = Color.White.copy(alpha = .85f), modifier = Modifier.padding(top = 12.dp))
                }
                Text("${card.format.label} · luminosità massima mentre la carta è aperta", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Eliminare ${card.name}?") },
        text = { Text("La cancellazione verrà propagata anche agli altri dispositivi al prossimo sync.") },
        confirmButton = { Button(onClick = onDelete) { Text("Elimina") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annulla") } },
    )
}

@Composable
private fun SyncDialog(viewModel: WalletViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coordinator = remember(activity) { activity?.let(::GoogleAccountCoordinator) }
    val scope = rememberCoroutineScope()
    val account by viewModel.account.collectAsState()
    val state by viewModel.syncState.collectAsState()
    val serverClientId = stringResource(R.string.google_web_client_id)
    var pendingIdentity by remember { mutableStateOf<GoogleAccountIdentity?>(null) }
    val authorizationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
        val identity = pendingIdentity
        if (result.resultCode == Activity.RESULT_OK && coordinator?.finishDriveAuthorization(result.data) == true && identity != null) {
            viewModel.setConnectedAccount(identity)
        } else if (identity != null) {
            viewModel.reportSyncError("Autorizzazione Google Drive annullata")
        }
        pendingIdentity = null
    }

    fun authorize(identity: GoogleAccountIdentity) {
        pendingIdentity = identity
        coordinator?.requestDriveAuthorization(
            accountEmail = identity.email,
            onAuthorized = { pendingIdentity = null; viewModel.setConnectedAccount(identity) },
            onResolution = { authorizationLauncher.launch(it) },
            onFailure = { pendingIdentity = null; viewModel.reportSyncError("Impossibile autorizzare Google Drive") },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Google Drive sync") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Il wallet resta sempre locale. Drive viene contattato solo quando premi Sincronizza.")
                if (account == null) {
                    Button(onClick = {
                        val auth = coordinator ?: return@Button
                        scope.launch {
                            when (val result = auth.signIn(serverClientId)) {
                                is GoogleSignInResult.Success -> authorize(result.account)
                                is GoogleSignInResult.Failure -> viewModel.reportSyncError(result.message)
                                GoogleSignInResult.Canceled -> Unit
                            }
                        }
                    }) { Text("Connetti account Google") }
                } else {
                    Text(account?.email.orEmpty(), fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::syncNow, enabled = state != SyncState.Running) { Text("Sincronizza ora") }
                        OutlinedButton(onClick = {
                            val email = account?.email ?: return@OutlinedButton
                            coordinator?.disconnect(email, onComplete = {
                                scope.launch { coordinator.clearCredentialSession(); viewModel.disconnectLocally() }
                            }, onFailure = { viewModel.reportSyncError("Disconnessione Google non riuscita") })
                        }) { Text("Disconnetti") }
                    }
                }
                when (val current = state) {
                    SyncState.Idle -> Unit
                    SyncState.Running -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Sincronizzazione…")
                    }
                    is SyncState.Success -> Text("Sincronizzato: ${current.cardCount} carte", color = MaterialTheme.colorScheme.primary)
                    is SyncState.Error -> Text(current.message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
    )
}

private val CARD_COLORS = listOf(
    0xFF315C9A.toInt(),
    0xFF1B6B57.toInt(),
    0xFF7A3E65.toInt(),
    0xFF9A4D2D.toInt(),
    0xFF4C4F69.toInt(),
    0xFF006A6A.toInt(),
)
