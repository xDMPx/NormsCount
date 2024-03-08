package com.xdmpx.normscount.counter

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdmpx.normscount.R
import com.xdmpx.normscount.counter.CounterUIHelper.ConfirmationAlertDialog
import com.xdmpx.normscount.database.CounterDatabase
import com.xdmpx.normscount.database.CounterEntity
import com.xdmpx.normscount.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Counter(
    private val counterEntity: CounterEntity,
    private val onDelete: (Counter) -> Unit,
    context: Context
) {
    private val TAG_DEBUG = "Counter"
    private var count: MutableState<Long> = mutableLongStateOf(counterEntity.value)
    private var name: MutableState<String> = mutableStateOf(counterEntity.name)
    private val settings = Settings.getInstance()
    private val database = CounterDatabase.getInstance(context).counterDatabase
    private val scope = CoroutineScope(Dispatchers.IO)

    @Composable
    fun CounterUI(modifier: Modifier = Modifier) {
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
                count.value++
                onClickFeedback()
            }
        } else {
            Modifier
        }
        Column(modifier = modifier) {
            LazyRow(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.7f)
            ) {
                item {
                    Text(
                        text = "${count.value}",
                        fontSize = 150.sp,
                        modifier = textModifier.padding(10.dp)
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    space = 10.dp, alignment = Alignment.Bottom
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.3f)
            ) {
                Button(
                    onClick = {
                        count.value++
                        onClickFeedback()
                    }, modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(height = 110.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                }
                Button(
                    onClick = {
                        count.value--
                        onClickFeedback()
                    }, modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Text(text = "-", fontSize = 45.sp)
                }
                Spacer(modifier = modifier.height(10.dp))
            }
        }
    }

    @Composable
    fun CounterName() {
        val nameWeight = 0.725f
        Row() {
            LazyRow(
                modifier = Modifier
                    .weight(nameWeight)
                    .fillMaxWidth()
            ) {
                item {
                    name.value = counterNameText()
                    Text(name.value)
                }
            }
            LazyRow(
                modifier = Modifier
                    .weight(1f - nameWeight)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                item {
                    Text(text = "${count.value}", textAlign = TextAlign.End)
                }
            }
        }
    }

    private fun counterNameText(): String {
        return if (counterEntity.name == "Counter #") {
            "Counter #${counterEntity.id}"
        } else {
            counterEntity.name
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CounterTopAppBar(onNavigationIconClick: () -> Unit, onNavigateToSettings: () -> Unit) {
        TopAppBar(colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ), title = {
            name.value = counterNameText()
            Text(
                name.value, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }, navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(imageVector = Icons.Filled.Menu, contentDescription = null)
            }
        }, actions = {
            TopAppBarMenu(onNavigateToSettings)
        })
    }

    @Composable
    fun TopAppBarMenu(onNavigateToSettings: () -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        var openResetAlertDialog by remember { mutableStateOf(false) }
        var openDeleteAlertDialog by remember { mutableStateOf(false) }
        var openEditDialog by remember { mutableStateOf(false) }

        IconButton(onClick = {
            if (!settings.confirmationDialogReset) reset()
            else openResetAlertDialog = true
        }) {
            Icon(
                imageVector = Icons.Filled.Refresh, contentDescription = "Reset counter"
            )
        }
        IconButton(onClick = {
            openEditDialog = true
        }) {
            Icon(
                imageVector = Icons.Filled.Create, contentDescription = "Edit counter"
            )
        }
        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                imageVector = Icons.Filled.MoreVert, contentDescription = "Top Bar Menu"
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(text = "Delete") }, onClick = {
                expanded = false
                if (!settings.confirmationDialogDelete) {
                    this@Counter.onDelete(this@Counter)
                    delete()
                } else openDeleteAlertDialog = true
            })
            DropdownMenuItem(text = { Text(text = "Settings") }, onClick = {
                expanded = false
                onNavigateToSettings()
            })
        }

        if (openResetAlertDialog) {
            ResetAlertDialog(onDismissRequest = { openResetAlertDialog = false }) {
                openResetAlertDialog = false
                reset()
            }
        }

        if (openDeleteAlertDialog) {
            DeleteAlertDialog(onDismissRequest = { openDeleteAlertDialog = false }) {
                openDeleteAlertDialog = false
                this@Counter.onDelete(this@Counter)
                delete()
            }
        }

        if (openEditDialog) {
            EditAlertDialog(onDismissRequest = { openEditDialog = false }) { name, value ->
                openEditDialog = false
                counterEntity.name = name
                this@Counter.name.value = name
                counterEntity.value = value
                this@Counter.count.value = value
            }
        }

    }

    @Composable
    private fun ResetAlertDialog(onDismissRequest: () -> Unit, onConfirmation: () -> Unit) {
        ConfirmationAlertDialog(
            stringResource(R.string.confirmation_reset), onDismissRequest, onConfirmation
        )
    }

    @Composable
    private fun DeleteAlertDialog(onDismissRequest: () -> Unit, onConfirmation: () -> Unit) {
        ConfirmationAlertDialog(
            stringResource(R.string.confirmation_delete), onDismissRequest, onConfirmation
        )
    }

    @Composable
    private fun EditAlertDialog(
        onDismissRequest: () -> Unit, onConfirmation: (String, Long) -> Unit
    ) {
        CounterUIHelper.EditAlertDialog(
            counterNameText(), count.value, onDismissRequest, onConfirmation
        )
    }

    private fun reset() {
        count.value = 0
    }

    fun increment() {
        count.value++
    }

    fun decrement() {
        count.value--
    }

    fun getCount() = count.value

    fun setCount(count: Long) {
        this.count.value = count
    }

    fun getCounterId() = counterEntity.id

    fun updateDatabase() {
        Log.d(TAG_DEBUG, "${this.hashCode()}::updateDatabase -> ${count.value}")
        counterEntity.value = count.value
        scope.launch {
            Log.d(TAG_DEBUG, "${this.hashCode()}::updateDatabase -> $counterEntity")
            database.update(counterEntity)
        }
    }

    fun delete() {
        scope.launch {
            Log.d(TAG_DEBUG, "${this.hashCode()}::delete -> $counterEntity")
            database.delete(counterEntity)
        }
    }

}