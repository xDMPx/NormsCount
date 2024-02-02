package com.xdmpx.normscount

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xdmpx.normscount.ui.theme.NormsCountTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NormsCountTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") { MainUI() { navController.navigate("settings") } }
                        composable("settings") { Settings() { navController.navigate("main") } }
                    }
                }
            }
        }
    }

    @Composable
    fun MainUI(onNavigateToSettings: () -> Unit) {
        val counter = Counter()

        Scaffold(
            topBar = { counter.CounterTopAppBar(onNavigateToSettings) },
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                counter.CounterUI()
            }
        }
    }

    @Composable
    fun Settings(onNavigateToMain: () -> Unit) {
        Scaffold(
            topBar = { },
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {}
        }
    }

}