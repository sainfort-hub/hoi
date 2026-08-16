package ch.santa.santatab.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val diffusers by viewModel.diffusers.collectAsStateWithLifecycle()
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SantaTAB") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Einstellungen")
                    }
                },
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
                )
            } else {
                SceneRow(
                    scenes = viewModel.scenes,
                    enabled = connection.status == ConnectionStatus.CONNECTED,
                    onSceneClick = viewModel::applyScene,
                    modifier = Modifier.padding(top = 8.dp),
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
) {
    val notConfigured = detail == "Kein Broker konfiguriert" ||
        status == ConnectionStatus.DISCONNECTED

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val message = when {
            notConfigured -> "Noch kein MQTT-Broker eingerichtet.\nHinterlege die Broker-Adresse, damit SantaTAB die Diffuser findet."
            status == ConnectionStatus.ERROR -> "Verbindung zum Broker fehlgeschlagen.\n${detail.orEmpty()}"
            status == ConnectionStatus.CONNECTING -> "Verbinde mit dem Broker …"
            else -> "Verbunden – suche Diffuser …\nStelle sicher, dass die ESPHome-Geräte laufen und per MQTT-Discovery angemeldet sind."
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (notConfigured || status == ConnectionStatus.ERROR) {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Text("Broker einrichten")
            }
        }
    }
}
