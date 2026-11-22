package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.model.AnalyzedFoodItem
import com.example.model.AnalyzedFoodResult
import com.example.model.FoodSource
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

class AiNutritionService {

    companion object {
        private const val TAG = "AiNutritionService"
        private const val MODEL_NAME = "gemini-3.5-flash-lite"
        private const val TIMEOUT_MS = 60_000L
    }

    private val foodItemSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(description = "Name of the food item in Bengali (or clear Bengali + common name)"),
            "quantity" to Schema.double(description = "Estimated numeric quantity, e.g. 1.0, 2.0, 150.0"),
            "unit" to Schema.string(description = "Portion unit in Bengali such as কাপ, পিস, বাটি, গ্রাম, প্লেট, গ্লাস, চামচ"),
            "gramsEquivalent" to Schema.double(description = "Approximate weight in grams"),
            "calories" to Schema.integer(description = "Estimated total calories (kcal) for this quantity"),
            "proteinG" to Schema.double(description = "Estimated protein in grams"),
            "carbsG" to Schema.double(description = "Estimated carbohydrates in grams"),
            "fatG" to Schema.double(description = "Estimated fat in grams"),
            "fiberG" to Schema.double(description = "Estimated dietary fiber in grams"),
            "vitaminAMcg" to Schema.double(description = "Estimated Vitamin A in micrograms RAE (0 if negligible)"),
            "vitaminCMg" to Schema.double(description = "Estimated Vitamin C in milligrams (0 if negligible)"),
            "vitaminDMcg" to Schema.double(description = "Estimated Vitamin D in micrograms (0 if negligible)"),
            "vitaminEMg" to Schema.double(description = "Estimated Vitamin E in milligrams (0 if negligible)"),
            "vitaminB6Mg" to Schema.double(description = "Estimated Vitamin B6 in milligrams (0 if negligible)"),
            "vitaminB12Mcg" to Schema.double(description = "Estimated Vitamin B12 in micrograms (0 if negligible)"),
            "confidence" to Schema.string(description = "Confidence level: High, Medium, or Low")
        )
    )

    private val foodAnalysisSchema = Schema.obj(
        properties = mapOf(
            "mealTitle" to Schema.string(description = "Short Bengali summary title of the meal"),
            "items" to Schema.array(
                items = foodItemSchema,
                description = "List of detected or parsed food items with estimated nutrition"
            ),
            "nutritionNote" to Schema.string(description = "One short actionable nutrition tip in Bengali")
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = foodAnalysisSchema
                temperature = 0.2f
            }
        )
    }

    private val coachModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.6f
            }
        )
    }

    suspend fun analyzeFoodImage(bitmap: Bitmap): Result<AnalyzedFoodResult> {
        return try {
            val scaledBitmap = scaleBitmapForAi(bitmap, maxDim = 1024)
            val prompt = """
                You are an expert clinical nutritionist familiar with South Asian, Bangladeshi, and international foods.
                Analyze all visible food items in this photo, estimate realistic portion sizes, and calculate calories, protein, carbs, fat, fiber, and key daily vitamins (vitaminAMcg, vitaminCMg, vitaminDMcg, vitaminEMg, vitaminB6Mg, vitaminB12Mcg).
                Respond strictly using the configured JSON schema with food names, units, mealTitle, and nutritionNote in clear Bengali.
            """.trimIndent()

            val response = withTimeout(TIMEOUT_MS) {
                structuredModel.generateContent(
                    content {
                        image(scaledBitmap)
                        text(prompt)
                    }
                )
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("AI কোনো উত্তর পাঠায়নি।"))
            Result.success(parseAnalysisJson(jsonText, FoodSource.AI_CAMERA))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image food analysis timed out", e)
            Result.failure(IllegalStateException("AI বিশ্লেষণে সময় বেশি লাগছে (৬০ সেকেন্ড)। অনুগ্রহ করে আবার চেষ্টা করুন।"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image food analysis failed", e)
            Result.failure(e)
        }
    }

    suspend fun parseNaturalLanguageFood(description: String): Result<AnalyzedFoodResult> {
        return try {
            val cleanQuery = description.trim()
            if (cleanQuery.isBlank()) {
                return Result.failure(IllegalArgumentException("অনুগ্রহ করে খাবারের বিবরণ লিখুন।"))
            }
            val prompt = """
                You are an expert nutritionist specializing in Bangladeshi, Indian, and global cuisine.
                Parse this natural-language meal description into individual food items with accurate portion sizes and nutrition:
                "$cleanQuery"
                Return mealTitle, items (each with Bengali name, numeric quantity, Bengali unit like কাপ/পিস/বাটি/গ্রাম, gramsEquivalent, calories, proteinG, carbsG, fatG, fiberG, vitaminAMcg, vitaminCMg, vitaminDMcg, vitaminEMg, vitaminB6Mg, vitaminB12Mcg, confidence), and one brief nutritionNote in Bengali.
            """.trimIndent()

            val response = withTimeout(TIMEOUT_MS) {
                structuredModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("AI কোনো উত্তর পাঠায়নি।"))
            Result.success(parseAnalysisJson(jsonText, FoodSource.AI_TEXT))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Text food parsing timed out", e)
            Result.failure(IllegalStateException("AI বিশ্লেষণ টাইমআউট হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন।"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Text food parsing failed", e)
            Result.failure(e)
        }
    }

    fun streamDailyNutritionInsight(
        goalLabel: String,
        targetCalories: Long,
        intakeCalories: Long,
        burnedCalories: Long,
        proteinG: Double,
        proteinTargetG: Long,
        carbsG: Double,
        carbsTargetG: Long,
        fatG: Double,
        fatTargetG: Long,
        waterMl: Long,
        waterTargetMl: Long,
        vitaminSummary: String = ""
    ): Flow<String> = flow {
        val prompt = """
            আপনি একজন অভিজ্ঞ পুষ্টিবিদ। নিচের আজকের ডেটা বিশ্লেষণ করে ব্যবহারকারীর জন্য ৩টি ছোট, বাস্তবসম্মত ও বৈজ্ঞানিক পরামর্শ দিন বাংলায় (মোট ৭৫-৯৫ শব্দের মধ্যে, বুলেট পয়েন্টে):
            - লক্ষ্য: $goalLabel
            - দৈনিক টার্গেট ক্যালোরি: $targetCalories kcal
            - আজ খাওয়া হয়েছে: $intakeCalories kcal, ব্যায়ামে বার্ন: $burnedCalories kcal, নেট: ${intakeCalories - burnedCalories} kcal
            - প্রোটিন: ${proteinG.toInt()}g / ${proteinTargetG}g
            - শর্করা: ${carbsG.toInt()}g / ${carbsTargetG}g
            - ফ্যাট: ${fatG.toInt()}g / ${fatTargetG}g
            - পানি পান: ${waterMl}ml / ${waterTargetMl}ml
            ${if (vitaminSummary.isNotBlank()) "- ভিটামিন স্ট্যাটাস: $vitaminSummary" else ""}
            প্রোটিন/ম্যাক্রো, দৈনিক ভিটামিন ঘাটতি (সহজ দেশীয় খাবারসহ), ও হাইড্রেশন নিয়ে কার্যকর পরামর্শ দিন।
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(TIMEOUT_MS) {
            coachModel.generateContentStream(prompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI কোচ থেকে কোনো পরামর্শ পাওয়া যায়নি।")
        }
    }

    private fun parseAnalysisJson(jsonString: String, source: FoodSource): AnalyzedFoodResult {
        val root = JSONObject(jsonString)
        val mealTitle = root.optString("mealTitle", "বিশ্লেষিত খাবার").ifBlank { "বিশ্লেষিত খাবার" }
        val nutritionNote = root.optString("nutritionNote", "").trim()
        val itemsJson = root.optJSONArray("items")
        val items = mutableListOf<AnalyzedFoodItem>()

        if (itemsJson != null) {
            for (i in 0 until itemsJson.length()) {
                val obj = itemsJson.getJSONObject(i)
                val qty = obj.optDouble("quantity", 1.0).let { if (it <= 0.0 || it.isNaN()) 1.0 else it }
                items.add(
                    AnalyzedFoodItem(
                        id = UUID.randomUUID().toString(),
                        name = obj.optString("name", "খাবার").ifBlank { "খাবার" },
                        baseQuantity = qty,
                        quantity = qty,
                        unit = obj.optString("unit", "সার্ভিং").ifBlank { "সার্ভিং" },
                        baseGrams = obj.optDouble("gramsEquivalent", 100.0).coerceAtLeast(0.0),
                        baseCalories = obj.optInt("calories", 0).coerceIn(0, 15000),
                        baseProteinG = obj.optDouble("proteinG", 0.0).coerceIn(0.0, 1000.0),
                        baseCarbsG = obj.optDouble("carbsG", 0.0).coerceIn(0.0, 1500.0),
                        baseFatG = obj.optDouble("fatG", 0.0).coerceIn(0.0, 1000.0),
                        baseFiberG = obj.optDouble("fiberG", 0.0).coerceIn(0.0, 500.0),
                        baseVitaminAMcg = obj.optDouble("vitaminAMcg", 0.0).coerceIn(0.0, 50000.0),
                        baseVitaminCMg = obj.optDouble("vitaminCMg", 0.0).coerceIn(0.0, 10000.0),
                        baseVitaminDMcg = obj.optDouble("vitaminDMcg", 0.0).coerceIn(0.0, 2000.0),
                        baseVitaminEMg = obj.optDouble("vitaminEMg", 0.0).coerceIn(0.0, 2000.0),
                        baseVitaminB6Mg = obj.optDouble("vitaminB6Mg", 0.0).coerceIn(0.0, 500.0),
                        baseVitaminB12Mcg = obj.optDouble("vitaminB12Mcg", 0.0).coerceIn(0.0, 5000.0),
                        confidence = obj.optString("confidence", "High")
                    )
                )
            }
        }

        if (items.isEmpty()) {
            throw IllegalStateException("কোনো খাবার শনাক্ত করা যায়নি। অনুগ্রহ করে আরও স্পষ্ট ছবি বা বিবরণ দিন।")
        }

        return AnalyzedFoodResult(
            mealTitle = mealTitle,
            items = items,
            nutritionNote = nutritionNote,
            source = source
        )
    }

    private fun scaleBitmapForAi(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
        val newW = (width * ratio).toInt().coerceAtLeast(1)
        val newH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
