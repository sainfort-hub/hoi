package ch.santa.santatab.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.santa.santatab.data.model.AppMode
import ch.santa.santatab.data.model.BrokerSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenGuide: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenGuide) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Anleitung")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Steuerung", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = form.mode == AppMode.MQTT,
                    onClick = { viewModel.setMode(AppMode.MQTT) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("MQTT-Broker") }
                SegmentedButton(
                    selected = form.mode == AppMode.DIRECT,
                    onClick = { viewModel.setMode(AppMode.DIRECT) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("Direkt") }
            }

            when (form.mode) {
                AppMode.MQTT -> BrokerSection(form.broker, viewModel)
                AppMode.DIRECT -> DirectSection(form.directHosts, viewModel)
            }

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = form.isConfigured,
            ) {
                Text(if (saved) "Gespeichert ✓" else "Speichern & verbinden")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onOpenGuide) { Text("Hilfe & Anleitung") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrokerSection(broker: BrokerSettings, viewModel: SettingsViewModel) {
    Text(
        "SantaTAB steuert die Diffuser über einen MQTT-Broker (z. B. Mosquitto). Läuft parallel zu Homey.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = broker.host,
        onValueChange = { v -> viewModel.updateBroker { it.copy(host = v) } },
        label = { Text("Broker-Host / IP") },
        placeholder = { Text("z. B. 10.1.1.50") },
        supportingText = { Text("Adresse des Brokers im WLAN") },
        leadingIcon = { Icon(Icons.Filled.Dns, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = if (broker.port == 0) "" else broker.port.toString(),
        onValueChange = { v -> viewModel.updateBroker { it.copy(port = v.toIntOrNull() ?: 0) } },
        label = { Text("Port") },
        placeholder = { Text("1883") },
        supportingText = { Text("MQTT-Standard: 1883") },
        leadingIcon = { Icon(Icons.Filled.Numbers, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = broker.username,
        onValueChange = { v -> viewModel.updateBroker { it.copy(username = v) } },
        label = { Text("Benutzername (optional)") },
        supportingText = { Text("Nur falls der Broker eine Anmeldung verlangt") },
        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = broker.password,
        onValueChange = { v -> viewModel.updateBroker { it.copy(password = v) } },
        label = { Text("Passwort (optional)") },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = broker.discoveryPrefix,
        onValueChange = { v -> viewModel.updateBroker { it.copy(discoveryPrefix = v) } },
        label = { Text("Discovery-Prefix") },
        placeholder = { Text(BrokerSettings.DEFAULT_DISCOVERY_PREFIX) },
        supportingText = { Text("Muss mit der ESPHome-Konfiguration übereinstimmen") },
        leadingIcon = { Icon(Icons.Filled.Sensors, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DirectSection(hosts: List<String>, viewModel: SettingsViewModel) {
    var newHost by remember { mutableStateOf("") }

    Text(
        "Ohne Broker: SantaTAB spricht jeden Diffuser direkt über seinen Web-Server an. " +
            "Trage die Adresse (IP oder Hostname) jedes Geräts ein – am besten mit fester IP (DHCP-Reservierung).",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    OutlinedTextField(
        value = newHost,
        onValueChange = { newHost = it },
        label = { Text("Geräte-Adresse hinzufügen") },
        placeholder = { Text("z. B. 10.1.1.61 oder duft-wohnzimmer.local") },
        leadingIcon = { Icon(Icons.Filled.Router, contentDescription = null) },
        trailingIcon = {
            IconButton(
                onClick = {
                    viewModel.addHost(newHost)
                    newHost = ""
                },
                enabled = newHost.isNotBlank(),
            ) { Icon(Icons.Filled.Add, contentDescription = "Hinzufügen") }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            viewModel.addHost(newHost)
            newHost = ""
        }),
        modifier = Modifier.fillMaxWidth(),
    )

    if (hosts.isEmpty()) {
        Text(
            "Noch keine Geräte eingetragen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        hosts.forEach { host ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(host, style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { viewModel.removeHost(host) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Entfernen")
                    }
                }
            }
        }
    }
}
