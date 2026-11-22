package com.example.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.model.ExerciseLog
import com.example.model.FoodLog
import com.example.model.UserProfile
import com.example.model.WaterLog
import com.example.model.WeightLog
import java.io.File

object ReportExporter {

    fun exportAndShareCsv(
        context: Context,
        profile: UserProfile?,
        foodLogs: List<FoodLog>,
        exerciseLogs: List<ExerciseLog>,
        waterLogs: List<WaterLog>,
        weightLogs: List<WeightLog>
    ): Result<Unit> {
        return runCatching {
            val sb = StringBuilder()
            sb.appendLine("=== NUTRIPULSE AI REPORT ===")
            if (profile != null) {
                sb.appendLine("Name,${escape(profile.displayName)}")
                sb.appendLine("Goal,${profile.goalType},TargetCalories,${profile.dailyCalorieTarget},BMR,${profile.bmr},TDEE,${profile.tdee}")
            }
            sb.appendLine()
            sb.appendLine("--- FOOD LOGS ---")
            sb.appendLine("Date,Meal,Name,Quantity,Unit,Grams,Calories,ProteinG,CarbsG,FatG,FiberG,VitAMcg,VitCMg,VitDMcg,VitEMg,VitB6Mg,VitB12Mcg,Source")
            foodLogs.sortedByDescending { it.dateKey }.forEach { f ->
                sb.appendLine("${f.dateKey},${f.mealType},${escape(f.name)},${f.quantity},${escape(f.unit)},${f.grams},${f.calories},${f.proteinG},${f.carbsG},${f.fatG},${f.fiberG},${f.vitaminAMcg},${f.vitaminCMg},${f.vitaminDMcg},${f.vitaminEMg},${f.vitaminB6Mg},${f.vitaminB12Mcg},${f.source}")
            }
            sb.appendLine()
            sb.appendLine("--- EXERCISE LOGS ---")
            sb.appendLine("Date,Activity,Category,Intensity,DurationMin,CaloriesBurned")
            exerciseLogs.sortedByDescending { it.dateKey }.forEach { e ->
                sb.appendLine("${e.dateKey},${escape(e.activityName)},${e.category},${e.intensity},${e.durationMinutes},${e.caloriesBurned}")
            }
            sb.appendLine()
            sb.appendLine("--- WATER LOGS ---")
            sb.appendLine("Date,AmountMl,TargetMl")
            waterLogs.sortedByDescending { it.dateKey }.forEach { w ->
                sb.appendLine("${w.dateKey},${w.amountMl},${w.targetMl}")
            }
            sb.appendLine()
            sb.appendLine("--- WEIGHT LOGS ---")
            sb.appendLine("Date,WeightKg,Note")
            weightLogs.sortedByDescending { it.dateKey }.forEach { wt ->
                sb.appendLine("${wt.dateKey},${wt.weightKg},${escape(wt.note.orEmpty())}")
            }

            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val outFile = File(exportsDir, "nutripulse_backup.csv")
            outFile.writeText(sb.toString(), Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "NutriPulse AI ডেটা ব্যাকআপ")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "ডেটা এক্সপোর্ট ও শেয়ার করুন")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    private fun escape(raw: String): String {
        val cleaned = raw.replace("\"", "\"\"")
        return "\"$cleaned\""
    }
}
