package com.xdmpx.normscount

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class Counter {
    val TAG_DEBUG = "Counter"
    private var count: MutableState<Int> = mutableStateOf(0)

    @Composable
    @Preview
    fun CounterUI(modifier: Modifier = Modifier) {
        var count by remember { count }

        val context = LocalContext.current
        val vibrator = context.getSystemService(Vibrator::class.java)
        val onClickFeedback =
            { vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)) }

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
                        text = "$count", fontSize = 150.sp, modifier = Modifier.padding(10.dp)
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
                        count++
                        onClickFeedback()
                    }, modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(height = 110.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                }
                Button(
                    onClick = {
                        count--
                        onClickFeedback()
                    }, modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Text(text = "-", fontSize = 45.sp)
                }
                Spacer(modifier = modifier.height(10.dp))
            }
        }
    }
}