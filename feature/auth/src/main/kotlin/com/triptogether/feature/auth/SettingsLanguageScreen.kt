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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.triptogether.core.ui.theme.TripTogetherTheme

/** Settings > ภาษา — applies immediately; the activity recreates in the chosen locale. */
@Composable
fun SettingsLanguageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val isThai = !current.startsWith("en")

    SettingsLanguageContent(
        isThai = isThai,
        onSelect = ::setAppLanguage,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsLanguageContent(
    isThai: Boolean,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_language)) },
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
            LanguageRow(
                label = stringResource(R.string.settings_language_th),
                selected = isThai,
                onClick = { onSelect("th") },
            )
            HorizontalDivider()
            LanguageRow(
                label = stringResource(R.string.settings_language_en),
                selected = !isThai,
                onClick = { onSelect("en") },
            )
        }
    }
}

@Composable
private fun LanguageRow(
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

private fun setAppLanguage(tag: String) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
}

@Preview(showBackground = true)
@Composable
private fun SettingsLanguageContentPreview() {
    TripTogetherTheme {
        SettingsLanguageContent(
            isThai = true,
            onSelect = {},
            onBack = {},
        )
    }
}
