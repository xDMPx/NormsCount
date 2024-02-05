package com.xdmpx.normscount

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
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
import com.xdmpx.normscount.settings.Settings
import com.xdmpx.normscount.ui.theme.NormsCountTheme

class MainActivity : ComponentActivity() {
    private var counter = Counter()
    private var overrideOnKeyDown = true
    private val settings = Settings.getInstance()

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
                        composable("main") {
                            MainUI() {
                                navController.navigate("settings")
                            }
                        }
                        composable("settings") {
                            overrideOnKeyDown = false
                            settings.SettingsUI {
                                navController.navigate("main")
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun MainUI(onNavigateToSettings: () -> Unit) {
        overrideOnKeyDown = true

        Scaffold(
            topBar = { counter.CounterTopAppBar(onNavigateToSettings) },
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                counter.CounterUI()
            }
        }
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d("MAIN", "$keyCode $overrideOnKeyDown")
        if (settings.changeCounterValueVolumeButtons && overrideOnKeyDown) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> counter.decrement()
                KeyEvent.KEYCODE_VOLUME_UP -> counter.increment()
                else -> return super.onKeyDown(keyCode, event)
            }
            return true
        } else {
            return super.onKeyDown(keyCode, event)
        }
    }

}