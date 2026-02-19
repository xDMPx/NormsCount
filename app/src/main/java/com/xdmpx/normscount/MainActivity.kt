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
    private val countersViewModel by viewModels<CountersViewModel>()

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
        settingsInstance.registerOnDeleteAllClick {
            scopeIO.launch {
                this@MainActivity.countersViewModel.deleteAllCounters(
                    this@MainActivity
                )
            }
        }
        settingsInstance.registerOnNotificationClick { this@MainActivity.requestNotificationPermission() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        createNotificationChannel()

        CountersViewModelInstance.setInstance(countersViewModel)

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
            this@MainActivity.countersViewModel.synchronizeCountersWithCurrentCounter()
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
                    this@MainActivity.countersViewModel.deleteCounterById(
                        this@MainActivity,
                        this@MainActivity.countersViewModel.getCurrentCounterId()
                    )
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

        Scaffold(
            topBar = {
                CounterUI.CounterTopAppBar(
                    countersViewModel,
                    onNavigationIconClick,
                    onNavigateToSettings,
                    onNavigateToAbout,
                    onDeleteCounter
                )
            },
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                CounterUI.CounterUI(countersViewModel)
            }
        }
    }

    @Composable
    fun NavigationDrawerContent(
        closeDrawer: () -> Unit, modifier: Modifier
    ) {
        val settings by settingsInstance.settingsState.collectAsState()
        val counters by countersViewModel.countersState.collectAsState()
        var openEditDialog by remember { mutableStateOf(false) }
        var lastCounterId by remember { mutableIntStateOf(1) }
        LaunchedEffect(counters.countStates.size) {
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
                            this@MainActivity.countersViewModel.addCounter(
                                this@MainActivity, null, 0
                            )
                        }
                    }
                    closeDrawer()
                }) {
                    Icon(Icons.Filled.Add, null)
                }
            }
            HorizontalDivider()
            LazyColumn {
                items(counters.countStates) { counter ->
                    NavigationDrawerItem(
                        label = { CounterUI.CounterName(counter) },
                        selected = false,
                        onClick = {
                            this@MainActivity.countersViewModel.synchronizeCurrentCounterWithDatabase(
                                this@MainActivity
                            )
                            this@MainActivity.countersViewModel.setCurrentCounter(
                                counter.id, counter.name, counter.count
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
                this@MainActivity.countersViewModel.synchronizeCurrentCounterWithDatabase(this@MainActivity)
                this@MainActivity.countersViewModel.addCounter(this@MainActivity, null, 0)
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
                    countersViewModel.decrementCurrentCounter()
                    if (settings.vibrateOnValueChange) vibrator.vibrate(
                        VibrationEffect.createPredefined(
                            VibrationEffect.EFFECT_CLICK
                        )
                    )
                }

                KeyEvent.KEYCODE_VOLUME_UP -> {
                    countersViewModel.incrementCurrentCounter()
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
            this@MainActivity.countersViewModel.synchronizeCountersWithCurrentCounter()
            this@MainActivity.countersViewModel.updateDatabase(this@MainActivity)
        }

        super.onStop()
    }

    private suspend fun saveCounterID() {
        val counterID = countersViewModel.getCurrentCounterId()
        val counterIDKey = intPreferencesKey("counter_id")
        this@MainActivity.dataStore.edit { settings ->
            Log.d(TAG_DEBUG, "saveCounterID -> counterID: $counterID")
            settings[counterIDKey] = counterID
        }
    }

    private fun exportToJSON() {
        scopeIO.launch {
            this@MainActivity.countersViewModel.updateDatabase(this@MainActivity)
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
            this@MainActivity.countersViewModel.updateDatabase(this@MainActivity)
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
                it.forEach { c ->
                    this@MainActivity.countersViewModel.addCounter(
                        this@MainActivity, c.name, c.value
                    )
                }
            }
        }
        scopeIO.launch {
            this@MainActivity.countersViewModel.updateDatabase(this@MainActivity)
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
            this@MainActivity.countersViewModel.loadCountersFromDatabase(this@MainActivity)
            if (countersViewModel.getCountersState().isEmpty()) {
                this@MainActivity.countersViewModel.addCounter(this@MainActivity, null, 0)
                loadCountersFromDatabase()
                return@launch
            }

            val counterID = savedCounterID.first()
            val counter =
                countersViewModel.getCountersState().find { counter -> counterID == counter.id }
                    ?: countersViewModel.getCountersState().first()

            this@MainActivity.countersViewModel.setCurrentCounter(
                counter.id, counter.name, counter.count
            )

            Log.d(TAG_DEBUG, "onCrate -> counterID: $counterID")
            Log.d(
                TAG_DEBUG,
                "onCrate -> counters: ${this@MainActivity.countersViewModel.getCountersState().size}"
            )
        }
    }

}
