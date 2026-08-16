package ch.santa.santatab.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.santa.santatab.data.model.ConnectionStatus
import ch.santa.santatab.ui.components.ConnectionBanner
import ch.santa.santatab.ui.components.DiffuserCard
import ch.santa.santatab.ui.components.SceneRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val diffusers by viewModel.diffusers.collectAsStateWithLifecycle()
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SantaTAB") },
                actions = {
                    IconButton(onClick = onOpenGuide) {
                        Icon(Icons.Filled.HelpOutline, contentDescription = "Anleitung")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Einstellungen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ConnectionBanner(state = connection)

            if (diffusers.isEmpty()) {
                EmptyState(
                    status = connection.status,
                    detail = connection.detail,
                    onOpenSettings = onOpenSettings,
                    onOpenGuide = onOpenGuide,
                )
            } else {
                SceneRow(
                    scenes = viewModel.scenes,
                    enabled = connection.status == ConnectionStatus.CONNECTED,
                    onSceneClick = viewModel::applyScene,
                    modifier = Modifier.padding(top = 12.dp),
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(diffusers, key = { it.id }) { diffuser ->
                        DiffuserCard(
                            diffuser = diffuser,
                            onPowerChange = { on -> viewModel.setPower(diffuser.id, on) },
                            onStepChange = { step -> viewModel.setStep(diffuser.id, step) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    status: ConnectionStatus,
    detail: String?,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
) {
    val notConfigured = detail == "Kein Broker konfiguriert" ||
        status == ConnectionStatus.DISCONNECTED

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Spa,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(20.dp))

        val (title, message) = when {
            notConfigured -> "Willkommen bei SantaTAB" to
                "Damit die Diffuser gefunden werden, hinterlege zuerst die Adresse deines MQTT-Brokers."
            status == ConnectionStatus.ERROR -> "Verbindung fehlgeschlagen" to
                (detail ?: "Der Broker ist nicht erreichbar. Bitte Adresse und Netzwerk prüfen.")
            status == ConnectionStatus.CONNECTING -> "Verbinde mit dem Broker …" to
                "Einen Moment bitte."
            else -> "Suche Diffuser …" to
                "Verbunden. Stelle sicher, dass die ESPHome-Geräte laufen und per MQTT-Discovery angemeldet sind."
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (notConfigured || status == ConnectionStatus.ERROR) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onOpenSettings) {
                Text("Broker einrichten")
            }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onOpenGuide) {
            Text("Wie funktioniert das? Anleitung öffnen")
        }
    }
}
