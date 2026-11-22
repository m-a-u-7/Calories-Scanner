package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.NutritionCalculator
import com.example.model.ActivityLevel
import com.example.model.Gender
import com.example.model.GoalType
import com.example.model.UserProfile
import com.example.ui.theme.CarbsAccent
import com.example.ui.theme.FatAccent
import com.example.ui.theme.FiberAccent
import com.example.ui.theme.ProteinAccent
import java.util.Locale
import kotlin.math.roundToLong

private enum class MacroPresetMode(val labelBn: String) {
    AUTO("অটো (বিজ্ঞানভিত্তিক)"),
    HIGH_PROTEIN("হাই-প্রোটিন"),
    LOW_CARB("লো-কার্ব"),
    CUSTOM("কাস্টম সেট করুন")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    existingProfile: UserProfile,
    onSaveProfile: (
        displayName: String,
        age: Int,
        gender: Gender,
        heightCm: Double,
        currentWeightKg: Double,
        targetWeightKg: Double,
        activityLevel: ActivityLevel,
        goalType: GoalType,
        weeklyRateKg: Double,
        customProteinTargetG: Long?,
        customCarbsTargetG: Long?,
        customFatTargetG: Long?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayName by remember(existingProfile) {
        mutableStateOf(existingProfile.displayName.ifBlank { "বন্ধু" })
    }
    var ageText by remember(existingProfile) { mutableStateOf(existingProfile.age.toString()) }
    var gender by remember(existingProfile) { mutableStateOf(Gender.fromRaw(existingProfile.gender)) }
    var heightText by remember(existingProfile) { mutableStateOf(existingProfile.heightCm.toInt().toString()) }
    var currentWeightText by remember(existingProfile) {
        mutableStateOf(String.format(Locale.US, "%.1f", existingProfile.currentWeightKg))
    }
    var targetWeightText by remember(existingProfile) {
        mutableStateOf(String.format(Locale.US, "%.1f", existingProfile.targetWeightKg))
    }
    var activityLevel by remember(existingProfile) {
        mutableStateOf(ActivityLevel.fromRaw(existingProfile.activityLevel))
    }
    var goalType by remember(existingProfile) { mutableStateOf(GoalType.fromRaw(existingProfile.goalType)) }
    var weeklyRate by remember(existingProfile) {
        mutableDoubleStateOf(existingProfile.weeklyRateKg.coerceIn(0.25, 1.0))
    }

    val age = ageText.toIntOrNull()?.coerceIn(13, 120) ?: 26
    val heightCm = heightText.toDoubleOrNull()?.coerceIn(80.0, 250.0) ?: 170.0
    val currentWeight = currentWeightText.toDoubleOrNull()?.coerceIn(25.0, 300.0) ?: 72.0
    val targetWeight = targetWeightText.toDoubleOrNull()?.coerceIn(25.0, 300.0) ?: 65.0

    val preview = remember(age, gender, heightCm, currentWeight, targetWeight, activityLevel, goalType, weeklyRate) {
        NutritionCalculator.calculatePlan(
            age = age,
            gender = gender,
            heightCm = heightCm,
            currentWeightKg = currentWeight,
            targetWeightKg = targetWeight,
            activityLevel = activityLevel,
            goalType = goalType,
            weeklyRateKg = weeklyRate
        )
    }

    var macroPreset by remember { mutableStateOf(MacroPresetMode.AUTO) }
    var customProteinText by remember(preview) { mutableStateOf(preview.proteinTargetG.toString()) }
    var customCarbsText by remember(preview) { mutableStateOf(preview.carbsTargetG.toString()) }
    var customFatText by remember(preview) { mutableStateOf(preview.fatTargetG.toString()) }

    val activeMacros = remember(macroPreset, preview, customProteinText, customCarbsText, customFatText) {
        when (macroPreset) {
            MacroPresetMode.AUTO -> Triple(
                preview.proteinTargetG.toLong(),
                preview.carbsTargetG.toLong(),
                preview.fatTargetG.toLong()
            )
            MacroPresetMode.HIGH_PROTEIN -> {
                val cal = preview.dailyCalorieTarget.toDouble()
                Triple(
                    ((cal * 0.35) / 4.0).roundToLong().coerceAtLeast(20L),
                    ((cal * 0.40) / 4.0).roundToLong().coerceAtLeast(20L),
                    ((cal * 0.25) / 9.0).roundToLong().coerceAtLeast(10L)
                )
            }
            MacroPresetMode.LOW_CARB -> {
                val cal = preview.dailyCalorieTarget.toDouble()
                Triple(
                    ((cal * 0.35) / 4.0).roundToLong().coerceAtLeast(20L),
                    ((cal * 0.25) / 4.0).roundToLong().coerceAtLeast(20L),
                    ((cal * 0.40) / 9.0).roundToLong().coerceAtLeast(10L)
                )
            }
            MacroPresetMode.CUSTOM -> Triple(
                (customProteinText.toLongOrNull() ?: preview.proteinTargetG.toLong()).coerceIn(20L, 600L),
                (customCarbsText.toLongOrNull() ?: preview.carbsTargetG.toLong()).coerceIn(20L, 1000L),
                (customFatText.toLongOrNull() ?: preview.fatTargetG.toLong()).coerceIn(10L, 400L)
            )
        }
    }

    val activeCalories = if (macroPreset == MacroPresetMode.AUTO) {
        preview.dailyCalorieTarget.toLong()
    } else {
        ((activeMacros.first * 4L) + (activeMacros.second * 4L) + (activeMacros.third * 9L)).coerceIn(800L, 10000L)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .widthIn(max = 600.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "আপনার স্মার্ট পুষ্টি প্রোফাইল তৈরি করুন",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "আপনার শারীরিক গঠন ও লক্ষ্য অনুযায়ী BMR, TDEE এবং দৈনিক ক্যালোরি, প্রোটিন, শর্করা ও ফ্যাটের টার্গেট হিসাব করা হবে।",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("আপনার নাম") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input")
        )

        Text("লিঙ্গ", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Gender.entries.forEach { item ->
                FilterChip(
                    selected = gender == item,
                    onClick = { gender = item },
                    label = { Text(item.labelBn) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = ageText,
                onValueChange = { ageText = it.filter { ch -> ch.isDigit() }.take(3) },
                label = { Text("বয়স (বছর)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("onboarding_age_input")
            )
            OutlinedTextField(
                value = heightText,
                onValueChange = { heightText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                label = { Text("উচ্চতা (সে.মি.)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("onboarding_height_input")
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = currentWeightText,
                onValueChange = { currentWeightText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                label = { Text("বর্তমান ওজন (কেজি)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("onboarding_weight_input")
            )
            OutlinedTextField(
                value = targetWeightText,
                onValueChange = { targetWeightText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                label = { Text("কাঙ্ক্ষিত ওজন (কেজি)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("onboarding_target_weight_input")
            )
        }

        Text("অ্যাক্টিভিটি লেভেল", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivityLevel.entries.forEach { level ->
                val selected = activityLevel == level
                Card(
                    onClick = { activityLevel = level },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = level.labelBn,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = level.descriptionBn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Text("আপনার মূল লক্ষ্য", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoalType.entries.forEach { goal ->
                FilterChip(
                    selected = goalType == goal,
                    onClick = { goalType = goal },
                    label = { Text(goal.labelBn) }
                )
            }
        }

        if (goalType != GoalType.MAINTAIN) {
            Column {
                Text(
                    text = "সাপ্তাহিক পরিবর্তনের হার: ${String.format(Locale.US, "%.2f", weeklyRate)} কেজি/সপ্তাহ",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = weeklyRate.toFloat(),
                    onValueChange = { weeklyRate = (kotlin.math.round(it * 4f) / 4.0).coerceIn(0.25, 1.0) },
                    valueRange = 0.25f..1.0f,
                    steps = 2
                )
            }
        }

        // Macro Target Mode Selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("ম্যাক্রো টার্গেট (প্রোটিন, শর্করা, ফ্যাট)", style = MaterialTheme.typography.titleMedium)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroPresetMode.entries.forEach { mode ->
                FilterChip(
                    selected = macroPreset == mode,
                    onClick = {
                        macroPreset = mode
                        if (mode == MacroPresetMode.CUSTOM) {
                            customProteinText = activeMacros.first.toString()
                            customCarbsText = activeMacros.second.toString()
                            customFatText = activeMacros.third.toString()
                        }
                    },
                    label = { Text(mode.labelBn) }
                )
            }
        }

        AnimatedVisibility(visible = macroPreset == MacroPresetMode.CUSTOM) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = customProteinText,
                    onValueChange = { customProteinText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("প্রোটিন (গ্রাম)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("onboarding_custom_protein")
                )
                OutlinedTextField(
                    value = customCarbsText,
                    onValueChange = { customCarbsText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("শর্করা (গ্রাম)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("onboarding_custom_carbs")
                )
                OutlinedTextField(
                    value = customFatText,
                    onValueChange = { customFatText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("ফ্যাট (গ্রাম)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("onboarding_custom_fat")
                )
            }
        }

        // Live Scientific Plan Preview Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "স্মার্ট ক্যালোরি ও ম্যাক্রো বিশ্লেষণ",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricPill("BMR", "${preview.bmr} kcal")
                    MetricPill("TDEE", "${preview.tdee} kcal")
                    MetricPill("দৈনিক টার্গেট", "$activeCalories kcal")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MacroTargetBox(
                        title = "প্রোটিন",
                        grams = activeMacros.first,
                        calories = activeMacros.first * 4L,
                        accent = ProteinAccent,
                        modifier = Modifier.weight(1f)
                    )
                    MacroTargetBox(
                        title = "শর্করা",
                        grams = activeMacros.second,
                        calories = activeMacros.second * 4L,
                        accent = CarbsAccent,
                        modifier = Modifier.weight(1f)
                    )
                    MacroTargetBox(
                        title = "ফ্যাট",
                        grams = activeMacros.third,
                        calories = activeMacros.third * 9L,
                        accent = FatAccent,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "দৈনিক ফাইবার: ${preview.fiberTargetG} গ্রাম",
                        style = MaterialTheme.typography.labelLarge,
                        color = FiberAccent
                    )
                    Text(
                        text = "পানির লক্ষ্য: ${preview.waterTargetMl} ml",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                if (preview.estimatedWeeksToGoal != null) {
                    Text(
                        text = "আনুমানিক সময়: ~${preview.estimatedWeeksToGoal} সপ্তাহে কাঙ্ক্ষিত ওজনে পৌঁছানো সম্ভব।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (preview.safetyWarning != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = "সতর্কতা",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = preview.safetyWarning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Button(
            onClick = {
                val useCustom = macroPreset != MacroPresetMode.AUTO
                onSaveProfile(
                    displayName.ifBlank { "ব্যবহারকারী" },
                    age,
                    gender,
                    heightCm,
                    currentWeight,
                    targetWeight,
                    activityLevel,
                    goalType,
                    weeklyRate,
                    if (useCustom) activeMacros.first else null,
                    if (useCustom) activeMacros.second else null,
                    if (useCustom) activeMacros.third else null
                )
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_onboarding_button")
        ) {
            Text(
                text = "আমার ক্যালোরি ও ম্যাক্রো প্ল্যান শুরু করুন",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MacroTargetBox(
    title: String,
    grams: Long,
    calories: Long,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = accent
            )
            Text(
                text = "${grams} গ্রাম",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$calories kcal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
