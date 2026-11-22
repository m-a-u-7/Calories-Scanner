package com.example

import com.example.domain.NutritionCalculator
import com.example.model.ActivityLevel
import com.example.model.ExerciseCategory
import com.example.model.ExerciseIntensity
import com.example.model.Gender
import com.example.model.GoalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionCalculatorTest {

    @Test
    fun `mifflin st jeor bmr and tdee calculations are accurate`() {
        val maleBmr = NutritionCalculator.calculateBmr(
            weightKg = 75.0,
            heightCm = 175.0,
            age = 28,
            gender = Gender.MALE
        )
        // 10*75 + 6.25*175 - 5*28 + 5 = 750 + 1093.75 - 140 + 5 = 1708.75 -> 1709
        assertEquals(1709, maleBmr)

        val tdee = NutritionCalculator.calculateTdee(maleBmr, ActivityLevel.MODERATELY_ACTIVE)
        assertEquals(2649, tdee)
    }

    @Test
    fun `weight loss plan computes deficit macros and warns on extreme low calorie`() {
        val safePlan = NutritionCalculator.calculatePlan(
            age = 28,
            gender = Gender.MALE,
            heightCm = 175.0,
            currentWeightKg = 80.0,
            targetWeightKg = 72.0,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            goalType = GoalType.LOSS,
            weeklyRateKg = 0.5
        )
        assertEquals(safePlan.tdee - 550, safePlan.dailyCalorieTarget)
        assertNull(safePlan.safetyWarning)
        assertTrue(safePlan.proteinTargetG > 100)

        val unsafeLowPlan = NutritionCalculator.calculatePlan(
            age = 45,
            gender = Gender.FEMALE,
            heightCm = 152.0,
            currentWeightKg = 52.0,
            targetWeightKg = 45.0,
            activityLevel = ActivityLevel.SEDENTARY,
            goalType = GoalType.LOSS,
            weeklyRateKg = 1.0
        )
        assertNotNull(unsafeLowPlan.safetyWarning)
    }

    @Test
    fun `met burned calories scales with intensity duration and weight`() {
        val burned = NutritionCalculator.calculateCaloriesBurned(
            category = ExerciseCategory.RUNNING,
            intensity = ExerciseIntensity.MODERATE,
            weightKg = 70.0,
            durationMinutes = 30
        )
        // 9.8 * 70 * 0.5 = 343
        assertEquals(343, burned)
    }

    @Test
    fun `custom macro targets recalculate total calories and fiber target`() {
        val profile = NutritionCalculator.buildProfileWithCalculations(
            existing = com.example.model.UserProfile(),
            userId = "u1",
            displayName = "Test",
            age = 28,
            gender = Gender.MALE,
            heightCm = 175.0,
            currentWeightKg = 75.0,
            targetWeightKg = 70.0,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            goalType = GoalType.LOSS,
            weeklyRateKg = 0.5,
            customProteinTargetG = 150L,
            customCarbsTargetG = 200L,
            customFatTargetG = 60L
        )
        assertEquals(150L, profile.proteinTargetG)
        assertEquals(200L, profile.carbsTargetG)
        assertEquals(60L, profile.fatTargetG)
        // 150*4 + 200*4 + 60*9 = 600 + 800 + 540 = 1940
        assertEquals(1940L, profile.dailyCalorieTarget)
        assertTrue(NutritionCalculator.fiberTargetG(profile) >= 25)
    }

    @Test
    fun `daily vitamin rda targets adjust by age and gender and catalog has vitamins`() {
        val youngMale = NutritionCalculator.calculateVitaminTargets(age = 28, gender = Gender.MALE)
        assertEquals(900.0, youngMale.vitaminAMcg, 0.01)
        assertEquals(90.0, youngMale.vitaminCMg, 0.01)
        assertEquals(15.0, youngMale.vitaminDMcg, 0.01)
        assertEquals(15.0, youngMale.vitaminEMg, 0.01)
        assertEquals(1.3, youngMale.vitaminB6Mg, 0.01)
        assertEquals(2.4, youngMale.vitaminB12Mcg, 0.01)

        val matureFemale = NutritionCalculator.calculateVitaminTargets(age = 55, gender = Gender.FEMALE)
        assertEquals(700.0, matureFemale.vitaminAMcg, 0.01)
        assertEquals(75.0, matureFemale.vitaminCMg, 0.01)
        assertEquals(1.5, matureFemale.vitaminB6Mg, 0.01)

        val seniorMale = NutritionCalculator.calculateVitaminTargets(age = 75, gender = Gender.MALE)
        assertEquals(20.0, seniorMale.vitaminDMcg, 0.01)
        assertEquals(1.7, seniorMale.vitaminB6Mg, 0.01)

        assertTrue(com.example.domain.CommonFoodCatalog.items.any { it.vitaminCMg > 100.0 })
        assertTrue(com.example.domain.CommonFoodCatalog.items.any { it.vitaminB12Mcg >= 2.0 })
    }
}
