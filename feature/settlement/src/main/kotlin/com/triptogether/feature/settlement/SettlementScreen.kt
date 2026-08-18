package com.triptogether.feature.settlement

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.triptogether.core.domain.model.Balance
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.Settlement
import com.triptogether.core.domain.model.Transfer
import com.triptogether.core.domain.money.PromptPay
import com.triptogether.core.ui.theme.TripTogetherTheme

/** S11 — stats, balance table, suggested transfers with PromptPay QR, pending/confirmed groups. */
@Composable
fun SettlementScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettlementViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettlementEvent.Message ->
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
            }
        }
    }

    SettlementContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onMarkTransferred = viewModel::markTransferred,
        onConfirm = viewModel::confirm,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettlementContent(
    uiState: SettlementUiState,
    snackbarHostState: SnackbarHostState,
    onMarkTransferred: (Transfer) -> Unit,
    onConfirm: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var qrTransfer by remember { mutableStateOf<Transfer?>(null) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settlement_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settlement_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading ->
                CircularProgressIndicator(modifier = Modifier.padding(innerPadding).padding(32.dp))
            uiState.allSettled && uiState.confirmed.isEmpty() && uiState.tripTotal.isZero ->
                EmptyState(Modifier.padding(innerPadding))
            else ->
                LazyColumn(
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                ) {
                    item { StatsRow(uiState = uiState) }
                    item { BalancesCard(uiState = uiState) }
                    if (uiState.allSettled) {
                        item {
                            Text(
                                text = stringResource(R.string.settlement_all_clear),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (uiState.transfers.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.settlement_suggested)) }
                        items(uiState.transfers.size) { index ->
                            TransferRow(
                                transfer = uiState.transfers[index],
                                uiState = uiState,
                                onShowQr = { qrTransfer = it },
                                onMarkTransferred = onMarkTransferred,
                            )
                        }
                    }
                    if (uiState.pending.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.settlement_pending)) }
                        items(uiState.pending.size) { index ->
                            PendingRow(
                                settlement = uiState.pending[index],
                                uiState = uiState,
                                onConfirm = onConfirm,
                            )
                        }
                    }
                    if (uiState.confirmed.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.settlement_cleared)) }
                        items(uiState.confirmed.size) { index ->
                            ConfirmedRow(settlement = uiState.confirmed[index], uiState = uiState)
                        }
                    }
                }
        }
    }

    qrTransfer?.let { transfer ->
        PromptPayQrDialog(
            transfer = transfer,
            uiState = uiState,
            onDismiss = { qrTransfer = null },
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.settlement_all_clear))
    }
}

@Composable
private fun StatsRow(
    uiState: SettlementUiState,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            label = stringResource(R.string.settlement_trip_total),
            value = uiState.tripTotal.formatWithSymbol(),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.settlement_per_person),
            value = uiState.perPersonAverage.formatWithSymbol(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun BalancesCard(
    uiState: SettlementUiState,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.settlement_balances),
                style = MaterialTheme.typography.titleSmall,
            )
            HorizontalDivider()
            uiState.balances.forEach { balance ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = uiState.memberName(balance.memberId),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = balance.amount.formatSigned(),
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            when {
                                balance.amount.isPositive -> MaterialTheme.colorScheme.tertiary
                                balance.amount.isNegative -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
private fun TransferRow(
    transfer: Transfer,
    uiState: SettlementUiState,
    onShowQr: (Transfer) -> Unit,
    onMarkTransferred: (Transfer) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text =
                    stringResource(
                        R.string.settlement_transfer_line,
                        uiState.memberName(transfer.fromMemberId),
                        uiState.memberName(transfer.toMemberId),
                        transfer.amount.formatWithSymbol(),
                    ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.promptpayIdOf(transfer.toMemberId) != null) {
                    OutlinedButton(onClick = { onShowQr(transfer) }, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Default.QrCode2, contentDescription = null)
                        Text(
                            text = stringResource(R.string.settlement_qr),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.settlement_no_promptpay),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(top = 12.dp),
                    )
                }
                if (transfer.fromMemberId == uiState.myMemberId) {
                    OutlinedButton(
                        onClick = { onMarkTransferred(transfer) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.settlement_mark_paid))
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingRow(
    settlement: Settlement,
    uiState: SettlementUiState,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text =
                    stringResource(
                        R.string.settlement_transfer_line,
                        uiState.memberName(settlement.fromMemberId),
                        uiState.memberName(settlement.toMemberId),
                        settlement.amount.formatWithSymbol(),
                    ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (settlement.toMemberId == uiState.myMemberId) {
                TextButton(onClick = { onConfirm(settlement.id) }) {
                    Text(stringResource(R.string.settlement_confirm))
                }
            } else {
                Text(
                    text = stringResource(R.string.settlement_waiting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConfirmedRow(
    settlement: Settlement,
    uiState: SettlementUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text =
                stringResource(
                    R.string.settlement_transfer_line,
                    uiState.memberName(settlement.fromMemberId),
                    uiState.memberName(settlement.toMemberId),
                    settlement.amount.formatWithSymbol(),
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun PromptPayQrDialog(
    transfer: Transfer,
    uiState: SettlementUiState,
    onDismiss: () -> Unit,
) {
    val promptpayId = uiState.promptpayIdOf(transfer.toMemberId) ?: return
    val payload = remember(promptpayId, transfer.amount) { PromptPay.buildPayload(promptpayId, transfer.amount) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiState.memberName(transfer.toMemberId)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (payload == null) {
                    Text(stringResource(R.string.settlement_qr_invalid))
                } else {
                    val bitmap = remember(payload) { qrBitmap(payload) }
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.settlement_qr),
                        modifier = Modifier.size(240.dp),
                    )
                    Text(
                        text = transfer.amount.formatWithSymbol(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settlement_qr_close))
            }
        },
    )
}

private const val QR_SIZE_PX = 512

private fun qrBitmap(payload: String): Bitmap {
    val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, QR_SIZE_PX, QR_SIZE_PX)
    val pixels =
        IntArray(QR_SIZE_PX * QR_SIZE_PX) { index ->
            val x = index % QR_SIZE_PX
            val y = index / QR_SIZE_PX
            if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    return Bitmap.createBitmap(pixels, QR_SIZE_PX, QR_SIZE_PX, Bitmap.Config.RGB_565)
}

@Preview(showBackground = true)
@Composable
private fun SettlementContentPreview() {
    TripTogetherTheme {
        SettlementContent(
            uiState =
                SettlementUiState(
                    isLoading = false,
                    balances =
                        listOf(
                            Balance("m1", Money(20_000)),
                            Balance("m2", Money(-20_000)),
                        ),
                    transfers = listOf(Transfer("m2", "m1", Money(20_000))),
                    members =
                        listOf(
                            Member(id = "m1", userId = "u1", displayName = "สมชาย", promptpayId = "0812345678"),
                            Member(id = "m2", userId = "u2", displayName = "สมหญิง"),
                        ),
                    myMemberId = "m2",
                    tripTotal = Money(185_000),
                    perPersonAverage = Money(92_500),
                ),
            snackbarHostState = SnackbarHostState(),
            onMarkTransferred = {},
            onConfirm = {},
            onBack = {},
        )
    }
}
