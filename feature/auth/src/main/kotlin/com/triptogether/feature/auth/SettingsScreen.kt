package com.triptogether.feature.auth

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.triptogether.core.domain.model.User
import com.triptogether.core.ui.theme.TripTogetherTheme

/** S14 — settings as a menu: icon rows that push per-topic screens. */
@Composable
fun SettingsScreen(
    onOpenProfile: () -> Unit,
    onOpenPromptpay: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenTheme: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsMenuContent(
        uiState = uiState,
        onOpenProfile = onOpenProfile,
        onOpenPromptpay = onOpenPromptpay,
        onOpenLanguage = onOpenLanguage,
        onOpenTheme = onOpenTheme,
        onSignOut = viewModel::signOut,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMenuContent(
    uiState: SettingsUiState,
    onOpenProfile: () -> Unit,
    onOpenPromptpay: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenTheme: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxWidth().padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            ProfileHeader(user = uiState.user)
            Spacer(modifier = Modifier.height(16.dp))

            val currentLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            val context = LocalContext.current
            val themeValue =
                when (ThemePreference.read(context)) {
                    AppCompatDelegate.MODE_NIGHT_NO -> stringResource(R.string.settings_theme_light)
                    AppCompatDelegate.MODE_NIGHT_YES -> stringResource(R.string.settings_theme_dark)
                    else -> stringResource(R.string.settings_theme_system)
                }
            Card(modifier = Modifier.fillMaxWidth()) {
                SettingsMenuRow(
                    icon = Icons.Default.Person,
                    label = stringResource(R.string.settings_section_profile),
                    value = uiState.user?.displayName.orEmpty(),
                    onClick = onOpenProfile,
                )
                HorizontalDivider()
                SettingsMenuRow(
                    icon = Icons.Default.QrCode2,
                    label = stringResource(R.string.settings_menu_promptpay),
                    value =
                        uiState.user?.promptpayId
                            ?: stringResource(R.string.settings_promptpay_not_set),
                    onClick = onOpenPromptpay,
                )
                HorizontalDivider()
                SettingsMenuRow(
                    icon = Icons.Default.Language,
                    label = stringResource(R.string.settings_language),
                    value =
                        if (currentLanguage.startsWith("en")) {
                            stringResource(R.string.settings_language_en)
                        } else {
                            stringResource(R.string.settings_language_th)
                        },
                    onClick = onOpenLanguage,
                )
                HorizontalDivider()
                SettingsMenuRow(
                    icon = Icons.Default.DarkMode,
                    label = stringResource(R.string.settings_theme),
                    value = themeValue,
                    onClick = onOpenTheme,
                )
                HorizontalDivider()
                // FCM notification preferences arrive with M6.1 (Blaze).
                SettingsMenuRow(
                    icon = Icons.Default.Notifications,
                    label = stringResource(R.string.settings_menu_notifications),
                    value = stringResource(R.string.settings_coming_soon),
                    enabled = false,
                    onClick = {},
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_sign_out),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.clickable(onClick = onSignOut),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsMenuRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val contentColor =
        if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else contentColor,
            )
        },
        headlineContent = { Text(text = label, color = contentColor) },
        supportingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            if (enabled) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
    )
}

/** Big avatar + name — who you are in every trip, front and center. */
@Composable
internal fun ProfileHeader(
    user: User?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (user?.photoUrl != null) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = null,
                modifier = Modifier.size(96.dp).clip(CircleShape),
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = user?.displayName?.take(1) ?: "?",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = user?.displayName.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsMenuContentPreview() {
    TripTogetherTheme {
        SettingsMenuContent(
            uiState =
                SettingsUiState(
                    isLoading = false,
                    user = User(id = "u1", displayName = "สมชาย", promptpayId = "0812345678"),
                ),
            onOpenProfile = {},
            onOpenPromptpay = {},
            onOpenLanguage = {},
            onOpenTheme = {},
            onSignOut = {},
            onBack = {},
        )
    }
}
