package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class Gender(val labelBn: String) {
    MALE("পুরুষ"),
    FEMALE("নারী"),
    OTHER("অন্যান্য");

    companion object {
        fun fromRaw(raw: String?): Gender =
            entries.firstOrNull { it.name == raw } ?: MALE
    }
}

enum class ActivityLevel(
    val labelBn: String,
    val descriptionBn: String,
    val multiplier: Double
) {
    SEDENTARY("বসে থাকা জীবনযাপন", "ব্যায়াম নেই বা খুব সামান্য", 1.2),
    LIGHTLY_ACTIVE("হালকা সক্রিয়", "সপ্তাহে ১–৩ দিন হালকা ব্যায়াম", 1.375),
    MODERATELY_ACTIVE("মাঝারি সক্রিয়", "সপ্তাহে ৩–৫ দিন মাঝারি ব্যায়াম", 1.55),
    VERY_ACTIVE("অত্যন্ত সক্রিয়", "সপ্তাহে ৬–৭ দিন ভারী ব্যায়াম বা খেলাধুলা", 1.725);

    companion object {
        fun fromRaw(raw: String?): ActivityLevel =
            entries.firstOrNull { it.name == raw } ?: MODERATELY_ACTIVE
    }
}

enum class GoalType(val labelBn: String, val subtitleBn: String) {
    LOSS("ওজন কমানো", "নিরাপদ ক্যালোরি ঘাটতি"),
    MAINTAIN("ওজন ধরে রাখা", "ভারসাম্যপূর্ণ দৈনিক শক্তি"),
    GAIN("ওজন বাড়ানো", "স্বাস্থ্যকর পেশি ও ওজন বৃদ্ধি");

    companion object {
        fun fromRaw(raw: String?): GoalType =
            entries.firstOrNull { it.name == raw } ?: LOSS
    }
}

enum class MealType(val labelBn: String, val targetShare: Double) {
    BREAKFAST("সকালের নাস্তা", 0.25),
    LUNCH("দুপুরের খাবার", 0.35),
    DINNER("রাতের খাবার", 0.30),
    SNACKS("হালকা নাস্তা", 0.10);

    companion object {
        fun fromRaw(raw: String?): MealType =
            entries.firstOrNull { it.name == raw } ?: LUNCH
    }
}

enum class FoodSource(val labelBn: String) {
    AI_CAMERA("AI স্ক্যান"),
    AI_TEXT("AI টেক্সট"),
    MANUAL("ম্যানুয়াল"),
    FAVORITE("ফেভারিট");

    companion object {
        fun fromRaw(raw: String?): FoodSource =
            entries.firstOrNull { it.name == raw } ?: MANUAL
    }
}

enum class ExerciseCategory(val labelBn: String, val metLight: Double, val metModerate: Double, val metVigorous: Double) {
    WALKING("হাঁটা", 3.2, 4.3, 5.2),
    RUNNING("দৌড়ানো", 7.0, 9.8, 11.5),
    CYCLING("সাইক্লিং", 4.0, 6.8, 10.0),
    SWIMMING("সাঁতার", 5.8, 7.0, 9.8),
    GYM("জিম / ওয়েটস", 3.5, 5.0, 6.5),
    BODYWEIGHT("বডিওয়েট / HIIT", 4.5, 8.0, 10.0),
    YOGA("যোগব্যায়াম", 2.5, 3.0, 4.0),
    CHORES("দৈনন্দিন কাজ", 2.5, 3.3, 4.0),
    CUSTOM("অন্যান্য ব্যায়াম", 3.5, 5.0, 7.0);

    companion object {
        fun fromRaw(raw: String?): ExerciseCategory =
            entries.firstOrNull { it.name == raw } ?: WALKING
    }
}

enum class ExerciseIntensity(val labelBn: String) {
    LIGHT("হালকা"),
    MODERATE("মাঝারি"),
    VIGOROUS("তীব্র");

    companion object {
        fun fromRaw(raw: String?): ExerciseIntensity =
            entries.firstOrNull { it.name == raw } ?: MODERATE
    }
}

enum class ThemeMode(val labelBn: String) {
    SYSTEM("সিস্টেম"),
    LIGHT("লাইট মোড"),
    DARK("ডার্ক মোড")
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val age: Long = 26L,
    val gender: String = Gender.MALE.name,
    val heightCm: Double = 170.0,
    val currentWeightKg: Double = 72.0,
    val targetWeightKg: Double = 65.0,
    val activityLevel: String = ActivityLevel.MODERATELY_ACTIVE.name,
    val goalType: String = GoalType.LOSS.name,
    val weeklyRateKg: Double = 0.5,
    val bmr: Long = 1650L,
    val tdee: Long = 2558L,
    val dailyCalorieTarget: Long = 2008L,
    val proteinTargetG: Long = 151L,
    val carbsTargetG: Long = 201L,
    val fatTargetG: Long = 67L,
    val waterTargetMl: Long = 2520L,
    val mealReminderEnabled: Boolean = false,
    val waterReminderEnabled: Boolean = false,
    val weightReminderEnabled: Boolean = false,
    val onboardingDone: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FoodLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val mealType: String = MealType.LUNCH.name,
    val name: String = "",
    val quantity: Double = 1.0,
    val unit: String = "সার্ভিং",
    val grams: Double = 100.0,
    val calories: Long = 0L,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val vitaminAMcg: Double = 0.0,
    val vitaminCMg: Double = 0.0,
    val vitaminDMcg: Double = 0.0,
    val vitaminEMg: Double = 0.0,
    val vitaminB6Mg: Double = 0.0,
    val vitaminB12Mcg: Double = 0.0,
    val source: String = FoodSource.MANUAL.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExerciseLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val activityName: String = "",
    val category: String = ExerciseCategory.WALKING.name,
    val intensity: String = ExerciseIntensity.MODERATE.name,
    val durationMinutes: Long = 30L,
    val caloriesBurned: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WaterLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val amountMl: Long = 0L,
    val targetMl: Long = 2500L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WeightLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val weightKg: Double = 70.0,
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FavoriteFood(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val defaultQuantity: Double = 1.0,
    val unit: String = "সার্ভিং",
    val grams: Double = 100.0,
    val calories: Long = 0L,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val vitaminAMcg: Double = 0.0,
    val vitaminCMg: Double = 0.0,
    val vitaminDMcg: Double = 0.0,
    val vitaminEMg: Double = 0.0,
    val vitaminB6Mg: Double = 0.0,
    val vitaminB12Mcg: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AnalyzedFoodItem(
    val id: String,
    val name: String,
    val baseQuantity: Double,
    val quantity: Double,
    val unit: String,
    val baseGrams: Double,
    val baseCalories: Int,
    val baseProteinG: Double,
    val baseCarbsG: Double,
    val baseFatG: Double,
    val baseFiberG: Double,
    val baseVitaminAMcg: Double = 0.0,
    val baseVitaminCMg: Double = 0.0,
    val baseVitaminDMcg: Double = 0.0,
    val baseVitaminEMg: Double = 0.0,
    val baseVitaminB6Mg: Double = 0.0,
    val baseVitaminB12Mcg: Double = 0.0,
    val confidence: String = "High"
) {
    private val multiplier: Double
        get() = if (baseQuantity > 0.0) quantity / baseQuantity else 1.0

    val scaledGrams: Double get() = (baseGrams * multiplier).coerceAtLeast(0.0)
    val scaledCalories: Int get() = (baseCalories * multiplier).toInt().coerceIn(0, 15000)
    val scaledProteinG: Double get() = (baseProteinG * multiplier).coerceIn(0.0, 1000.0)
    val scaledCarbsG: Double get() = (baseCarbsG * multiplier).coerceIn(0.0, 1500.0)
    val scaledFatG: Double get() = (baseFatG * multiplier).coerceIn(0.0, 1000.0)
    val scaledFiberG: Double get() = (baseFiberG * multiplier).coerceIn(0.0, 500.0)
    val scaledVitaminAMcg: Double get() = (baseVitaminAMcg * multiplier).coerceIn(0.0, 50000.0)
    val scaledVitaminCMg: Double get() = (baseVitaminCMg * multiplier).coerceIn(0.0, 10000.0)
    val scaledVitaminDMcg: Double get() = (baseVitaminDMcg * multiplier).coerceIn(0.0, 2000.0)
    val scaledVitaminEMg: Double get() = (baseVitaminEMg * multiplier).coerceIn(0.0, 2000.0)
    val scaledVitaminB6Mg: Double get() = (baseVitaminB6Mg * multiplier).coerceIn(0.0, 500.0)
    val scaledVitaminB12Mcg: Double get() = (baseVitaminB12Mcg * multiplier).coerceIn(0.0, 5000.0)
}

data class AnalyzedFoodResult(
    val mealTitle: String,
    val items: List<AnalyzedFoodItem>,
    val nutritionNote: String,
    val source: FoodSource
)

data class PresetFoodItem(
    val name: String,
    val defaultQuantity: Double,
    val unit: String,
    val grams: Double,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val vitaminAMcg: Double = 0.0,
    val vitaminCMg: Double = 0.0,
    val vitaminDMcg: Double = 0.0,
    val vitaminEMg: Double = 0.0,
    val vitaminB6Mg: Double = 0.0,
    val vitaminB12Mcg: Double = 0.0
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
