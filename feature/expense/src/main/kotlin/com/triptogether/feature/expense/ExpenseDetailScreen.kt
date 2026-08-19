package com.triptogether.feature.expense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.triptogether.core.domain.model.Expense
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.Share
import com.triptogether.core.domain.model.SplitMode
import com.triptogether.core.ui.theme.TripTogetherTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter

/** S10 — full bill info, per-member shares, slip thumbnails with a zoomable viewer. */
@Composable
fun ExpenseDetailScreen(
    onEdit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ExpenseDetailEvent.Deleted -> onBack()
                is ExpenseDetailEvent.Error ->
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
            }
        }
    }

    ExpenseDetailContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEdit = onEdit,
        onDelete = viewModel::delete,
        onUpdateMyShare = viewModel::updateOwnShare,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDetailContent(
    uiState: ExpenseDetailUiState,
    snackbarHostState: SnackbarHostState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdateMyShare: (Money) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var viewerUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.expense?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.expense_back),
                        )
                    }
                },
                actions = {
                    if (uiState.canEdit) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.expense_detail_edit),
                            )
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.expense_detail_delete),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val expense = uiState.expense
        when {
            uiState.isLoading ->
                CircularProgressIndicator(modifier = Modifier.padding(innerPadding).padding(32.dp))
            expense == null ->
                Text(
                    text = stringResource(R.string.expense_detail_not_found),
                    modifier = Modifier.padding(innerPadding).padding(32.dp),
                )
            else ->
                Column(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SummaryCard(expense = expense, uiState = uiState)
                    SharesCard(expense = expense, uiState = uiState, onUpdateMyShare = onUpdateMyShare)
                    if (expense.slipUrls.isNotEmpty()) {
                        SlipsCard(urls = expense.slipUrls, onOpen = { viewerUrl = it })
                    }
                }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.expense_detail_delete_confirm_title)) },
            text = { Text(stringResource(R.string.expense_detail_delete_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.expense_detail_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.expense_detail_cancel))
                }
            },
        )
    }

    viewerUrl?.let { url ->
        SlipViewerDialog(url = url, onDismiss = { viewerUrl = null })
    }
}

@Composable
private fun SummaryCard(
    expense: Expense,
    uiState: ExpenseDetailUiState,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = expense.category.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = expense.totalAmount.formatWithSymbol(),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Text(
                text =
                    buildString {
                        append(expense.date.toJavaLocalDate().format(DateTimeFormatter.ofPattern("E d MMM yy")))
                        expense.time?.let { append(" · ${formatExpenseTime(it)}") }
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.expense_paid_by, uiState.memberName(expense.paidByMemberId)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.expense_detail_created_by, uiState.memberName(expense.createdBy)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SharesCard(
    expense: Expense,
    uiState: ExpenseDetailUiState,
    onUpdateMyShare: (Money) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingShare by remember { mutableStateOf<Money?>(null) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text =
                    stringResource(
                        R.string.expense_detail_shares_title,
                        stringResource(expense.splitMode.labelRes()),
                    ),
                style = MaterialTheme.typography.titleSmall,
            )
            HorizontalDivider()
            expense.shares.forEach { share ->
                val isMine = uiState.canEditOwnShare && share.memberId == uiState.myMemberId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text =
                            buildString {
                                append(uiState.memberName(share.memberId))
                                share.weight?.let { append(" ×$it") }
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = share.amount.formatWithSymbol(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (isMine) {
                        IconButton(
                            onClick = { editingShare = share.amount },
                            modifier = Modifier.size(32.dp).testTag("edit_my_share"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.expense_edit_my_share),
                            )
                        }
                    }
                }
            }
        }
    }

    editingShare?.let { current ->
        EditMyShareDialog(
            current = current,
            onDismiss = { editingShare = null },
            onSave = {
                onUpdateMyShare(it)
                editingShare = null
            },
        )
    }
}

@Composable
private fun EditMyShareDialog(
    current: Money,
    onDismiss: () -> Unit,
    onSave: (Money) -> Unit,
) {
    var input by remember { mutableStateOf(current.format()) }
    val parsed = Money.parse(input.trim())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.expense_edit_my_share)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.expense_edit_my_share_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("edit_my_share_input"),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onSave) },
                enabled = parsed != null,
            ) {
                Text(stringResource(R.string.expense_edit_my_share_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.expense_detail_cancel))
            }
        },
    )
}

@Composable
private fun SlipsCard(
    urls: List<String>,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.expense_detail_slips),
                style = MaterialTheme.typography.titleSmall,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(urls.size) { index ->
                    AsyncImage(
                        model = urls[index],
                        contentDescription = stringResource(R.string.expense_detail_slips),
                        modifier =
                            Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpen(urls[index]) },
                    )
                }
            }
        }
    }
}

/** Zoom/pan viewer for a slip photo. */
@Composable
private fun SlipViewerDialog(
    url: String,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier =
                    Modifier
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY,
                        ),
            )
        }
    }
}

private const val MAX_ZOOM = 5f

@Preview(showBackground = true)
@Composable
private fun ExpenseDetailContentPreview() {
    TripTogetherTheme {
        ExpenseDetailContent(
            uiState =
                ExpenseDetailUiState(
                    isLoading = false,
                    expense =
                        Expense(
                            id = "e1",
                            title = "มื้อเย็น — ร้านหมูกระทะ",
                            totalAmount = Money(185_000),
                            paidByMemberId = "m1",
                            date = LocalDate(2026, 12, 5),
                            splitMode = SplitMode.EXACT,
                            shares = listOf(Share("m1", Money(100_000)), Share("m2", Money(85_000))),
                            createdBy = "m1",
                        ),
                    members = listOf(Member(id = "m1", userId = "u1", displayName = "สมชาย")),
                    canEdit = true,
                ),
            snackbarHostState = SnackbarHostState(),
            onEdit = {},
            onDelete = {},
            onUpdateMyShare = {},
            onBack = {},
        )
    }
}
