package com.xdmpx.normscount

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.KeyEvent
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
import com.xdmpx.normscount.counter.CounterUI
import com.xdmpx.normscount.counter.CounterUIHelper
import com.xdmpx.normscount.counter.CountersViewModel
import com.xdmpx.normscount.counter.CountersViewModelInstance
import com.xdmpx.normscount.database.CounterDatabase
import com.xdmpx.normscount.settings.Settings
import com.xdmpx.normscount.settings.SettingsUI
import com.xdmpx.normscount.ui.theme.NormsCountTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    private val TAG_DEBUG = "MainActivity"
    private var overrideOnKeyDown = true
    private val settingsInstance = Settings.getInstance()
    private val scopeIO = CoroutineScope(Dispatchers.IO)
    private val counters by viewModels<CountersViewModel>()

    private val createDocumentJSON =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            exportToJSONCallback(uri)
        }
    private val createDocumentCSV =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            exportToCSVCallback(uri)
        }
    private val openDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            importFromJSONCallback(uri)
        }
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        settingsInstance.setNotification(isGranted)
    }

    init {
        settingsInstance.registerOnExportJSONClick { this@MainActivity.exportToJSON() }
        settingsInstance.registerOnExportCSVClick { this@MainActivity.exportToCSV() }
        settingsInstance.registerOnImportClick { this@MainActivity.importFromJSON() }
        settingsInstance.registerOnDeleteAllClick { this@MainActivity.deleteAll() }
        settingsInstance.registerOnNotificationClick { this@MainActivity.requestNotificationPermission() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        createNotificationChannel()

        CountersViewModelInstance.setInstance(counters)

        setContent {
            val settings by settingsInstance.settingsState.collectAsState()
            val view = LocalView.current
            val loadSettings = rememberSaveable { mutableStateOf(true) }
            val loadCounters = rememberSaveable { mutableStateOf(true) }

            LaunchedEffect(loadSettings) {
                if (loadSettings.value) {
                    loadSettings.value = false
                    Log.i("MainActivity", "Setting Load")
                    settingsInstance.loadSettings(this@MainActivity)
                }
            }
            LaunchedEffect(settings.keepScreenOn) {
                Log.i("MainActivity", "KeepScreenOn-> ${settings.keepScreenOn}")
                val window = (view.context as Activity).window
                setKeepScreenOnFlag(window, settings.keepScreenOn)
            }
            LaunchedEffect(loadCounters) {
                if (loadCounters.value) {
                    loadCounters.value = false
                    Log.i("MainActivity", "Counters Load")
                    loadCountersFromDatabase()
                }
            }

            NormsCountTheme(
                theme = settings.theme,
                pureDarkTheme = settings.usePureDark,
                dynamicColor = settings.useDynamicColor
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
                            }
                        }
                        composable("about") {
                            overrideOnKeyDown = false
                            About.AboutUI {
                                navController.navigate("main")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setKeepScreenOnFlag(window: Window, keepScreenOn: Boolean) {
        Log.d(TAG_DEBUG, "setKeepScreenOnFlag -> $keepScreenOn")
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private suspend fun addCounter(name: String? = null, value: Long = 0) {
        counters.addCounter(this@MainActivity, name, value)

        val counter = counters.last()
        this@MainActivity.counters.setCurrentCounter(
            counter.id, counter.counterState.value.name, counter.counterState.value.count
        )
    }

    private suspend fun deleteCounter(counterId: Int) {
        var index = counters.indexOfFirst { it.id == counterId }
        counters.deleteCounterById(this@MainActivity, counterId)
        index = if (index > 0) index - 1 else 0
        if (index == counters.size()) {
            addCounter()
        } else {
            val counter = counters[index]
            this@MainActivity.counters.setCurrentCounter(
                counter.id, counter.counterState.value.name, counter.counterState.value.count
            )
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
            val i = counters.indexOfFirst { it.id == counters.currentCounterState.value.id }
            if (i != -1) {
                counters[i].setCounterValue(counters.currentCounterState.value.counterState.value.count)
                counters[i].setCounterName(counters.currentCounterState.value.counterState.value.name)
            }
        }

        val landscapeOrientation =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

        ModalNavigationDrawer(
            gesturesEnabled = true,
            drawerState = drawerState,
            drawerContent = {
                if (!landscapeOrientation) {
                    NavigationDrawerContent(closeDrawer, Modifier.fillMaxWidth(0.7f))
                } else {
                    NavigationDrawerContent(closeDrawer, Modifier.requiredWidth(300.dp))
                }
            },
        ) {
            // Screen content
            MainUIScreen(openDrawer, onNavigateToSettings, onNavigateToAbout, onDeleteCounter = {
                scopeIO.launch {
                    deleteCounter(this@MainActivity.counters.currentCounterState.value.id)
                }
            })
        }
    }

    @Composable
    fun MainUIScreen(
        onNavigationIconClick: () -> Unit,
        onNavigateToSettings: () -> Unit,
        onNavigateToAbout: () -> Unit,
        onDeleteCounter: () -> Unit,
    ) {
        overrideOnKeyDown = true
        val counterViewModel by counters.currentCounterState.collectAsState()

        Scaffold(
            topBar = {
                CounterUI.CounterTopAppBar(
                    counterViewModel,
                    onNavigationIconClick,
                    onNavigateToSettings,
                    onNavigateToAbout,
                    onDeleteCounter
                )
            },
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                CounterUI.CounterUI(counterViewModel)
            }
        }
    }

    @Composable
    fun NavigationDrawerContent(
        closeDrawer: () -> Unit, modifier: Modifier
    ) {
        val settings by settingsInstance.settingsState.collectAsState()
        val counters by counters.countersState.collectAsState()
        var openEditDialog by remember { mutableStateOf(false) }
        var lastCounterId by remember { mutableIntStateOf(1) }
        LaunchedEffect(counters.countersViewModels.size) {
            val id = CounterDatabase.getInstance(this@MainActivity).counterDatabase.getLastID() ?: 1
            lastCounterId = id
            Log.d(TAG_DEBUG, "Effect -> lastCounterID: $lastCounterId")
        }


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
            HorizontalDivider()
            LazyColumn {
                items(counters.countersViewModels) { counter ->
                    NavigationDrawerItem(
                        label = { CounterUI.CounterName(counter) },
                        selected = false,
                        onClick = {
                            this@MainActivity.counters.setCurrentCounter(
                                counter.id,
                                counter.counterState.value.name,
                                counter.counterState.value.count
                            )
                            closeDrawer()
                        })
                }
            }
        }

        EditAlertDialog(
            opened = openEditDialog,
            lastCounterId,
            onDismissRequest = { openEditDialog = false }) { name, value ->
            openEditDialog = false
            scopeIO.launch {
                addCounter(name, value)
            }
        }
    }

    @Composable
    private fun EditAlertDialog(
        opened: Boolean,
        lastCounterID: Int,
        onDismissRequest: () -> Unit,
        onConfirmation: (String, Long) -> Unit
    ) {
        if (!opened) return
        CounterUIHelper.EditAlertDialog(
            "Counter #${lastCounterID + 1}",
            0,
            onDismissRequest = onDismissRequest,
            onConfirmation = onConfirmation
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val settings = settingsInstance.settingsState.value
        if (settings.changeCounterValueVolumeButtons && overrideOnKeyDown) {
            val vibrator = this@MainActivity.getSystemService(Vibrator::class.java)
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    counters.currentCounterState.value.decrementCounter()
                    if (settings.vibrateOnValueChange) vibrator.vibrate(
                        VibrationEffect.createPredefined(
                            VibrationEffect.EFFECT_CLICK
                        )
                    )
                }

                KeyEvent.KEYCODE_VOLUME_UP -> {
                    counters.currentCounterState.value.incrementCounter()
                    if (settings.vibrateOnValueChange) vibrator.vibrate(
                        VibrationEffect.createPredefined(
                            VibrationEffect.EFFECT_CLICK
                        )
                    )
                }

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
            val i = counters.indexOfFirst { it.id == counters.currentCounterState.value.id }
            if (i != -1) {
                counters[i].setCounterValue(counters.currentCounterState.value.counterState.value.count)
                counters[i].setCounterName(counters.currentCounterState.value.counterState.value.name)
            }
            counters.forEach {
                it.updateDatabase(this@MainActivity)
            }
        }

        super.onStop()
    }

    private suspend fun saveCounterID() {
        val counterID = counters.currentCounterState.value.id
        val counterIDKey = intPreferencesKey("counter_id")
        this@MainActivity.dataStore.edit { settings ->
            Log.d(TAG_DEBUG, "saveCounterID -> counterID: $counterID")
            settings[counterIDKey] = counterID
        }
    }

    private fun exportToJSON() {
        scopeIO.launch {
            counters.forEach {
                it.updateDatabase(this@MainActivity)
            }
            Log.d(TAG_DEBUG, "exportToJson")

            val date = LocalDate.now()
            val year = date.year
            val month = String.format(null, "%02d", date.monthValue)
            val day = date.dayOfMonth
            createDocumentJSON.launch("counters_export_${year}_${month}_$day.json")
        }
    }

    private fun exportToCSV() {
        scopeIO.launch {
            counters.forEach {
                it.updateDatabase(this@MainActivity)
            }
            Log.d(TAG_DEBUG, "exportToCSV")

            val date = LocalDate.now()
            val year = date.year
            val month = String.format(null, "%02d", date.monthValue)
            val day = date.dayOfMonth
            createDocumentCSV.launch("counters_export_${year}_${month}_$day.csv")
        }
    }

    private fun exportToJSONCallback(uri: Uri?) {
        if (uri == null) return

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

    private fun exportToCSVCallback(uri: Uri?) {
        if (uri == null) return

        scopeIO.launch {
            if (Utils.exportToCSV(this@MainActivity, uri)) {
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

    private fun importFromJSON() {
        openDocument.launch(arrayOf("application/json"))
    }

    private fun importFromJSONCallback(uri: Uri?) {
        if (uri == null) return

        val addCounters: (Array<Utils.CounterData>) -> Unit = {
            scopeIO.launch {
                it.forEach {
                    addCounter(it.name, it.value)
                }
            }
        }
        scopeIO.launch {
            counters.forEach {
                it.updateDatabase(this@MainActivity)
            }
            if (Utils.importFromJSON(this@MainActivity, uri, addCounters)) {
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

    private fun deleteAll() {
        scopeIO.launch {
            counters.forEach {
                deleteCounter(it.id)
            }
        }
    }

    private fun createNotificationChannel() {
        val name = this.getString(this.applicationInfo.labelRes)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(name, name, importance)
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_DENIED
            ) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        ("package:" + BuildConfig.APPLICATION_ID).toUri()
                    ).apply { startActivity(this) }
                }
            } else {
                settingsInstance.setNotification(true)
            }
        }
    }

    private fun loadCountersFromDatabase() {
        val counterIDKey = intPreferencesKey("counter_id")
        val savedCounterID: Flow<Int> = dataStore.data.map { preferences ->
            preferences[counterIDKey] ?: 1
        }
        scopeIO.launch {
            this@MainActivity.counters.loadCountersFromDatabase(this@MainActivity)
            if (counters.countersState.value.countersViewModels.isEmpty()) {
                addCounter()
                loadCountersFromDatabase()
                return@launch
            }

            val counterID = savedCounterID.first()
            val counter =
                counters.countersState.value.countersViewModels.find { counter -> counterID == counter.id }
                    ?: counters.countersState.value.countersViewModels.first()

            this@MainActivity.counters.setCurrentCounter(
                counter.id, counter.counterState.value.name, counter.counterState.value.count
            )

            Log.d(TAG_DEBUG, "onCrate -> counterID: $counterID")
            Log.d(TAG_DEBUG, "onCrate -> counters: ${this@MainActivity.counters.size()}")
        }
    }

}
