package com.xdmpx.normscount.about

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.core.graphics.drawable.toBitmap
import com.xdmpx.normscount.BuildConfig
import com.xdmpx.normscount.R

object About {

    private val aboutPadding = 10.dp

    @Composable
    fun AboutUI(onNavigateToMain: () -> Unit) {
        val context = LocalContext.current

        Scaffold(
            topBar = { AboutTopAppBar(onNavigateToMain) },
        ) { innerPadding ->
            Box(
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                OutlinedCard(
                    border = BorderStroke(0.25.dp, Color.Gray), modifier = Modifier.padding(12.dp)
                ) {
                    AppInfo()
                    AboutButton(text = "${stringResource(R.string.about_version)} v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") {}
                    AboutButton(text = stringResource(R.string.about_source_code)) {
                        openURL(context, "https://github.com/xDMPx/NormsCount")
                    }
                    AboutButton(text = stringResource(R.string.about_license)) {
                        openURL(context, "https://github.com/xDMPx/NormsCount/blob/main/LICENSE")
                    }
                    AboutButton(text = stringResource(R.string.about_author)) {
                        openURL(context, "https://github.com/xDMPx")
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AboutTopAppBar(onNavigateToMain: () -> Unit) {
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
                stringResource(R.string.about_screen),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }, actions = {})
    }

    @Composable
    private fun AppInfo(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        Box(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon()
                    Text(
                        text = context.getString(context.applicationInfo.labelRes),
                        fontWeight = FontWeight.W500
                    )
                }
                AppDescription()
            }
        }
    }

    @Composable
    private fun AppIcon(modifier: Modifier = Modifier) {
        val drawable =
            LocalContext.current.packageManager.getApplicationIcon(BuildConfig.APPLICATION_ID)
        Box(contentAlignment = Alignment.Center, modifier = modifier) {
            Image(
                drawable.toBitmap(config = Bitmap.Config.ARGB_8888).asImageBitmap(),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(75.dp)
                    .padding(8.dp)
            )
        }
    }

    @Composable
    private fun AppDescription(modifier: Modifier = Modifier) {
        Box(modifier = modifier) {
            Text(text = stringResource(id = R.string.about_app_description))
        }
    }

    @Composable
    fun AboutButton(
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
            val modifier = Modifier.padding(aboutPadding)
            icon(modifier)
            Text(
                text = text,
                Modifier
                    .fillMaxWidth()
                    .weight(0.75f)
                    .padding(aboutPadding)
            )
        }
    }

    private fun openURL(context: Context, url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(context, browserIntent, null)
    }
}