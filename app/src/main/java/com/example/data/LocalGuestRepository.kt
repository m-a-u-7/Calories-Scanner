package com.example.data

import android.content.Context
import com.example.model.ExerciseLog
import com.example.model.FavoriteFood
import com.example.model.FoodLog
import com.example.model.UserProfile
import com.example.model.WaterLog
import com.example.model.WeightLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Device-local persistence used strictly when the user chooses to skip login (Guest Mode).
 * Keeps zero dependency on Firestore or FirebaseAuth so no unauthenticated Firestore queries ever run.
 */
class LocalGuestRepository(context: Context) : NutritionDataSource {

    private val storeFile = File(context.applicationContext.filesDir, "nutripulse_guest_store.json")
    private val mutex = Mutex()

    private val profileState = MutableStateFlow<UserProfile?>(null)
    private val foodLogsState = MutableStateFlow<List<FoodLog>>(emptyList())
    private val exerciseLogsState = MutableStateFlow<List<ExerciseLog>>(emptyList())
    private val waterLogsState = MutableStateFlow<List<WaterLog>>(emptyList())
    private val weightLogsState = MutableStateFlow<List<WeightLog>>(emptyList())
    private val favoritesState = MutableStateFlow<List<FavoriteFood>>(emptyList())

    init {
        loadFromDisk()
    }

    override fun observeUserProfile(userId: String): Flow<UserProfile?> = profileState.asStateFlow()
    override fun observeFoodLogs(userId: String): Flow<List<FoodLog>> = foodLogsState.asStateFlow()
    override fun observeExerciseLogs(userId: String): Flow<List<ExerciseLog>> = exerciseLogsState.asStateFlow()
    override fun observeWaterLogs(userId: String): Flow<List<WaterLog>> = waterLogsState.asStateFlow()
    override fun observeWeightLogs(userId: String): Flow<List<WeightLog>> = weightLogsState.asStateFlow()
    override fun observeFavoriteFoods(userId: String): Flow<List<FavoriteFood>> = favoritesState.asStateFlow()

    override suspend fun saveUserProfile(userId: String, profile: UserProfile): Result<Unit> =
        mutex.withLock {
            runCatching {
                profileState.value = profile.copy(id = userId, userId = userId)
                persistToDisk()
            }
        }

    override suspend fun updateReminderSettings(
        userId: String,
        mealReminder: Boolean,
        waterReminder: Boolean,
        weightReminder: Boolean
    ): Result<Unit> = mutex.withLock {
        runCatching {
            val current = profileState.value ?: UserProfile(id = userId, userId = userId)
            profileState.value = current.copy(
                mealReminderEnabled = mealReminder,
                waterReminderEnabled = waterReminder,
                weightReminderEnabled = weightReminder
            )
            persistToDisk()
        }
    }

    override suspend fun addFoodLogs(userId: String, items: List<FoodLog>): Result<Unit> =
        mutex.withLock {
            runCatching {
                val created = items.map {
                    it.copy(id = UUID.randomUUID().toString(), userId = userId)
                }
                foodLogsState.value = foodLogsState.value + created
                persistToDisk()
            }
        }

    override suspend fun updateFoodLog(userId: String, item: FoodLog): Result<Unit> =
        mutex.withLock {
            runCatching {
                foodLogsState.value = foodLogsState.value.map {
                    if (it.id == item.id) item else it
                }
                persistToDisk()
            }
        }

    override suspend fun deleteFoodLog(userId: String, logId: String): Result<Unit> =
        mutex.withLock {
            runCatching {
                foodLogsState.value = foodLogsState.value.filterNot { it.id == logId }
                persistToDisk()
            }
        }

    override suspend fun addExerciseLog(userId: String, item: ExerciseLog): Result<Unit> =
        mutex.withLock {
            runCatching {
                val created = item.copy(id = UUID.randomUUID().toString(), userId = userId)
                exerciseLogsState.value = exerciseLogsState.value + created
                persistToDisk()
            }
        }

    override suspend fun deleteExerciseLog(userId: String, exerciseId: String): Result<Unit> =
        mutex.withLock {
            runCatching {
                exerciseLogsState.value = exerciseLogsState.value.filterNot { it.id == exerciseId }
                persistToDisk()
            }
        }

    override suspend fun setWaterIntake(
        userId: String,
        dateKey: String,
        newAmountMl: Long,
        targetMl: Long
    ): Result<Unit> = mutex.withLock {
        runCatching {
            val entry = WaterLog(
                id = dateKey,
                userId = userId,
                dateKey = dateKey,
                amountMl = newAmountMl.coerceIn(0L, 20000L),
                targetMl = targetMl.coerceIn(500L, 10000L)
            )
            val updated = waterLogsState.value.filterNot { it.dateKey == dateKey } + entry
            waterLogsState.value = updated
            persistToDisk()
        }
    }

    override suspend fun addWeightLog(
        userId: String,
        dateKey: String,
        weightKg: Double,
        note: String?,
        updatedProfile: UserProfile?
    ): Result<Unit> = mutex.withLock {
        runCatching {
            val entry = WeightLog(
                id = UUID.randomUUID().toString(),
                userId = userId,
                dateKey = dateKey,
                weightKg = weightKg.coerceIn(25.0, 300.0),
                note = note?.trim()?.take(200)?.ifBlank { null }
            )
            weightLogsState.value = weightLogsState.value + entry
            if (updatedProfile != null) {
                profileState.value = updatedProfile.copy(id = userId, userId = userId)
            }
            persistToDisk()
        }
    }

    override suspend fun addFavoriteFood(userId: String, fav: FavoriteFood): Result<Unit> =
        mutex.withLock {
            runCatching {
                val entry = fav.copy(id = UUID.randomUUID().toString(), userId = userId)
                favoritesState.value = favoritesState.value + entry
                persistToDisk()
            }
        }

    override suspend fun deleteFavoriteFood(userId: String, favId: String): Result<Unit> =
        mutex.withLock {
            runCatching {
                favoritesState.value = favoritesState.value.filterNot { it.id == favId }
                persistToDisk()
            }
        }

    private fun loadFromDisk() {
        runCatching {
            if (!storeFile.exists()) return
            val root = JSONObject(storeFile.readText(Charsets.UTF_8))

            root.optJSONObject("profile")?.let { p ->
                profileState.value = UserProfile(
                    id = p.optString("userId", "guest"),
                    userId = p.optString("userId", "guest"),
                    displayName = p.optString("displayName", "গেস্ট"),
                    age = p.optLong("age", 26L),
                    gender = p.optString("gender", "MALE"),
                    heightCm = p.optDouble("heightCm", 170.0),
                    currentWeightKg = p.optDouble("currentWeightKg", 72.0),
                    targetWeightKg = p.optDouble("targetWeightKg", 65.0),
                    activityLevel = p.optString("activityLevel", "MODERATELY_ACTIVE"),
                    goalType = p.optString("goalType", "LOSS"),
                    weeklyRateKg = p.optDouble("weeklyRateKg", 0.5),
                    bmr = p.optLong("bmr", 1650L),
                    tdee = p.optLong("tdee", 2558L),
                    dailyCalorieTarget = p.optLong("dailyCalorieTarget", 2008L),
                    proteinTargetG = p.optLong("proteinTargetG", 151L),
                    carbsTargetG = p.optLong("carbsTargetG", 201L),
                    fatTargetG = p.optLong("fatTargetG", 67L),
                    waterTargetMl = p.optLong("waterTargetMl", 2520L),
                    mealReminderEnabled = p.optBoolean("mealReminderEnabled", false),
                    waterReminderEnabled = p.optBoolean("waterReminderEnabled", false),
                    weightReminderEnabled = p.optBoolean("weightReminderEnabled", false),
                    onboardingDone = p.optBoolean("onboardingDone", false)
                )
            }

            root.optJSONArray("food_logs")?.let { arr ->
                foodLogsState.value = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    FoodLog(
                        id = o.optString("id"),
                        userId = o.optString("userId"),
                        dateKey = o.optString("dateKey"),
                        mealType = o.optString("mealType"),
                        name = o.optString("name"),
                        quantity = o.optDouble("quantity", 1.0),
                        unit = o.optString("unit", "সার্ভিং"),
                        grams = o.optDouble("grams", 100.0),
                        calories = o.optLong("calories", 0L),
                        proteinG = o.optDouble("proteinG", 0.0),
                        carbsG = o.optDouble("carbsG", 0.0),
                        fatG = o.optDouble("fatG", 0.0),
                        fiberG = o.optDouble("fiberG", 0.0),
                        vitaminAMcg = o.optDouble("vitaminAMcg", 0.0),
                        vitaminCMg = o.optDouble("vitaminCMg", 0.0),
                        vitaminDMcg = o.optDouble("vitaminDMcg", 0.0),
                        vitaminEMg = o.optDouble("vitaminEMg", 0.0),
                        vitaminB6Mg = o.optDouble("vitaminB6Mg", 0.0),
                        vitaminB12Mcg = o.optDouble("vitaminB12Mcg", 0.0),
                        source = o.optString("source", "MANUAL")
                    )
                }
            }

            root.optJSONArray("exercise_logs")?.let { arr ->
                exerciseLogsState.value = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    ExerciseLog(
                        id = o.optString("id"),
                        userId = o.optString("userId"),
                        dateKey = o.optString("dateKey"),
                        activityName = o.optString("activityName"),
                        category = o.optString("category"),
                        intensity = o.optString("intensity"),
                        durationMinutes = o.optLong("durationMinutes", 30L),
                        caloriesBurned = o.optLong("caloriesBurned", 0L)
                    )
                }
            }

            root.optJSONArray("water_logs")?.let { arr ->
                waterLogsState.value = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    WaterLog(
                        id = o.optString("id"),
                        userId = o.optString("userId"),
                        dateKey = o.optString("dateKey"),
                        amountMl = o.optLong("amountMl", 0L),
                        targetMl = o.optLong("targetMl", 2500L)
                    )
                }
            }

            root.optJSONArray("weight_logs")?.let { arr ->
                weightLogsState.value = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    WeightLog(
                        id = o.optString("id"),
                        userId = o.optString("userId"),
                        dateKey = o.optString("dateKey"),
                        weightKg = o.optDouble("weightKg", 70.0),
                        note = o.optString("note", "").ifBlank { null }
                    )
                }
            }

            root.optJSONArray("favorite_foods")?.let { arr ->
                favoritesState.value = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    FavoriteFood(
                        id = o.optString("id"),
                        userId = o.optString("userId"),
                        name = o.optString("name"),
                        defaultQuantity = o.optDouble("defaultQuantity", 1.0),
                        unit = o.optString("unit", "সার্ভিং"),
                        grams = o.optDouble("grams", 100.0),
                        calories = o.optLong("calories", 0L),
                        proteinG = o.optDouble("proteinG", 0.0),
                        carbsG = o.optDouble("carbsG", 0.0),
                        fatG = o.optDouble("fatG", 0.0),
                        fiberG = o.optDouble("fiberG", 0.0),
                        vitaminAMcg = o.optDouble("vitaminAMcg", 0.0),
                        vitaminCMg = o.optDouble("vitaminCMg", 0.0),
                        vitaminDMcg = o.optDouble("vitaminDMcg", 0.0),
                        vitaminEMg = o.optDouble("vitaminEMg", 0.0),
                        vitaminB6Mg = o.optDouble("vitaminB6Mg", 0.0),
                        vitaminB12Mcg = o.optDouble("vitaminB12Mcg", 0.0)
                    )
                }
            }
        }
    }

    private suspend fun persistToDisk() {
        withContext(Dispatchers.IO) {
            val root = JSONObject()
            profileState.value?.let { p ->
                root.put("profile", JSONObject().apply {
                    put("userId", p.userId)
                    put("displayName", p.displayName)
                    put("age", p.age)
                    put("gender", p.gender)
                    put("heightCm", p.heightCm)
                    put("currentWeightKg", p.currentWeightKg)
                    put("targetWeightKg", p.targetWeightKg)
                    put("activityLevel", p.activityLevel)
                    put("goalType", p.goalType)
                    put("weeklyRateKg", p.weeklyRateKg)
                    put("bmr", p.bmr)
                    put("tdee", p.tdee)
                    put("dailyCalorieTarget", p.dailyCalorieTarget)
                    put("proteinTargetG", p.proteinTargetG)
                    put("carbsTargetG", p.carbsTargetG)
                    put("fatTargetG", p.fatTargetG)
                    put("waterTargetMl", p.waterTargetMl)
                    put("mealReminderEnabled", p.mealReminderEnabled)
                    put("waterReminderEnabled", p.waterReminderEnabled)
                    put("weightReminderEnabled", p.weightReminderEnabled)
                    put("onboardingDone", p.onboardingDone)
                })
            }

            root.put("food_logs", JSONArray().apply {
                foodLogsState.value.forEach { f ->
                    put(JSONObject().apply {
                        put("id", f.id)
                        put("userId", f.userId)
                        put("dateKey", f.dateKey)
                        put("mealType", f.mealType)
                        put("name", f.name)
                        put("quantity", f.quantity)
                        put("unit", f.unit)
                        put("grams", f.grams)
                        put("calories", f.calories)
                        put("proteinG", f.proteinG)
                        put("carbsG", f.carbsG)
                        put("fatG", f.fatG)
                        put("fiberG", f.fiberG)
                        put("vitaminAMcg", f.vitaminAMcg)
                        put("vitaminCMg", f.vitaminCMg)
                        put("vitaminDMcg", f.vitaminDMcg)
                        put("vitaminEMg", f.vitaminEMg)
                        put("vitaminB6Mg", f.vitaminB6Mg)
                        put("vitaminB12Mcg", f.vitaminB12Mcg)
                        put("source", f.source)
                    })
                }
            })

            root.put("exercise_logs", JSONArray().apply {
                exerciseLogsState.value.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("userId", e.userId)
                        put("dateKey", e.dateKey)
                        put("activityName", e.activityName)
                        put("category", e.category)
                        put("intensity", e.intensity)
                        put("durationMinutes", e.durationMinutes)
                        put("caloriesBurned", e.caloriesBurned)
                    })
                }
            })

            root.put("water_logs", JSONArray().apply {
                waterLogsState.value.forEach { w ->
                    put(JSONObject().apply {
                        put("id", w.id)
                        put("userId", w.userId)
                        put("dateKey", w.dateKey)
                        put("amountMl", w.amountMl)
                        put("targetMl", w.targetMl)
                    })
                }
            })

            root.put("weight_logs", JSONArray().apply {
                weightLogsState.value.forEach { wt ->
                    put(JSONObject().apply {
                        put("id", wt.id)
                        put("userId", wt.userId)
                        put("dateKey", wt.dateKey)
                        put("weightKg", wt.weightKg)
                        if (wt.note != null) put("note", wt.note)
                    })
                }
            })

            root.put("favorite_foods", JSONArray().apply {
                favoritesState.value.forEach { fav ->
                    put(JSONObject().apply {
                        put("id", fav.id)
                        put("userId", fav.userId)
                        put("name", fav.name)
                        put("defaultQuantity", fav.defaultQuantity)
                        put("unit", fav.unit)
                        put("grams", fav.grams)
                        put("calories", fav.calories)
                        put("proteinG", fav.proteinG)
                        put("carbsG", fav.carbsG)
                        put("fatG", fav.fatG)
                        put("fiberG", fav.fiberG)
                        put("vitaminAMcg", fav.vitaminAMcg)
                        put("vitaminCMg", fav.vitaminCMg)
                        put("vitaminDMcg", fav.vitaminDMcg)
                        put("vitaminEMg", fav.vitaminEMg)
                        put("vitaminB6Mg", fav.vitaminB6Mg)
                        put("vitaminB12Mcg", fav.vitaminB12Mcg)
                    })
                }
            })

            storeFile.writeText(root.toString(), Charsets.UTF_8)
        }
    }
}
