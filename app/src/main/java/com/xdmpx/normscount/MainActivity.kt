package com.xdmpx.normscount

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.coroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xdmpx.normscount.database.CounterDatabase
import com.xdmpx.normscount.database.CounterEntity
import com.xdmpx.normscount.settings.Settings
import com.xdmpx.normscount.ui.theme.NormsCountTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")


class MainActivity : ComponentActivity() {
    private val TAG_DEBUG = "MainActivity"
    private var counters = mutableStateListOf<Counter>()
    private lateinit var counter: MutableState<Counter>
    private var counterIndex = 0
    private var overrideOnKeyDown = true
    private val settings = Settings.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        counter = mutableStateOf(Counter(CounterEntity(value = 0), this@MainActivity))

        val counterValueKey = intPreferencesKey("counter_index")
        val savedCounterIndex: Flow<Int> = this.dataStore.data.catch { }.map { preferences ->
            preferences[counterValueKey] ?: 0
        }

        this.lifecycle.coroutineScope.launch {
            var counters = CounterDatabase.getInstance(this@MainActivity).counterDatabase.getAll()
                .map { counterEntity -> Counter(counterEntity, this@MainActivity) }
            if (counters.isEmpty()) {
                val counter = CounterEntity(value = 0)
                CounterDatabase.getInstance(this@MainActivity).counterDatabase.insert(counter)
                counters = CounterDatabase.getInstance(this@MainActivity).counterDatabase.getAll()
                    .map { counterEntity -> Counter(counterEntity, this@MainActivity) }
            }
            this@MainActivity.counters.clear()
            counters.forEach {
                this@MainActivity.counters.add(it)
            }
            counterIndex = savedCounterIndex.first()
            counter.value = counters[counterIndex]
            Log.d(TAG_DEBUG, "onCrate -> counterIndex: $counterIndex")
            Log.d(TAG_DEBUG, "onCrate -> counters: ${this@MainActivity.counters.size}")
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

    private fun addCounter() {
        this.lifecycle.coroutineScope.launch {
            val database = CounterDatabase.getInstance(this@MainActivity).counterDatabase
            val counter = CounterEntity(value = 0)
            database.insert(counter)
            counters.add(Counter(database.getAll().last(), this@MainActivity))
            counterIndex = counters.lastIndex
            this@MainActivity.counter.value = counters.last()
        }
    }

    @Composable
    fun MainUI(
        onNavigateToSettings: () -> Unit
    ) {
        ModalNavigationDrawer(
            gesturesEnabled = true,
            drawerState = rememberDrawerState(initialValue = DrawerValue.Open),
            drawerContent = { NavigationDrawerContent(Modifier.fillMaxWidth(0.7f)) },
        ) {
            // Screen content
            MainUIScreen(onNavigateToSettings)
        }
    }

    @Composable
    fun MainUIScreen(onNavigateToSettings: () -> Unit) {
        overrideOnKeyDown = true

        Scaffold(
            topBar = { counter.value.CounterTopAppBar(onNavigateToSettings) },
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                counter.value.CounterUI()
            }
        }
    }

    @Composable
    fun NavigationDrawerContent(modifier: Modifier) {
        ModalDrawerSheet(modifier = modifier) {
            Row {
                Text(
                    "Counters", modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(0.8f)
                )
                IconButton(onClick = { addCounter() }) {
                    Icon(Icons.Filled.Add, null)
                }
            }
            Divider()
            LazyColumn {
                itemsIndexed(counters) { index, counter ->
                    NavigationDrawerItem(label = { Text(text = "Counter #${counter.getCounterId()}") },
                        selected = false,
                        onClick = {
                            this@MainActivity.counter.value = counter
                            this@MainActivity.counterIndex = index
                        })
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (settings.changeCounterValueVolumeButtons && overrideOnKeyDown) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> counter.value.decrement()
                KeyEvent.KEYCODE_VOLUME_UP -> counter.value.increment()
                else -> return super.onKeyDown(keyCode, event)
            }
            return true
        } else {
            return super.onKeyDown(keyCode, event)
        }
    }

    override fun onStop() {
        this.lifecycle.coroutineScope.launch {
            saveCounterIndex()
            counters.forEach { it.updateDatabase() }
        }

        super.onStop()
    }

    private suspend fun saveCounterIndex() {
        val counterIndexKey = intPreferencesKey("counter_index")
        this@MainActivity.dataStore.edit { settings ->
            Log.d(TAG_DEBUG, "saveCounterIndex -> counterIndex: $counterIndex")
            settings[counterIndexKey] = counterIndex
        }
    }

}