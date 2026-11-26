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
        contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HeaderBar(state = state, onEditClick = { showManualEditDialog = true }) }
        item { LiveTelemetryCard(state = state, onEditClick = { showManualEditDialog = true }) }
        item {
            ActionButtonsRow(
                isTracking = state.isTracking,
                onToggleTracking = onToggleTracking,
                onOpenRefuel = { showRefuelDialog = true }
            )
        }
        item { CycleSummaryCard(state = state) }
        item {
            Text(
                text = "HISTORIAL DE RECARGAS (SÚPER)",
                color = TextSecondary,
                fontSize = 15.sp,
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
                        fontSize = 17.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(state.entries, key = { it.id }) { entry ->
                FuelEntryCard(entry = entry, onEdit = { editingEntry = entry }, onDelete = { onDeleteEntry(entry.id) })
            }
        }
    }

    if (showManualEditDialog) {
        ManualTelemetryDialog(state = state, onDismiss = { showManualEditDialog = false }, onConfirm = { baseOdo, tripKm, price, eff ->
            onSaveManualEdit(baseOdo, tripKm, price, eff); showManualEditDialog = false
        })
    }

    if (showRefuelDialog) {
        RefuelDialog(defaultOdometerKm = state.currentOdometerKm, defaultPricePerGallon = state.pricePerGallon, onDismiss = { showRefuelDialog = false }, onConfirm = { odo, dollars, price, isFull, note ->
            onRegisterRefuel(odo, dollars, price, isFull, note); showRefuelDialog = false
        })
    }

    val entryToEdit = editingEntry
    if (entryToEdit != null) {
        EditEntryDialog(entry = entryToEdit, onDismiss = { editingEntry = null }, onConfirm = { odo, dollars, price, isFull, note ->
            onSaveEntryEdit(entryToEdit.id, odo, dollars, price, isFull, note); editingEntry = null
        })
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
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = String.format(Locale.US, "Odómetro: %.1f km", state.currentOdometerKm),
                color = TextPrimary,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
        }
        OutlinedButton(
            onClick = onEditClick,
            border = BorderStroke(1.dp, BorderDark),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = CyanAccent, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Editar", color = TextPrimary, fontSize = 16.sp)
        }
    }
}

@Composable
private fun LiveTelemetryCard(state: TrackerUiState, onEditClick: () -> Unit) {
    val consumedLiters = state.consumedGallonsEstimate * 3.78541
    val pricePerLiter = state.pricePerGallon / 3.78541
    val distanceMeters = state.tripDistanceKm * 1000
    val speedMs = state.currentSpeedKmh / 3.6

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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onEditClick) {
                    Text("Ajustar km", color = CyanAccent, fontSize = 15.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("DISTANCIA", color = TextSecondary, fontSize = 14.sp)
                    Text(
                        text = String.format(Locale.US, "%.1f km", state.tripDistanceKm),
                        color = TextPrimary,
                        fontSize = 33.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = String.format(Locale.US, "%.0f m", distanceMeters),
                        color = TextSecondary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("VELOCIDAD", color = TextSecondary, fontSize = 14.sp)
                    Text(
                        text = String.format(Locale.US, "%.0f km/h", state.currentSpeedKmh),
                        color = CyanAccent,
                        fontSize = 33.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f m/s", speedMs),
                        color = CyanAccent,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.HorizontalDivider(color = BorderDark)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("CONSUMO ESTIMADO", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "$%.2f/gal", state.pricePerGallon),
                            color = TextSecondary,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f gal", state.consumedGallonsEstimate),
                            color = AmberWarn,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                        Text(
                            text = String.format(Locale.US, "$%.2f/L", pricePerLiter),
                            color = TextSecondary,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(end = 8.dp, bottom = 3.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f L", consumedLiters),
                            color = AmberWarn,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("COSTO TOTAL", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "$%.2f", state.consumedDollarsEstimate),
                        color = TextPrimary,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(isTracking: Boolean, onToggleTracking: () -> Unit, onOpenRefuel: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onToggleTracking,
            colors = ButtonDefaults.buttonColors(containerColor = if (isTracking) Color(0xFFB71C1C) else YamahaBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(54.dp)
        ) {
            Icon(imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = if (isTracking) "Detener GPS" else "Iniciar GPS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        Button(
            onClick = onOpenRefuel,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF163A24)),
            border = BorderStroke(1.dp, Color(0xFF2E7D32)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(54.dp)
        ) {
            Icon(imageVector = Icons.Default.LocalGasStation, contentDescription = null, tint = Color(0xFF69F0AE))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Recargar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
                Text("Odómetro base del ciclo:", color = TextSecondary, fontSize = 16.sp)
                Text(String.format(Locale.US, "%.1f km", state.baseOdometerKm), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Rendimiento calculado:", color = TextSecondary, fontSize = 16.sp)
                Text(String.format(Locale.US, "%.1f km/gal", state.assumedEfficiencyKmPerGal), color = CyanAccent, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Parciales acumuladas:", color = TextSecondary, fontSize = 16.sp)
                Text(String.format(Locale.US, "%.2f gal ($%.2f)", state.pendingCycleGallons, state.pendingCycleDollars), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Autonomía antes reserva:", color = TextSecondary, fontSize = 16.sp)
                Text(String.format(Locale.US, "~%.0f km", maxOf(0.0, state.estimatedKmBeforeReserve)), color = if (state.estimatedKmBeforeReserve < 50.0) AmberWarn else TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FuelEntryCard(entry: FuelEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${entry.formattedDate()} • ${if (entry.isFullTank) "Lleno" else "Parcial"}", color = CyanAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(String.format(Locale.US, "Odo: %.1f km • %.2f gal ($%.2f)", entry.odometerKm, entry.gallons, entry.amountDollars), color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text("${entry.note} (@ $${String.format(Locale.US, "%.2f", entry.pricePerGallon)}/gal)", color = TextSecondary, fontSize = 15.sp)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar", tint = CyanAccent) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF6E6E)) }
        }
    }
}

@Composable
private fun ManualTelemetryDialog(state: TrackerUiState, onDismiss: () -> Unit, onConfirm: (Double, Double, Double, Double) -> Unit) {
    var baseOdo by remember { mutableStateOf(String.format(Locale.US, "%.1f", state.baseOdometerKm)) }
    var tripKm by remember { mutableStateOf(String.format(Locale.US, "%.1f", state.tripDistanceKm)) }
    var price by remember { mutableStateOf(String.format(Locale.US, "%.2f", state.pricePerGallon)) }
    var eff by remember { mutableStateOf(String.format(Locale.US, "%.1f", state.assumedEfficiencyKmPerGal)) }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceDark,
        title = { Text("Editar Kilometraje", color = TextPrimary, fontSize = 23.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkNumberField("Odómetro base (km)", baseOdo) { baseOdo = it }
                DarkNumberField("Recorrido tramo (km)", tripKm) { tripKm = it }
                DarkNumberField("Precio Súper ($/gal)", price) { price = it }
                DarkNumberField("Rendimiento (km/gal)", eff) { eff = it }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(baseOdo.toDoubleOrNull() ?: state.baseOdometerKm, tripKm.toDoubleOrNull() ?: state.tripDistanceKm, price.toDoubleOrNull() ?: state.pricePerGallon, eff.toDoubleOrNull() ?: state.assumedEfficiencyKmPerGal) }) { Text("Guardar", color = CyanAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary, fontSize = 17.sp) } }
    )
}

@Composable
private fun RefuelDialog(defaultOdometerKm: Double, defaultPricePerGallon: Double, onDismiss: () -> Unit, onConfirm: (Double, Double, Double, Boolean, String) -> Unit) {
    var odo by remember { mutableStateOf(String.format(Locale.US, "%.1f", defaultOdometerKm)) }
    var dollars by remember { mutableStateOf("5.00") }
    var price by remember { mutableStateOf(String.format(Locale.US, "%.2f", defaultPricePerGallon)) }
    var isFull by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceDark,
        title = { Text("Registrar Súper", color = TextPrimary, fontSize = 23.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkNumberField("Odómetro actual (km)", odo) { odo = it }
                DarkNumberField("Monto pagado ($)", dollars) { dollars = it }
                DarkNumberField("Precio Súper ($/gal)", price) { price = it }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("¿Tanque lleno?", color = TextPrimary, fontSize = 17.sp)
                    Switch(checked = isFull, onCheckedChange = { isFull = it }, colors = SwitchDefaults.colors(checkedTrackColor = YamahaBlue))
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it }, label = { Text("Nota opcional", color = TextSecondary, fontSize = 15.sp) }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = CyanAccent, unfocusedBorderColor = BorderDark)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(odo.toDoubleOrNull() ?: defaultOdometerKm, dollars.toDoubleOrNull() ?: 0.0, price.toDoubleOrNull() ?: defaultPricePerGallon, isFull, note) }) { Text("Registrar", color = CyanAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary, fontSize = 17.sp) } }
    )
}

@Composable
private fun EditEntryDialog(entry: FuelEntry, onDismiss: () -> Unit, onConfirm: (Double, Double, Double, Boolean, String) -> Unit) {
    var odo by remember { mutableStateOf(String.format(Locale.US, "%.1f", entry.odometerKm)) }
    var dollars by remember { mutableStateOf(String.format(Locale.US, "%.2f", entry.amountDollars)) }
    var price by remember { mutableStateOf(String.format(Locale.US, "%.2f", entry.pricePerGallon)) }
    var isFull by remember { mutableStateOf(entry.isFullTank) }
    var note by remember { mutableStateOf(entry.note) }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceDark,
        title = { Text("Editar Registro", color = TextPrimary, fontSize = 23.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkNumberField("Odómetro (km)", odo) { odo = it }
                DarkNumberField("Monto ($)", dollars) { dollars = it }
                DarkNumberField("Precio Súper ($/gal)", price) { price = it }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("¿Tanque lleno?", color = TextPrimary, fontSize = 17.sp)
                    Switch(checked = isFull, onCheckedChange = { isFull = it }, colors = SwitchDefaults.colors(checkedTrackColor = YamahaBlue))
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it }, label = { Text("Nota", color = TextSecondary, fontSize = 15.sp) }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = CyanAccent, unfocusedBorderColor = BorderDark)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(odo.toDoubleOrNull() ?: entry.odometerKm, dollars.toDoubleOrNull() ?: entry.amountDollars, price.toDoubleOrNull() ?: entry.pricePerGallon, isFull, note) }) { Text("Guardar", color = CyanAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary, fontSize = 17.sp) } }
    )
}

@Composable
private fun DarkNumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label, color = TextSecondary, fontSize = 15.sp) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = CyanAccent, unfocusedBorderColor = BorderDark), modifier = Modifier.fillMaxWidth()
    )
}
