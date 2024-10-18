package com.xdmpx.normscount

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import com.xdmpx.normscount.counter.CounterUI
import com.xdmpx.normscount.counter.CounterUIHelper
import com.xdmpx.normscount.database.CounterDatabase
import com.xdmpx.normscount.database.CounterEntity
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
    private lateinit var counter: MutableState<Counter>
    private var counterID = 1
    private var lastCounterID = 1
    private var overrideOnKeyDown = true
    private val settingsInstance = Settings.getInstance()

    private val scopeIO = CoroutineScope(Dispatchers.IO)
    private val createDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            exportToJSONCallback(uri)
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
        settingsInstance.registerOnExportClick { this@MainActivity.exportToJSON() }
        settingsInstance.registerOnImportClick { this@MainActivity.importFromJSON() }
        settingsInstance.registerOnDeleteAllClick { this@MainActivity.deleteAll() }
        settingsInstance.registerOnNotificationClick { this@MainActivity.requestNotificationPermission() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CurrentActivity.setInstance(this@MainActivity)
        val deleteCounter: (Counter) -> Unit = { scopeIO.launch { deleteCounter(it) } }
        createNotificationChannel()

        setKeepScreenOnFlag()
        counter = mutableStateOf(Counter(
            0, 0, "Counter #"
        ) { deleteCounter(it) })

        val counterIDKey = intPreferencesKey("counter_id")
        val savedCounterID: Flow<Int> = this.dataStore.data.catch { }.map { preferences ->
            preferences[counterIDKey] ?: 1
        }

        scopeIO.launch {
            settingsInstance.loadSettings(this@MainActivity)
            setKeepScreenOnFlag()

            var counters = CounterDatabase.getInstance(this@MainActivity).counterDatabase.getAll()
                .map { counterEntity ->
                    Counter(
                        counterEntity.id,
                        counterEntity.value,
                        counterEntity.name,
                    ) { deleteCounter(it) }
                }
            if (counters.isEmpty()) {
                addCounter()
                counters = CounterDatabase.getInstance(this@MainActivity).counterDatabase.getAll()
                    .map { counterEntity ->
                        Counter(
                            counterEntity.id,
                            counterEntity.value,
                            counterEntity.name,
                        ) { deleteCounter(it) }
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
            val settings by settingsInstance.settingsState.collectAsState()

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

    private suspend fun addCounter(name: String? = null, value: Long = 0) {
        val deleteCounter: (Counter) -> Unit = { scopeIO.launch { deleteCounter(it) } }
        val database = CounterDatabase.getInstance(this@MainActivity).counterDatabase

        val lastID = database.getLastID() ?: 0
        val name = name ?: "Counter #${lastID + 1}"
        val counter = CounterEntity(name = name, value = value)
        database.insert(counter)
        val counterEntity = database.getLast()!!
        counters.add(Counter(
            counterEntity.id,
            counterEntity.value,
            counterEntity.name,
        ) { deleteCounter(it) })
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

        val database = CounterDatabase.getInstance(this@MainActivity).counterDatabase
        database.deleteByID(counter.getCounterId())

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
                CounterUI.CounterTopAppBar(
                    counter.value, onNavigationIconClick, onNavigateToSettings, onNavigateToAbout
                )
            },
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                CounterUI.CounterUI(counter.value)
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
            HorizontalDivider()
            LazyColumn {
                items(counters) { counter ->
                    if (counter == null) return@items
                    NavigationDrawerItem(label = { CounterUI.CounterName(counter) },
                        selected = false,
                        onClick = {
                            this@MainActivity.counter.value = counter
                            this@MainActivity.counterID = counter.getCounterId()
                            closeDrawer()
                        })
                }
            }
        }

        EditAlertDialog(opened = openEditDialog,
            onDismissRequest = { openEditDialog = false }) { name, value ->
            openEditDialog = false
            scopeIO.launch {
                addCounter(name, value)
            }
        }
    }

    @Composable
    private fun EditAlertDialog(
        opened: Boolean, onDismissRequest: () -> Unit, onConfirmation: (String, Long) -> Unit
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
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> counter.value.decrementCounter()
                KeyEvent.KEYCODE_VOLUME_UP -> counter.value.incrementCounter()
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
                it?.updateDatabase(this@MainActivity)
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

    private fun exportToJSON() {
        scopeIO.launch {
            counters.forEach {
                it?.updateDatabase(this@MainActivity)
            }
            Log.d(TAG_DEBUG, "exportToJson")

            val date = LocalDate.now()
            val year = date.year
            val month = String.format(null, "%02d", date.monthValue)
            val day = date.dayOfMonth
            createDocument.launch("counters_export_${year}_${month}_$day.json")
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
                it?.updateDatabase(this@MainActivity)
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
        counters.filterNotNull().forEach {
            scopeIO.launch {
                deleteCounter(it)
            }
        }
    }

    fun getCounterViewModel(): Counter {
        return counter.value
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
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(
                            "package:" + BuildConfig.APPLICATION_ID
                        )
                    ).apply { startActivity(this) }
                }
            } else {
                settingsInstance.setNotification(true)
            }
        }
    }

}