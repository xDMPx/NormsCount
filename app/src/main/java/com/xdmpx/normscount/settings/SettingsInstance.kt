package com.xdmpx.normscount.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow

class SettingsInstance {

    var vibrateOnValueChange = true
    var tapCounterValueToIncrement = true
    var changeCounterValueVolumeButtons = true
    var confirmationDialogReset = true
    var confirmationDialogDelete = true
    var keepScreenOn = true

    @Composable
    fun SettingsUI(onNavigateToMain: () -> Unit) {
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
                }
            }
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
                .clickable {
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
                Checkbox(checked = checked, onCheckedChange = {
                    checked = it
                    onCheckedChange(it)
                })
            }
        }
    }

}