package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.DateUtils
import com.example.ui.components.CalorieTrendBarChart
import com.example.ui.components.WeightTrendLineChart
import com.example.ui.viewmodel.DailyChartPoint
import com.example.ui.viewmodel.DashboardSummary
import java.util.Locale

@Composable
fun AnalyticsHistoryScreen(
    summary: DashboardSummary,
    buildChartSeries: (Int) -> List<DailyChartPoint>,
    onLogWeight: (Double, String?) -> Unit,
    onSelectHistoryDate: (String) -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRangeDays by remember { mutableIntStateOf(7) }
    var showWeightDialog by remember { mutableStateOf(false) }

    val chartPoints = remember(selectedRangeDays, summary.allFoodLogs, summary.allExerciseLogs, summary.profile.dailyCalorieTarget) {
        buildChartSeries(selectedRangeDays)
    }

    val recentDates = remember(summary.allFoodLogs, summary.allExerciseLogs) {
        (summary.allFoodLogs.map { it.dateKey } + summary.allExerciseLogs.map { it.dateKey } + DateUtils.todayDateKey())
            .distinct()
            .sortedDescending()
            .take(14)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("প্রোগ্রেস ও হিস্ট্রি", style = MaterialTheme.typography.headlineMedium)
                FilledTonalButton(
                    onClick = onExportCsv,
                    modifier = Modifier.testTag("export_csv_button")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("এক্সপোর্ট")
                }
            }
        }

        // Calorie Chart Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ক্যালোরি গ্রহণ বনাম বার্ন", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedRangeDays == 7,
                                onClick = { selectedRangeDays = 7 },
                                label = { Text("৭ দিন") }
                            )
                            FilterChip(
                                selected = selectedRangeDays == 30,
                                onClick = { selectedRangeDays = 30 },
                                label = { Text("৩০ দিন") }
                            )
                        }
                    }

                    CalorieTrendBarChart(points = chartPoints)

                    val avgIntake = chartPoints.filter { it.intakeCalories > 0 }
                        .map { it.intakeCalories }.average().let { if (it.isNaN()) 0 else it.toInt() }
                    Text(
                        text = "গড় দৈনিক গ্রহণ: $avgIntake kcal • টার্গেট: ${summary.profile.dailyCalorieTarget} kcal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Weight Progress Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Column {
                                Text("ওজন পরিবর্তনের গ্রাফ", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "বর্তমান: ${String.format(Locale.US, "%.1f", summary.profile.currentWeightKg)} কেজি • লক্ষ্য: ${String.format(Locale.US, "%.1f", summary.profile.targetWeightKg)} কেজি",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = { showWeightDialog = true },
                            modifier = Modifier.testTag("log_weight_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ওজন লগ")
                        }
                    }

                    if (summary.weightLogs.isEmpty()) {
                        Text(
                            text = "এখনো ওজনের ইতিহাস নেই। 'ওজন লগ' বাটনে ট্যাপ করে আজকের ওজন যোগ করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        WeightTrendLineChart(
                            logs = summary.weightLogs,
                            targetWeightKg = summary.profile.targetWeightKg
                        )
                        summary.weightLogs.takeLast(5).reversed().forEach { w ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${w.dateKey}${if (!w.note.isNullOrBlank()) " • ${w.note}" else ""}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", w.weightKg)} কেজি",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Today / Average Macro Summary Card
        item {
            val activeDays = summary.allFoodLogs.map { it.dateKey }.distinct().size.coerceAtLeast(1)
            val avgProtein = summary.allFoodLogs.sumOf { it.proteinG } / activeDays
            val avgCarbs = summary.allFoodLogs.sumOf { it.carbsG } / activeDays
            val avgFat = summary.allFoodLogs.sumOf { it.fatG } / activeDays

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("দৈনিক গড় ম্যাক্রো বনাম টার্গেট", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("প্রোটিন", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${avgProtein.toInt()} / ${summary.profile.proteinTargetG}g", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("শর্করা", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${avgCarbs.toInt()} / ${summary.profile.carbsTargetG}g", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("ফ্যাট", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${avgFat.toInt()} / ${summary.profile.fatTargetG}g", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Daily History Cards
        item {
            Text("সাম্প্রতিক দিনের ডায়েরি (দেখতে ও এডিট করতে ট্যাপ করুন)", style = MaterialTheme.typography.titleMedium)
        }

        items(recentDates, key = { it }) { dateKey ->
            val dayFoods = summary.allFoodLogs.filter { it.dateKey == dateKey }
            val dayIn = dayFoods.sumOf { it.calories }
            val dayP = dayFoods.sumOf { it.proteinG }.toInt()
            val dayC = dayFoods.sumOf { it.carbsG }.toInt()
            val dayF = dayFoods.sumOf { it.fatG }.toInt()
            val dayBurn = summary.allExerciseLogs.filter { it.dateKey == dateKey }.sumOf { it.caloriesBurned }
            val dayWater = summary.allWaterLogs.firstOrNull { it.dateKey == dateKey }?.amountMl ?: 0L

            Card(
                onClick = { onSelectHistoryDate(dateKey) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = DateUtils.formatReadableDate(dateKey),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "গ্রহণ: $dayIn kcal • বার্ন: $dayBurn kcal • পানি: ${dayWater}ml",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "প্রোটিন ${dayP}g • শর্করা ${dayC}g • ফ্যাট ${dayF}g",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${dayIn - dayBurn} নেট",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showWeightDialog) {
        var weightInput by remember {
            mutableStateOf(String.format(Locale.US, "%.1f", summary.profile.currentWeightKg))
        }
        var noteInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showWeightDialog = false },
            title = { Text("আজকের ওজন লগ করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                        label = { Text("ওজন (কেজি)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("weight_dialog_input")
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it.take(200) },
                        label = { Text("নোট (ঐচ্ছিক, যেমন: সকালে খালি পেটে)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = weightInput.toDoubleOrNull()?.coerceIn(25.0, 300.0)
                        if (w != null) {
                            onLogWeight(w, noteInput.ifBlank { null })
                            showWeightDialog = false
                        }
                    }
                ) { Text("সেভ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showWeightDialog = false }) { Text("বাতিল") }
            }
        )
    }
}
