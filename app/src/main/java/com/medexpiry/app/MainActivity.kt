package com.medexpiry.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.medexpiry.app.data.UserPreferences
import com.medexpiry.app.ui.*
import com.medexpiry.app.util.AppUpdater
import com.medexpiry.app.worker.NotificationScheduler

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        AppUpdater.cleanupDownloadedApks(this)
        NotificationScheduler.scheduleAll(this)

        val app = application as MedExpiryApp
        val preferences = UserPreferences.get(this)

        setContent {
            val settings by preferences.settings.collectAsState()

            MedExpiryTheme(
                themeMode = settings.themeMode,
                colorStyle = settings.colorStyle,
                textSize = settings.textSize,
                customPrimaryColor = settings.customPrimaryColor,
                liquidGlass = settings.liquidGlass
            ) {
                val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
                val glassDark = when (settings.themeMode) {
                    com.medexpiry.app.data.ThemeMode.DARK -> true
                    com.medexpiry.app.data.ThemeMode.LIGHT -> false
                    com.medexpiry.app.data.ThemeMode.SYSTEM -> systemDark
                }
                val glassBrush = if (glassDark) {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF111827), Color(0xFF26364F), Color(0xFF172B3D))
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFE8F2FF), Color(0xFFF4EAFE), Color(0xFFDDF7F4))
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (settings.liquidGlass) Modifier.background(glassBrush) else Modifier)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (settings.liquidGlass) Color.Transparent else MaterialTheme.colorScheme.background
                    ) {
                        val viewModel: MedicineViewModel = viewModel(
                            factory = MedicineViewModelFactory(app.repository)
                        )
                        AppNavigation(viewModel, preferences)
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: MedicineViewModel,
    preferences: UserPreferences
) {
    val navController = rememberNavController()
    val medicines by viewModel.medicines.collectAsState()
    val settings by preferences.settings.collectAsState()

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            MedicineListScreen(
                medicines = medicines,
                settings = settings,
                onAddClick = { navController.navigate("add") },
                onItemClick = { medicine ->
                    navController.navigate("edit/${medicine.id}")
                },
                onDelete = { viewModel.deleteMedicine(it) },
                onImport = { list, replace, onDone ->
                    viewModel.importMedicines(list, replace, onDone)
                },
                getAllMedicines = { viewModel.getAllOnce() },
                onOpenSettings = { navController.navigate("settings") }
            )
        }

        composable("settings") {
            SettingsScreen(
                preferences = preferences,
                onBack = { navController.popBackStack() }
            )
        }

        composable("add") {
            AddEditMedicineScreen(
                onSave = { medicine ->
                    viewModel.addMedicine(medicine)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            val medicine = medicines.find { it.id == id }
            if (medicine != null) {
                AddEditMedicineScreen(
                    medicine = medicine,
                    onSave = { updated ->
                        viewModel.updateMedicine(updated)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
