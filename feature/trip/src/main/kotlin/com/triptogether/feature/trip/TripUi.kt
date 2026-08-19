package com.triptogether.feature.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.triptogether.core.domain.model.Member
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Small shared pieces used by the trip screens. */
@Composable
internal fun MemberAvatar(
    member: Member,
    modifier: Modifier = Modifier,
) {
    if (member.photoUrl != null) {
        AsyncImage(
            model = member.photoUrl,
            contentDescription = member.displayName,
            modifier = modifier.size(28.dp).clip(CircleShape),
        )
    } else {
        Box(
            modifier =
                modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = member.displayName.take(1),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

internal fun formatDateRange(
    start: LocalDate,
    end: LocalDate,
): String {
    // Locale.getDefault() follows the per-app locale, so month names match the UI language.
    val formatter = DateTimeFormatter.ofPattern("d MMM yy", Locale.getDefault())
    return "${formatter.format(start.toJavaLocalDate())} – ${formatter.format(end.toJavaLocalDate())}"
}
