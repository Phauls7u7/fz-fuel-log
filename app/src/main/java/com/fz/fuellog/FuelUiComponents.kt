package com.fz.fuellog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

val AmoledBlack = Color(0xFF000000)
val SurfaceDark = Color(0xFF0E1116)
val BorderDark = Color(0xFF222834)
val YamahaBlue = Color(0xFF2979FF)
val CyanAccent = Color(0xFF00E5FF)
val AmberWarn = Color(0xFFFFAB00)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF9AA4B2)

@Composable
fun FuelDashboardScreen(
    state: TrackerUiState,
    onToggleTracking: () -> Unit,
    onSaveManualEdit: (Double, Double, Double, Double) -> Unit,
    onRegisterRefuel: (Double, Double, Double, Boolean, String) -> Unit,
    onSaveEntryEdit: (Long, Double, Double, Double, Boolean, String) -> Unit,
    onDeleteEntry: (Long) -> Unit
) {
    var showManualEditDialog by remember { mutableStateOf(false) }
    var showRefuelDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<FuelEntry?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeaderBar(state = state, onEditClick = { showManualEditDialog = true })
        }

        item {
            LiveTelemetryCard(state = state, onEditClick = { showManualEditDialog = true })
        }

        item {
            ActionButtonsRow(
                isTracking = state.isTracking,
                onToggleTracking = onToggleTracking,
                onOpenRefuel = { showRefuelDialog = true }
            )
        }

        item {
            CycleSummaryCard(state = state)
        }

        item {
            Text(
                text = "HISTORIAL DE RECARGAS (SÚPER)",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (state.entries.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderDark),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Aún no hay recargas registradas. Marca tu odómetro base o registra un llenado/recarga de Súper cuando gustes.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(state.entries, key = { it.id }) { entry ->
                FuelEntryCard(
                    entry = entry,
                    onEdit = { editingEntry = entry },
                    onDelete = { onDeleteEntry(entry.id) }
                )
            }
        }
    }

    if (showManualEditDialog) {
        ManualTelemetryDialog(
            state = state,
            onDismiss = { showManualEditDialog = false },
            onConfirm = { baseOdo, tripKm, price, eff ->
                onSaveManualEdit(baseOdo, tripKm, price, eff)
                showManualEditDialog = false
            }
        )
    }

    if (showRefuelDialog) {
        RefuelDialog(
            defaultOdometerKm = state.currentOdometerKm,
            defaultPricePerGallon = state.pricePerGallon,
            onDismiss = { showRefuelDialog = false },
            onConfirm = { odo, dollars, price, isFull, note ->
                onRegisterRefuel(odo, dollars, price, isFull, note)
                showRefuelDialog = false
            }
        )
    }

    val entryToEdit = editingEntry
    if (entryToEdit != null) {
        EditEntryDialog(
            entry = entryToEdit,
            onDismiss = { editingEntry = null },
            onConfirm = { odo, dollars, price, isFull, note ->
                onSaveEntryEdit(entryToEdit.id, odo, dollars, price, isFull, note)
                editingEntry = null
            }
        )
    }
}

@Composable
private fun HeaderBar(state: TrackerUiState, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "YAMAHA FZ 2.0 • SÚPER",
                color = CyanAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = String.format(Locale.US, "Odómetro: %.1f km", state.currentOdometerKm),
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onEditClick,
            border = BorderStroke(1.dp, BorderDark),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Editar",
                tint = CyanAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Editar", color = TextPrimary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun LiveTelemetryCard(state: TrackerUiState, onEditClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.isTracking) "GPS ACTIVO EN RUTA" else "LECTURA ACTUAL",
                    color = if (state.isTracking) CyanAccent else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onEditClick) {
                    Text("Ajustar km", color = CyanAccent, fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("DISTANCIA", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = String.format(Locale.US, "%.1f km", state.tripDistanceKm),
                        color = TextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("VELOCIDAD", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = String.format(Locale.US, "%.0f km/h", state.currentSpeedKmh),
                        color = CyanAccent,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            androidx.compose.material3.HorizontalDivider(color = BorderDark)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("CONSUMO ESTIMADO", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = String.format(Locale.US, "%.2f gal", state.consumedGallonsEstimate),
                        color = AmberWarn,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("COSTO GASOLINA SÚPER", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = String.format(
                            Locale.US,
                            "$%.2f (@$%.2f/gal)",
                            state.consumedDollarsEstimate,
                            state.pricePerGallon
                        ),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    isTracking: Boolean,
    onToggleTracking: () -> Unit,
    onOpenRefuel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onToggleTracking,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTracking) Color(0xFFB71C1C) else YamahaBlue
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
        ) {
            Icon(
                imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isTracking) "Detener GPS" else "Iniciar GPS",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Button(
            onClick = onOpenRefuel,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF163A24)),
            border = BorderStroke(1.dp, Color(0xFF2E7D32)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalGasStation,
                contentDescription = null,
                tint = Color(0xFF69F0AE)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Recargar Súper",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CycleSummaryCard(state: TrackerUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Odómetro base del ciclo:", color = TextSecondary, fontSize = 13.sp)
                Text(
                    String.format(Locale.US, "%.1f km", state.baseOdometerKm),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Rendimiento calculado:", color = TextSecondary, fontSize = 13.sp)
                Text(
                    String.format(Locale.US, "%.1f km/gal", state.assumedEfficiencyKmPerGal),
                    color = CyanAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recargas parciales acumuladas:", color = TextSecondary, fontSize = 13.sp)
                Text(
                    String.format(Locale.US, "%.2f gal ($%.2f)", state.pendingCycleGallons, state.pendingCycleDollars),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Autonomía aprox. antes de reserva:", color = TextSecondary, fontSize = 13.sp)
                Text(
                    String.format(Locale.US, "~%.0f km", maxOf(0.0, state.estimatedKmBeforeReserve)),
                    color = if (state.estimatedKmBeforeReserve < 50.0) AmberWarn else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun FuelEntryCard(
    entry: FuelEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${entry.formattedDate()} • ${if (entry.isFullTank) "Tanque Lleno" else "Parcial"}",
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "Odómetro: %.1f km • %.2f gal ($%.2f)",
                        entry.odometerKm,
                        entry.gallons,
                        entry.amountDollars
                    ),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${entry.note} (@ $${String.format(Locale.US, "%.2f", entry.pricePerGallon)}/gal)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar registro", tint = CyanAccent)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar registro", tint = Color(0xFFFF6E6E))
            }
        }
    }
}

@Composable
private fun ManualTelemetryDialog(
    state: TrackerUiState,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, Double, Double) -> Unit
) {
    var baseOdoText by remember { mutableStateOf(String.format(Locale.US, "%.1f", state.baseOdometerKm)) }
    var tripKmText by remember { mutableStateOf(String.format(Locale.US, "%.1f", state.tripDistanceKm)) }
    var priceText by remember { mutableStateOf(String.format(Locale.US, "%.2f", state.pricePerGallon)) }
    var effText by remember { mutableStateOf(String.format(Locale.US, "%.1f", state.assumedEfficiencyKmPerGal)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Editar Kilometraje y Súper", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkNumberField("Odómetro base (km)", baseOdoText) { baseOdoText = it }
                DarkNumberField("Distancia recorrida tramo (km)", tripKmText) { tripKmText = it }
                DarkNumberField("Precio Gasolina Súper ($/gal)", priceText) { priceText = it }
                DarkNumberField("Rendimiento base (km/gal)", effText) { effText = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    baseOdoText.toDoubleOrNull() ?: state.baseOdometerKm,
                    tripKmText.toDoubleOrNull() ?: state.tripDistanceKm,
                    priceText.toDoubleOrNull() ?: state.pricePerGallon,
                    effText.toDoubleOrNull() ?: state.assumedEfficiencyKmPerGal
                )
            }) {
                Text("Guardar", color = CyanAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun RefuelDialog(
    defaultOdometerKm: Double,
    defaultPricePerGallon: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, Double, Boolean, String) -> Unit
) {
    var odoText by remember { mutableStateOf(String.format(Locale.US, "%.1f", defaultOdometerKm)) }
    var dollarsText by remember { mutableStateOf("5.00") }
    var priceText by remember { mutableStateOf(String.format(Locale.US, "%.2f", defaultPricePerGallon)) }
    var isFullTank by remember { mutableStateOf(true) }
    var noteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Registrar Recarga Súper", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkNumberField("Odómetro actual (km)", odoText) { odoText = it }
                DarkNumberField("Monto pagado ($)", dollarsText) { dollarsText = it }
                DarkNumberField("Precio Súper ($/gal)", priceText) { priceText = it }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿Tanque lleno?", color = TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = isFullTank,
                        onCheckedChange = { isFullTank = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = YamahaBlue)
                    )
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Nota opcional", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderDark
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    odoText.toDoubleOrNull() ?: defaultOdometerKm,
                    dollarsText.toDoubleOrNull() ?: 0.0,
                    priceText.toDoubleOrNull() ?: defaultPricePerGallon,
                    isFullTank,
                    noteText
                )
            }) {
                Text("Registrar", color = CyanAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun EditEntryDialog(
    entry: FuelEntry,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, Double, Boolean, String) -> Unit
) {
    var odoText by remember { mutableStateOf(String.format(Locale.US, "%.1f", entry.odometerKm)) }
    var dollarsText by remember { mutableStateOf(String.format(Locale.US, "%.2f", entry.amountDollars)) }
    var priceText by remember { mutableStateOf(String.format(Locale.US, "%.2f", entry.pricePerGallon)) }
    var isFullTank by remember { mutableStateOf(entry.isFullTank) }
    var noteText by remember { mutableStateOf(entry.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Editar Registro", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkNumberField("Odómetro (km)", odoText) { odoText = it }
                DarkNumberField("Monto ($)", dollarsText) { dollarsText = it }
                DarkNumberField("Precio Súper ($/gal)", priceText) { priceText = it }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿Tanque lleno?", color = TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = isFullTank,
                        onCheckedChange = { isFullTank = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = YamahaBlue)
                    )
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Nota", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderDark
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    odoText.toDoubleOrNull() ?: entry.odometerKm,
                    dollarsText.toDoubleOrNull() ?: entry.amountDollars,
                    priceText.toDoubleOrNull() ?: entry.pricePerGallon,
                    isFullTank,
                    noteText
                )
            }) {
                Text("Guardar cambios", color = CyanAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun DarkNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary, fontSize = 12.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = CyanAccent,
            unfocusedBorderColor = BorderDark
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
