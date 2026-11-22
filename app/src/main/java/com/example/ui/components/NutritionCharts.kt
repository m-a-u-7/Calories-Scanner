package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.WeightLog
import com.example.ui.theme.BurnAccent
import com.example.ui.theme.WaterAccent
import com.example.ui.viewmodel.DailyChartPoint
import kotlin.math.max
import kotlin.math.min

@Composable
fun CalorieProgressRing(
    intakeCalories: Long,
    burnedCalories: Long,
    targetCalories: Long,
    remainingCalories: Long,
    modifier: Modifier = Modifier
) {
    val net = (intakeCalories - burnedCalories).coerceAtLeast(0L)
    val safeTarget = targetCalories.coerceAtLeast(1L)
    val rawFraction = net.toFloat() / safeTarget.toFloat()
    val animatedSweep by animateFloatAsState(
        targetValue = (rawFraction.coerceIn(0f, 1.15f)) * 270f,
        animationSpec = tween(durationMillis = 650),
        label = "CalorieRingSweep"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val overColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = if (remainingCalories < 0) overColor else primaryColor

    Box(
        modifier = modifier.size(196.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val strokePx = 18.dp.toPx()
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(activeColor.copy(alpha = 0.75f), activeColor, activeColor)
                ),
                startAngle = 135f,
                sweepAngle = animatedSweep.coerceAtMost(270f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (remainingCalories >= 0) "বাকি ক্যালোরি" else "অতিরিক্ত ক্যালোরি",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${kotlin.math.abs(remainingCalories)}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = if (remainingCalories < 0) overColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "kcal • লক্ষ্য $targetCalories",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HydrationProgressBar(
    amountMl: Long,
    targetMl: Long,
    modifier: Modifier = Modifier
) {
    val safeTarget = targetMl.coerceAtLeast(1L)
    val fraction by animateFloatAsState(
        targetValue = (amountMl.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "WaterProgress"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
    ) {
        val corner = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(
            color = trackColor,
            size = size,
            cornerRadius = corner
        )
        if (fraction > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(WaterAccent.copy(alpha = 0.75f), WaterAccent)
                ),
                size = Size(width = size.width * fraction, height = size.height),
                cornerRadius = corner
            )
        }
    }
}

@Composable
fun CalorieTrendBarChart(
    points: List<DailyChartPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val intakeColor = MaterialTheme.colorScheme.primary
    val targetLineColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            val maxVal = max(
                points.maxOfOrNull { max(it.intakeCalories, it.targetCalories) } ?: 2000L,
                1000L
            ).toFloat()

            val slotWidth = size.width / points.size.coerceAtLeast(1)
            val barWidth = (slotWidth * 0.42f).coerceIn(6.dp.toPx(), 24.dp.toPx())

            // Target line
            val avgTarget = points.firstOrNull()?.targetCalories ?: 2000L
            val targetY = size.height - (avgTarget / maxVal) * size.height
            drawLine(
                color = targetLineColor,
                start = Offset(0f, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )

            points.forEachIndexed { idx, pt ->
                val centerX = idx * slotWidth + slotWidth / 2f
                val intakeH = ((pt.intakeCalories / maxVal) * size.height).coerceIn(0f, size.height)
                val burnH = ((pt.burnedCalories / maxVal) * size.height).coerceIn(0f, size.height)

                if (intakeH > 0f) {
                    drawRoundRect(
                        color = intakeColor,
                        topLeft = Offset(centerX - barWidth * 0.65f, size.height - intakeH),
                        size = Size(barWidth * 0.75f, intakeH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
                if (burnH > 0f) {
                    drawRoundRect(
                        color = BurnAccent,
                        topLeft = Offset(centerX + barWidth * 0.18f, size.height - burnH),
                        size = Size(barWidth * 0.55f, burnH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        val step = if (points.size > 10) 5 else 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { index, point ->
                if (index % step == 0 || index == points.lastIndex) {
                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                }
            }
        }
    }
}

@Composable
fun WeightTrendLineChart(
    logs: List<WeightLog>,
    targetWeightKg: Double,
    modifier: Modifier = Modifier
) {
    if (logs.isEmpty()) return
    val lineColor = MaterialTheme.colorScheme.tertiary
    val targetLineColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        val weights = logs.map { it.weightKg } + targetWeightKg
        val minW = (weights.minOrNull() ?: 50.0) - 1.5
        val maxW = (weights.maxOrNull() ?: 80.0) + 1.5
        val range = max(maxW - minW, 2.0)

        val targetY = (size.height - ((targetWeightKg - minW) / range) * size.height).toFloat()
        drawLine(
            color = targetLineColor.copy(alpha = 0.7f),
            start = Offset(0f, targetY),
            end = Offset(size.width, targetY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )

        if (logs.size == 1) {
            val y = (size.height - ((logs.first().weightKg - minW) / range) * size.height).toFloat()
            drawCircle(
                color = lineColor,
                radius = 6.dp.toPx(),
                center = Offset(size.width / 2f, y)
            )
            return@Canvas
        }

        val stepX = size.width / (logs.size - 1).coerceAtLeast(1)
        val path = Path()
        val points = logs.mapIndexed { idx, log ->
            val x = idx * stepX
            val y = (size.height - ((log.weightKg - minW) / range) * size.height).toFloat()
            Offset(x, min(max(y, 0f), size.height))
        }

        points.forEachIndexed { idx, offset ->
            if (idx == 0) path.moveTo(offset.x, offset.y)
            else path.lineTo(offset.x, offset.y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        points.forEach { offset ->
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = offset)
            drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = offset)
        }
    }
}
