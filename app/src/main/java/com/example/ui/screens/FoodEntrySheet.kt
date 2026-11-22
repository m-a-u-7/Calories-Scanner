package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.domain.CommonFoodCatalog
import com.example.model.AnalyzedFoodItem
import com.example.model.FavoriteFood
import com.example.model.FoodLog
import com.example.model.FoodSource
import com.example.model.MealType
import com.example.ui.theme.CarbsAccent
import com.example.ui.theme.FatAccent
import com.example.ui.theme.ProteinAccent
import com.example.ui.viewmodel.AiParseState
import java.io.File
import java.util.Locale
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FoodEntrySheet(
    initialMealType: MealType,
    aiParseState: AiParseState,
    favorites: List<FavoriteFood>,
    onDismiss: () -> Unit,
    onAnalyzeBitmap: (Bitmap) -> Unit,
    onParseNaturalText: (String) -> Unit,
    onUpdateQuantity: (String, Double) -> Unit,
    onUpdateUnit: (String, String) -> Unit,
    onRemoveAnalyzedItem: (String) -> Unit,
    onSaveAnalyzed: (List<AnalyzedFoodItem>, MealType, FoodSource) -> Unit,
    onSaveSingleFood: (FoodLog) -> Unit,
    onAddFavorite: (FavoriteFood) -> Unit,
    onDeleteFavorite: (String) -> Unit,
    onResetAiState: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedMeal by remember { mutableStateOf(initialMealType) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            val bmp = loadBitmapFromUri(context, uri)
            if (bmp != null) {
                capturedBitmap = bmp
                onAnalyzeBitmap(bmp)
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = loadBitmapFromUri(context, uri)
            if (bmp != null) {
                capturedBitmap = bmp
                onAnalyzeBitmap(bmp)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            onResetAiState()
            onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("খাবার যোগ করুন", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "বন্ধ করুন")
                }
            }

            // Meal selector chips
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MealType.entries.forEach { meal ->
                    FilterChip(
                        selected = selectedMeal == meal,
                        onClick = { selectedMeal = meal },
                        label = { Text(meal.labelBn) }
                    )
                }
            }

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = { Text("AI ক্যামেরা") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = { Text("AI টেক্সট") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = { Text("সার্চ / ম্যানুয়াল") }
                )
            }

            when (selectedTab) {
                0 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val photoDir = File(context.cacheDir, "photos").apply { mkdirs() }
                                val photoFile = File(photoDir, "meal_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            },
                            modifier = Modifier.weight(1f).testTag("take_food_photo_button")
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ছবি তুলুন")
                        }
                        FilledTonalButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("pick_food_photo_button")
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("গ্যালারি")
                        }
                    }

                    if (capturedBitmap != null) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "স্ক্যান করা খাবারের ছবি",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }

                    AiReviewSection(
                        aiParseState = aiParseState,
                        selectedMeal = selectedMeal,
                        onUpdateQuantity = onUpdateQuantity,
                        onUpdateUnit = onUpdateUnit,
                        onRemoveItem = onRemoveAnalyzedItem,
                        onSaveAnalyzed = onSaveAnalyzed
                    )
                }

                1 -> {
                    var query by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("খাবারের বিবরণ লিখুন") },
                        placeholder = { Text("যেমন: ২ কাপ ভাত, ১ পিস মাছের ঝোল, ১টা কলা") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("ai_text_food_input")
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "২টা রুটি, ১টা ডিম ভাজি, ১ কাপ দুধ চা",
                            "২ কাপ ভাত, ১ পিস মাছের ঝোল, ১টা কলা",
                            "১ বাটি ওটস, ১টা আপেল, ১০টি বাদাম"
                        ).forEach { sample ->
                            SuggestionChip(
                                onClick = { query = sample },
                                label = { Text(sample, maxLines = 1) }
                            )
                        }
                    }

                    Button(
                        onClick = { onParseNaturalText(query) },
                        enabled = query.isNotBlank() && aiParseState !is AiParseState.Loading,
                        modifier = Modifier.fillMaxWidth().testTag("ai_parse_text_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI দিয়ে ক্যালোরি ও ম্যাক্রো বিশ্লেষণ করুন")
                    }

                    AiReviewSection(
                        aiParseState = aiParseState,
                        selectedMeal = selectedMeal,
                        onUpdateQuantity = onUpdateQuantity,
                        onUpdateUnit = onUpdateUnit,
                        onRemoveItem = onRemoveAnalyzedItem,
                        onSaveAnalyzed = onSaveAnalyzed
                    )
                }

                2 -> {
                    ManualAndFavoritesSection(
                        selectedMeal = selectedMeal,
                        favorites = favorites,
                        onSaveSingleFood = onSaveSingleFood,
                        onAddFavorite = onAddFavorite,
                        onDeleteFavorite = onDeleteFavorite
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiReviewSection(
    aiParseState: AiParseState,
    selectedMeal: MealType,
    onUpdateQuantity: (String, Double) -> Unit,
    onUpdateUnit: (String, String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onSaveAnalyzed: (List<AnalyzedFoodItem>, MealType, FoodSource) -> Unit
) {
    when (aiParseState) {
        is AiParseState.Idle -> {
            Text(
                text = "Gemini 3.5 Flash-Lite ছবি বা সাধারণ বাংলা/ইংরেজি বর্ণনা থেকে ক্যালোরি, প্রোটিন, শর্করা, ফ্যাট ও পরিমাণ হিসাব করবে।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        is AiParseState.Loading -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("AI খাবার ও পুষ্টিগুণ বিশ্লেষণ করছে...", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        is AiParseState.Error -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = aiParseState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
        is AiParseState.Success -> {
            val result = aiParseState.result
            val totalCal = result.items.sumOf { it.scaledCalories }
            val totalProtein = result.items.sumOf { it.scaledProteinG }
            val totalCarbs = result.items.sumOf { it.scaledCarbsG }
            val totalFat = result.items.sumOf { it.scaledFatG }
            val totalVitA = result.items.sumOf { it.scaledVitaminAMcg }
            val totalVitC = result.items.sumOf { it.scaledVitaminCMg }
            val totalVitD = result.items.sumOf { it.scaledVitaminDMcg }
            val totalVitE = result.items.sumOf { it.scaledVitaminEMg }
            val totalVitB6 = result.items.sumOf { it.scaledVitaminB6Mg }
            val totalVitB12 = result.items.sumOf { it.scaledVitaminB12Mcg }
            val vitaminHighlights = buildList {
                if (totalVitA >= 1.0) add("A ${totalVitA.toInt()}mcg")
                if (totalVitC >= 0.5) add("C ${String.format(Locale.US, "%.1f", totalVitC)}mg")
                if (totalVitD >= 0.1) add("D ${String.format(Locale.US, "%.1f", totalVitD)}mcg")
                if (totalVitE >= 0.1) add("E ${String.format(Locale.US, "%.1f", totalVitE)}mg")
                if (totalVitB6 >= 0.05) add("B6 ${String.format(Locale.US, "%.2f", totalVitB6)}mg")
                if (totalVitB12 >= 0.05) add("B12 ${String.format(Locale.US, "%.2f", totalVitB12)}mcg")
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${result.mealTitle} • মোট $totalCal kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroChip("প্রোটিন", "${String.format(Locale.US, "%.1f", totalProtein)}g", ProteinAccent, Modifier.weight(1f))
                        MacroChip("শর্করা", "${String.format(Locale.US, "%.1f", totalCarbs)}g", CarbsAccent, Modifier.weight(1f))
                        MacroChip("ফ্যাট", "${String.format(Locale.US, "%.1f", totalFat)}g", FatAccent, Modifier.weight(1f))
                    }
                    if (vitaminHighlights.isNotEmpty()) {
                        Text(
                            text = "ভিটামিন: ${vitaminHighlights.joinToString(" • ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (result.nutritionNote.isNotBlank()) {
                        Text(text = result.nutritionNote, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            result.items.forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${item.scaledCalories} kcal (~${item.scaledGrams.toInt()}g) • প্রোটিন ${String.format(Locale.US, "%.1f", item.scaledProteinG)}g • শর্করা ${String.format(Locale.US, "%.1f", item.scaledCarbsG)}g • ফ্যাট ${String.format(Locale.US, "%.1f", item.scaledFatG)}g",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onRemoveItem(item.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "বাদ দিন")
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    val step = if (item.quantity > 10.0) 25.0 else 0.5
                                    onUpdateQuantity(item.id, (item.quantity - step).coerceAtLeast(0.25))
                                }
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "পরিমাণ কমান", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.1f", item.quantity)} ${item.unit}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FilledTonalButton(
                                onClick = {
                                    val step = if (item.quantity >= 10.0) 25.0 else 0.5
                                    onUpdateQuantity(item.id, item.quantity + step)
                                }
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "পরিমাণ বাড়ান", modifier = Modifier.size(16.dp))
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("গ্রাম", "কাপ", "পিস", "বাটি").forEach { unitLabel ->
                                FilterChip(
                                    selected = item.unit == unitLabel,
                                    onClick = { onUpdateUnit(item.id, unitLabel) },
                                    label = { Text(unitLabel) }
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { onSaveAnalyzed(result.items, selectedMeal, result.source) },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_ai_foods_button")
            ) {
                Text("${selectedMeal.labelBn}-তে যোগ করুন ($totalCal kcal)")
            }
        }
    }
}

@Composable
private fun MacroChip(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualAndFavoritesSection(
    selectedMeal: MealType,
    favorites: List<FavoriteFood>,
    onSaveSingleFood: (FoodLog) -> Unit,
    onAddFavorite: (FavoriteFood) -> Unit,
    onDeleteFavorite: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCustomForm by remember { mutableStateOf(false) }
    var servingMultiplier by remember { mutableDoubleStateOf(1.0) }

    OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        label = { Text("খাবার খুঁজুন (যেমন: ভাত, রুটি, ডিম, মাছ)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("search_food_input")
    )

    // Quick Serving Size Multiplier
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("সার্ভিং পরিমাণ:", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0.5, 1.0, 1.5, 2.0).forEach { mult ->
                FilterChip(
                    selected = servingMultiplier == mult,
                    onClick = { servingMultiplier = mult },
                    label = { Text("${mult}x") }
                )
            }
        }
    }

    OutlinedButton(
        onClick = { showCustomForm = !showCustomForm },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text(if (showCustomForm) "কাস্টম ফর্ম বন্ধ করুন" else "কাস্টম খাবার, প্রোটিন, শর্করা ও ফ্যাট লিখুন")
    }

    if (showCustomForm) {
        var name by remember { mutableStateOf("") }
        var qtyText by remember { mutableStateOf("1") }
        var unit by remember { mutableStateOf("বাটি") }
        var calText by remember { mutableStateOf("") }
        var proteinText by remember { mutableStateOf("") }
        var carbsText by remember { mutableStateOf("") }
        var fatText by remember { mutableStateOf("") }

        val pNum = proteinText.toDoubleOrNull() ?: 0.0
        val cNum = carbsText.toDoubleOrNull() ?: 0.0
        val fNum = fatText.toDoubleOrNull() ?: 0.0
        val macroEstimatedCal = ((pNum * 4.0) + (cNum * 4.0) + (fNum * 9.0)).roundToLong()

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("খাবারের নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("পরিমাণ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("একক (গ্রাম/কাপ/পিস)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = calText,
                        onValueChange = { calText = it.filter { c -> c.isDigit() } },
                        label = { Text("ক্যালোরি") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { proteinText = it },
                        label = { Text("প্রোটিন (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { carbsText = it },
                        label = { Text("শর্করা (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it },
                        label = { Text("ফ্যাট (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (macroEstimatedCal > 0 && calText.isBlank()) {
                    TextButton(onClick = { calText = macroEstimatedCal.toString() }) {
                        Text("ম্যাক্রো থেকে ক্যালোরি হিসাব করুন (~$macroEstimatedCal kcal)")
                    }
                }
                Button(
                    onClick = {
                        val cal = calText.toLongOrNull() ?: macroEstimatedCal
                        val qty = qtyText.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0
                        onSaveSingleFood(
                            FoodLog(
                                mealType = selectedMeal.name,
                                name = name.ifBlank { "কাস্টম খাবার" },
                                quantity = qty,
                                unit = unit.ifBlank { "সার্ভিং" },
                                grams = 100.0,
                                calories = cal,
                                proteinG = pNum,
                                carbsG = cNum,
                                fatG = fNum,
                                fiberG = 0.0,
                                source = FoodSource.MANUAL.name
                            )
                        )
                    },
                    enabled = name.isNotBlank() && ((calText.toLongOrNull() ?: macroEstimatedCal) > 0L),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("কাস্টম খাবার সেভ করুন")
                }
            }
        }
    }

    if (favorites.isNotEmpty()) {
        Text("আপনার ফেভারিট খাবার (১-ক্লিকে যোগ)", style = MaterialTheme.typography.titleMedium)
        favorites.forEach { fav ->
            val scaledQty = fav.defaultQuantity * servingMultiplier
            val scaledCal = (fav.calories * servingMultiplier).roundToLong()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(fav.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        text = "${String.format(Locale.US, "%.1f", scaledQty)} ${fav.unit} • $scaledCal kcal • প্রোটিন ${String.format(Locale.US, "%.1f", fav.proteinG * servingMultiplier)}g • শর্করা ${String.format(Locale.US, "%.1f", fav.carbsG * servingMultiplier)}g • ফ্যাট ${String.format(Locale.US, "%.1f", fav.fatG * servingMultiplier)}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onDeleteFavorite(fav.id) }) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "ফেভারিট সরান")
                }
                FilledTonalButton(
                    onClick = {
                        onSaveSingleFood(
                            FoodLog(
                                mealType = selectedMeal.name,
                                name = fav.name,
                                quantity = scaledQty,
                                unit = fav.unit,
                                grams = fav.grams * servingMultiplier,
                                calories = scaledCal,
                                proteinG = fav.proteinG * servingMultiplier,
                                carbsG = fav.carbsG * servingMultiplier,
                                fatG = fav.fatG * servingMultiplier,
                                fiberG = fav.fiberG * servingMultiplier,
                                vitaminAMcg = fav.vitaminAMcg * servingMultiplier,
                                vitaminCMg = fav.vitaminCMg * servingMultiplier,
                                vitaminDMcg = fav.vitaminDMcg * servingMultiplier,
                                vitaminEMg = fav.vitaminEMg * servingMultiplier,
                                vitaminB6Mg = fav.vitaminB6Mg * servingMultiplier,
                                vitaminB12Mcg = fav.vitaminB12Mcg * servingMultiplier,
                                source = FoodSource.FAVORITE.name
                            )
                        )
                    }
                ) {
                    Text("+ যোগ")
                }
            }
        }
        HorizontalDivider()
    }

    Text("জনপ্রিয় ও দেশীয় খাবারের তালিকা", style = MaterialTheme.typography.titleMedium)
    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) CommonFoodCatalog.items
        else CommonFoodCatalog.items.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }
    filtered.forEach { preset ->
        val scaledQty = preset.defaultQuantity * servingMultiplier
        val scaledCal = (preset.calories * servingMultiplier).roundToLong()
        val presetVitTags = buildList {
            val vitA = preset.vitaminAMcg * servingMultiplier
            val vitC = preset.vitaminCMg * servingMultiplier
            val vitD = preset.vitaminDMcg * servingMultiplier
            val vitE = preset.vitaminEMg * servingMultiplier
            val vitB6 = preset.vitaminB6Mg * servingMultiplier
            val vitB12 = preset.vitaminB12Mcg * servingMultiplier
            if (vitA >= 20.0) add("Vit A ${vitA.toInt()}mcg")
            if (vitC >= 3.0) add("Vit C ${vitC.toInt()}mg")
            if (vitD >= 0.5) add("Vit D ${String.format(Locale.US, "%.1f", vitD)}mcg")
            if (vitE >= 1.0) add("Vit E ${String.format(Locale.US, "%.1f", vitE)}mg")
            if (vitB6 >= 0.2) add("B6 ${String.format(Locale.US, "%.2f", vitB6)}mg")
            if (vitB12 >= 0.2) add("B12 ${String.format(Locale.US, "%.1f", vitB12)}mcg")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(preset.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${String.format(Locale.US, "%.1f", scaledQty)} ${preset.unit} • $scaledCal kcal • প্রোটিন ${String.format(Locale.US, "%.1f", preset.proteinG * servingMultiplier)}g • শর্করা ${String.format(Locale.US, "%.1f", preset.carbsG * servingMultiplier)}g • ফ্যাট ${String.format(Locale.US, "%.1f", preset.fatG * servingMultiplier)}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (presetVitTags.isNotEmpty()) {
                    Text(
                        text = presetVitTags.take(3).joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(
                onClick = {
                    onAddFavorite(
                        FavoriteFood(
                            name = preset.name,
                            defaultQuantity = preset.defaultQuantity,
                            unit = preset.unit,
                            grams = preset.grams,
                            calories = preset.calories.toLong(),
                            proteinG = preset.proteinG,
                            carbsG = preset.carbsG,
                            fatG = preset.fatG,
                            fiberG = preset.fiberG,
                            vitaminAMcg = preset.vitaminAMcg,
                            vitaminCMg = preset.vitaminCMg,
                            vitaminDMcg = preset.vitaminDMcg,
                            vitaminEMg = preset.vitaminEMg,
                            vitaminB6Mg = preset.vitaminB6Mg,
                            vitaminB12Mcg = preset.vitaminB12Mcg
                        )
                    )
                }
            ) {
                Icon(Icons.Filled.BookmarkAdd, contentDescription = "ফেভারিটে সেভ করুন")
            }
            FilledTonalButton(
                onClick = {
                    onSaveSingleFood(
                        FoodLog(
                            mealType = selectedMeal.name,
                            name = preset.name,
                            quantity = scaledQty,
                            unit = preset.unit,
                            grams = preset.grams * servingMultiplier,
                            calories = scaledCal,
                            proteinG = preset.proteinG * servingMultiplier,
                            carbsG = preset.carbsG * servingMultiplier,
                            fatG = preset.fatG * servingMultiplier,
                            fiberG = preset.fiberG * servingMultiplier,
                            vitaminAMcg = preset.vitaminAMcg * servingMultiplier,
                            vitaminCMg = preset.vitaminCMg * servingMultiplier,
                            vitaminDMcg = preset.vitaminDMcg * servingMultiplier,
                            vitaminEMg = preset.vitaminEMg * servingMultiplier,
                            vitaminB6Mg = preset.vitaminB6Mg * servingMultiplier,
                            vitaminB12Mcg = preset.vitaminB12Mcg * servingMultiplier,
                            source = FoodSource.MANUAL.name
                        )
                    )
                }
            ) {
                Text("+ যোগ")
            }
        }
    }
}

private fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }.getOrNull()
}
