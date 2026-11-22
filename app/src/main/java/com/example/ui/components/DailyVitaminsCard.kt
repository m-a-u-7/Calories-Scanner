package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BurnAccent
import com.example.ui.theme.CarbsAccent
import com.example.ui.theme.FatAccent
import com.example.ui.theme.FiberAccent
import com.example.ui.theme.ProteinAccent
import com.example.ui.theme.WaterAccent
import com.example.ui.viewmodel.DashboardSummary
import java.util.Locale

private data class VitaminUiItem(
    val shortName: String,
    val titleBn: String,
    val current: Double,
    val target: Double,
    val unit: String,
    val accent: Color,
    val sourcesBn: String
)

@Composable
fun DailyVitaminsCard(
    summary: DashboardSummary,
    modifier: Modifier = Modifier
) {
    var showSources by remember { mutableStateOf(false) }
    val targets = summary.vitaminTargets

    val vitamins = remember(summary) {
        listOf(
            VitaminUiItem(
                shortName = "Vit A",
                titleBn = "ভিটামিন A",
                current = summary.totalVitaminAMcg,
                target = targets.vitaminAMcg,
                unit = "mcg",
                accent = CarbsAccent,
                sourcesBn = "শাকসবজি, গাজর, ডিম, দুধ, পাকা পেঁপে"
            ),
            VitaminUiItem(
                shortName = "Vit C",
                titleBn = "ভিটামিন C",
                current = summary.totalVitaminCMg,
                target = targets.vitaminCMg,
                unit = "mg",
                accent = FiberAccent,
                sourcesBn = "পেয়ারা, আমলকি, লেবু, কমলা, টমেটো"
            ),
            VitaminUiItem(
                shortName = "Vit D",
                titleBn = "ভিটামিন D",
                current = summary.totalVitaminDMcg,
                target = targets.vitaminDMcg,
                unit = "mcg",
                accent = FatAccent,
                sourcesBn = "ইলিশ/রুই মাছ, ডিমের কুসুম, দুধ ও সকালের রোদ"
            ),
            VitaminUiItem(
                shortName = "Vit E",
                titleBn = "ভিটামিন E",
                current = summary.totalVitaminEMg,
                target = targets.vitaminEMg,
                unit = "mg",
                accent = ProteinAccent,
                sourcesBn = "আমন্ড/চিনাবাদাম, পালং শাক, উদ্ভিজ্জ তেল"
            ),
            VitaminUiItem(
                shortName = "Vit B6",
                titleBn = "ভিটামিন B6",
                current = summary.totalVitaminB6Mg,
                target = targets.vitaminB6Mg,
                unit = "mg",
                accent = WaterAccent,
                sourcesBn = "কলা, মুরগির মাংস, মাছ, ডাল, আলু"
            ),
            VitaminUiItem(
                shortName = "Vit B12",
                titleBn = "ভিটামিন B12",
                current = summary.totalVitaminB12Mcg,
                target = targets.vitaminB12Mcg,
                unit = "mcg",
                accent = BurnAccent,
                sourcesBn = "মাছ, মাংস, ডিম, দুধ ও টক দই"
            )
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth().testTag("daily_vitamins_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Medication,
                        contentDescription = "দৈনিক ভিটামিন",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "দৈনিক প্রয়োজনীয় ভিটামিন (RDA)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = { showSources = !showSources }) {
                    Text(if (showSources) "লুকান" else "উৎস")
                    Icon(
                        imageVector = if (showSources) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            vitamins.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { item ->
                        VitaminTile(
                            item = item,
                            showSource = showSources,
                            onClick = { showSources = !showSources },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VitaminTile(
    item: VitaminUiItem,
    showSource: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeTarget = item.target.coerceAtLeast(0.1)
    val progress = (item.current / safeTarget).toFloat().coerceIn(0f, 1f)
    val pct = ((item.current / safeTarget) * 100.0).toInt()
    val currentFormatted = if (item.target >= 10.0) {
        String.format(Locale.US, "%.0f", item.current)
    } else {
        String.format(Locale.US, "%.1f", item.current)
    }
    val targetFormatted = if (item.target >= 10.0) {
        String.format(Locale.US, "%.0f", item.target)
    } else {
        String.format(Locale.US, "%.1f", item.target)
    }

    Surface(
        color = item.accent.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.titleBn,
                    style = MaterialTheme.typography.labelLarge,
                    color = item.accent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "$currentFormatted / $targetFormatted ${item.unit}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            LinearProgressIndicator(
                progress = { progress },
                color = item.accent,
                trackColor = item.accent.copy(alpha = 0.18f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
            )

            AnimatedVisibility(visible = showSource) {
                Text(
                    text = "উৎস: ${item.sourcesBn}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
