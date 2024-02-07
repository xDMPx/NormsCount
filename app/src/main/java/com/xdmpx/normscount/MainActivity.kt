package com.xdmpx.normscount

import android.content.Context
import android.os.Bundle
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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.coroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xdmpx.normscount.settings.Settings
import com.xdmpx.normscount.ui.theme.NormsCountTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")


class MainActivity : ComponentActivity() {
    private var counter = Counter()
    private var overrideOnKeyDown = true
    private val settings = Settings.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val counterValueKey = intPreferencesKey("counter_value")
        val counterValue: Flow<Int> = this.dataStore.data.catch { }.map { preferences ->
            preferences[counterValueKey] ?: 0
        }

        this.lifecycle.coroutineScope.launch {
            counter.setCount(counterValue.first())
        }

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

    override fun onPause() {
        this.lifecycle.coroutineScope.launch {
            saveCounterValue()
        }

        super.onPause()
    }

    private suspend fun saveCounterValue() {
        val counterValueKey = intPreferencesKey("counter_value")
        this@MainActivity.dataStore.edit { settings ->
            settings[counterValueKey] = counter.getCount()
        }
    }

}