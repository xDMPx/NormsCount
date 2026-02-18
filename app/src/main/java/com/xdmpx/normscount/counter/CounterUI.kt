package com.xdmpx.normscount.counter

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import com.xdmpx.normscount.NotificationReceiver
import com.xdmpx.normscount.R
import com.xdmpx.normscount.counter.CounterUIHelper.ConfirmationAlertDialog
import com.xdmpx.normscount.settings.Settings

object CounterUI {
    private val settingsInstance = Settings.getInstance()

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun CounterUI(countersViewModel: CountersViewModel, modifier: Modifier = Modifier) {
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
            Modifier.combinedClickable(interactionSource, null, onClick = {
                countersViewModel.incrementCurrentCounter()
                onClickFeedback()
            }, onLongClick = {
                countersViewModel.decrementCurrentCounter()
                onClickFeedback()
            })
        } else {
            Modifier
        }

        val landscapeOrientation =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (!landscapeOrientation) CounterUIPortrait(
            countersViewModel, modifier, textModifier, onClickFeedback
        )
        else CounterUILandscape(countersViewModel, modifier, textModifier, onClickFeedback)

        if (settings.notification) {
            val countersState by countersViewModel.currentCounterState.collectAsState()
            val counterState by countersState.counterState.collectAsState()

            // TODO: ONGOING NOTIFICATION
            val incrementIntent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("action", "increment")
            }
            val decrementIntent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("action", "decrement")
            }
            val incrementPIntent: PendingIntent = PendingIntent.getBroadcast(
                context, 0, incrementIntent, PendingIntent.FLAG_MUTABLE
            )
            val decrementPIntent: PendingIntent = PendingIntent.getBroadcast(
                context, 1, decrementIntent, PendingIntent.FLAG_MUTABLE
            )

            val name = context.getString(context.applicationInfo.labelRes)
            val builder =
                NotificationCompat.Builder(context, name).setContentTitle(counterState.name)
                    .setSmallIcon(R.mipmap.ic_launcher).setContentText("${counterState.count}")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT).setOnlyAlertOnce(true)
                    .addAction(0, "-", decrementPIntent).addAction(0, "+", incrementPIntent)

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
        countersViewModel: CountersViewModel,
        modifier: Modifier = Modifier,
        textModifier: Modifier,
        onClickFeedback: () -> Unit
    ) {
        Column(modifier = modifier) {
            CounterUIValue(countersViewModel, Modifier.weight(0.7f), textModifier)
            CounterUIButtons(
                countersViewModel, modifier.weight(0.3f), Alignment.Bottom, onClickFeedback
            )
        }
    }

    @Composable
    private fun CounterUILandscape(
        countersViewModel: CountersViewModel,
        modifier: Modifier = Modifier,
        textModifier: Modifier,
        onClickFeedback: () -> Unit
    ) {
        Row(modifier = modifier) {
            CounterUIValue(countersViewModel, Modifier.weight(0.7f), textModifier)
            CounterUIButtons(
                countersViewModel,
                modifier.weight(0.3f),
                Alignment.CenterVertically,
                onClickFeedback
            )
        }
    }

    @Composable
    private fun CounterUIValue(
        countersViewModel: CountersViewModel,
        modifier: Modifier = Modifier,
        textModifier: Modifier,
    ) {
        val countersState by countersViewModel.currentCounterState.collectAsState()
        val counterState by countersState.counterState.collectAsState()

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
        countersViewModel: CountersViewModel,
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
                    countersViewModel.incrementCurrentCounter()
                    onClickFeedback()
                }, modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(height = 110.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
            }
            Button(
                onClick = {
                    countersViewModel.decrementCurrentCounter()
                    onClickFeedback()
                }, modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text(text = "-", fontSize = 45.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    @Composable
    fun CounterName(counterState: CounterState) {
        val nameWeight = 0.725f

        Row {
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
        countersViewModel: CountersViewModel,
        onNavigationIconClick: () -> Unit,
        onNavigateToSettings: () -> Unit,
        onNavigateToAbout: () -> Unit,
        onDeleteCounter: () -> Unit,
    ) {
        val countersState by countersViewModel.currentCounterState.collectAsState()
        val counterState by countersState.counterState.collectAsState()

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
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
            TopAppBarMenu(
                countersViewModel, onNavigateToSettings, onNavigateToAbout, onDeleteCounter
            )
        })
    }

    @Composable
    fun TopAppBarMenu(
        countersViewModel: CountersViewModel,
        onNavigateToSettings: () -> Unit,
        onNavigateToAbout: () -> Unit,
        onDeleteCounter: () -> Unit,
    ) {
        val countersState by countersViewModel.currentCounterState.collectAsState()
        val counterState by countersState.counterState.collectAsState()

        val settings by settingsInstance.settingsState.collectAsState()
        var expanded by remember { mutableStateOf(false) }
        var openResetAlertDialog by remember { mutableStateOf(false) }
        var openDeleteAlertDialog by remember { mutableStateOf(false) }
        var openEditDialog by remember { mutableStateOf(false) }

        IconButton(onClick = {
            if (!settings.confirmationDialogReset) countersViewModel.resetCurrentCounter()
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
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.counter_topappbar_delete)) },
                onClick = {
                    expanded = false
                    if (!settings.confirmationDialogDelete) {
                        onDeleteCounter()
                    } else openDeleteAlertDialog = true
                })
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.settings_screen)) },
                onClick = {
                    expanded = false
                    onNavigateToSettings()
                })
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.about_screen)) },
                onClick = {
                    expanded = false
                    onNavigateToAbout()
                })
        }

        ResetAlertDialog(
            openResetAlertDialog, onDismissRequest = { openResetAlertDialog = false }) {
            openResetAlertDialog = false
            countersViewModel.resetCurrentCounter()
        }

        DeleteAlertDialog(
            openDeleteAlertDialog, onDismissRequest = { openDeleteAlertDialog = false }) {
            openDeleteAlertDialog = false
            onDeleteCounter()
        }

        EditAlertDialog(
            openEditDialog,
            counterState.name,
            counterState.count,
            onDismissRequest = { openEditDialog = false }) { name, value ->
            openEditDialog = false
            countersViewModel.setCurrentCounterValue(value)
            countersViewModel.setCurrentCounterName(name)
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