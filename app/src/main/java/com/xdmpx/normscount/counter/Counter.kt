package com.xdmpx.normscount.counter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.xdmpx.normscount.R
import com.xdmpx.normscount.counter.CounterUIHelper.ConfirmationAlertDialog
import com.xdmpx.normscount.database.CounterDatabase
import com.xdmpx.normscount.database.CounterEntity
import com.xdmpx.normscount.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CounterState(
    val count: Long = 0, val name: String = ""
)

class Counter(
    private val id: Int,
    value: Long,
    name: String,
    val onDelete: (Counter) -> Unit,
) : ViewModel() {
    private val _counterState = MutableStateFlow(CounterState(value, name))
    val counterState: StateFlow<CounterState> = _counterState.asStateFlow()

    fun resetCounter() {
        _counterState.value.let {
            _counterState.value = it.copy(count = 0)
        }
    }

    fun incrementCounter() {
        _counterState.value.let {
            _counterState.value = it.copy(count = it.count + 1)
        }
    }

    fun decrementCounter() {
        _counterState.value.let {
            _counterState.value = it.copy(count = it.count - 1)
        }
    }

    fun setCounterValue(value: Long) {
        _counterState.value.let {
            _counterState.value = it.copy(count = value)
        }
    }

    fun setCounterName(name: String) {
        _counterState.value.let {
            _counterState.value = it.copy(name = name)
        }
    }

    fun getCounterId() = id

    private fun getCounterEntity() =
        CounterEntity(id, _counterState.value.name, _counterState.value.count)

    suspend fun updateDatabase(context: Context) {
        val database = CounterDatabase.getInstance(context).counterDatabase
        val counterEntity = getCounterEntity()
        database.update(counterEntity)
    }

}

object CounterUI {
    private val settingsInstance = Settings.getInstance()

    @Composable
    fun CounterUI(counterViewModel: Counter, modifier: Modifier = Modifier) {
        val settings by settingsInstance.settingsState.collectAsState()

        val context = LocalContext.current
        val vibrator = context.getSystemService(Vibrator::class.java)
        val onClickFeedback = {
            if (settings.vibrateOnValueChange) vibrator.vibrate(
                VibrationEffect.createPredefined(
                    VibrationEffect.EFFECT_CLICK
                )
            )
        }

        val textModifier = if (settings.tapCounterValueToIncrement) {
            val interactionSource = remember { MutableInteractionSource() }
            Modifier.clickable(interactionSource, null) {
                counterViewModel.incrementCounter()
                onClickFeedback()
            }
        } else {
            Modifier
        }

        val landscapeOrientation =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (!landscapeOrientation) CounterUIPortrait(
            counterViewModel, modifier, textModifier, onClickFeedback
        )
        else CounterUILandscape(counterViewModel, modifier, textModifier, onClickFeedback)

        if (settings.notification) {
            val counterState by counterViewModel.counterState.collectAsState()

            // TODO: ONGOING NOTIFICATION
            val name = context.getString(context.applicationInfo.labelRes)
            val builder =
                NotificationCompat.Builder(context, name).setContentTitle(counterState.name)
                    .setSmallIcon(R.mipmap.ic_launcher).setContentText("${counterState.count}")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT).setOnlyAlertOnce(true)

            with(NotificationManagerCompat.from(context)) {
                if (ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(42069, builder.build())
                }
            }
        } else {
            with(NotificationManagerCompat.from(context)) {
                cancel(42069)
            }
        }

    }

    @Composable
    private fun CounterUIPortrait(
        counterViewModel: Counter,
        modifier: Modifier = Modifier,
        textModifier: Modifier,
        onClickFeedback: () -> Unit
    ) {
        Column(modifier = modifier) {
            CounterUIValue(counterViewModel, Modifier.weight(0.7f), textModifier)
            CounterUIButtons(
                counterViewModel, modifier.weight(0.3f), Alignment.Bottom, onClickFeedback
            )
        }
    }

    @Composable
    private fun CounterUILandscape(
        counterViewModel: Counter,
        modifier: Modifier = Modifier,
        textModifier: Modifier,
        onClickFeedback: () -> Unit
    ) {
        Row(modifier = modifier) {
            CounterUIValue(counterViewModel, Modifier.weight(0.7f), textModifier)
            CounterUIButtons(
                counterViewModel, modifier.weight(0.3f), Alignment.CenterVertically, onClickFeedback
            )
        }
    }

    @Composable
    private fun CounterUIValue(
        counterViewModel: Counter,
        modifier: Modifier = Modifier,
        textModifier: Modifier,
    ) {
        val counterState by counterViewModel.counterState.collectAsState()
        LazyRow(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "${counterState.count}",
                    fontSize = 150.sp,
                    modifier = textModifier.padding(10.dp)
                )
            }
        }
    }

    @Composable
    private fun CounterUIButtons(
        counterViewModel: Counter,
        modifier: Modifier = Modifier,
        verticalArrangementAlignment: Alignment.Vertical,
        onClickFeedback: () -> Unit
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(
                space = 10.dp, alignment = verticalArrangementAlignment
            ), horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxSize()
        ) {
            Button(
                onClick = {
                    counterViewModel.incrementCounter()
                    onClickFeedback()
                }, modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(height = 110.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
            }
            Button(
                onClick = {
                    counterViewModel.decrementCounter()
                    onClickFeedback()
                }, modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text(text = "-", fontSize = 45.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    @Composable
    fun CounterName(counterViewModel: Counter) {
        val counterState by counterViewModel.counterState.collectAsState()
        val nameWeight = 0.725f

        Row() {
            LazyRow(
                modifier = Modifier
                    .weight(nameWeight)
                    .fillMaxWidth()
            ) {
                item {
                    Text(counterState.name)
                }
            }
            LazyRow(
                modifier = Modifier
                    .weight(1f - nameWeight)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                item {
                    Text(text = "${counterState.count}", textAlign = TextAlign.End)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CounterTopAppBar(
        counterViewModel: Counter,
        onNavigationIconClick: () -> Unit,
        onNavigateToSettings: () -> Unit,
        onNavigateToAbout: () -> Unit
    ) {
        val counterState by counterViewModel.counterState.collectAsState()

        TopAppBar(colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ), title = {
            Text(
                counterState.name, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }, navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(imageVector = Icons.Filled.Menu, contentDescription = null)
            }
        }, actions = {
            TopAppBarMenu(counterViewModel, onNavigateToSettings, onNavigateToAbout)
        })
    }

    @Composable
    fun TopAppBarMenu(
        counterViewModel: Counter, onNavigateToSettings: () -> Unit, onNavigateToAbout: () -> Unit
    ) {
        val counterState by counterViewModel.counterState.collectAsState()
        val settings by settingsInstance.settingsState.collectAsState()
        var expanded by remember { mutableStateOf(false) }
        var openResetAlertDialog by remember { mutableStateOf(false) }
        var openDeleteAlertDialog by remember { mutableStateOf(false) }
        var openEditDialog by remember { mutableStateOf(false) }

        IconButton(onClick = {
            if (!settings.confirmationDialogReset) counterViewModel.resetCounter()
            else openResetAlertDialog = true
        }) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.counter_topappbar_reset)
            )
        }
        IconButton(onClick = {
            openEditDialog = true
        }) {
            Icon(
                imageVector = Icons.Filled.Create,
                contentDescription = stringResource(R.string.counter_topappbar_edit)
            )
        }
        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.counter_topappbar_menu)
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(text = stringResource(R.string.counter_topappbar_delete)) },
                onClick = {
                    expanded = false
                    if (!settings.confirmationDialogDelete) {
                        counterViewModel.onDelete(counterViewModel)
                    } else openDeleteAlertDialog = true
                })
            DropdownMenuItem(text = { Text(text = stringResource(R.string.settings_screen)) },
                onClick = {
                    expanded = false
                    onNavigateToSettings()
                })
            DropdownMenuItem(text = { Text(text = stringResource(R.string.about_screen)) },
                onClick = {
                    expanded = false
                    onNavigateToAbout()
                })
        }

        ResetAlertDialog(openResetAlertDialog,
            onDismissRequest = { openResetAlertDialog = false }) {
            openResetAlertDialog = false
            counterViewModel.resetCounter()
        }

        DeleteAlertDialog(openDeleteAlertDialog,
            onDismissRequest = { openDeleteAlertDialog = false }) {
            openDeleteAlertDialog = false
            counterViewModel.onDelete(counterViewModel)
        }

        EditAlertDialog(openEditDialog,
            counterState.name,
            counterState.count,
            onDismissRequest = { openEditDialog = false }) { name, value ->
            openEditDialog = false
            counterViewModel.setCounterValue(value)
            counterViewModel.setCounterName(name)
        }

    }

    @Composable
    private fun ResetAlertDialog(
        opened: Boolean, onDismissRequest: () -> Unit, onConfirmation: () -> Unit
    ) {
        if (!opened) return

        ConfirmationAlertDialog(
            stringResource(R.string.confirmation_reset), onDismissRequest, onConfirmation
        )
    }

    @Composable
    private fun DeleteAlertDialog(
        opened: Boolean, onDismissRequest: () -> Unit, onConfirmation: () -> Unit
    ) {
        if (!opened) return

        ConfirmationAlertDialog(
            stringResource(R.string.confirmation_delete), onDismissRequest, onConfirmation
        )
    }

    @Composable
    private fun EditAlertDialog(
        opened: Boolean,
        counterName: String,
        counterValue: Long,
        onDismissRequest: () -> Unit,
        onConfirmation: (String, Long) -> Unit
    ) {
        if (!opened) return

        CounterUIHelper.EditAlertDialog(
            counterName, counterValue, onDismissRequest, onConfirmation
        )
    }

}