package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.NutritionCalculator
import com.example.firebase.GoogleSignInButton
import com.example.model.ActivityLevel
import com.example.model.Gender
import com.example.model.GoalType
import com.example.model.ThemeMode
import com.example.notifications.ReminderScheduler
import com.example.ui.theme.CarbsAccent
import com.example.ui.theme.FatAccent
import com.example.ui.theme.FiberAccent
import com.example.ui.theme.ProteinAccent
import com.example.ui.theme.WaterAccent
import com.example.ui.viewmodel.DashboardSummary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    summary: DashboardSummary,
    themeMode: ThemeMode,
    isGuestMode: Boolean,
    onChangeTheme: (ThemeMode) -> Unit,
    onEditBiometrics: () -> Unit,
    onUpdateMacroTargets: (Long, Long, Long) -> Unit,
    onUpdateReminders: (Boolean, Boolean, Boolean) -> Unit,
    onExportCsv: () -> Unit,
    onAuthSuccess: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile = summary.profile
    var pendingSync by remember { mutableStateOf<Triple<Boolean, Boolean, Boolean>?>(null) }
    var showMacroDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingSync
        if (granted && pending != null) {
            ReminderScheduler.syncReminders(context, pending.first, pending.second, pending.third)
            onUpdateReminders(pending.first, pending.second, pending.third)
        }
        pendingSync = null
    }

    fun applyReminders(meal: Boolean, water: Boolean, weight: Boolean) {
        if ((meal || water || weight) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingSync = Triple(meal, water, weight)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ReminderScheduler.syncReminders(context, meal, water, weight)
            onUpdateReminders(meal, water, weight)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.displayName, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                text = "${GoalType.fromRaw(profile.goalType).labelBn} • ${ActivityLevel.fromRaw(profile.activityLevel).labelBn}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onEditBiometrics,
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "প্রোফাইল এডিট")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("এডিট")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ProfileStat("বয়স", "${profile.age} বছর")
                        ProfileStat("লিঙ্গ", Gender.fromRaw(profile.gender).labelBn)
                        ProfileStat("উচ্চতা", "${profile.heightCm.toInt()} সে.মি.")
                        ProfileStat("ওজন", "${profile.currentWeightKg} কেজি")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ProfileStat("BMR", "${profile.bmr} kcal")
                        ProfileStat("TDEE", "${profile.tdee} kcal")
                        ProfileStat("দৈনিক লক্ষ্য", "${profile.dailyCalorieTarget} kcal")
                    }
                }
            }
        }

        // Daily Macro & Hydration Targets Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("দৈনিক প্রোটিন, শর্করা ও ফ্যাট টার্গেট", style = MaterialTheme.typography.titleMedium)
                        TextButton(
                            onClick = { showMacroDialog = true },
                            modifier = Modifier.testTag("edit_macro_targets_button")
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("কাস্টম")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileMacroPill(
                            label = "প্রোটিন",
                            value = "${profile.proteinTargetG} গ্রাম",
                            subValue = "${profile.proteinTargetG * 4} kcal",
                            color = ProteinAccent,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileMacroPill(
                            label = "শর্করা",
                            value = "${profile.carbsTargetG} গ্রাম",
                            subValue = "${profile.carbsTargetG * 4} kcal",
                            color = CarbsAccent,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileMacroPill(
                            label = "ফ্যাট",
                            value = "${profile.fatTargetG} গ্রাম",
                            subValue = "${profile.fatTargetG * 9} kcal",
                            color = FatAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ফাইবার লক্ষ্য: ${NutritionCalculator.fiberTargetG(profile)} গ্রাম",
                            style = MaterialTheme.typography.labelLarge,
                            color = FiberAccent
                        )
                        Text(
                            text = "পানির লক্ষ্য: ${profile.waterTargetMl} ml",
                            style = MaterialTheme.typography.labelLarge,
                            color = WaterAccent
                        )
                    }

                    val vitTargets = summary.vitaminTargets
                    Text(
                        text = "দৈনিক ভিটামিন টার্গেট: Vit A ${vitTargets.vitaminAMcg.toInt()}mcg • Vit C ${vitTargets.vitaminCMg.toInt()}mg • Vit D ${vitTargets.vitaminDMcg.toInt()}mcg • Vit E ${vitTargets.vitaminEMg.toInt()}mg • Vit B6 ${vitTargets.vitaminB6Mg}mg • Vit B12 ${vitTargets.vitaminB12Mcg}mcg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Theme Mode Selection
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("অ্যাপ থিম (Light / Dark Mode)", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onChangeTheme(mode) },
                                label = { Text(mode.labelBn) }
                            )
                        }
                    }
                }
            }
        }

        // Smart Reminders
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("স্মার্ট রিমাইন্ডার", style = MaterialTheme.typography.titleMedium)
                    }

                    ReminderRow(
                        title = "খাবার লগ রিমাইন্ডার",
                        subtitle = "দুপুর ১:০০টায় খাবার লগ করার নোটিফিকেশন",
                        checked = profile.mealReminderEnabled,
                        onCheckedChange = {
                            applyReminders(it, profile.waterReminderEnabled, profile.weightReminderEnabled)
                        }
                    )
                    ReminderRow(
                        title = "পানি পান রিমাইন্ডার",
                        subtitle = "প্রতি ৩ ঘণ্টা পর পর পানি পানের সতর্কতা",
                        checked = profile.waterReminderEnabled,
                        onCheckedChange = {
                            applyReminders(profile.mealReminderEnabled, it, profile.weightReminderEnabled)
                        }
                    )
                    ReminderRow(
                        title = "ওজন মাপার রিমাইন্ডার",
                        subtitle = "প্রতিদিন সকাল ৮:০০টায় ওজন লগ করার রিমাইন্ডার",
                        checked = profile.weightReminderEnabled,
                        onCheckedChange = {
                            applyReminders(profile.mealReminderEnabled, profile.waterReminderEnabled, it)
                        }
                    )
                }
            }
        }

        // Backup & Sign Out
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onExportCsv,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("সম্পূর্ণ ডেটা CSV ব্যাকআপ ও শেয়ার করুন")
                }

                if (isGuestMode) {
                    var authError by remember { mutableStateOf<String?>(null) }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "আপনি এখন গেস্ট মোডে আছেন",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "সব ডেটা এখন আপনার ফোনে সংরক্ষিত হচ্ছে। ক্লাউড ব্যাকআপ ও সিঙ্ক চালু করতে Google দিয়ে সাইন ইন করুন।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (authError != null) {
                                Text(
                                    text = authError!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            GoogleSignInButton(
                                onAuthSuccess = {
                                    authError = null
                                    onAuthSuccess()
                                },
                                onAuthError = { msg -> authError = msg },
                                label = "Google দিয়ে সাইন ইন করুন"
                            )
                        }
                    }
                }

                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isGuestMode) "গেস্ট মোড থেকে বের হন" else "সাইন আউট করুন")
                }
            }
        }
    }

    if (showMacroDialog) {
        var proteinInput by remember(profile) { mutableStateOf(profile.proteinTargetG.toString()) }
        var carbsInput by remember(profile) { mutableStateOf(profile.carbsTargetG.toString()) }
        var fatInput by remember(profile) { mutableStateOf(profile.fatTargetG.toString()) }
        val pVal = (proteinInput.toLongOrNull() ?: profile.proteinTargetG).coerceIn(20L, 600L)
        val cVal = (carbsInput.toLongOrNull() ?: profile.carbsTargetG).coerceIn(20L, 1000L)
        val fVal = (fatInput.toLongOrNull() ?: profile.fatTargetG).coerceIn(10L, 400L)
        val previewCal = (pVal * 4L) + (cVal * 4L) + (fVal * 9L)

        AlertDialog(
            onDismissRequest = { showMacroDialog = false },
            title = { Text("ম্যাক্রো টার্গেট সেট করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = proteinInput,
                        onValueChange = { proteinInput = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("প্রোটিন টার্গেট (গ্রাম)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = carbsInput,
                        onValueChange = { carbsInput = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("শর্করা (কার্বস) টার্গেট (গ্রাম)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fatInput,
                        onValueChange = { fatInput = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("ফ্যাট টার্গেট (গ্রাম)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "মোট হিসাবকৃত ক্যালোরি: $previewCal kcal",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = {
                            val auto = NutritionCalculator.calculatePlan(
                                age = profile.age.toInt(),
                                gender = Gender.fromRaw(profile.gender),
                                heightCm = profile.heightCm,
                                currentWeightKg = profile.currentWeightKg,
                                targetWeightKg = profile.targetWeightKg,
                                activityLevel = ActivityLevel.fromRaw(profile.activityLevel),
                                goalType = GoalType.fromRaw(profile.goalType),
                                weeklyRateKg = profile.weeklyRateKg
                            )
                            proteinInput = auto.proteinTargetG.toString()
                            carbsInput = auto.carbsTargetG.toString()
                            fatInput = auto.fatTargetG.toString()
                        }
                    ) {
                        Text("অটো বিজ্ঞানভিত্তিক হিসাব ফিরিয়ে আনুন")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateMacroTargets(pVal, cVal, fVal)
                        showMacroDialog = false
                    }
                ) { Text("সেভ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showMacroDialog = false }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProfileMacroPill(
    label: String,
    value: String,
    subValue: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = color)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subValue, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReminderRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
