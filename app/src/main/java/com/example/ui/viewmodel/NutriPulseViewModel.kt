package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiNutritionService
import com.example.data.NutritionDataSource
import com.example.domain.DailyVitaminTargets
import com.example.domain.DateUtils
import com.example.domain.NutritionCalculator
import com.example.model.ActivityLevel
import com.example.model.AnalyzedFoodItem
import com.example.model.AnalyzedFoodResult
import com.example.model.ExerciseLog
import com.example.model.FavoriteFood
import com.example.model.FoodLog
import com.example.model.FoodSource
import com.example.model.Gender
import com.example.model.GoalType
import com.example.model.MealType
import com.example.model.ThemeMode
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.WaterLog
import com.example.model.WeightLog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiParseState {
    data object Idle : AiParseState
    data object Loading : AiParseState
    data class Success(val result: AnalyzedFoodResult) : AiParseState
    data class Error(val message: String) : AiParseState
}

sealed interface AiCoachState {
    data object Idle : AiCoachState
    data object Loading : AiCoachState
    data class Streaming(val text: String) : AiCoachState
    data class Success(val text: String) : AiCoachState
    data class Error(val message: String) : AiCoachState
}

data class DailyChartPoint(
    val dateKey: String,
    val label: String,
    val intakeCalories: Long,
    val burnedCalories: Long,
    val targetCalories: Long
)

data class DashboardSummary(
    val profile: UserProfile,
    val selectedDateKey: String,
    val dayFoodLogs: List<FoodLog>,
    val dayExerciseLogs: List<ExerciseLog>,
    val dayWaterLog: WaterLog?,
    val intakeCalories: Long,
    val burnedCalories: Long,
    val netCalories: Long,
    val remainingCalories: Long,
    val totalProteinG: Double,
    val totalCarbsG: Double,
    val totalFatG: Double,
    val totalFiberG: Double,
    val totalVitaminAMcg: Double,
    val totalVitaminCMg: Double,
    val totalVitaminDMcg: Double,
    val totalVitaminEMg: Double,
    val totalVitaminB6Mg: Double,
    val totalVitaminB12Mcg: Double,
    val vitaminTargets: DailyVitaminTargets,
    val safetyAlert: String?,
    val allFoodLogs: List<FoodLog>,
    val allExerciseLogs: List<ExerciseLog>,
    val allWaterLogs: List<WaterLog>,
    val weightLogs: List<WeightLog>,
    val favorites: List<FavoriteFood>
)

class NutriPulseViewModel(
    private val repository: NutritionDataSource,
    private val aiService: AiNutritionService,
    private val currentUserId: String,
    private val initialDisplayName: String
) : ViewModel() {

    companion object {
        private const val TAG = "NutriPulseVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private val _selectedDateKey = MutableStateFlow(DateUtils.todayDateKey())
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _aiParseState = MutableStateFlow<AiParseState>(AiParseState.Idle)
    val aiParseState: StateFlow<AiParseState> = _aiParseState.asStateFlow()

    private val _aiCoachState = MutableStateFlow<AiCoachState>(AiCoachState.Idle)
    val aiCoachState: StateFlow<AiCoachState> = _aiCoachState.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val profileFlow = repository.observeUserProfile(currentUserId)
    private val foodLogsFlow = repository.observeFoodLogs(currentUserId)
    private val exerciseLogsFlow = repository.observeExerciseLogs(currentUserId)
    private val waterLogsFlow = repository.observeWaterLogs(currentUserId)
    private val weightLogsFlow = repository.observeWeightLogs(currentUserId)
    private val favoritesFlow = repository.observeFavoriteFoods(currentUserId)

    val dashboardState: StateFlow<UiState<DashboardSummary>> = combine(
        profileFlow,
        foodLogsFlow,
        exerciseLogsFlow,
        waterLogsFlow,
        weightLogsFlow,
        favoritesFlow,
        _selectedDateKey
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val rawProfile = values[0] as UserProfile?
        @Suppress("UNCHECKED_CAST")
        val allFoods = values[1] as List<FoodLog>
        @Suppress("UNCHECKED_CAST")
        val allExercises = values[2] as List<ExerciseLog>
        @Suppress("UNCHECKED_CAST")
        val allWaters = values[3] as List<WaterLog>
        @Suppress("UNCHECKED_CAST")
        val allWeights = values[4] as List<WeightLog>
        @Suppress("UNCHECKED_CAST")
        val allFavs = values[5] as List<FavoriteFood>
        val dateKey = values[6] as String

        val profile = rawProfile ?: UserProfile(
            userId = currentUserId,
            displayName = initialDisplayName.ifBlank { "ব্যবহারকারী" },
            onboardingDone = false
        )

        val dayFoods = allFoods.filter { it.dateKey == dateKey }
        val dayExercises = allExercises.filter { it.dateKey == dateKey }
        val dayWater = allWaters.firstOrNull { it.dateKey == dateKey }

        val intakeCalories = dayFoods.sumOf { it.calories }
        val burnedCalories = dayExercises.sumOf { it.caloriesBurned }
        val netCalories = intakeCalories - burnedCalories
        val remainingCalories = profile.dailyCalorieTarget - netCalories

        val totalProtein = dayFoods.sumOf { it.proteinG }
        val totalCarbs = dayFoods.sumOf { it.carbsG }
        val totalFat = dayFoods.sumOf { it.fatG }
        val totalFiber = dayFoods.sumOf { it.fiberG }
        val totalVitA = dayFoods.sumOf { it.vitaminAMcg }
        val totalVitC = dayFoods.sumOf { it.vitaminCMg }
        val totalVitD = dayFoods.sumOf { it.vitaminDMcg }
        val totalVitE = dayFoods.sumOf { it.vitaminEMg }
        val totalVitB6 = dayFoods.sumOf { it.vitaminB6Mg }
        val totalVitB12 = dayFoods.sumOf { it.vitaminB12Mcg }
        val vitaminTargets = NutritionCalculator.calculateVitaminTargets(
            age = profile.age.toInt(),
            gender = Gender.fromRaw(profile.gender)
        )

        val safetyAlert = buildSafetyAlert(profile, intakeCalories, netCalories)

        DashboardSummary(
            profile = profile,
            selectedDateKey = dateKey,
            dayFoodLogs = dayFoods,
            dayExerciseLogs = dayExercises,
            dayWaterLog = dayWater,
            intakeCalories = intakeCalories,
            burnedCalories = burnedCalories,
            netCalories = netCalories,
            remainingCalories = remainingCalories,
            totalProteinG = totalProtein,
            totalCarbsG = totalCarbs,
            totalFatG = totalFat,
            totalFiberG = totalFiber,
            totalVitaminAMcg = totalVitA,
            totalVitaminCMg = totalVitC,
            totalVitaminDMcg = totalVitD,
            totalVitaminEMg = totalVitE,
            totalVitaminB6Mg = totalVitB6,
            totalVitaminB12Mcg = totalVitB12,
            vitaminTargets = vitaminTargets,
            safetyAlert = safetyAlert,
            allFoodLogs = allFoods,
            allExerciseLogs = allExercises,
            allWaterLogs = allWaters,
            weightLogs = allWeights.sortedBy { it.dateKey },
            favorites = allFavs.sortedBy { it.name }
        )
    }
        .map<DashboardSummary, UiState<DashboardSummary>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing dashboard state", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "ডেটা লোড করতে সমস্যা হয়েছে।"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private fun buildSafetyAlert(
        profile: UserProfile,
        intakeCalories: Long,
        netCalories: Long
    ): String? {
        if (!profile.onboardingDone) return null
        val gender = Gender.fromRaw(profile.gender)
        val minSafe = when (gender) {
            Gender.MALE -> 1500L
            Gender.FEMALE -> 1200L
            Gender.OTHER -> 1350L
        }
        return when {
            profile.dailyCalorieTarget < minSafe ->
                "সতর্কতা: আপনার দৈনিক টার্গেট ($minSafe kcal নিরাপদ সীমার নিচে)। প্রোফাইল থেকে ওজন কমানোর হার কিছুটা কমান।"
            intakeCalories > 0 && netCalories < 800L && profile.dailyCalorieTarget >= 1500L ->
                "নেট ক্যালোরি বেশ কম ($netCalories kcal) — পর্যাপ্ত পুষ্টি ও শক্তি বজায় রাখতে সুষম খাবার গ্রহণ করুন।"
            remainingCaloriesTooHighOver(profile.dailyCalorieTarget, netCalories) ->
                "আজকের দৈনিক টার্গেট অতিক্রম করেছে — হালকা হাঁটা বা রাতের খাবার হালকা রাখলে ভারসাম্য ফিরবে।"
            else -> null
        }
    }

    private fun remainingCaloriesTooHighOver(target: Long, net: Long): Boolean {
        return net > target + 450L
    }

    fun selectDate(dateKey: String) {
        _selectedDateKey.value = dateKey
    }

    fun shiftDate(deltaDays: Int) {
        _selectedDateKey.value = DateUtils.shiftDateKey(_selectedDateKey.value, deltaDays)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun saveProfileAndGoals(
        existing: UserProfile,
        displayName: String,
        age: Int,
        gender: Gender,
        heightCm: Double,
        currentWeightKg: Double,
        targetWeightKg: Double,
        activityLevel: ActivityLevel,
        goalType: GoalType,
        weeklyRateKg: Double,
        customProteinTargetG: Long? = null,
        customCarbsTargetG: Long? = null,
        customFatTargetG: Long? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val updated = NutritionCalculator.buildProfileWithCalculations(
                existing = existing,
                userId = currentUserId,
                displayName = displayName,
                age = age,
                gender = gender,
                heightCm = heightCm,
                currentWeightKg = currentWeightKg,
                targetWeightKg = targetWeightKg,
                activityLevel = activityLevel,
                goalType = goalType,
                weeklyRateKg = weeklyRateKg,
                onboardingDone = true,
                customProteinTargetG = customProteinTargetG,
                customCarbsTargetG = customCarbsTargetG,
                customFatTargetG = customFatTargetG
            )
            repository.saveUserProfile(currentUserId, updated)
                .onSuccess {
                    if (!existing.onboardingDone) {
                        repository.addWeightLog(
                            userId = currentUserId,
                            dateKey = DateUtils.todayDateKey(),
                            weightKg = currentWeightKg,
                            note = "প্রাথমিক ওজন",
                            updatedProfile = null
                        )
                    }
                    _feedbackMessage.value = "প্রোফাইল ও দৈনিক ক্যালোরি ও ম্যাক্রো লক্ষ্য সংরক্ষিত হয়েছে!"
                    onComplete()
                }
                .onFailure { e ->
                    _feedbackMessage.value = e.localizedMessage ?: "প্রোফাইল সংরক্ষণ ব্যর্থ হয়েছে"
                }
        }
    }

    fun updateMacroTargets(
        existing: UserProfile,
        proteinTargetG: Long,
        carbsTargetG: Long,
        fatTargetG: Long
    ) {
        viewModelScope.launch {
            val safeP = proteinTargetG.coerceIn(20L, 600L)
            val safeC = carbsTargetG.coerceIn(20L, 1000L)
            val safeF = fatTargetG.coerceIn(10L, 400L)
            val computedCalories = ((safeP * 4L) + (safeC * 4L) + (safeF * 9L)).coerceIn(800L, 10000L)
            val updated = existing.copy(
                proteinTargetG = safeP,
                carbsTargetG = safeC,
                fatTargetG = safeF,
                dailyCalorieTarget = computedCalories
            )
            repository.saveUserProfile(currentUserId, updated)
                .onSuccess {
                    _feedbackMessage.value = "প্রোটিন, শর্করা ও ফ্যাট টার্গেট আপডেট হয়েছে!"
                }
                .onFailure { e ->
                    _feedbackMessage.value = e.localizedMessage ?: "টার্গেট আপডেট ব্যর্থ হয়েছে"
                }
        }
    }

    fun updateReminders(
        mealReminder: Boolean,
        waterReminder: Boolean,
        weightReminder: Boolean
    ) {
        viewModelScope.launch {
            repository.updateReminderSettings(
                userId = currentUserId,
                mealReminder = mealReminder,
                waterReminder = waterReminder,
                weightReminder = weightReminder
            ).onSuccess {
                _feedbackMessage.value = "রিমাইন্ডার সেটিংস আপডেট হয়েছে"
            }.onFailure { e ->
                _feedbackMessage.value = e.localizedMessage ?: "রিমাইন্ডার আপডেট ব্যর্থ হয়েছে"
            }
        }
    }

    fun analyzeFoodBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _aiParseState.value = AiParseState.Loading
            aiService.analyzeFoodImage(bitmap)
                .onSuccess { result ->
                    _aiParseState.value = AiParseState.Success(result)
                }
                .onFailure { e ->
                    _aiParseState.value = AiParseState.Error(
                        e.localizedMessage ?: "ছবি বিশ্লেষণ করা যায়নি। আবার চেষ্টা করুন।"
                    )
                }
        }
    }

    fun parseNaturalLanguageFood(description: String) {
        viewModelScope.launch {
            _aiParseState.value = AiParseState.Loading
            aiService.parseNaturalLanguageFood(description)
                .onSuccess { result ->
                    _aiParseState.value = AiParseState.Success(result)
                }
                .onFailure { e ->
                    _aiParseState.value = AiParseState.Error(
                        e.localizedMessage ?: "খাবারের বিবরণ বিশ্লেষণ করা যায়নি।"
                    )
                }
        }
    }

    fun updateAnalyzedItemQuantity(itemId: String, newQuantity: Double) {
        val current = _aiParseState.value
        if (current !is AiParseState.Success) return
        val clampedQty = newQuantity.coerceIn(0.25, 1000.0)
        val updatedItems = current.result.items.map { item ->
            if (item.id == itemId) item.copy(quantity = clampedQty) else item
        }
        _aiParseState.value = AiParseState.Success(current.result.copy(items = updatedItems))
    }

    fun updateAnalyzedItemUnit(itemId: String, newUnit: String) {
        val current = _aiParseState.value
        if (current !is AiParseState.Success) return
        val updatedItems = current.result.items.map { item ->
            if (item.id == itemId) item.copy(unit = newUnit) else item
        }
        _aiParseState.value = AiParseState.Success(current.result.copy(items = updatedItems))
    }

    fun removeAnalyzedItem(itemId: String) {
        val current = _aiParseState.value
        if (current !is AiParseState.Success) return
        val updatedItems = current.result.items.filterNot { it.id == itemId }
        if (updatedItems.isEmpty()) {
            _aiParseState.value = AiParseState.Idle
        } else {
            _aiParseState.value = AiParseState.Success(current.result.copy(items = updatedItems))
        }
    }

    fun resetAiParseState() {
        _aiParseState.value = AiParseState.Idle
    }

    fun saveAnalyzedItems(
        items: List<AnalyzedFoodItem>,
        mealType: MealType,
        source: FoodSource,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val dateKey = _selectedDateKey.value
            val logs = items.map { item ->
                FoodLog(
                    userId = currentUserId,
                    dateKey = dateKey,
                    mealType = mealType.name,
                    name = item.name,
                    quantity = item.quantity,
                    unit = item.unit,
                    grams = item.scaledGrams,
                    calories = item.scaledCalories.toLong(),
                    proteinG = item.scaledProteinG,
                    carbsG = item.scaledCarbsG,
                    fatG = item.scaledFatG,
                    fiberG = item.scaledFiberG,
                    vitaminAMcg = item.scaledVitaminAMcg,
                    vitaminCMg = item.scaledVitaminCMg,
                    vitaminDMcg = item.scaledVitaminDMcg,
                    vitaminEMg = item.scaledVitaminEMg,
                    vitaminB6Mg = item.scaledVitaminB6Mg,
                    vitaminB12Mcg = item.scaledVitaminB12Mcg,
                    source = source.name
                )
            }
            repository.addFoodLogs(currentUserId, logs)
                .onSuccess {
                    _aiParseState.value = AiParseState.Idle
                    _feedbackMessage.value = "${logs.size}টি খাবার (${mealType.labelBn}) লগ করা হয়েছে!"
                    onSaved()
                }
                .onFailure { e ->
                    _feedbackMessage.value = e.localizedMessage ?: "খাবার সংরক্ষণ ব্যর্থ হয়েছে"
                }
        }
    }

    fun addSingleFoodLog(log: FoodLog, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.addFoodLogs(
                currentUserId,
                listOf(log.copy(userId = currentUserId, dateKey = _selectedDateKey.value))
            ).onSuccess {
                _feedbackMessage.value = "${log.name} লগ করা হয়েছে!"
                onSaved()
            }.onFailure { e ->
                _feedbackMessage.value = e.localizedMessage ?: "খাবার সংরক্ষণ ব্যর্থ হয়েছে"
            }
        }
    }

    fun updateFoodLog(log: FoodLog) {
        viewModelScope.launch {
            repository.updateFoodLog(currentUserId, log)
                .onSuccess { _feedbackMessage.value = "খাবার আপডেট করা হয়েছে" }
                .onFailure { e -> _feedbackMessage.value = e.localizedMessage ?: "আপডেট ব্যর্থ হয়েছে" }
        }
    }

    fun deleteFoodLog(logId: String) {
        viewModelScope.launch {
            repository.deleteFoodLog(currentUserId, logId)
                .onSuccess { _feedbackMessage.value = "খাবার মুছে ফেলা হয়েছে" }
                .onFailure { e -> _feedbackMessage.value = e.localizedMessage ?: "মুছে ফেলা যায়নি" }
        }
    }

    fun addFavoriteFood(fav: FavoriteFood) {
        viewModelScope.launch {
            repository.addFavoriteFood(currentUserId, fav)
                .onSuccess { _feedbackMessage.value = "${fav.name} ফেভারিটে সেভ হয়েছে" }
                .onFailure { e -> _feedbackMessage.value = e.localizedMessage ?: "ফেভারিট সেভ ব্যর্থ হয়েছে" }
        }
    }

    fun deleteFavoriteFood(favId: String) {
        viewModelScope.launch {
            repository.deleteFavoriteFood(currentUserId, favId)
                .onSuccess { _feedbackMessage.value = "ফেভারিট থেকে সরানো হয়েছে" }
                .onFailure { e -> _feedbackMessage.value = e.localizedMessage ?: "সরানো যায়নি" }
        }
    }

    fun addExerciseLog(log: ExerciseLog) {
        viewModelScope.launch {
            repository.addExerciseLog(
                currentUserId,
                log.copy(userId = currentUserId, dateKey = _selectedDateKey.value)
            ).onSuccess {
                _feedbackMessage.value = "${log.activityName} (${log.caloriesBurned} kcal বার্ন) যোগ হয়েছে!"
            }.onFailure { e ->
                _feedbackMessage.value = e.localizedMessage ?: "ব্যায়াম লগ ব্যর্থ হয়েছে"
            }
        }
    }

    fun deleteExerciseLog(exerciseId: String) {
        viewModelScope.launch {
            repository.deleteExerciseLog(currentUserId, exerciseId)
                .onSuccess { _feedbackMessage.value = "ব্যায়ামের লগ মুছে ফেলা হয়েছে" }
                .onFailure { e -> _feedbackMessage.value = e.localizedMessage ?: "মুছে ফেলা যায়নি" }
        }
    }

    fun adjustWater(deltaMl: Long, currentAmountMl: Long, targetMl: Long) {
        viewModelScope.launch {
            val nextAmount = (currentAmountMl + deltaMl).coerceIn(0L, 20000L)
            repository.setWaterIntake(
                userId = currentUserId,
                dateKey = _selectedDateKey.value,
                newAmountMl = nextAmount,
                targetMl = targetMl
            ).onFailure { e ->
                _feedbackMessage.value = e.localizedMessage ?: "পানি লগ আপডেট ব্যর্থ হয়েছে"
            }
        }
    }

    fun logWeight(weightKg: Double, note: String?, currentProfile: UserProfile) {
        viewModelScope.launch {
            val recalculated = NutritionCalculator.buildProfileWithCalculations(
                existing = currentProfile,
                userId = currentUserId,
                displayName = currentProfile.displayName,
                age = currentProfile.age.toInt(),
                gender = Gender.fromRaw(currentProfile.gender),
                heightCm = currentProfile.heightCm,
                currentWeightKg = weightKg,
                targetWeightKg = currentProfile.targetWeightKg,
                activityLevel = ActivityLevel.fromRaw(currentProfile.activityLevel),
                goalType = GoalType.fromRaw(currentProfile.goalType),
                weeklyRateKg = currentProfile.weeklyRateKg,
                onboardingDone = currentProfile.onboardingDone
            )
            repository.addWeightLog(
                userId = currentUserId,
                dateKey = _selectedDateKey.value,
                weightKg = weightKg,
                note = note,
                updatedProfile = recalculated
            ).onSuccess {
                _feedbackMessage.value = "ওজন ($weightKg কেজি) সংরক্ষিত হয়েছে ও টার্গেট আপডেট হয়েছে!"
            }.onFailure { e ->
                _feedbackMessage.value = e.localizedMessage ?: "ওজন লগ ব্যর্থ হয়েছে"
            }
        }
    }

    fun generateSmartCoachInsight(summary: DashboardSummary) {
        viewModelScope.launch {
            _aiCoachState.value = AiCoachState.Loading
            try {
                val goalLabel = GoalType.fromRaw(summary.profile.goalType).labelBn
                var latestText = ""
                val vitSummary = "Vit A ${summary.totalVitaminAMcg.toInt()}/${summary.vitaminTargets.vitaminAMcg.toInt()}mcg, " +
                    "Vit C ${summary.totalVitaminCMg.toInt()}/${summary.vitaminTargets.vitaminCMg.toInt()}mg, " +
                    "Vit D ${String.format(java.util.Locale.US, "%.1f", summary.totalVitaminDMcg)}/${summary.vitaminTargets.vitaminDMcg.toInt()}mcg, " +
                    "Vit B12 ${String.format(java.util.Locale.US, "%.1f", summary.totalVitaminB12Mcg)}/${summary.vitaminTargets.vitaminB12Mcg}mcg"
                aiService.streamDailyNutritionInsight(
                    goalLabel = goalLabel,
                    targetCalories = summary.profile.dailyCalorieTarget,
                    intakeCalories = summary.intakeCalories,
                    burnedCalories = summary.burnedCalories,
                    proteinG = summary.totalProteinG,
                    proteinTargetG = summary.profile.proteinTargetG,
                    carbsG = summary.totalCarbsG,
                    carbsTargetG = summary.profile.carbsTargetG,
                    fatG = summary.totalFatG,
                    fatTargetG = summary.profile.fatTargetG,
                    waterMl = summary.dayWaterLog?.amountMl ?: 0L,
                    waterTargetMl = summary.profile.waterTargetMl,
                    vitaminSummary = vitSummary
                ).collect { streamed ->
                    latestText = streamed
                    _aiCoachState.value = AiCoachState.Streaming(streamed)
                }
                if (latestText.isNotBlank()) {
                    _aiCoachState.value = AiCoachState.Success(latestText)
                } else {
                    _aiCoachState.value = AiCoachState.Error("AI কোচ থেকে কোনো পরামর্শ পাওয়া যায়নি।")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Coach insight failed", e)
                _aiCoachState.value = AiCoachState.Error(
                    e.localizedMessage ?: "স্মার্ট পরামর্শ লোড করতে সমস্যা হয়েছে।"
                )
            }
        }
    }

    fun buildChartSeries(summary: DashboardSummary, days: Int): List<DailyChartPoint> {
        val keys = DateUtils.lastNDaysKeys(days, DateUtils.todayDateKey())
        val foodByDay = summary.allFoodLogs.groupBy { it.dateKey }
        val exerciseByDay = summary.allExerciseLogs.groupBy { it.dateKey }
        return keys.map { key ->
            DailyChartPoint(
                dateKey = key,
                label = DateUtils.formatShortChartLabel(key),
                intakeCalories = foodByDay[key]?.sumOf { it.calories } ?: 0L,
                burnedCalories = exerciseByDay[key]?.sumOf { it.caloriesBurned } ?: 0L,
                targetCalories = summary.profile.dailyCalorieTarget
            )
        }
    }
}
