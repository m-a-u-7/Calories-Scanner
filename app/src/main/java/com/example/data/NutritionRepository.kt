package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.ExerciseLog
import com.example.model.FavoriteFood
import com.example.model.FoodLog
import com.example.model.UserProfile
import com.example.model.WaterLog
import com.example.model.WeightLog
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NutritionRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : NutritionDataSource {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Google দিয়ে সাইন-ইন না করে ডেটা অ্যাক্সেস করা যাবে না।")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "ব্যবহারকারীর অ্যাকাউন্ট পরিবর্তন হয়েছে।" }
        }
        return uid
    }

    override fun observeUserProfile(userId: String): Flow<UserProfile?> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    override fun observeFoodLogs(userId: String): Flow<List<FoodLog>> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        return db.collection("users").document(userId).collection("food_logs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FoodLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    override fun observeExerciseLogs(userId: String): Flow<List<ExerciseLog>> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        return db.collection("users").document(userId).collection("exercise_logs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ExerciseLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    override fun observeWaterLogs(userId: String): Flow<List<WaterLog>> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        return db.collection("users").document(userId).collection("water_logs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    WaterLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    override fun observeWeightLogs(userId: String): Flow<List<WeightLog>> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        return db.collection("users").document(userId).collection("weight_logs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    WeightLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    override fun observeFavoriteFoods(userId: String): Flow<List<FavoriteFood>> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        return db.collection("users").document(userId).collection("favorite_foods")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FavoriteFood::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    override suspend fun saveUserProfile(userId: String, profile: UserProfile): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
            val existingSnapshot = docRef.get().await()

            val age = profile.age.coerceIn(13L, 120L)
            val heightCm = profile.heightCm.coerceIn(80.0, 250.0)
            val currentWeight = profile.currentWeightKg.coerceIn(25.0, 300.0)
            val targetWeight = profile.targetWeightKg.coerceIn(25.0, 300.0)
            val weeklyRate = profile.weeklyRateKg.coerceIn(0.0, 1.5)
            val bmr = profile.bmr.coerceIn(500L, 6000L)
            val tdee = profile.tdee.coerceIn(600L, 10000L)
            val calorieTarget = profile.dailyCalorieTarget.coerceIn(800L, 10000L)
            val proteinTarget = profile.proteinTargetG.coerceIn(20L, 600L)
            val carbsTarget = profile.carbsTargetG.coerceIn(20L, 1000L)
            val fatTarget = profile.fatTargetG.coerceIn(10L, 400L)
            val waterTarget = profile.waterTargetMl.coerceIn(1000L, 8000L)
            val displayName = profile.displayName.trim().take(80)

            if (!existingSnapshot.exists()) {
                val createPayload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "age" to age,
                    "gender" to profile.gender,
                    "heightCm" to heightCm,
                    "currentWeightKg" to currentWeight,
                    "targetWeightKg" to targetWeight,
                    "activityLevel" to profile.activityLevel,
                    "goalType" to profile.goalType,
                    "weeklyRateKg" to weeklyRate,
                    "bmr" to bmr,
                    "tdee" to tdee,
                    "dailyCalorieTarget" to calorieTarget,
                    "proteinTargetG" to proteinTarget,
                    "carbsTargetG" to carbsTarget,
                    "fatTargetG" to fatTarget,
                    "waterTargetMl" to waterTarget,
                    "mealReminderEnabled" to profile.mealReminderEnabled,
                    "waterReminderEnabled" to profile.waterReminderEnabled,
                    "weightReminderEnabled" to profile.weightReminderEnabled,
                    "onboardingDone" to profile.onboardingDone,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "displayName" to displayName,
                    "age" to age,
                    "gender" to profile.gender,
                    "heightCm" to heightCm,
                    "currentWeightKg" to currentWeight,
                    "targetWeightKg" to targetWeight,
                    "activityLevel" to profile.activityLevel,
                    "goalType" to profile.goalType,
                    "weeklyRateKg" to weeklyRate,
                    "bmr" to bmr,
                    "tdee" to tdee,
                    "dailyCalorieTarget" to calorieTarget,
                    "proteinTargetG" to proteinTarget,
                    "carbsTargetG" to carbsTarget,
                    "fatTargetG" to fatTarget,
                    "waterTargetMl" to waterTarget,
                    "mealReminderEnabled" to profile.mealReminderEnabled,
                    "waterReminderEnabled" to profile.waterReminderEnabled,
                    "weightReminderEnabled" to profile.weightReminderEnabled,
                    "onboardingDone" to profile.onboardingDone,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun updateReminderSettings(
        userId: String,
        mealReminder: Boolean,
        waterReminder: Boolean,
        weightReminder: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            db.collection("users").document(uid).update(
                mapOf(
                    "mealReminderEnabled" to mealReminder,
                    "waterReminderEnabled" to waterReminder,
                    "weightReminderEnabled" to weightReminder,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun addFoodLogs(userId: String, items: List<FoodLog>): Result<Unit> {
        val basePath = "users/$userId/food_logs"
        return try {
            val uid = requireUserId(userId)
            val batch = db.batch()
            items.forEach { item ->
                val cleanName = item.name.trim().take(120).ifBlank { "খাবার" }
                val cleanUnit = item.unit.trim().take(30).ifBlank { "সার্ভিং" }
                val docRef = db.collection("users").document(uid).collection("food_logs").document()
                val payload = mapOf(
                    "userId" to uid,
                    "dateKey" to item.dateKey,
                    "mealType" to item.mealType,
                    "name" to cleanName,
                    "quantity" to item.quantity.coerceIn(0.1, 10000.0),
                    "unit" to cleanUnit,
                    "grams" to item.grams.coerceIn(0.0, 10000.0),
                    "calories" to item.calories.coerceIn(0L, 15000L),
                    "proteinG" to item.proteinG.coerceIn(0.0, 1000.0),
                    "carbsG" to item.carbsG.coerceIn(0.0, 1500.0),
                    "fatG" to item.fatG.coerceIn(0.0, 1000.0),
                    "fiberG" to item.fiberG.coerceIn(0.0, 500.0),
                    "vitaminAMcg" to item.vitaminAMcg.coerceIn(0.0, 50000.0),
                    "vitaminCMg" to item.vitaminCMg.coerceIn(0.0, 10000.0),
                    "vitaminDMcg" to item.vitaminDMcg.coerceIn(0.0, 2000.0),
                    "vitaminEMg" to item.vitaminEMg.coerceIn(0.0, 2000.0),
                    "vitaminB6Mg" to item.vitaminB6Mg.coerceIn(0.0, 500.0),
                    "vitaminB12Mcg" to item.vitaminB12Mcg.coerceIn(0.0, 5000.0),
                    "source" to item.source,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, basePath)
            Result.failure(e)
        }
    }

    override suspend fun updateFoodLog(userId: String, item: FoodLog): Result<Unit> {
        val docPath = "users/$userId/food_logs/${item.id}"
        return try {
            val uid = requireUserId(userId)
            require(item.id.isNotBlank()) { "Food log ID must not be blank" }
            db.collection("users").document(uid)
                .collection("food_logs").document(item.id)
                .update(
                    mapOf(
                        "dateKey" to item.dateKey,
                        "mealType" to item.mealType,
                        "name" to item.name.trim().take(120).ifBlank { "খাবার" },
                        "quantity" to item.quantity.coerceIn(0.1, 10000.0),
                        "unit" to item.unit.trim().take(30).ifBlank { "সার্ভিং" },
                        "grams" to item.grams.coerceIn(0.0, 10000.0),
                        "calories" to item.calories.coerceIn(0L, 15000L),
                        "proteinG" to item.proteinG.coerceIn(0.0, 1000.0),
                        "carbsG" to item.carbsG.coerceIn(0.0, 1500.0),
                        "fatG" to item.fatG.coerceIn(0.0, 1000.0),
                        "fiberG" to item.fiberG.coerceIn(0.0, 500.0),
                        "vitaminAMcg" to item.vitaminAMcg.coerceIn(0.0, 50000.0),
                        "vitaminCMg" to item.vitaminCMg.coerceIn(0.0, 10000.0),
                        "vitaminDMcg" to item.vitaminDMcg.coerceIn(0.0, 2000.0),
                        "vitaminEMg" to item.vitaminEMg.coerceIn(0.0, 2000.0),
                        "vitaminB6Mg" to item.vitaminB6Mg.coerceIn(0.0, 500.0),
                        "vitaminB12Mcg" to item.vitaminB12Mcg.coerceIn(0.0, 5000.0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun deleteFoodLog(userId: String, logId: String): Result<Unit> {
        val docPath = "users/$userId/food_logs/$logId"
        return try {
            val uid = requireUserId(userId)
            require(logId.isNotBlank()) { "Food log ID must not be blank" }
            db.collection("users").document(uid)
                .collection("food_logs").document(logId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun addExerciseLog(userId: String, item: ExerciseLog): Result<Unit> {
        val basePath = "users/$userId/exercise_logs"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("exercise_logs").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "dateKey" to item.dateKey,
                    "activityName" to item.activityName.trim().take(100).ifBlank { "ব্যায়াম" },
                    "category" to item.category,
                    "intensity" to item.intensity,
                    "durationMinutes" to item.durationMinutes.coerceIn(1L, 720L),
                    "caloriesBurned" to item.caloriesBurned.coerceIn(0L, 10000L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, basePath)
            Result.failure(e)
        }
    }

    override suspend fun deleteExerciseLog(userId: String, exerciseId: String): Result<Unit> {
        val docPath = "users/$userId/exercise_logs/$exerciseId"
        return try {
            val uid = requireUserId(userId)
            require(exerciseId.isNotBlank()) { "Exercise ID must not be blank" }
            db.collection("users").document(uid)
                .collection("exercise_logs").document(exerciseId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun setWaterIntake(
        userId: String,
        dateKey: String,
        newAmountMl: Long,
        targetMl: Long
    ): Result<Unit> {
        val docPath = "users/$userId/water_logs/$dateKey"
        return try {
            val uid = requireUserId(userId)
            require(dateKey.length == 10) { "Invalid dateKey" }
            val docRef = db.collection("users").document(uid)
                .collection("water_logs").document(dateKey)
            val existing = docRef.get().await()
            val clampedAmount = newAmountMl.coerceIn(0L, 20000L)
            val clampedTarget = targetMl.coerceIn(500L, 10000L)

            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "dateKey" to dateKey,
                        "amountMl" to clampedAmount,
                        "targetMl" to clampedTarget,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "amountMl" to clampedAmount,
                        "targetMl" to clampedTarget,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun addWeightLog(
        userId: String,
        dateKey: String,
        weightKg: Double,
        note: String?,
        updatedProfile: UserProfile?
    ): Result<Unit> {
        val basePath = "users/$userId/weight_logs"
        return try {
            val uid = requireUserId(userId)
            val clampedWeight = weightKg.coerceIn(25.0, 300.0)
            val docRef = db.collection("users").document(uid).collection("weight_logs").document()
            val cleanNote = note?.trim()?.take(200)?.ifBlank { null }

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "dateKey" to dateKey,
                "weightKg" to clampedWeight,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanNote != null) {
                payload["note"] = cleanNote
            }
            docRef.set(payload).await()

            if (updatedProfile != null) {
                saveUserProfile(uid, updatedProfile)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, basePath)
            Result.failure(e)
        }
    }

    override suspend fun addFavoriteFood(userId: String, fav: FavoriteFood): Result<Unit> {
        val basePath = "users/$userId/favorite_foods"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("favorite_foods").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to fav.name.trim().take(120).ifBlank { "প্রিয় খাবার" },
                    "defaultQuantity" to fav.defaultQuantity.coerceIn(0.1, 10000.0),
                    "unit" to fav.unit.trim().take(30).ifBlank { "সার্ভিং" },
                    "grams" to fav.grams.coerceIn(0.0, 10000.0),
                    "calories" to fav.calories.coerceIn(0L, 15000L),
                    "proteinG" to fav.proteinG.coerceIn(0.0, 1000.0),
                    "carbsG" to fav.carbsG.coerceIn(0.0, 1500.0),
                    "fatG" to fav.fatG.coerceIn(0.0, 1000.0),
                    "fiberG" to fav.fiberG.coerceIn(0.0, 500.0),
                    "vitaminAMcg" to fav.vitaminAMcg.coerceIn(0.0, 50000.0),
                    "vitaminCMg" to fav.vitaminCMg.coerceIn(0.0, 10000.0),
                    "vitaminDMcg" to fav.vitaminDMcg.coerceIn(0.0, 2000.0),
                    "vitaminEMg" to fav.vitaminEMg.coerceIn(0.0, 2000.0),
                    "vitaminB6Mg" to fav.vitaminB6Mg.coerceIn(0.0, 500.0),
                    "vitaminB12Mcg" to fav.vitaminB12Mcg.coerceIn(0.0, 5000.0),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, basePath)
            Result.failure(e)
        }
    }

    override suspend fun deleteFavoriteFood(userId: String, favId: String): Result<Unit> {
        val docPath = "users/$userId/favorite_foods/$favId"
        return try {
            val uid = requireUserId(userId)
            require(favId.isNotBlank()) { "Favorite ID must not be blank" }
            db.collection("users").document(uid)
                .collection("favorite_foods").document(favId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
