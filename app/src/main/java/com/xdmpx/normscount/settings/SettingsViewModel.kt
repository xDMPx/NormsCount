package com.xdmpx.normscount.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.lifecycle.ViewModel
import com.xdmpx.normscount.datastore.SettingsProto
import com.xdmpx.normscount.datastore.ThemeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

val Context.settingsDataStore: DataStore<SettingsProto> by dataStore(
    fileName = "settings.pb", serializer = SettingsSerializer
)

data class SettingsState(
    val vibrateOnValueChange: Boolean = true,
    val tapCounterValueToIncrement: Boolean = true,
    val changeCounterValueVolumeButtons: Boolean = true,
    val confirmationDialogReset: Boolean = true,
    val confirmationDialogDelete: Boolean = true,
    val keepScreenOn: Boolean = true,
    val notification: Boolean = false,
    val askForInitialValuesWhenNewCounter: Boolean = true,
    val usePureDark: Boolean = false,
    val useDynamicColor: Boolean = false,
    val theme: ThemeType = ThemeType.SYSTEM
)

class SettingsViewModel : ViewModel() {

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    lateinit var onExportJSONClick: () -> Unit
    lateinit var onExportCSVClick: () -> Unit
    lateinit var onImportClick: () -> Unit
    lateinit var onDeleteAllClick: () -> Unit
    lateinit var onNotificationClick: () -> Unit

    fun registerOnExportJSONClick(onExportClick: () -> Unit) {
        this.onExportJSONClick = onExportClick
    }

    fun registerOnExportCSVClick(onExportClick: () -> Unit) {
        this.onExportCSVClick = onExportClick
    }

    fun registerOnImportClick(onImportClick: () -> Unit) {
        this.onImportClick = onImportClick
    }

    fun registerOnDeleteAllClick(onDeleteAllClick: () -> Unit) {
        this.onDeleteAllClick = onDeleteAllClick
    }

    fun registerOnNotificationClick(onNotificationClick: () -> Unit) {
        this.onNotificationClick = onNotificationClick
    }

    fun toggleVibrateOnValueChange() {
        _settingsState.value.let {
            _settingsState.value = it.copy(vibrateOnValueChange = !it.vibrateOnValueChange)
        }
    }

    fun toggleTapCounterValueToIncrement() {
        _settingsState.value.let {
            _settingsState.value =
                it.copy(tapCounterValueToIncrement = !it.tapCounterValueToIncrement)
        }
    }

    fun toggleChangeCounterValueVolumeButtons() {
        _settingsState.value.let {
            _settingsState.value =
                it.copy(changeCounterValueVolumeButtons = !it.changeCounterValueVolumeButtons)
        }
    }

    fun toggleConfirmationDialogReset() {
        _settingsState.value.let {
            _settingsState.value = it.copy(confirmationDialogReset = !it.confirmationDialogReset)
        }
    }

    fun toggleConfirmationDialogDelete() {
        _settingsState.value.let {
            _settingsState.value = it.copy(confirmationDialogDelete = !it.confirmationDialogDelete)
        }
    }

    fun toggleKeepScreenOn() {
        _settingsState.value.let {
            _settingsState.value = it.copy(keepScreenOn = !it.keepScreenOn)
        }
    }

    fun setNotification(notification: Boolean) {
        _settingsState.value.let {
            _settingsState.value = it.copy(notification = notification)
        }
    }

    fun toggleAskForInitialValuesWhenNewCounter() {
        _settingsState.value.let {
            _settingsState.value =
                it.copy(askForInitialValuesWhenNewCounter = !it.askForInitialValuesWhenNewCounter)
        }
    }

    fun toggleUsePureDark() {
        _settingsState.value.let {
            _settingsState.value = it.copy(usePureDark = !it.usePureDark)
        }
    }

    fun toggleUseDynamicColor() {
        _settingsState.value.let {
            _settingsState.value = it.copy(useDynamicColor = !it.useDynamicColor)
        }
    }

    fun setTheme(theme: ThemeType) {
        _settingsState.value.let {
            _settingsState.value = it.copy(theme = theme)
        }
    }

    suspend fun loadSettings(context: Context) {
        val settingsData = context.settingsDataStore.data.catch { }.first()
        _settingsState.value.let {
            _settingsState.value = it.copy(
                vibrateOnValueChange = settingsData.vibrateOnValueChange,
                tapCounterValueToIncrement = settingsData.tapCounterValueToIncrement,
                changeCounterValueVolumeButtons = settingsData.changeCounterValueVolumeButtons,
                confirmationDialogReset = settingsData.confirmationDialogReset,
                confirmationDialogDelete = settingsData.confirmationDialogDelete,
                keepScreenOn = settingsData.keepScreenOn,
                notification = settingsData.notification,
                askForInitialValuesWhenNewCounter = settingsData.askForInitialValuesWhenNewCounter,
                usePureDark = settingsData.usePureDark,
                useDynamicColor = settingsData.useDynamicColor,
                theme = settingsData.theme
            )
        }
    }

    suspend fun saveSettings(context: Context) {
        context.settingsDataStore.updateData {
            it.toBuilder().apply {
                vibrateOnValueChange =
                    this@SettingsViewModel._settingsState.value.vibrateOnValueChange
                tapCounterValueToIncrement =
                    this@SettingsViewModel._settingsState.value.tapCounterValueToIncrement
                changeCounterValueVolumeButtons =
                    this@SettingsViewModel._settingsState.value.changeCounterValueVolumeButtons
                confirmationDialogReset =
                    this@SettingsViewModel._settingsState.value.confirmationDialogReset
                confirmationDialogDelete =
                    this@SettingsViewModel._settingsState.value.confirmationDialogDelete
                keepScreenOn = this@SettingsViewModel._settingsState.value.keepScreenOn
                notification = this@SettingsViewModel._settingsState.value.notification
                askForInitialValuesWhenNewCounter =
                    this@SettingsViewModel._settingsState.value.askForInitialValuesWhenNewCounter
                usePureDark = this@SettingsViewModel._settingsState.value.usePureDark
                useDynamicColor = this@SettingsViewModel._settingsState.value.useDynamicColor
                theme = this@SettingsViewModel._settingsState.value.theme
            }.build()
        }
    }

}
