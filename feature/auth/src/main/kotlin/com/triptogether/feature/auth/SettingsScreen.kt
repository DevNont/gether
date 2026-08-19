package com.triptogether.feature.auth

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.Message ->
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
            }
        }
    }

    SettingsMenuContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onOpenProfile = onOpenProfile,
        onOpenPromptpay = onOpenPromptpay,
        onOpenLanguage = onOpenLanguage,
        onOpenTheme = onOpenTheme,
        onLinkLine = { context.findAuthUiHost()?.let(viewModel::linkLine) },
        onSignOut = viewModel::signOut,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMenuContent(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onOpenProfile: () -> Unit,
    onOpenPromptpay: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenTheme: () -> Unit,
    onLinkLine: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                StatusRow(
                    icon = Icons.Default.AccountCircle,
                    label = stringResource(R.string.settings_account_status),
                    value =
                        stringResource(
                            if (uiState.isAnonymous) {
                                R.string.settings_account_anonymous
                            } else {
                                R.string.settings_account_line
                            },
                        ),
                )
                HorizontalDivider()
                StatusRow(
                    icon = if (uiState.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    label = stringResource(R.string.settings_sync_status),
                    value =
                        stringResource(
                            if (uiState.isOnline) {
                                R.string.settings_sync_online
                            } else {
                                R.string.settings_sync_offline
                            },
                        ),
                    tint =
                        if (uiState.isOnline) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
            }

            if (uiState.isAnonymous) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    SettingsMenuRow(
                        icon = Icons.Default.Link,
                        label = stringResource(R.string.settings_link_line),
                        value = stringResource(R.string.settings_link_line_hint),
                        onClick = onLinkLine,
                    )
                }
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
private fun StatusRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    CompactRow(modifier = modifier) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    CompactRow(modifier = modifier.clickable(enabled = enabled, onClick = onClick)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else contentColor,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = contentColor)
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (enabled) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Dense two-line row — ListItem's 72dp two-line height wastes half the screen here. */
@Composable
private fun CompactRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
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
            snackbarHostState = SnackbarHostState(),
            onOpenProfile = {},
            onOpenPromptpay = {},
            onOpenLanguage = {},
            onOpenTheme = {},
            onLinkLine = {},
            onSignOut = {},
            onBack = {},
        )
    }
}
