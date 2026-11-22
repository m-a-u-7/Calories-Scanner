package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.DateUtils
import com.example.domain.NutritionCalculator
import com.example.model.FoodLog
import com.example.model.MealType
import com.example.ui.components.CalorieProgressRing
import com.example.ui.components.DailyVitaminsCard
import com.example.ui.components.HydrationProgressBar
import com.example.ui.theme.BurnAccent
import com.example.ui.theme.CarbsAccent
import com.example.ui.theme.FatAccent
import com.example.ui.theme.FiberAccent
import com.example.ui.theme.ProteinAccent
import com.example.ui.theme.WaterAccent
import com.example.ui.viewmodel.AiCoachState
import com.example.ui.viewmodel.DashboardSummary
import java.util.Locale
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    summary: DashboardSummary,
    aiCoachState: AiCoachState,
    onShiftDate: (Int) -> Unit,
    onJumpToToday: () -> Unit,
    onOpenFoodLogger: (MealType) -> Unit,
    onOpenExerciseTab: () -> Unit,
    onUpdateFoodLog: (FoodLog) -> Unit,
    onDeleteFoodLog: (String) -> Unit,
    onAdjustWater: (Long) -> Unit,
    onGenerateCoachInsight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = summary.profile
    val waterMl = summary.dayWaterLog?.amountMl ?: 0L
    val waterTarget = profile.waterTargetMl
    val fiberTarget = NutritionCalculator.fiberTargetG(profile)
    val isToday = summary.selectedDateKey == DateUtils.todayDateKey()
    var editingFood by remember { mutableStateOf<FoodLog?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting & Date Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "হ্যালো, ${profile.displayName}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = DateUtils.formatReadableDate(summary.selectedDateKey),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!isToday) {
                            AssistChip(
                                onClick = onJumpToToday,
                                leadingIcon = {
                                    Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("আজ") }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onShiftDate(-1) },
                        modifier = Modifier.testTag("prev_day_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "আগের দিন")
                    }
                    IconButton(
                        onClick = { onShiftDate(1) },
                        modifier = Modifier.testTag("next_day_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "পরের দিন")
                    }
                }
            }
        }

        // Quick 1-Tap Actions Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionPill(
                    icon = Icons.Filled.CameraAlt,
                    label = "+ খাবার",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = { onOpenFoodLogger(MealType.LUNCH) },
                    modifier = Modifier.weight(1f).testTag("quick_add_food")
                )
                QuickActionPill(
                    icon = Icons.Filled.DirectionsRun,
                    label = "+ ব্যায়াম",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onOpenExerciseTab,
                    modifier = Modifier.weight(1f).testTag("quick_add_exercise")
                )
                QuickActionPill(
                    icon = Icons.Filled.LocalDrink,
                    label = "+১ গ্লাস পানি",
                    containerColor = WaterAccent.copy(alpha = 0.14f),
                    contentColor = WaterAccent,
                    onClick = { onAdjustWater(250L) },
                    modifier = Modifier.weight(1.1f).testTag("quick_add_water")
                )
            }
        }

        // Safety / Calorie Alert Banner
        if (summary.safetyAlert != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = "সতর্কতা",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = summary.safetyAlert,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Hero Calorie Ring & Net Balance Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(26.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CalorieProgressRing(
                        intakeCalories = summary.intakeCalories,
                        burnedCalories = summary.burnedCalories,
                        targetCalories = profile.dailyCalorieTarget,
                        remainingCalories = summary.remainingCalories
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CalorieStatChip(
                            icon = Icons.Filled.Restaurant,
                            label = "গ্রহণ (Intake)",
                            value = "${summary.intakeCalories} kcal",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        CalorieStatChip(
                            icon = Icons.Filled.LocalFireDepartment,
                            label = "বার্ন (Burned)",
                            value = "${summary.burnedCalories} kcal",
                            tint = BurnAccent
                        )
                        CalorieStatChip(
                            icon = Icons.Filled.AutoAwesome,
                            label = "নেট (Net)",
                            value = "${summary.netCalories} kcal",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "BMR: ${profile.bmr} kcal • TDEE: ${profile.tdee} kcal • দৈনিক লক্ষ্য: ${profile.dailyCalorieTarget} kcal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Dedicated Macro Targets & Amounts Card (Protein, Fat, Carbs/শর্করা, Fiber)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth().testTag("macro_summary_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "প্রোটিন, শর্করা ও ফ্যাটের পরিমাণ ও টার্গেট",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    MacroDetailRow(
                        name = "প্রোটিন (Protein)",
                        currentG = summary.totalProteinG,
                        targetG = profile.proteinTargetG,
                        kcalPerGram = 4,
                        accent = ProteinAccent
                    )
                    MacroDetailRow(
                        name = "শর্করা (Carbs)",
                        currentG = summary.totalCarbsG,
                        targetG = profile.carbsTargetG,
                        kcalPerGram = 4,
                        accent = CarbsAccent
                    )
                    MacroDetailRow(
                        name = "ফ্যাট (Fat)",
                        currentG = summary.totalFatG,
                        targetG = profile.fatTargetG,
                        kcalPerGram = 9,
                        accent = FatAccent
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ফাইবার (আঁশ)",
                            style = MaterialTheme.typography.labelLarge,
                            color = FiberAccent
                        )
                        Text(
                            text = "গৃহীত: ${String.format(Locale.US, "%.1f", summary.totalFiberG)} গ্রাম / টার্গেট ${fiberTarget} গ্রাম",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Daily Needed Vitamins Card (A, C, D, E, B6, B12)
        item {
            DailyVitaminsCard(summary = summary)
        }

        // Smart AI Coach Card (Gemini 3.5 Flash-Lite)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "AI পরামর্শ",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "স্মার্ট পুষ্টি কোচ (Gemini 3.5)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        FilledTonalButton(
                            onClick = onGenerateCoachInsight,
                            enabled = aiCoachState !is AiCoachState.Loading,
                            modifier = Modifier.testTag("ai_coach_button")
                        ) {
                            Text(if (aiCoachState is AiCoachState.Success) "রিফ্রেশ" else "পরামর্শ নিন")
                        }
                    }

                    when (aiCoachState) {
                        is AiCoachState.Idle -> {
                            Text(
                                text = "আপনার আজকের ক্যালোরি, প্রোটিন, শর্করা, ফ্যাট ও পানির হিসাব দেখে ব্যক্তিগত পুষ্টি পরামর্শ পেতে 'পরামর্শ নিন' ট্যাপ করুন।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiCoachState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("AI আপনার আজকের ডেটা বিশ্লেষণ করছে...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        is AiCoachState.Streaming -> {
                            Text(text = aiCoachState.text, style = MaterialTheme.typography.bodyMedium)
                        }
                        is AiCoachState.Success -> {
                            Text(text = aiCoachState.text, style = MaterialTheme.typography.bodyMedium)
                        }
                        is AiCoachState.Error -> {
                            Text(
                                text = aiCoachState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Water Intake Tracker Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WaterDrop,
                                contentDescription = "পানি",
                                tint = WaterAccent
                            )
                            Text("পানি পান ট্র্যাকার", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "$waterMl / $waterTarget ml (${waterMl / 250} গ্লাস)",
                            style = MaterialTheme.typography.labelLarge,
                            color = WaterAccent
                        )
                    }

                    HydrationProgressBar(amountMl = waterMl, targetMl = waterTarget)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onAdjustWater(-250L) },
                            enabled = waterMl >= 250L,
                            modifier = Modifier.testTag("water_minus_button")
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "২৫০ মি.লি. কমান", modifier = Modifier.size(18.dp))
                        }
                        Button(
                            onClick = { onAdjustWater(250L) },
                            modifier = Modifier.weight(1f).testTag("water_add_glass_button")
                        ) {
                            Icon(Icons.Filled.LocalDrink, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+১ গ্লাস (২৫০ ml)")
                        }
                        FilledTonalButton(
                            onClick = { onAdjustWater(500L) },
                            modifier = Modifier.testTag("water_add_bottle_button")
                        ) {
                            Text("+৫০০ ml")
                        }
                    }
                }
            }
        }

        // Meal Timeline Section
        item {
            Text(
                text = "মিল টাইমলাইন ও ক্যালোরি বণ্টন",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        MealType.entries.forEach { meal ->
            val mealFoods = summary.dayFoodLogs.filter { it.mealType == meal.name }
            val mealCalories = mealFoods.sumOf { it.calories }
            val suggestedCalories = NutritionCalculator.mealBudgetCalories(profile, meal)

            item(key = "meal_${meal.name}") {
                MealTimelineCard(
                    mealType = meal,
                    foods = mealFoods,
                    mealCalories = mealCalories,
                    suggestedCalories = suggestedCalories,
                    onAddClick = { onOpenFoodLogger(meal) },
                    onEditFood = { editingFood = it },
                    onDeleteFood = onDeleteFoodLog
                )
            }
        }
    }

    val foodBeingEdited = editingFood
    if (foodBeingEdited != null) {
        EditLoggedFoodDialog(
            food = foodBeingEdited,
            onDismiss = { editingFood = null },
            onSave = { updated ->
                onUpdateFoodLog(updated)
                editingFood = null
            }
        )
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CalorieStatChip(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MacroDetailRow(
    name: String,
    currentG: Double,
    targetG: Long,
    kcalPerGram: Int,
    accent: Color
) {
    val safeTarget = targetG.coerceAtLeast(1L)
    val progress = (currentG.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)
    val pct = ((currentG / safeTarget.toDouble()) * 100.0).toInt()
    val diffG = safeTarget - currentG
    val statusText = when {
        diffG > 0.5 -> "বাকি ${String.format(Locale.US, "%.1f", diffG)} গ্রাম"
        diffG < -0.5 -> "+${String.format(Locale.US, "%.1f", -diffG)} গ্রাম বেশি"
        else -> "টার্গেট পূর্ণ!"
    }
    val currentKcal = (currentG * kcalPerGram).roundToLong()

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = "${String.format(Locale.US, "%.1f", currentG)} / ${targetG} গ্রাম ($pct%)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            color = accent,
            trackColor = accent.copy(alpha = 0.16f),
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(50))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$currentKcal kcal শক্তি",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = if (diffG < -0.5) MaterialTheme.colorScheme.error else accent
            )
        }
    }
}

@Composable
private fun MealTimelineCard(
    mealType: MealType,
    foods: List<FoodLog>,
    mealCalories: Long,
    suggestedCalories: Int,
    onAddClick: () -> Unit,
    onEditFood: (FoodLog) -> Unit,
    onDeleteFood: (String) -> Unit
) {
    val mealIcon = when (mealType) {
        MealType.BREAKFAST -> Icons.Filled.FreeBreakfast
        MealType.LUNCH -> Icons.Filled.LunchDining
        MealType.DINNER -> Icons.Filled.DinnerDining
        MealType.SNACKS -> Icons.Filled.BakeryDining
    }
    val mealProgress = (mealCalories.toFloat() / suggestedCalories.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val mealP = foods.sumOf { it.proteinG }.toInt()
    val mealC = foods.sumOf { it.carbsG }.toInt()
    val mealF = foods.sumOf { it.fatG }.toInt()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = mealIcon,
                                contentDescription = mealType.labelBn,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(mealType.labelBn, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (foods.isEmpty()) {
                                "প্রস্তাবিত: ~$suggestedCalories kcal"
                            } else {
                                "$mealCalories / $suggestedCalories kcal • প্রো ${mealP}g • শর্ক ${mealC}g • ফ্যা ${mealF}g"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onAddClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_meal_${mealType.name.lowercase()}")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("যোগ")
                }
            }

            LinearProgressIndicator(
                progress = { mealProgress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
            )

            if (foods.isEmpty()) {
                Text(
                    text = "এখনো কিছু যোগ করা হয়নি — 'যোগ' বাটনে ট্যাপ করুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = foods.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    foods.forEach { food ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onEditFood(food) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = food.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", food.quantity)} ${food.unit} • প্রোটিন ${String.format(Locale.US, "%.1f", food.proteinG)}g • শর্করা ${String.format(Locale.US, "%.1f", food.carbsG)}g • ফ্যাট ${String.format(Locale.US, "%.1f", food.fatG)}g",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${food.calories} kcal",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { onEditFood(food) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "পরিমাণ এডিট করুন",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { onDeleteFood(food.id) }) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "খাবার মুছুন",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditLoggedFoodDialog(
    food: FoodLog,
    onDismiss: () -> Unit,
    onSave: (FoodLog) -> Unit
) {
    val baseQty = food.quantity.coerceAtLeast(0.1)
    var newQty by remember(food) { mutableDoubleStateOf(baseQty) }
    var selectedMeal by remember(food) { mutableStateOf(MealType.fromRaw(food.mealType)) }
    val ratio = newQty / baseQty
    val scaledCal = (food.calories * ratio).roundToLong().coerceAtLeast(0L)
    val scaledP = food.proteinG * ratio
    val scaledC = food.carbsG * ratio
    val scaledF = food.fatG * ratio

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${food.name} এডিট করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("পরিমাণ পরিবর্তন (${food.unit})", style = MaterialTheme.typography.labelLarge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val step = if (newQty > 10.0) 25.0 else 0.5
                            newQty = (newQty - step).coerceAtLeast(0.25)
                        }
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "কমান", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "${String.format(Locale.US, "%.1f", newQty)} ${food.unit}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalButton(
                        onClick = {
                            val step = if (newQty >= 10.0) 25.0 else 0.5
                            newQty += step
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "বাড়ান", modifier = Modifier.size(16.dp))
                    }
                }

                Text(
                    text = "$scaledCal kcal • প্রোটিন ${String.format(Locale.US, "%.1f", scaledP)}g • শর্করা ${String.format(Locale.US, "%.1f", scaledC)}g • ফ্যাট ${String.format(Locale.US, "%.1f", scaledF)}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text("কোন মিল?", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MealType.entries.forEach { m ->
                        FilterChip(
                            selected = selectedMeal == m,
                            onClick = { selectedMeal = m },
                            label = { Text(m.labelBn) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        food.copy(
                            mealType = selectedMeal.name,
                            quantity = newQty,
                            grams = food.grams * ratio,
                            calories = scaledCal,
                            proteinG = scaledP,
                            carbsG = scaledC,
                            fatG = scaledF,
                            fiberG = food.fiberG * ratio,
                            vitaminAMcg = food.vitaminAMcg * ratio,
                            vitaminCMg = food.vitaminCMg * ratio,
                            vitaminDMcg = food.vitaminDMcg * ratio,
                            vitaminEMg = food.vitaminEMg * ratio,
                            vitaminB6Mg = food.vitaminB6Mg * ratio,
                            vitaminB12Mcg = food.vitaminB12Mcg * ratio
                        )
                    )
                }
            ) { Text("আপডেট করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}
