package ch.santa.santatab.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Anleitung") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
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
            InfoCard(
                title = "So funktioniert SantaTAB",
                text = "SantaTAB steuert die HIDE-Diffuser komplett lokal im eigenen WLAN – " +
                    "ohne Cloud. Die App spricht mit einem MQTT-Broker; die umgebauten Diffuser " +
                    "(ESPHome) melden sich dort automatisch an und erscheinen in der App.",
            )

            SectionTitle("Einrichtung in 4 Schritten")

            StepCard(
                number = 1,
                title = "MQTT-Broker bereitstellen",
                text = "Einen MQTT-Broker (z. B. Mosquitto) im eigenen Netz betreiben – als " +
                    "Homey-App, auf einem NAS oder einem kleinen Rechner. Er ist die zentrale " +
                    "Vermittlungsstelle zwischen App und Diffusern.",
            )
            StepCard(
                number = 2,
                title = "Diffuser umbauen & flashen",
                text = "Die Original-Logikplatine durch einen ESP32-C3 mit ESPHome ersetzen " +
                    "(siehe Ordner firmware/ im Projekt). Nach dem ersten USB-Flash laufen " +
                    "Updates drahtlos. Die Firmware ist bereits fertig konfiguriert.",
            )
            StepCard(
                number = 3,
                title = "Broker in SantaTAB eintragen",
                text = "Unter Einstellungen die Adresse (Host/IP) und den Port des Brokers " +
                    "hinterlegen, bei Bedarf Benutzername und Passwort. Dann „Speichern & verbinden“.",
            )
            StepCard(
                number = 4,
                title = "Fertig – Diffuser erscheinen",
                text = "Sobald die Verbindung steht, tauchen die Diffuser automatisch auf. " +
                    "Ein/Aus, Intensität (1–10) und die Szenen sind sofort nutzbar.",
                last = true,
            )

            SectionTitle("Zwei Betriebsarten")

            FieldCard(
                "MQTT-Broker",
                "Steuerung über einen zentralen Broker (Mosquitto). Läuft parallel zu Homey, " +
                    "Geräte werden automatisch erkannt. Broker im Netz nötig.",
            )
            FieldCard(
                "Direkt (ohne Broker)",
                "Die App spricht jeden Diffuser direkt über seinen Web-Server an – kein Broker nötig. " +
                    "Dafür trägst du die Geräte-Adressen (feste IP empfohlen) von Hand ein.",
            )
            InfoCard(
                title = "Zeitpläne im Direkt-Modus",
                text = "Wiederkehrende Taktung (z. B. 3 Min pro Stunde) läuft autonom auf dem Gerät " +
                    "selbst – auch wenn das Handy aus ist. Über die App/den Web-Server lässt sich der " +
                    "Automatik-Betrieb ein- und ausschalten.",
            )

            SectionTitle("Die Felder erklärt")

            FieldCard("Broker-Host / IP", "Adresse des MQTT-Brokers im Netz, z. B. 10.1.1.50 oder mosquitto.local.")
            FieldCard("Port", "MQTT-Standardport ist 1883 (unverschlüsselt im eigenen Netz). Nur ändern, wenn der Broker anders eingestellt ist.")
            FieldCard("Benutzername / Passwort", "Nur nötig, wenn der Broker eine Anmeldung verlangt. Sonst leer lassen.")
            FieldCard("Discovery-Prefix", "Das Topic-Präfix, unter dem sich die Geräte anmelden. Standard „homeassistant“ – muss mit der ESPHome-Konfiguration übereinstimmen.")

            SectionTitle("Gut zu wissen")

            InfoCard(
                title = "Zusammenspiel mit Homey",
                text = "SantaTAB und Homey nutzen denselben Broker. Zeitpläne, Automationen und " +
                    "Sprachsteuerung laufen weiterhin über Homey; SantaTAB ist für die direkte " +
                    "Bedienung und die Szenen da.",
            )

            WarningCard(
                text = "Die Diffuser sind netzseitig mit 230 V verbunden. Umbauarbeiten nur am " +
                    "ausgebauten, spannungsfreien Gerät und niemals gleichzeitig Netz und USB. " +
                    "Im Zweifel eine Fachperson beiziehen.",
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun StepCard(number: Int, title: String, text: String, last: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun FieldCard(title: String, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun WarningCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
