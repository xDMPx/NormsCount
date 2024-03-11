package com.xdmpx.normscount.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.xdmpx.normscount.R
import com.xdmpx.normscount.counter.CounterUIHelper.ConfirmationAlertDialog
import com.xdmpx.normscount.datastore.SettingsProto
import com.xdmpx.normscount.datastore.ThemeType
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

val Context.settingsDataStore: DataStore<SettingsProto> by dataStore(
    fileName = "settings.pb", serializer = SettingsSerializer
)

class SettingsInstance {

    private val settingPadding = 10.dp

    var vibrateOnValueChange = true
    var tapCounterValueToIncrement = true
    var changeCounterValueVolumeButtons = true
    var confirmationDialogReset = true
    var confirmationDialogDelete = true
    var keepScreenOn = true
    var askForInitialValuesWhenNewCounter = true
    var usePureDark = false
    var useDynamicColor = false
    var theme = ThemeType.SYSTEM

    private lateinit var onExportClick: () -> Unit
    private lateinit var onImportClick: () -> Unit
    private lateinit var onDeleteAllClick: () -> Unit
    private lateinit var onThemeUpdate: (Boolean, Boolean, ThemeType) -> Unit

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
                    Setting("Vibrate on value change ", vibrateOnValueChange, icon = { modifier ->
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_vibration_24),
                            contentDescription = null,
                            modifier = modifier
                        )
                    }) { vibrateOnValueChange = it }
                    Setting("Tap counter value to increment",
                        tapCounterValueToIncrement,
                        icon = { modifier ->
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_touch_app_24),
                                contentDescription = null,
                                modifier = modifier
                            )
                        }) { tapCounterValueToIncrement = it }
                    Setting("Change counter value using hardware volume buttons",
                        changeCounterValueVolumeButtons,
                        icon = { modifier ->
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_phone_android_24),
                                contentDescription = null,
                                modifier = modifier
                            )
                        }) { changeCounterValueVolumeButtons = it }
                    Setting("Keep the screen on", keepScreenOn, icon = { modifier ->
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_visibility_lock_24),
                            contentDescription = null,
                            modifier = modifier
                        )
                    }) { keepScreenOn = it }
                    ThemeSelectorSetting("Theme", theme) {
                        theme = it
                        onThemeUpdate(usePureDark, useDynamicColor, theme)
                    }
                    Setting("Use pure(AMOLED) dark in dark theme", usePureDark, icon = { modifier ->
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_invert_colors_24),
                            contentDescription = null,
                            modifier = modifier
                        )
                    }) {
                        usePureDark = it
                        onThemeUpdate(usePureDark, useDynamicColor, theme)
                    }
                    Setting("Use dynamic colors in theme", useDynamicColor, icon = { modifier ->
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_palette_24),
                            contentDescription = null,
                            modifier = modifier
                        )
                    }) {
                        useDynamicColor = it
                        onThemeUpdate(usePureDark, useDynamicColor, theme)
                    }


                    Divider(Modifier.padding(settingPadding))

                    Setting(
                        "Enable reset confirmation dialog", confirmationDialogReset
                    ) { confirmationDialogReset = it }
                    Setting(
                        "Enable delete confirmation dialog", confirmationDialogDelete
                    ) { confirmationDialogDelete = it }
                    Setting(
                        "Ask for initial value and name when initializing counter",
                        askForInitialValuesWhenNewCounter
                    ) { askForInitialValuesWhenNewCounter = it }

                    Divider(Modifier.padding(settingPadding))

                    SettingButton("Export all counters to a JSON file", icon = { modifier ->
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_file_save_24),
                            contentDescription = null,
                            modifier = modifier
                        )
                    }) { onExportClick() }
                    SettingButton("Import counters from a JSON file", icon = { modifier ->
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_file_open_24),
                            contentDescription = null,
                            modifier = modifier
                        )
                    }) { onImportClick() }
                    SettingButton("Delete all counters", icon = { modifier ->
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_delete_forever_24),
                            contentDescription = null,
                            modifier = modifier
                        )
                    }) { showDeleteAllConfirmationDialog = true }
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
        ConfirmationAlertDialog(
            stringResource(R.string.confirmation_delete_all), onDismissRequest, onConfirmation
        )
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

    fun registerOnThemeUpdate(onThemeUpdate: (Boolean, Boolean, ThemeType) -> Unit) {
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
        this.useDynamicColor = settingsData.useDynamicColor
        this.theme = settingsData.theme
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
                useDynamicColor = this@SettingsInstance.useDynamicColor
                theme = this@SettingsInstance.theme
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
        icon: @Composable (modifier: Modifier) -> Unit = {},
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(0.75f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val modifier = Modifier.padding(settingPadding)
                icon(modifier)
                Text(text = text, modifier = modifier)
            }
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
        icon: @Composable (modifier: Modifier) -> Unit = {},
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
            val modifier = Modifier.padding(settingPadding)
            icon(modifier)
            Text(
                text = text,
                Modifier
                    .fillMaxWidth()
                    .weight(0.75f)
                    .padding(settingPadding)
            )
        }
    }

    @Composable
    fun ThemeSelectorSetting(
        text: String,
        theme: ThemeType,
        onChange: (ThemeType) -> Unit,
    ) {
        var openDialog by remember { mutableStateOf(false) }

        var themeText by remember { mutableStateOf("") }

        val modifier = Modifier.padding(settingPadding)
        val icon: @Composable () -> Unit = when (theme) {
            ThemeType.SYSTEM, ThemeType.UNRECOGNIZED -> {
                {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_brightness_auto_24),
                        contentDescription = null,
                        modifier = modifier
                    )
                    themeText = "System"
                }
            }

            ThemeType.DARK -> {
                {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_dark_mode_24),
                        contentDescription = null,
                        modifier = modifier
                    )
                    themeText = "Dark"
                }
            }

            ThemeType.LIGHT -> {
                {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_light_mode_24),
                        contentDescription = null,
                        modifier = modifier
                    )
                    themeText = "Light"
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    openDialog = !openDialog
                },
        ) {
            icon()
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(0.75f)
                    .padding(settingPadding)
            ) {
                Text(text = text)
                Text(
                    text = themeText,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (openDialog) {
            ThemeSelectorDialog(theme, onDismissRequest = { openDialog = !openDialog }) {
                onChange(it)
                openDialog = !openDialog
            }
        }
    }

    @Composable
    fun ThemeSelectorDialog(
        theme: ThemeType, onDismissRequest: () -> Unit, onSelect: (ThemeType) -> Unit
    ) {
        Dialog(onDismissRequest = { onDismissRequest() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                RadioTextButton(text = "System", selected = theme == ThemeType.SYSTEM) {
                    onSelect(ThemeType.SYSTEM)
                }
                RadioTextButton(text = "Dark", selected = theme == ThemeType.DARK) {
                    onSelect(ThemeType.DARK)
                }
                RadioTextButton(text = "Light", selected = theme == ThemeType.LIGHT) {
                    onSelect(ThemeType.LIGHT)
                }

            }
        }
    }

    @Composable
    fun RadioTextButton(text: String, selected: Boolean, onClick: () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = {
                onClick()
            })
            Text(text = text)
        }
    }

}