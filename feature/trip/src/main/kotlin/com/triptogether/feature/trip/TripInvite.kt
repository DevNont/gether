package com.triptogether.feature.trip

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.triptogether.core.domain.model.Trip
import com.triptogether.core.ui.qr.rememberQrBitmap

/** Invite friends three ways: scan the QR, copy the 6-char code, or share/copy the link. */
@Composable
internal fun InviteDialog(
    trip: Trip,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val joinLink = "https://triptogether.app/join/${trip.inviteCode}"
    // The QR carries the app deep link so a camera scan opens the app directly;
    // switch to the https link once the domain hosts assetlinks.json.
    val qr = rememberQrBitmap("triptogether://join/${trip.inviteCode}")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.overview_invite)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    bitmap = qr.asImageBitmap(),
                    contentDescription = stringResource(R.string.overview_invite_qr),
                    modifier = Modifier.size(200.dp),
                )
                Text(
                    text = trip.inviteCode.toCharArray().joinToString(" "),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier =
                        Modifier.clickable {
                            clipboard.setText(AnnotatedString(trip.inviteCode))
                        },
                )
                Text(
                    text = stringResource(R.string.overview_invite_code_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(joinLink)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.overview_invite_copy_link))
                    }
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.overview_invite_share))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.overview_invite_close))
            }
        },
    )
}

/** Fire the system share sheet with the trip's join code + link. */
internal fun shareInvite(
    context: Context,
    trip: Trip,
) {
    val message =
        context.getString(
            R.string.overview_share_message,
            trip.name,
            trip.inviteCode,
            "https://triptogether.app/join/${trip.inviteCode}",
        )
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.overview_invite)),
    )
}
