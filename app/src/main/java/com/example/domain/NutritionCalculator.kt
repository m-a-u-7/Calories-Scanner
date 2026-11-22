package com.example.domain

import com.example.model.ActivityLevel
import com.example.model.ExerciseCategory
import com.example.model.ExerciseIntensity
import com.example.model.Gender
import com.example.model.GoalType
import com.example.model.MealType
import com.example.model.PresetFoodItem
import com.example.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class CalorieCalculationResult(
    val bmr: Int,
    val tdee: Int,
    val dailyCalorieTarget: Int,
    val proteinTargetG: Int,
    val carbsTargetG: Int,
    val fatTargetG: Int,
    val fiberTargetG: Int,
    val waterTargetMl: Int,
    val estimatedWeeksToGoal: Int?,
    val safetyWarning: String?
)

data class DailyVitaminTargets(
    val vitaminAMcg: Double,
    val vitaminCMg: Double,
    val vitaminDMcg: Double,
    val vitaminEMg: Double,
    val vitaminB6Mg: Double,
    val vitaminB12Mcg: Double
)

object NutritionCalculator {

    /**
     * Mifflin-St Jeor Equation for BMR
     */
    fun calculateBmr(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        gender: Gender
    ): Int {
        val base = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age)
        val offset = when (gender) {
            Gender.MALE -> 5.0
            Gender.FEMALE -> -161.0
            Gender.OTHER -> -78.0
        }
        return (base + offset).roundToInt().coerceIn(500, 6000)
    }

    fun calculateTdee(bmr: Int, activityLevel: ActivityLevel): Int {
        return (bmr * activityLevel.multiplier).roundToInt().coerceIn(600, 10000)
    }

    fun calculatePlan(
        age: Int,
        gender: Gender,
        heightCm: Double,
        currentWeightKg: Double,
        targetWeightKg: Double,
        activityLevel: ActivityLevel,
        goalType: GoalType,
        weeklyRateKg: Double
    ): CalorieCalculationResult {
        val bmr = calculateBmr(currentWeightKg, heightCm, age, gender)
        val tdee = calculateTdee(bmr, activityLevel)

        val safeRate = when (goalType) {
            GoalType.MAINTAIN -> 0.0
            else -> weeklyRateKg.coerceIn(0.25, 1.25)
        }
        // 1 kg body tissue ≈ 7700 kcal => daily delta = weeklyRateKg * 1100
        val dailyDelta = (safeRate * 1100.0).roundToInt()

        val rawTarget = when (goalType) {
            GoalType.LOSS -> tdee - dailyDelta
            GoalType.MAINTAIN -> tdee
            GoalType.GAIN -> tdee + dailyDelta
        }

        val minSafeFloor = when (gender) {
            Gender.MALE -> 1500
            Gender.FEMALE -> 1200
            Gender.OTHER -> 1350
        }

        val warning = buildString {
            if (goalType == GoalType.LOSS && rawTarget < minSafeFloor) {
                append("সতর্কতা: হিসাবকৃত ক্যালোরি ($rawTarget kcal) ন্যূনতম নিরাপদ সীমা ($minSafeFloor kcal)-এর নিচে। সুস্থ ও টেকসই ওজন কমানোর জন্য সাপ্তাহিক হার কমান বা ক্যালোরি বাড়ান।")
            } else if (goalType == GoalType.LOSS && safeRate > 0.9) {
                append("সতর্কতা: সপ্তাহে ${String.format(Locale.US, "%.2f", safeRate)} কেজি কমানো দ্রুতগতির; পেশি ক্ষয় এড়াতে ০.৫–০.৭৫ কেজি/সপ্তাহ নিরাপদ।")
            } else if (goalType == GoalType.LOSS && rawTarget < bmr) {
                append("পরামর্শ: আপনার দৈনিক টার্গেট BMR ($bmr kcal)-এর নিচে নেমে গেছে; পর্যাপ্ত প্রোটিন ও মাইক্রোনিউট্রিয়েন্ট নিশ্চিত করুন।")
            }
        }.ifBlank { null }

        val targetCalories = rawTarget.coerceIn(800, 10000)

        // Macro distribution ratios
        val (proteinPct, carbsPct, fatPct) = when (goalType) {
            GoalType.LOSS -> Triple(0.30, 0.40, 0.30)
            GoalType.MAINTAIN -> Triple(0.25, 0.45, 0.30)
            GoalType.GAIN -> Triple(0.25, 0.50, 0.25)
        }

        val proteinG = ((targetCalories * proteinPct) / 4.0).roundToInt().coerceIn(20, 600)
        val carbsG = ((targetCalories * carbsPct) / 4.0).roundToInt().coerceIn(20, 1000)
        val fatG = ((targetCalories * fatPct) / 9.0).roundToInt().coerceIn(10, 400)
        val fiberG = ((targetCalories / 1000.0) * 14.0).roundToInt().coerceAtLeast(25)
        val waterMl = (currentWeightKg * 35.0).roundToInt().coerceIn(2000, 4500)

        val diffKg = abs(currentWeightKg - targetWeightKg)
        val estWeeks = if (goalType != GoalType.MAINTAIN && safeRate > 0.05 && diffKg > 0.1) {
            (diffKg / safeRate).roundToInt().coerceAtLeast(1)
        } else null

        return CalorieCalculationResult(
            bmr = bmr,
            tdee = tdee,
            dailyCalorieTarget = targetCalories,
            proteinTargetG = proteinG,
            carbsTargetG = carbsG,
            fatTargetG = fatG,
            fiberTargetG = fiberG,
            waterTargetMl = waterMl,
            estimatedWeeksToGoal = estWeeks,
            safetyWarning = warning
        )
    }

    fun calculateCaloriesBurned(
        category: ExerciseCategory,
        intensity: ExerciseIntensity,
        weightKg: Double,
        durationMinutes: Int
    ): Int {
        val met = when (intensity) {
            ExerciseIntensity.LIGHT -> category.metLight
            ExerciseIntensity.MODERATE -> category.metModerate
            ExerciseIntensity.VIGOROUS -> category.metVigorous
        }
        return (met * weightKg * (durationMinutes / 60.0)).roundToInt().coerceIn(1, 10000)
    }

    fun mealBudgetCalories(profile: UserProfile, mealType: MealType): Int {
        return (profile.dailyCalorieTarget * mealType.targetShare).roundToInt()
    }

    fun fiberTargetG(profile: UserProfile): Int {
        return ((profile.dailyCalorieTarget / 1000.0) * 14.0).roundToInt().coerceAtLeast(25)
    }

    fun calculateVitaminTargets(age: Int, gender: Gender): DailyVitaminTargets {
        val vitA = when (gender) {
            Gender.MALE -> 900.0
            Gender.FEMALE -> 700.0
            Gender.OTHER -> 800.0
        }
        val vitC = when (gender) {
            Gender.MALE -> 90.0
            Gender.FEMALE -> 75.0
            Gender.OTHER -> 85.0
        }
        val vitD = if (age > 70) 20.0 else 15.0
        val vitE = 15.0
        val vitB6 = when {
            age >= 51 && gender == Gender.MALE -> 1.7
            age >= 51 && gender == Gender.FEMALE -> 1.5
            age >= 51 -> 1.6
            else -> 1.3
        }
        val vitB12 = 2.4
        return DailyVitaminTargets(
            vitaminAMcg = vitA,
            vitaminCMg = vitC,
            vitaminDMcg = vitD,
            vitaminEMg = vitE,
            vitaminB6Mg = vitB6,
            vitaminB12Mcg = vitB12
        )
    }

    fun buildProfileWithCalculations(
        existing: UserProfile,
        userId: String,
        displayName: String,
        age: Int,
        gender: Gender,
        heightCm: Double,
        currentWeightKg: Double,
        targetWeightKg: Double,
        activityLevel: ActivityLevel,
        goalType: GoalType,
        weeklyRateKg: Double,
        onboardingDone: Boolean = true,
        customProteinTargetG: Long? = null,
        customCarbsTargetG: Long? = null,
        customFatTargetG: Long? = null
    ): UserProfile {
        val calc = calculatePlan(
            age = age,
            gender = gender,
            heightCm = heightCm,
            currentWeightKg = currentWeightKg,
            targetWeightKg = targetWeightKg,
            activityLevel = activityLevel,
            goalType = goalType,
            weeklyRateKg = weeklyRateKg
        )
        val finalProtein = customProteinTargetG?.coerceIn(20L, 600L) ?: calc.proteinTargetG.toLong()
        val finalCarbs = customCarbsTargetG?.coerceIn(20L, 1000L) ?: calc.carbsTargetG.toLong()
        val finalFat = customFatTargetG?.coerceIn(10L, 400L) ?: calc.fatTargetG.toLong()
        val finalCalories = if (customProteinTargetG != null && customCarbsTargetG != null && customFatTargetG != null) {
            ((finalProtein * 4L) + (finalCarbs * 4L) + (finalFat * 9L)).coerceIn(800L, 10000L)
        } else {
            calc.dailyCalorieTarget.toLong()
        }
        return existing.copy(
            userId = userId,
            displayName = displayName.trim().take(80),
            age = age.toLong(),
            gender = gender.name,
            heightCm = heightCm,
            currentWeightKg = currentWeightKg,
            targetWeightKg = targetWeightKg,
            activityLevel = activityLevel.name,
            goalType = goalType.name,
            weeklyRateKg = if (goalType == GoalType.MAINTAIN) 0.0 else weeklyRateKg,
            bmr = calc.bmr.toLong(),
            tdee = calc.tdee.toLong(),
            dailyCalorieTarget = finalCalories,
            proteinTargetG = finalProtein,
            carbsTargetG = finalCarbs,
            fatTargetG = finalFat,
            waterTargetMl = calc.waterTargetMl.toLong(),
            onboardingDone = onboardingDone
        )
    }
}

object DateUtils {
    private val keyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormatter = SimpleDateFormat("dd MMM, EEE", Locale("bn", "BD"))
    private val shortFormatter = SimpleDateFormat("dd/MM", Locale.US)

    fun todayDateKey(): String = keyFormatter.format(Calendar.getInstance().time)

    fun shiftDateKey(dateKey: String, deltaDays: Int): String {
        val cal = Calendar.getInstance()
        val parsed = runCatching { keyFormatter.parse(dateKey) }.getOrNull()
        if (parsed != null) cal.time = parsed
        cal.add(Calendar.DAY_OF_YEAR, deltaDays)
        return keyFormatter.format(cal.time)
    }

    fun lastNDaysKeys(days: Int, endDateKey: String = todayDateKey()): List<String> {
        val cal = Calendar.getInstance()
        val parsed = runCatching { keyFormatter.parse(endDateKey) }.getOrNull()
        if (parsed != null) cal.time = parsed
        val result = mutableListOf<String>()
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1))
        repeat(days) {
            result.add(keyFormatter.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    fun formatReadableDate(dateKey: String): String {
        if (dateKey == todayDateKey()) return "আজ • $dateKey"
        if (dateKey == shiftDateKey(todayDateKey(), -1)) return "গতকাল • $dateKey"
        val parsed = runCatching { keyFormatter.parse(dateKey) }.getOrNull() ?: return dateKey
        return displayFormatter.format(parsed)
    }

    fun formatShortChartLabel(dateKey: String): String {
        val parsed = runCatching { keyFormatter.parse(dateKey) }.getOrNull() ?: return dateKey
        return shortFormatter.format(parsed)
    }
}

object CommonFoodCatalog {
    val items: List<PresetFoodItem> = listOf(
        PresetFoodItem("সাদা ভাত (রান্না)", 1.0, "কাপ", 160.0, 206, 4.3, 44.5, 0.4, 0.6, 0.0, 0.0, 0.0, 0.1, 0.15, 0.0),
        PresetFoodItem("লাল চালের ভাত", 1.0, "কাপ", 160.0, 195, 4.5, 41.0, 1.5, 2.8, 0.0, 0.0, 0.0, 0.4, 0.25, 0.0),
        PresetFoodItem("আটার রুটি", 1.0, "পিস", 40.0, 105, 3.5, 20.0, 1.0, 2.5, 0.0, 0.0, 0.0, 0.3, 0.09, 0.0),
        PresetFoodItem("পরোটা", 1.0, "পিস", 70.0, 235, 4.5, 30.0, 10.5, 1.8, 12.0, 0.0, 0.0, 1.4, 0.08, 0.0),
        PresetFoodItem("মসুর ডাল", 1.0, "বাটি", 150.0, 145, 9.2, 22.0, 2.2, 4.8, 4.0, 1.8, 0.0, 0.4, 0.22, 0.0),
        PresetFoodItem("ভুনা খিচুড়ি", 1.0, "প্লেট", 250.0, 410, 12.0, 62.0, 12.5, 4.2, 25.0, 3.0, 0.0, 1.5, 0.28, 0.0),
        PresetFoodItem("চিকেন বিরিয়ানি", 1.0, "প্লেট", 300.0, 560, 26.0, 64.0, 21.0, 2.5, 38.0, 4.5, 0.3, 2.1, 0.48, 0.6),
        PresetFoodItem("সিদ্ধ ডিম", 1.0, "পিস", 50.0, 78, 6.3, 0.6, 5.3, 0.0, 75.0, 0.0, 1.1, 0.5, 0.07, 0.55),
        PresetFoodItem("ডিম ভাজি / অমলেট", 1.0, "পিস", 60.0, 125, 6.5, 1.0, 10.2, 0.0, 82.0, 0.5, 1.1, 1.3, 0.08, 0.55),
        PresetFoodItem("রুই মাছের ঝোল", 1.0, "পিস", 100.0, 165, 19.5, 3.0, 8.2, 0.5, 35.0, 2.5, 4.2, 1.2, 0.32, 2.4),
        PresetFoodItem("ইলিশ মাছ ভাজা/ভুনা", 1.0, "পিস", 100.0, 260, 21.0, 2.0, 18.5, 0.2, 48.0, 1.5, 6.5, 1.8, 0.35, 3.2),
        PresetFoodItem("মুরগির ঝোল (স্কিন ছাড়া)", 1.0, "বাটি", 150.0, 230, 27.0, 4.5, 11.0, 0.8, 22.0, 3.0, 0.2, 1.1, 0.55, 0.45),
        PresetFoodItem("গ্রিল্ড চিকেন ব্রেস্ট", 100.0, "গ্রাম", 100.0, 165, 31.0, 0.0, 3.6, 0.0, 9.0, 0.0, 0.1, 0.4, 0.60, 0.35),
        PresetFoodItem("গরুর মাংস ভুনা", 1.0, "বাটি", 120.0, 295, 24.0, 5.0, 19.5, 0.6, 15.0, 2.0, 0.2, 1.4, 0.42, 2.6),
        PresetFoodItem("মিক্সড সবজি ভাজি", 1.0, "বাটি", 140.0, 115, 3.5, 14.0, 5.0, 4.5, 185.0, 28.0, 0.0, 1.6, 0.24, 0.0),
        PresetFoodItem("শাক ভাজি (পালং/লাল)", 1.0, "বাটি", 120.0, 78, 3.8, 6.5, 4.2, 3.6, 390.0, 24.0, 0.0, 2.4, 0.22, 0.0),
        PresetFoodItem("আলু ভর্তা", 1.0, "সার্ভিং", 80.0, 112, 2.0, 17.5, 3.8, 1.8, 4.0, 11.0, 0.0, 0.9, 0.21, 0.0),
        PresetFoodItem("শসা ও টমেটো সালাদ", 1.0, "বাটি", 150.0, 34, 1.4, 7.0, 0.3, 2.2, 52.0, 18.0, 0.0, 0.5, 0.10, 0.0),
        PresetFoodItem("কলা (মাঝারি)", 1.0, "পিস", 118.0, 105, 1.3, 27.0, 0.4, 3.1, 4.0, 10.3, 0.0, 0.1, 0.43, 0.0),
        PresetFoodItem("আপেল", 1.0, "পিস", 180.0, 95, 0.5, 25.0, 0.3, 4.4, 5.0, 8.4, 0.0, 0.3, 0.07, 0.0),
        PresetFoodItem("পেয়ারা", 1.0, "পিস", 120.0, 82, 3.0, 17.0, 1.1, 6.5, 37.0, 228.0, 0.0, 0.9, 0.13, 0.0),
        PresetFoodItem("টক দই", 1.0, "কাপ", 150.0, 92, 6.5, 7.0, 4.2, 0.0, 45.0, 1.0, 0.8, 0.1, 0.07, 0.75),
        PresetFoodItem("দুধ (লো-ফ্যাট)", 1.0, "গ্লাস", 250.0, 110, 8.5, 12.0, 2.5, 0.0, 145.0, 0.0, 2.5, 0.1, 0.10, 1.2),
        PresetFoodItem("ওটস (দুধ ছাড়া)", 1.0, "কাপ", 45.0, 170, 6.0, 29.0, 3.2, 4.5, 0.0, 0.0, 0.0, 0.3, 0.06, 0.0),
        PresetFoodItem("চিড়া ও কলা", 1.0, "বাটি", 150.0, 245, 4.2, 54.0, 1.2, 3.4, 5.0, 10.0, 0.0, 0.2, 0.44, 0.0),
        PresetFoodItem("মুড়ি (চানাচুর ছাড়া)", 1.0, "বাটি", 30.0, 112, 2.2, 24.5, 0.3, 0.8, 0.0, 0.0, 0.0, 0.1, 0.04, 0.0),
        PresetFoodItem("বাদাম (আমন্ড/চিনাবাদাম)", 25.0, "গ্রাম", 25.0, 148, 5.8, 4.5, 12.5, 2.5, 0.0, 0.0, 0.0, 5.2, 0.06, 0.0),
        PresetFoodItem("চিনি ছাড়া চা / ব্ল্যাক কফি", 1.0, "কাপ", 200.0, 5, 0.2, 0.8, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        PresetFoodItem("দুধ চা (হালকা চিনি)", 1.0, "কাপ", 180.0, 75, 2.2, 10.5, 2.4, 0.0, 25.0, 0.0, 0.3, 0.05, 0.02, 0.2),
        PresetFoodItem("খেজুর", 2.0, "পিস", 16.0, 46, 0.4, 12.0, 0.1, 1.2, 1.0, 0.0, 0.0, 0.0, 0.04, 0.0)
    )
}
