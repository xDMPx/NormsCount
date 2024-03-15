package com.xdmpx.normscount

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xdmpx.normscount.Utils.ShortToast
import com.xdmpx.normscount.about.About
import com.xdmpx.normscount.counter.Counter
import com.xdmpx.normscount.counter.CounterUIHelper
import com.xdmpx.normscount.database.CounterDatabase
import com.xdmpx.normscount.database.CounterEntity
import com.xdmpx.normscount.datastore.ThemeType
import com.xdmpx.normscount.settings.Settings
import com.xdmpx.normscount.settings.SettingsUI
import com.xdmpx.normscount.ui.theme.NormsCountTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    private val TAG_DEBUG = "MainActivity"
    private var counters = mutableStateListOf<Counter?>()
    private var usePureDark = mutableStateOf(false)
    private var useDynamicColor = mutableStateOf(false)
    private var theme = mutableStateOf(ThemeType.SYSTEM)
    private lateinit var counter: MutableState<Counter>
    private var counterID = 1
    private var lastCounterID = 1
    private var overrideOnKeyDown = true
    private val settingsInstance = Settings.getInstance()

    private val scopeIO = CoroutineScope(Dispatchers.IO)
    private val createDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                scopeIO.launch {
                    if (Utils.exportToJSON(this@MainActivity, uri)) {
                        runOnUiThread {
                            ShortToast(
                                this@MainActivity, resources.getString(R.string.export_successful)
                            )
                        }
                    } else {
                        runOnUiThread {
                            ShortToast(
                                this@MainActivity, resources.getString(R.string.export_failed)
                            )
                        }
                    }
                }
            }
        }
    private val openDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                val addCounter: (String, Long) -> Unit =
                    { name, value -> scopeIO.launch { addCounter(name, value) } }
                scopeIO.launch {
                    counters.forEach {
                        it?.updateDatabase()
                    }
                    if (Utils.importFromJSON(this@MainActivity, uri) { name, value ->
                            addCounter(
                                name, value
                            )
                        }) {
                        runOnUiThread {
                            ShortToast(
                                this@MainActivity, resources.getString(R.string.import_successful)
                            )
                        }
                    } else {
                        runOnUiThread {
                            ShortToast(
                                this@MainActivity, resources.getString(R.string.import_failed)
                            )
                        }
                    }
                }
            }
        }

    init {
        settingsInstance.registerOnExportClick { this@MainActivity.exportToJson() }
        settingsInstance.registerOnImportClick { this@MainActivity.importFromJson() }
        settingsInstance.registerOnDeleteAllClick { this@MainActivity.deleteAll() }
        settingsInstance.registerOnThemeUpdate { usePureDark, useDynamicColor, theme ->
            this@MainActivity.usePureDark.value = usePureDark
            this@MainActivity.useDynamicColor.value = useDynamicColor
            this@MainActivity.theme.value = theme
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deleteCounter: (Counter) -> Unit = { scopeIO.launch { deleteCounter(it) } }

        setKeepScreenOnFlag()
        counter = mutableStateOf(
            Counter(
                CounterEntity(value = 0), { deleteCounter(it) }, this@MainActivity
            )
        )

        val counterIDKey = intPreferencesKey("counter_id")
        val savedCounterID: Flow<Int> = this.dataStore.data.catch { }.map { preferences ->
            preferences[counterIDKey] ?: 1
        }

        scopeIO.launch {
            settingsInstance.loadSettings(this@MainActivity)
            val settings = settingsInstance.settingsState.value
            usePureDark.value = settings.usePureDark
            useDynamicColor.value = settings.useDynamicColor
            theme.value = settings.theme
            setKeepScreenOnFlag()

            var counters = CounterDatabase.getInstance(this@MainActivity).counterDatabase.getAll()
                .map { counterEntity ->
                    Counter(
                        counterEntity, { deleteCounter(it) }, this@MainActivity
                    )
                }
            if (counters.isEmpty()) {
                val counter = CounterEntity(value = 0)
                CounterDatabase.getInstance(this@MainActivity).counterDatabase.insert(counter)
                counters = CounterDatabase.getInstance(this@MainActivity).counterDatabase.getAll()
                    .map { counterEntity ->
                        Counter(
                            counterEntity, { deleteCounter(it) }, this@MainActivity
                        )
                    }
            }
            this@MainActivity.counters.clear()
            counters.forEach {
                this@MainActivity.counters.add(it)
            }
            counterID = savedCounterID.first()
            counter.value =
                counters.find { counter -> counterID == counter.getCounterId() } ?: counters.first()
                    .also { counterID = it.getCounterId() }

            this@MainActivity.lastCounterID =
                CounterDatabase.getInstance(this@MainActivity).counterDatabase.getLastID() ?: 1
            Log.d(TAG_DEBUG, "onCrate -> counterID: $counterID")
            Log.d(TAG_DEBUG, "onCrate -> lastCounterID: $lastCounterID")
            Log.d(TAG_DEBUG, "onCrate -> counters: ${this@MainActivity.counters.size}")
        }

        setContent {
            NormsCountTheme(
                theme = theme.value,
                pureDarkTheme = usePureDark.value,
                dynamicColor = useDynamicColor.value
            ) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            MainUI(onNavigateToSettings = { navController.navigate("settings") }) {
                                navController.navigate("about")
                            }
                        }
                        composable("settings") {
                            overrideOnKeyDown = false
                            SettingsUI.SettingsUI(settingsInstance) {
                                navController.navigate("main")
                                setKeepScreenOnFlag()
                            }
                        }
                        composable("about") {
                            overrideOnKeyDown = false
                            About.AboutUI {
                                navController.navigate("main")
                                setKeepScreenOnFlag()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setKeepScreenOnFlag() {
        val settings = settingsInstance.settingsState.value
        Log.d(TAG_DEBUG, "setKeepScreenOnFlag -> ${settings.keepScreenOn} ")
        if (settings.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private suspend fun addCounter(name: String = "Counter #", value: Long = 0) {
        val deleteCounter: (Counter) -> Unit = { scopeIO.launch { deleteCounter(it) } }

        val database = CounterDatabase.getInstance(this@MainActivity).counterDatabase
        val counter = CounterEntity(name = name, value = value)
        database.insert(counter)
        val counterEntity = database.getLast()!!
        counters.add(Counter(counterEntity, { deleteCounter(it) }, this@MainActivity))
        counterID = counterEntity.id
        lastCounterID = counterID
        this@MainActivity.counter.value = counters.last()!!
    }

    private suspend fun deleteCounter(counter: Counter) {
        var index = counters.indexOf(counter)
        counters[index] = null
        index = if (index > 0) index - 1 else 0
        while (index != 0 && counters[index] == null) index -= 1
        while (index != counters.size && counters[index] == null) index += 1
        if (index == counters.size) {
            addCounter()
        } else {
            this@MainActivity.counter.value = counters[index]!!
            counterID = this@MainActivity.counter.value.getCounterId()
        }
    }

    @Composable
    fun MainUI(
        onNavigateToSettings: () -> Unit, onNavigateToAbout: () -> Unit
    ) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        val closeDrawer: () -> Unit = {
            scope.launch {
                drawerState.close()
            }
        }

        val openDrawer: () -> Unit = {
            scope.launch {
                drawerState.open()
            }
        }

        ModalNavigationDrawer(
            gesturesEnabled = true,
            drawerState = drawerState,
            drawerContent = { NavigationDrawerContent(closeDrawer, Modifier.fillMaxWidth(0.7f)) },
        ) {
            // Screen content
            MainUIScreen(openDrawer, onNavigateToSettings, onNavigateToAbout)
        }
    }

    @Composable
    fun MainUIScreen(
        onNavigationIconClick: () -> Unit,
        onNavigateToSettings: () -> Unit,
        onNavigateToAbout: () -> Unit
    ) {
        overrideOnKeyDown = true

        Scaffold(
            topBar = {
                counter.value.CounterTopAppBar(
                    onNavigationIconClick, onNavigateToSettings, onNavigateToAbout
                )
            },
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                counter.value.CounterUI()
            }
        }
    }

    @Composable
    fun NavigationDrawerContent(
        closeDrawer: () -> Unit, modifier: Modifier
    ) {
        val settings by settingsInstance.settingsState.collectAsState()
        var openEditDialog by remember { mutableStateOf(false) }

        ModalDrawerSheet(modifier = modifier) {
            Row {
                Text(
                    stringResource(R.string.navigation_drawer_title),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(0.8f)
                )
                IconButton(onClick = {
                    if (settings.askForInitialValuesWhenNewCounter) {
                        openEditDialog = true
                    } else {
                        scopeIO.launch {
                            addCounter()
                        }
                    }
                    closeDrawer()
                }) {
                    Icon(Icons.Filled.Add, null)
                }
            }
            Divider()
            LazyColumn {
                items(counters) { counter ->
                    if (counter == null) return@items
                    NavigationDrawerItem(label = { counter.CounterName() },
                        selected = false,
                        onClick = {
                            this@MainActivity.counter.value = counter
                            this@MainActivity.counterID = counter.getCounterId()
                            closeDrawer()
                        })
                }
            }
        }

        if (openEditDialog) {
            CounterUIHelper.EditAlertDialog(
                "Counter #${lastCounterID + 1}",
                0,
                onDismissRequest = { openEditDialog = false }) { name, value ->
                openEditDialog = false
                scopeIO.launch {
                    addCounter(name, value)
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val settings = settingsInstance.settingsState.value
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
        scopeIO.launch {
            saveCounterID()
            settingsInstance.saveSettings(this@MainActivity)
            counters.forEach {
                it?.updateDatabase()
            }
        }

        super.onStop()
    }

    private suspend fun saveCounterID() {
        val counterIDKey = intPreferencesKey("counter_id")
        this@MainActivity.dataStore.edit { settings ->
            Log.d(TAG_DEBUG, "saveCounterID -> counterID: $counterID")
            settings[counterIDKey] = counterID
        }
    }

    private fun exportToJson() {
        scopeIO.launch {
            counters.forEach {
                it?.updateDatabase()
            }
            Log.d(TAG_DEBUG, "exportToJson")

            val date = LocalDate.now()
            val year = date.year
            val month = String.format("%02d", date.monthValue)
            val day = date.dayOfMonth
            createDocument.launch("counters_export_${year}_${month}_$day.json")
        }
    }

    private fun importFromJson() {
        openDocument.launch(arrayOf("application/json"))
    }

    private fun deleteAll() {
        counters.filterNotNull().forEach {
            it.delete()
            scopeIO.launch {
                deleteCounter(it)
            }
        }
    }

}