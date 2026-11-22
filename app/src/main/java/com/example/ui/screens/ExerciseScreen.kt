package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.DateUtils
import com.example.domain.NutritionCalculator
import com.example.model.ExerciseCategory
import com.example.model.ExerciseIntensity
import com.example.model.ExerciseLog
import com.example.ui.theme.BurnAccent
import com.example.ui.viewmodel.DashboardSummary
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseScreen(
    summary: DashboardSummary,
    onAddExercise: (ExerciseLog) -> Unit,
    onDeleteExercise: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(ExerciseCategory.WALKING) }
    var selectedIntensity by remember { mutableStateOf(ExerciseIntensity.MODERATE) }
    var durationMinutes by remember { mutableIntStateOf(30) }
    var customActivityName by remember { mutableStateOf("") }

    val estimatedBurn = remember(selectedCategory, selectedIntensity, durationMinutes, summary.profile.currentWeightKg) {
        NutritionCalculator.calculateCaloriesBurned(
            category = selectedCategory,
            intensity = selectedIntensity,
            weightKg = summary.profile.currentWeightKg,
            durationMinutes = durationMinutes
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "আজকের বার্ন ক্যালোরি",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${summary.burnedCalories} kcal",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = BurnAccent
                        )
                        Text(
                            text = "${DateUtils.formatReadableDate(summary.selectedDateKey)} • নেট ${summary.netCalories} kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(BurnAccent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "বার্ন ক্যালোরি",
                            tint = BurnAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("দ্রুত ব্যায়াম লগ (১-ট্যাপ)", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickPresets = listOf(
                            Triple(ExerciseCategory.WALKING, ExerciseIntensity.MODERATE, 30),
                            Triple(ExerciseCategory.RUNNING, ExerciseIntensity.MODERATE, 20),
                            Triple(ExerciseCategory.GYM, ExerciseIntensity.MODERATE, 45),
                            Triple(ExerciseCategory.YOGA, ExerciseIntensity.LIGHT, 20)
                        )
                        quickPresets.forEach { (cat, inten, mins) ->
                            val burn = NutritionCalculator.calculateCaloriesBurned(
                                category = cat,
                                intensity = inten,
                                weightKg = summary.profile.currentWeightKg,
                                durationMinutes = mins
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    onAddExercise(
                                        ExerciseLog(
                                            activityName = "${cat.labelBn} (${inten.labelBn})",
                                            category = cat.name,
                                            intensity = inten.name,
                                            durationMinutes = mins.toLong(),
                                            caloriesBurned = burn.toLong()
                                        )
                                    )
                                },
                                label = { Text("${mins}মি. ${cat.labelBn} ($burn kcal)") }
                            )
                        }
                    }

                    Text("নতুন ব্যায়াম বা কার্যক্রম যোগ করুন", style = MaterialTheme.typography.titleLarge)

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ExerciseCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                leadingIcon = {
                                    Icon(
                                        imageVector = categoryIcon(cat),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(cat.labelBn) }
                            )
                        }
                    }

                    if (selectedCategory == ExerciseCategory.CUSTOM) {
                        OutlinedTextField(
                            value = customActivityName,
                            onValueChange = { customActivityName = it },
                            label = { Text("কার্যক্রমের নাম লিখুন") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text("তীব্রতা (Intensity)", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExerciseIntensity.entries.forEach { level ->
                            FilterChip(
                                selected = selectedIntensity == level,
                                onClick = { selectedIntensity = level },
                                label = { Text(level.labelBn) }
                            )
                        }
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("সময়কাল", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "$durationMinutes মিনিট",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = durationMinutes.toFloat(),
                            onValueChange = { durationMinutes = ((it / 5f).roundToInt() * 5).coerceAtLeast(5) },
                            valueRange = 5f..180f
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("আনুমানিক বার্ন (${summary.profile.currentWeightKg} কেজি অনুযায়ী):")
                            Text(
                                text = "$estimatedBurn kcal",
                                style = MaterialTheme.typography.titleLarge,
                                color = BurnAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val title = if (selectedCategory == ExerciseCategory.CUSTOM && customActivityName.isNotBlank()) {
                                customActivityName.trim()
                            } else {
                                "${selectedCategory.labelBn} (${selectedIntensity.labelBn})"
                            }
                            onAddExercise(
                                ExerciseLog(
                                    activityName = title,
                                    category = selectedCategory.name,
                                    intensity = selectedIntensity.name,
                                    durationMinutes = durationMinutes.toLong(),
                                    caloriesBurned = estimatedBurn.toLong()
                                )
                            )
                            customActivityName = ""
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("add_exercise_button")
                    ) {
                        Text("ব্যায়াম লগ করুন (+$estimatedBurn kcal বার্ন)")
                    }
                }
            }
        }

        item {
            Text("আজকের ব্যায়ামের তালিকা", style = MaterialTheme.typography.titleLarge)
        }

        if (summary.dayExerciseLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "এই তারিখে এখনও কোনো ব্যায়াম লগ করা হয়নি। উপরের কার্ড থেকে আপনার হাঁটা, দৌড়ানো বা ওয়ার্কআউট যোগ করুন।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(summary.dayExerciseLogs, key = { it.id }) { ex ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = categoryIcon(ExerciseCategory.fromRaw(ex.category)),
                                contentDescription = ex.activityName,
                                tint = BurnAccent
                            )
                            Column {
                                Text(ex.activityName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${ex.durationMinutes} মিনিট • ${ex.caloriesBurned} kcal বার্ন",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onDeleteExercise(ex.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "মুছুন")
                        }
                    }
                }
            }
        }
    }
}

private fun categoryIcon(cat: ExerciseCategory): ImageVector = when (cat) {
    ExerciseCategory.WALKING -> Icons.Filled.DirectionsWalk
    ExerciseCategory.RUNNING -> Icons.Filled.DirectionsRun
    ExerciseCategory.CYCLING -> Icons.Filled.DirectionsBike
    ExerciseCategory.SWIMMING -> Icons.Filled.Pool
    ExerciseCategory.GYM -> Icons.Filled.FitnessCenter
    ExerciseCategory.BODYWEIGHT -> Icons.Filled.SportsGymnastics
    ExerciseCategory.YOGA -> Icons.Filled.SelfImprovement
    ExerciseCategory.CHORES -> Icons.Filled.CleaningServices
    ExerciseCategory.CUSTOM -> Icons.Filled.LocalFireDepartment
}
