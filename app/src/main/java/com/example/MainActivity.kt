package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AiNutritionService
import com.example.data.LocalGuestRepository
import com.example.data.NutritionRepository
import com.example.domain.DateUtils
import com.example.export.ReportExporter
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.configureAppCheck
import com.example.firebase.signOut
import com.example.model.MealType
import com.example.model.ThemeMode
import com.example.model.UiState
import com.example.ui.screens.AnalyticsHistoryScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExerciseScreen
import com.example.ui.screens.FoodEntrySheet
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.NutriPulseTheme
import com.example.ui.viewmodel.NutriPulseViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class AppTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("ড্যাশবোর্ড", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    EXERCISE("ব্যায়াম", Icons.Filled.DirectionsRun, Icons.Outlined.DirectionsRun),
    ANALYTICS("প্রোগ্রেস", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    PROFILE("প্রোফাইল", Icons.Filled.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any Compose UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val prefs = remember(context) {
        context.getSharedPreferences("nutripulse_prefs", Context.MODE_PRIVATE)
    }

    val isFirebaseReady = remember(context) { FirebaseApp.getApps(context).isNotEmpty() }

    var currentUser by remember {
        mutableStateOf(if (isFirebaseReady) Firebase.auth.currentUser else null)
    }
    var isGuestMode by remember {
        mutableStateOf(!isFirebaseReady || prefs.getBoolean("guest_mode", false))
    }
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }

    DisposableEffect(isFirebaseReady) {
        if (!isFirebaseReady) {
            return@DisposableEffect onDispose {}
        }
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                prefs.edit().putBoolean("guest_mode", false).apply()
                isGuestMode = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(isFirebaseReady) {
        if (isFirebaseReady && currentUser == null && !isGuestMode) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    NutriPulseTheme(themeMode = themeMode) {
        val user = currentUser
        when {
            user != null -> {
                val uid = user.uid
                key(uid) {
                    AuthenticatedApp(
                        currentUserId = uid,
                        displayName = user.displayName ?: "বন্ধু",
                        isGuestMode = false,
                        onThemeChange = { themeMode = it },
                        onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                        onSignOutClick = {
                            signOut(
                                credentialManager = credentialManager,
                                scope = scope,
                                onSignOutComplete = { currentUser = null }
                            )
                        }
                    )
                }
            }
            isGuestMode -> {
                key("guest_local") {
                    AuthenticatedApp(
                        currentUserId = "guest_local",
                        displayName = "অতিথি",
                        isGuestMode = true,
                        onThemeChange = { themeMode = it },
                        onAuthSuccess = {
                            prefs.edit().putBoolean("guest_mode", false).apply()
                            isGuestMode = false
                            currentUser = Firebase.auth.currentUser
                        },
                        onSignOutClick = {
                            prefs.edit().putBoolean("guest_mode", false).apply()
                            isGuestMode = false
                        }
                    )
                }
            }
            else -> {
                AuthScreen(
                    onAuthSuccess = {
                        prefs.edit().putBoolean("guest_mode", false).apply()
                        isGuestMode = false
                        currentUser = Firebase.auth.currentUser
                    },
                    onSkipLogin = {
                        prefs.edit().putBoolean("guest_mode", true).apply()
                        isGuestMode = true
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedApp(
    currentUserId: String,
    displayName: String,
    isGuestMode: Boolean,
    onThemeChange: (ThemeMode) -> Unit,
    onAuthSuccess: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: NutriPulseViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val dataSource = if (isGuestMode) {
                    LocalGuestRepository(app)
                } else {
                    val databaseId = app.getString(R.string.firestore_database_id)
                    NutritionRepository(FirebaseFirestore.getInstance(databaseId))
                }
                NutriPulseViewModel(
                    repository = dataSource,
                    aiService = AiNutritionService(),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName
                )
            }
        }
    )

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val aiParseState by viewModel.aiParseState.collectAsStateWithLifecycle()
    val aiCoachState by viewModel.aiCoachState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    LaunchedEffect(themeMode) {
        onThemeChange(themeMode)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedback) {
        val msg = feedback
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    var currentTab by rememberSaveable { mutableStateOf(AppTab.DASHBOARD) }
    var editingProfile by rememberSaveable { mutableStateOf(false) }
    var showFoodLoggerSheet by remember { mutableStateOf(false) }
    var initialMealForLogger by remember { mutableStateOf(MealType.LUNCH) }

    when (val state = dashboardState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("আপনার প্রোফাইল ও লগ লোড হচ্ছে...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        is UiState.Success -> {
            val summary = state.data
            if (!summary.profile.onboardingDone || editingProfile) {
                if (editingProfile) {
                    BackHandler { editingProfile = false }
                }
                OnboardingScreen(
                    existingProfile = summary.profile,
                    onSaveProfile = { name, age, gender, height, weight, targetW, act, goal, rate, customP, customC, customF ->
                        viewModel.saveProfileAndGoals(
                            existing = summary.profile,
                            displayName = name,
                            age = age,
                            gender = gender,
                            heightCm = height,
                            currentWeightKg = weight,
                            targetWeightKg = targetW,
                            activityLevel = act,
                            goalType = goal,
                            weeklyRateKg = rate,
                            customProteinTargetG = customP,
                            customCarbsTargetG = customC,
                            customFatTargetG = customF,
                            onComplete = { editingProfile = false }
                        )
                    }
                )
            } else {
                if (currentTab != AppTab.DASHBOARD) {
                    BackHandler { currentTab = AppTab.DASHBOARD }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 640.dp
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (useRail) {
                            NavigationRail {
                                AppTab.entries.forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = { Text(tab.label) }
                                    )
                                }
                            }
                        }

                        Scaffold(
                            contentWindowInsets = WindowInsets.safeDrawing,
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            bottomBar = {
                                if (!useRail) {
                                    NavigationBar {
                                        AppTab.entries.forEach { tab ->
                                            val selected = currentTab == tab
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { currentTab = tab },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = tab.label
                                                    )
                                                },
                                                label = { Text(tab.label) },
                                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                            )
                                        }
                                    }
                                }
                            },
                            floatingActionButton = {
                                if (currentTab == AppTab.DASHBOARD) {
                                    ExtendedFloatingActionButton(
                                        onClick = {
                                            initialMealForLogger = MealType.LUNCH
                                            showFoodLoggerSheet = true
                                        },
                                        icon = {
                                            Icon(Icons.Filled.CameraAlt, contentDescription = "AI স্ক্যান")
                                        },
                                        text = { Text("AI খাবার স্ক্যান") },
                                        modifier = Modifier.testTag("fab_ai_scan_food")
                                    )
                                }
                            }
                        ) { innerPadding ->
                            val contentModifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)

                            when (currentTab) {
                                AppTab.DASHBOARD -> DashboardScreen(
                                    summary = summary,
                                    aiCoachState = aiCoachState,
                                    onShiftDate = viewModel::shiftDate,
                                    onJumpToToday = { viewModel.selectDate(DateUtils.todayDateKey()) },
                                    onOpenFoodLogger = { meal ->
                                        initialMealForLogger = meal
                                        showFoodLoggerSheet = true
                                    },
                                    onOpenExerciseTab = { currentTab = AppTab.EXERCISE },
                                    onUpdateFoodLog = viewModel::updateFoodLog,
                                    onDeleteFoodLog = viewModel::deleteFoodLog,
                                    onAdjustWater = { delta ->
                                        viewModel.adjustWater(
                                            deltaMl = delta,
                                            currentAmountMl = summary.dayWaterLog?.amountMl ?: 0L,
                                            targetMl = summary.profile.waterTargetMl
                                        )
                                    },
                                    onGenerateCoachInsight = { viewModel.generateSmartCoachInsight(summary) },
                                    modifier = contentModifier
                                )

                                AppTab.EXERCISE -> ExerciseScreen(
                                    summary = summary,
                                    onAddExercise = viewModel::addExerciseLog,
                                    onDeleteExercise = viewModel::deleteExerciseLog,
                                    modifier = contentModifier
                                )

                                AppTab.ANALYTICS -> AnalyticsHistoryScreen(
                                    summary = summary,
                                    buildChartSeries = { days -> viewModel.buildChartSeries(summary, days) },
                                    onLogWeight = { weightKg, note ->
                                        viewModel.logWeight(weightKg, note, summary.profile)
                                    },
                                    onSelectHistoryDate = { dateKey ->
                                        viewModel.selectDate(dateKey)
                                        currentTab = AppTab.DASHBOARD
                                    },
                                    onExportCsv = {
                                        ReportExporter.exportAndShareCsv(
                                            context = context,
                                            profile = summary.profile,
                                            foodLogs = summary.allFoodLogs,
                                            exerciseLogs = summary.allExerciseLogs,
                                            waterLogs = summary.allWaterLogs,
                                            weightLogs = summary.weightLogs
                                        )
                                    },
                                    modifier = contentModifier
                                )

                                AppTab.PROFILE -> ProfileScreen(
                                    summary = summary,
                                    themeMode = themeMode,
                                    isGuestMode = isGuestMode,
                                    onChangeTheme = viewModel::setThemeMode,
                                    onEditBiometrics = { editingProfile = true },
                                    onUpdateMacroTargets = { p, c, f ->
                                        viewModel.updateMacroTargets(summary.profile, p, c, f)
                                    },
                                    onUpdateReminders = viewModel::updateReminders,
                                    onExportCsv = {
                                        ReportExporter.exportAndShareCsv(
                                            context = context,
                                            profile = summary.profile,
                                            foodLogs = summary.allFoodLogs,
                                            exerciseLogs = summary.allExerciseLogs,
                                            waterLogs = summary.allWaterLogs,
                                            weightLogs = summary.weightLogs
                                        )
                                    },
                                    onAuthSuccess = onAuthSuccess,
                                    onSignOut = onSignOutClick,
                                    modifier = contentModifier
                                )
                            }
                        }
                    }
                }

                if (showFoodLoggerSheet) {
                    FoodEntrySheet(
                        initialMealType = initialMealForLogger,
                        aiParseState = aiParseState,
                        favorites = summary.favorites,
                        onDismiss = { showFoodLoggerSheet = false },
                        onAnalyzeBitmap = viewModel::analyzeFoodBitmap,
                        onParseNaturalText = viewModel::parseNaturalLanguageFood,
                        onUpdateQuantity = viewModel::updateAnalyzedItemQuantity,
                        onUpdateUnit = viewModel::updateAnalyzedItemUnit,
                        onRemoveAnalyzedItem = viewModel::removeAnalyzedItem,
                        onSaveAnalyzed = { items, meal, source ->
                            viewModel.saveAnalyzedItems(items, meal, source) {
                                showFoodLoggerSheet = false
                            }
                        },
                        onSaveSingleFood = { log ->
                            viewModel.addSingleFoodLog(log) {
                                showFoodLoggerSheet = false
                            }
                        },
                        onAddFavorite = viewModel::addFavoriteFood,
                        onDeleteFavorite = viewModel::deleteFavoriteFood,
                        onResetAiState = viewModel::resetAiParseState
                    )
                }
            }
        }
    }
}
