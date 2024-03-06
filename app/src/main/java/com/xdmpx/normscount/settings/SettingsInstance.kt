package com.xdmpx.normscount.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.xdmpx.normscount.datastore.SettingsProto
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

val Context.settingsDataStore: DataStore<SettingsProto> by dataStore(
    fileName = "settings.pb", serializer = SettingsSerializer
)

class SettingsInstance {

    var vibrateOnValueChange = true
    var tapCounterValueToIncrement = true
    var changeCounterValueVolumeButtons = true
    var confirmationDialogReset = true
    var confirmationDialogDelete = true
    var keepScreenOn = true
    var askForInitialValuesWhenNewCounter = true
    var usePureDark = false

    private lateinit var onExportClick: () -> Unit
    private lateinit var onImportClick: () -> Unit
    private lateinit var onDeleteAllClick: () -> Unit
    private lateinit var onThemeUpdate: (Boolean) -> Unit

    @Composable
    fun SettingsUI(onNavigateToMain: () -> Unit) {
        var showDeleteAllConfirmationDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = { SettingsTopAppBar(onNavigateToMain) },
        ) { innerPadding ->
            Box(
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                Column {
                    Setting(
                        "Vibrate on value change ", vibrateOnValueChange
                    ) { vibrateOnValueChange = it }
                    Setting(
                        "Tap counter value to increment", tapCounterValueToIncrement
                    ) { tapCounterValueToIncrement = it }
                    Setting(
                        "Change counter value using hardware volume buttons",
                        changeCounterValueVolumeButtons
                    ) { changeCounterValueVolumeButtons = it }
                    Setting(
                        "Enable reset confirmation dialog", confirmationDialogReset
                    ) { confirmationDialogReset = it }
                    Setting(
                        "Enable delete confirmation dialog", confirmationDialogDelete
                    ) { confirmationDialogDelete = it }
                    Setting(
                        "Keep the screen on", keepScreenOn
                    ) { keepScreenOn = it }
                    Setting(
                        "Ask for initial value and name when initializing counter",
                        askForInitialValuesWhenNewCounter
                    ) { askForInitialValuesWhenNewCounter = it }
                    Setting(
                        "Use pure(AMOLED) dark in dark theme", usePureDark
                    ) {
                        usePureDark = it
                        onThemeUpdate(usePureDark)
                    }
                    SettingButton(
                        "Export all counters to a JSON file",
                    ) { onExportClick() }
                    SettingButton(
                        "Import counters from a JSON file",
                    ) { onImportClick() }
                    SettingButton(
                        "Delete all counters",
                    ) { showDeleteAllConfirmationDialog = true }
                }
            }
        }

        if (showDeleteAllConfirmationDialog) {
            DeleteAllAlertDialog(onDismissRequest = { showDeleteAllConfirmationDialog = false }) {
                showDeleteAllConfirmationDialog = false
                onDeleteAllClick()
            }
        }

    }

    @Composable
    private fun DeleteAllAlertDialog(onDismissRequest: () -> Unit, onConfirmation: () -> Unit) {
        AlertDialog(text = {
            Text(text = "Are you sure you want to delete all counters? This action cannot be undone.")
        }, onDismissRequest = {
            onDismissRequest()
        }, confirmButton = {
            TextButton(onClick = {
                onConfirmation()
            }) {
                Text("Confirm")
            }
        }, dismissButton = {
            TextButton(onClick = {
                onDismissRequest()
            }) {
                Text("Cancel")
            }
        })
    }

    fun registerOnExportClick(onExportClick: () -> Unit) {
        this@SettingsInstance.onExportClick = onExportClick
    }

    fun registerOnImportClick(onImportClick: () -> Unit) {
        this@SettingsInstance.onImportClick = onImportClick
    }

    fun registerOnDeleteAllClick(onDeleteAllClick: () -> Unit) {
        this@SettingsInstance.onDeleteAllClick = onDeleteAllClick
    }

    fun registerOnThemeUpdate(onThemeUpdate: (Boolean) -> Unit) {
        this@SettingsInstance.onThemeUpdate = onThemeUpdate
    }

    suspend fun loadSettings(context: Context) {
        val settingsData = context.settingsDataStore.data.catch { }.first()
        this.vibrateOnValueChange = settingsData.vibrateOnValueChange
        this.tapCounterValueToIncrement = settingsData.tapCounterValueToIncrement
        this.changeCounterValueVolumeButtons = settingsData.changeCounterValueVolumeButtons
        this.confirmationDialogReset = settingsData.confirmationDialogReset
        this.confirmationDialogDelete = settingsData.confirmationDialogDelete
        this.keepScreenOn = settingsData.keepScreenOn
        this.askForInitialValuesWhenNewCounter = settingsData.askForInitialValuesWhenNewCounter
        this.usePureDark = settingsData.usePureDark
    }

    suspend fun saveSettings(context: Context) {
        context.settingsDataStore.updateData {
            it.toBuilder().apply {
                vibrateOnValueChange = this@SettingsInstance.vibrateOnValueChange
                tapCounterValueToIncrement = this@SettingsInstance.tapCounterValueToIncrement
                changeCounterValueVolumeButtons =
                    this@SettingsInstance.changeCounterValueVolumeButtons
                confirmationDialogReset = this@SettingsInstance.confirmationDialogReset
                confirmationDialogDelete = this@SettingsInstance.confirmationDialogDelete
                keepScreenOn = this@SettingsInstance.keepScreenOn
                askForInitialValuesWhenNewCounter =
                    this@SettingsInstance.askForInitialValuesWhenNewCounter
                usePureDark = this@SettingsInstance.usePureDark
            }.build()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SettingsTopAppBar(onNavigateToMain: () -> Unit) {
        TopAppBar(colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ), navigationIcon = {
            IconButton(onClick = { onNavigateToMain() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null
                )
            }
        }, title = {
            Text(
                "Settings", maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }, actions = {})
    }

    @Composable
    fun Setting(
        text: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        var checked by remember { mutableStateOf(checked) }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Checkbox) {
                    checked = !checked
                    onCheckedChange(checked)
                },
        ) {
            Text(
                text = text,
                Modifier
                    .fillMaxWidth()
                    .weight(0.75f)
            )
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f)
            ) {
                Checkbox(checked = checked, onCheckedChange = null)
            }
        }
    }

    @Composable
    fun SettingButton(
        text: String,
        onClick: () -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },
        ) {
            Text(
                text = text,
                Modifier
                    .fillMaxWidth()
                    .weight(0.75f)
            )
        }
    }

}