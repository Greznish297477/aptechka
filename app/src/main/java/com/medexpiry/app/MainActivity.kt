package com.medexpiry.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

        // Удаляем старые APK обновлений, чтобы не засорять память
        AppUpdater.cleanupDownloadedApks(this)

        NotificationScheduler.scheduleAll(this)

        val app = application as MedExpiryApp

        setContent {
            MedExpiryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MedicineViewModel = viewModel(
                        factory = MedicineViewModelFactory(app.repository)
                    )
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: MedicineViewModel) {
    val navController = rememberNavController()
    val medicines by viewModel.medicines.collectAsState()

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            MedicineListScreen(
                medicines = medicines,
                onAddClick = { navController.navigate("add") },
                onItemClick = { medicine ->
                    navController.navigate("edit/${medicine.id}")
                },
                onDelete = { viewModel.deleteMedicine(it) },
                onImport = { list, replace, onDone ->
                    viewModel.importMedicines(list, replace, onDone)
                },
                getAllMedicines = { viewModel.getAllOnce() }
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
