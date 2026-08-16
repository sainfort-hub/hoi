package ch.santa.santatab.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
                title = { Text("Broker-Einstellungen") },
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
            Text(
                text = "SantaTAB steuert die HIDE-Diffuser lokal über einen MQTT-Broker (z. B. Mosquitto). Die Geräte melden sich per Discovery selbst an.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = form.host,
                onValueChange = { v -> viewModel.update { it.copy(host = v) } },
                label = { Text("Broker-Host / IP") },
                placeholder = { Text("z. B. 10.1.1.50") },
                supportingText = { Text("Adresse des Brokers im WLAN") },
                leadingIcon = { Icon(Icons.Filled.Dns, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = if (form.port == 0) "" else form.port.toString(),
                onValueChange = { v -> viewModel.update { it.copy(port = v.toIntOrNull() ?: 0) } },
                label = { Text("Port") },
                placeholder = { Text("1883") },
                supportingText = { Text("MQTT-Standard: 1883") },
                leadingIcon = { Icon(Icons.Filled.Numbers, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.username,
                onValueChange = { v -> viewModel.update { it.copy(username = v) } },
                label = { Text("Benutzername (optional)") },
                supportingText = { Text("Nur falls der Broker eine Anmeldung verlangt") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.password,
                onValueChange = { v -> viewModel.update { it.copy(password = v) } },
                label = { Text("Passwort (optional)") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.discoveryPrefix,
                onValueChange = { v -> viewModel.update { it.copy(discoveryPrefix = v) } },
                label = { Text("Discovery-Prefix") },
                placeholder = { Text(BrokerSettings.DEFAULT_DISCOVERY_PREFIX) },
                supportingText = { Text("Muss mit der ESPHome-Konfiguration übereinstimmen") },
                leadingIcon = { Icon(Icons.Filled.Sensors, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = form.host.isNotBlank(),
            ) {
                Text(if (saved) "Gespeichert ✓" else "Speichern & verbinden")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onOpenGuide) {
                    Text("Hilfe & Anleitung")
                }
            }
        }
    }
}
