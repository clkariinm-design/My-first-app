package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MacroBuilderScreen
import com.example.ui.screens.MacroExecutionScreen
import com.example.ui.screens.MacroRecorderScreen
import com.example.ui.theme.MacroFlowTheme
import com.example.ui.viewmodel.MacroViewModel

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object MacroBuilder : Screen("builder/{macroId}") {
        fun createRoute(macroId: Long) = "builder/$macroId"
    }
    object MacroRecorder : Screen("recorder")
    object MacroExecution : Screen("execution")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MacroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MacroFlowTheme {
                MacroFlowApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions(this)
    }
}

@Composable
fun MacroFlowApp(viewModel: MacroViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToBuilder = { macroId ->
                    navController.navigate(Screen.MacroBuilder.createRoute(macroId))
                },
                onNavigateToRecorder = {
                    navController.navigate(Screen.MacroRecorder.route)
                },
                onNavigateToExecution = {
                    navController.navigate(Screen.MacroExecution.route)
                }
            )
        }

        composable(
            route = Screen.MacroBuilder.route,
            arguments = listOf(
                navArgument("macroId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val macroId = backStackEntry.arguments?.getLong("macroId") ?: 0L
            MacroBuilderScreen(
                macroId = macroId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onExecute = {
                    navController.navigate(Screen.MacroExecution.route)
                }
            )
        }

        composable(Screen.MacroRecorder.route) {
            MacroRecorderScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = { newId ->
                    navController.popBackStack()
                    navController.navigate(Screen.MacroBuilder.createRoute(newId))
                }
            )
        }

        composable(Screen.MacroExecution.route) {
            MacroExecutionScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
