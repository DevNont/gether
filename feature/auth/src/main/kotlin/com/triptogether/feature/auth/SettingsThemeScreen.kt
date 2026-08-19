package com.triptogether.feature.auth

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.triptogether.core.ui.theme.TripTogetherTheme

/** Settings > ธีม — Light / Dark / System; applies immediately, the activity recreates. */
@Composable
fun SettingsThemeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var mode by remember { mutableIntStateOf(ThemePreference.read(context)) }

    SettingsThemeContent(
        mode = mode,
        onSelect = { value ->
            mode = value
            ThemePreference.set(context, value)
        },
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsThemeContent(
    mode: Int,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_theme)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Card(modifier = Modifier.padding(innerPadding).padding(16.dp).fillMaxWidth()) {
            ThemeRow(
                label = stringResource(R.string.settings_theme_system),
                selected = mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                onClick = { onSelect(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) },
            )
            HorizontalDivider()
            ThemeRow(
                label = stringResource(R.string.settings_theme_light),
                selected = mode == AppCompatDelegate.MODE_NIGHT_NO,
                onClick = { onSelect(AppCompatDelegate.MODE_NIGHT_NO) },
            )
            HorizontalDivider()
            ThemeRow(
                label = stringResource(R.string.settings_theme_dark),
                selected = mode == AppCompatDelegate.MODE_NIGHT_YES,
                onClick = { onSelect(AppCompatDelegate.MODE_NIGHT_YES) },
            )
        }
    }
}

@Composable
private fun ThemeRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        leadingContent = { RadioButton(selected = selected, onClick = onClick) },
        headlineContent = { Text(label) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.clickable(onClick = onClick),
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsThemeContentPreview() {
    TripTogetherTheme {
        SettingsThemeContent(
            mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            onSelect = {},
            onBack = {},
        )
    }
}
