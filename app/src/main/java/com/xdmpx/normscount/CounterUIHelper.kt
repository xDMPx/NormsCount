package com.xdmpx.normscount

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType

object CounterUIHelper {

    @Composable
    fun EditAlertDialog(
        counterName: String,
        counterValue: Long,
        onDismissRequest: () -> Unit,
        onConfirmation: (String, Long) -> Unit
    ) {
        var counterName by remember { mutableStateOf(counterName) }
        var counterValue by remember { mutableStateOf(counterValue.toString()) }

        AlertDialog(text = {
            Column {
                OutlinedTextField(
                    value = counterName,
                    onValueChange = { counterName = it },
                    maxLines = 1,
                    label = { Text("Counter Name") },
                )
                OutlinedTextField(
                    value = counterValue,
                    onValueChange = { counterValue = it },
                    maxLines = 1,
                    label = { Text("Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }, onDismissRequest = {
            onDismissRequest()
        }, confirmButton = {
            TextButton(onClick = {
                // TODO: Invalid input handling
                onConfirmation(counterName, counterValue.replace(',', '.').toDouble().toLong())
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

}