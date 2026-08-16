package ch.santa.santatab.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.santa.santatab.data.model.Diffuser
import kotlin.math.roundToInt

@Composable
fun DiffuserCard(
    diffuser: Diffuser,
    onPowerChange: (Boolean) -> Unit,
    onStepChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Air,
                        contentDescription = null,
                        tint = if (diffuser.isOn && diffuser.available) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = diffuser.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = statusLine(diffuser),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = diffuser.isOn,
                    onCheckedChange = onPowerChange,
                    enabled = diffuser.available,
                )
            }

            if (diffuser.supportsSpeed) {
                IntensitySlider(
                    diffuser = diffuser,
                    enabled = diffuser.available && diffuser.isOn,
                    onStepChange = onStepChange,
                )
            }
        }
    }
}

@Composable
private fun IntensitySlider(
    diffuser: Diffuser,
    enabled: Boolean,
    onStepChange: (Int) -> Unit,
) {
    // Lokaler Schieber-Zustand, damit das Ziehen flüssig bleibt; der bestätigte
    // Wert geht erst beim Loslassen an das Gerät.
    var sliderValue by remember(diffuser.displayStep) {
        mutableFloatStateOf(diffuser.displayStep.toFloat())
    }
    val steps = (diffuser.stepCount - 2).coerceAtLeast(0)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onStepChange(sliderValue.roundToInt()) },
            valueRange = 1f..diffuser.stepCount.toFloat(),
            steps = steps,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(),
        )
        Text(
            text = "${sliderValue.roundToInt()}/${diffuser.stepCount}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

private fun statusLine(diffuser: Diffuser): String = when {
    !diffuser.available -> "Offline"
    diffuser.isOn -> "Läuft · Stufe ${diffuser.displayStep}"
    else -> "Aus"
}
