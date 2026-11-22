package com.example.data

import com.example.model.ExerciseLog
import com.example.model.FavoriteFood
import com.example.model.FoodLog
import com.example.model.UserProfile
import com.example.model.WaterLog
import com.example.model.WeightLog
import kotlinx.coroutines.flow.Flow

interface NutritionDataSource {
    fun observeUserProfile(userId: String): Flow<UserProfile?>
    fun observeFoodLogs(userId: String): Flow<List<FoodLog>>
    fun observeExerciseLogs(userId: String): Flow<List<ExerciseLog>>
    fun observeWaterLogs(userId: String): Flow<List<WaterLog>>
    fun observeWeightLogs(userId: String): Flow<List<WeightLog>>
    fun observeFavoriteFoods(userId: String): Flow<List<FavoriteFood>>

    suspend fun saveUserProfile(userId: String, profile: UserProfile): Result<Unit>
    suspend fun updateReminderSettings(
        userId: String,
        mealReminder: Boolean,
        waterReminder: Boolean,
        weightReminder: Boolean
    ): Result<Unit>

    suspend fun addFoodLogs(userId: String, items: List<FoodLog>): Result<Unit>
    suspend fun updateFoodLog(userId: String, item: FoodLog): Result<Unit>
    suspend fun deleteFoodLog(userId: String, logId: String): Result<Unit>

    suspend fun addExerciseLog(userId: String, item: ExerciseLog): Result<Unit>
    suspend fun deleteExerciseLog(userId: String, exerciseId: String): Result<Unit>

    suspend fun setWaterIntake(
        userId: String,
        dateKey: String,
        newAmountMl: Long,
        targetMl: Long
    ): Result<Unit>

    suspend fun addWeightLog(
        userId: String,
        dateKey: String,
        weightKg: Double,
        note: String?,
        updatedProfile: UserProfile?
    ): Result<Unit>

    suspend fun addFavoriteFood(userId: String, fav: FavoriteFood): Result<Unit>
    suspend fun deleteFavoriteFood(userId: String, favId: String): Result<Unit>
}
