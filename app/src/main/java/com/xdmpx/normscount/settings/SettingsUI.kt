package com.xdmpx.normscount.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.xdmpx.normscount.R
import com.xdmpx.normscount.counter.CounterUIHelper
import com.xdmpx.normscount.datastore.ThemeType

object SettingsUI {

    private val settingPadding = 10.dp

    @Composable
    fun SettingsUI(settingsViewModel: SettingsViewModel, onNavigateToMain: () -> Unit) {
        val settingsState by settingsViewModel.settingsState.collectAsState()
        var showDeleteAllConfirmationDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = { SettingsTopAppBar(onNavigateToMain) },
        ) { innerPadding ->
            Box(
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                LazyColumn(Modifier.padding(10.dp)) {
                    item {
                        Setting(stringResource(R.string.settings_vibrate),
                            settingsState.vibrateOnValueChange,
                            icon = { modifier ->
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_vibration_24),
                                    contentDescription = null,
                                    modifier = modifier
                                )
                            }) { settingsViewModel.toggleVibrateOnValueChange() }
                        Setting(stringResource(R.string.settings_tap),
                            settingsState.tapCounterValueToIncrement,
                            icon = { modifier ->
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_touch_app_24),
                                    contentDescription = null,
                                    modifier = modifier
                                )
                            }) { settingsViewModel.toggleTapCounterValueToIncrement() }
                        Setting(stringResource(R.string.settings_hardware_button),
                            settingsState.changeCounterValueVolumeButtons,
                            icon = { modifier ->
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_phone_android_24),
                                    contentDescription = null,
                                    modifier = modifier
                                )
                            }) { settingsViewModel.toggleChangeCounterValueVolumeButtons() }
                        Setting(stringResource(R.string.settings_keep_screen),
                            settingsState.keepScreenOn,
                            icon = { modifier ->
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_visibility_lock_24),
                                    contentDescription = null,
                                    modifier = modifier
                                )
                            }) { settingsViewModel.toggleKeepScreenOn() }
                        Setting(stringResource(R.string.settings_notification),
                            settingsState.notification,
                            icon = { modifier ->
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_circle_notifications_24),
                                    contentDescription = null,
                                    modifier = modifier
                                )
                            }) {
                            if (settingsState.notification) {
                                settingsViewModel.setNotification(false)
                            } else {
                                settingsViewModel.onNotificationClick()
                            }
                        }
                        ThemeSelectorSetting(
                            stringResource(R.string.settings_theme), settingsState.theme
                        ) {
                            settingsViewModel.setTheme(it)
                        }
                        Setting(stringResource(R.string.settings_pure_dark),
                            settingsState.usePureDark,
                            icon = { modifier ->
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_invert_colors_24),
                                    contentDescription = null,
                                    modifier = modifier
                                )
                            }) {
                            settingsViewModel.toggleUsePureDark()
                        }
                        Setting(stringResource(R.string.settings_dynamic_color),
                            settingsState.useDynamicColor,
                            icon = { modifier ->
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_palette_24),
                                    contentDescription = null,
                                    modifier = modifier
                                )
                            }) {
                            settingsViewModel.toggleUseDynamicColor()
                        }

                        HorizontalDivider(Modifier.padding(settingPadding))

                        Setting(
                            stringResource(R.string.settings_reset_confirmation_dialog),
                            settingsState.confirmationDialogReset
                        ) { settingsViewModel.toggleConfirmationDialogReset() }
                        Setting(
                            stringResource(R.string.settings_delete_dialog),
                            settingsState.confirmationDialogDelete
                        ) { settingsViewModel.toggleConfirmationDialogDelete() }
                        Setting(
                            stringResource(R.string.settings_ask_initial_value),
                            settingsState.askForInitialValuesWhenNewCounter
                        ) { settingsViewModel.toggleAskForInitialValuesWhenNewCounter() }

                        HorizontalDivider(Modifier.padding(settingPadding))

                        SettingButton(stringResource(R.string.settings_export_json),
                            icon = { modifier ->
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_file_save_24),
                                    contentDescription = null,
                                    modifier = modifier
                                )
                            }) { settingsViewModel.onExportClick() }
                        SettingButton(stringResource(R.string.settings_import_json),
                            icon = { modifier ->
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_file_open_24),
                                    contentDescription = null,
                                    modifier = modifier
                                )
                            }) { settingsViewModel.onImportClick() }
                        SettingButton(
                            stringResource(R.string.settings_delete_all),
                            icon = { modifier ->
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_delete_forever_24),
                                    contentDescription = null,
                                    modifier = modifier
                                )
                            }) { showDeleteAllConfirmationDialog = true }
                    }
                }
            }
        }

        DeleteAllAlertDialog(showDeleteAllConfirmationDialog,
            onDismissRequest = { showDeleteAllConfirmationDialog = false }) {
            showDeleteAllConfirmationDialog = false
            settingsViewModel.onDeleteAllClick()
        }

    }

    @Composable
    private fun DeleteAllAlertDialog(
        opened: Boolean, onDismissRequest: () -> Unit, onConfirmation: () -> Unit
    ) {
        if (!opened) return

        CounterUIHelper.ConfirmationAlertDialog(
            stringResource(R.string.confirmation_delete_all), onDismissRequest, onConfirmation
        )
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
                stringResource(R.string.settings_screen),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }, actions = {})
    }

    @Composable
    fun Setting(
        text: String,
        checked: Boolean,
        icon: @Composable (modifier: Modifier) -> Unit = {},
        onCheckedChange: () -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Checkbox) {
                    onCheckedChange()
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
        var openThemeSelectorDialog by remember { mutableStateOf(false) }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    openThemeSelectorDialog = !openThemeSelectorDialog
                },
        ) {
            ThemeIcon(theme)
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(0.75f)
                    .padding(settingPadding)
            ) {
                Text(text = text)
                ThemeText(theme)
            }
        }

        ThemeSelectorDialog(openThemeSelectorDialog,
            theme,
            onDismissRequest = { openThemeSelectorDialog = !openThemeSelectorDialog }) {
            onChange(it)
            openThemeSelectorDialog = !openThemeSelectorDialog
        }
    }

    @Composable
    private fun ThemeIcon(theme: ThemeType) {
        val modifier = Modifier.padding(settingPadding)

        val painter = when (theme) {
            ThemeType.SYSTEM, ThemeType.UNRECOGNIZED -> painterResource(id = R.drawable.rounded_brightness_auto_24)
            ThemeType.DARK -> painterResource(id = R.drawable.rounded_dark_mode_24)
            ThemeType.LIGHT -> painterResource(id = R.drawable.rounded_light_mode_24)
        }

        Icon(
            painter = painter, contentDescription = null, modifier = modifier
        )
    }

    @Composable
    private fun ThemeText(theme: ThemeType) {
        val themeText = when (theme) {
            ThemeType.SYSTEM, ThemeType.UNRECOGNIZED -> stringResource(R.string.settings_theme_system)
            ThemeType.DARK -> stringResource(R.string.settings_theme_dark)
            ThemeType.LIGHT -> stringResource(R.string.settings_theme_light)
        }

        Text(
            text = themeText, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    fun ThemeSelectorDialog(
        opened: Boolean,
        theme: ThemeType,
        onDismissRequest: () -> Unit,
        onSelect: (ThemeType) -> Unit
    ) {
        if (!opened) return

        Dialog(onDismissRequest = { onDismissRequest() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                RadioTextButton(
                    text = stringResource(R.string.settings_theme_system),
                    selected = theme == ThemeType.SYSTEM
                ) {
                    onSelect(ThemeType.SYSTEM)
                }
                RadioTextButton(
                    text = stringResource(R.string.settings_theme_dark),
                    selected = theme == ThemeType.DARK
                ) {
                    onSelect(ThemeType.DARK)
                }
                RadioTextButton(
                    text = stringResource(R.string.settings_theme_light),
                    selected = theme == ThemeType.LIGHT
                ) {
                    onSelect(ThemeType.LIGHT)
                }

            }
        }
    }

    @Composable
    fun RadioTextButton(text: String, selected: Boolean, onClick: () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onClick() }
                .fillMaxWidth()) {
            RadioButton(selected = selected, onClick = {
                onClick()
            })
            Text(text = text)
        }
    }
}