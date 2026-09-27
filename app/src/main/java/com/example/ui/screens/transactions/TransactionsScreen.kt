package com.example.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AirtimeTransactionEntity
import com.example.data.local.entity.WithdrawalEntity
import com.example.data.model.MobileNetwork
import com.example.ui.components.Air2CashAppBar
import com.example.ui.components.Formatters
import com.example.ui.components.NetworkBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.viewmodel.Air2CashViewModel

sealed class TransactionItem(
    val reference: String,
    val timestamp: Long,
    val status: String,
    val title: String,
    val amountDisplay: String,
    val isCredit: Boolean
) {
    class Airtime(val entity: AirtimeTransactionEntity) : TransactionItem(
        reference = entity.reference,
        timestamp = entity.createdAt,
        status = entity.verificationStatus,
        title = "Airtime to Cash (${entity.network})",
        amountDisplay = "+${Formatters.formatMwk(entity.netCash)}",
        isCredit = true
    )

    class Withdrawal(val entity: WithdrawalEntity) : TransactionItem(
        reference = entity.reference,
        timestamp = entity.createdAt,
        status = entity.status,
        title = "Withdrawal (${entity.method.replace('_', ' ')})",
        amountDisplay = "-${Formatters.formatMwk(entity.amount)}",
        isCredit = false
    )
}

@Composable
fun TransactionsScreen(
    viewModel: Air2CashViewModel,
    onReportSupportWithRef: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val airtimeTxns by viewModel.userAirtimeTransactions.collectAsState()
    val withdrawals by viewModel.userWithdrawals.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("ALL") }
    var selectedItemForDetail by remember { mutableStateOf<TransactionItem?>(null) }

    val allTransactions: List<TransactionItem> = remember(airtimeTxns, withdrawals) {
        val list = mutableListOf<TransactionItem>()
        airtimeTxns.forEach { list.add(TransactionItem.Airtime(it)) }
        withdrawals.forEach { list.add(TransactionItem.Withdrawal(it)) }
        list.sortedByDescending { it.timestamp }
    }

    val filteredList = remember(allTransactions, selectedTab, searchQuery) {
        allTransactions.filter { item ->
            val matchesTab = when (selectedTab) {
                "PENDING" -> item.status == "PENDING" || item.status == "PROCESSING"
                "COMPLETED" -> item.status == "COMPLETED"
                "FAILED" -> item.status == "FAILED" || item.status == "CANCELLED" || item.status == "REVERSED"
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.reference.contains(searchQuery, ignoreCase = true) ||
                        item.title.contains(searchQuery, ignoreCase = true)
            }

            matchesTab && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Air2CashAppBar(
            title = "Transaction History",
            canNavigateBack = true,
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by reference, e.g. A2C-MW-...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == "ALL",
                    onClick = { selectedTab = "ALL" },
                    label = { Text("All (${allTransactions.size})") }
                )
                FilterChip(
                    selected = selectedTab == "PENDING",
                    onClick = { selectedTab = "PENDING" },
                    label = { Text("Pending") }
                )
                FilterChip(
                    selected = selectedTab == "COMPLETED",
                    onClick = { selectedTab = "COMPLETED" },
                    label = { Text("Completed") }
                )
                FilterChip(
                    selected = selectedTab == "FAILED",
                    onClick = { selectedTab = "FAILED" },
                    label = { Text("Failed") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No transactions found",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting your search or filter tab",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList) { item ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedItemForDetail = item }
                                .testTag("transaction_item_${item.reference}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = item.reference,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = Formatters.formatDate(item.timestamp),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = item.amountDisplay,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = if (item.isCredit) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    StatusBadge(status = item.status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Transaction Details Dialog
    if (selectedItemForDetail != null) {
        val detail = selectedItemForDetail!!
        val clipboard = LocalClipboardManager.current
        var isVerifyingNow by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedItemForDetail = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Transaction Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    StatusBadge(status = detail.status)
                }
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Reference:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = detail.reference, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = { clipboard.setText(AnnotatedString(detail.reference)) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Date & Time:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = Formatters.formatDate(detail.timestamp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    if (detail is TransactionItem.Airtime) {
                        val airtime = detail.entity
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Network:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            NetworkBadge(MobileNetwork.fromString(airtime.network))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Airtime Amount:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = Formatters.formatMwk(airtime.airtimeAmount), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Rate:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${"%.0f".format(airtime.conversionRatePercent)}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Cash Credited:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = Formatters.formatMwk(airtime.netCash), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldPrimaryLight)
                        }
                        if (airtime.providerReference != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Telecom Gateway Ref:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = airtime.providerReference, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else if (detail is TransactionItem.Withdrawal) {
                        val wdr = detail.entity
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Payout Service:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = wdr.method.replace('_', ' '), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Destination Account:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = wdr.destinationNumber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Fee:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = Formatters.formatMwk(wdr.fee), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Net Cash Payout:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = Formatters.formatMwk(wdr.netPayout), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        if (wdr.providerTransactionId != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Provider Txn ID:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = wdr.providerTransactionId, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row {
                    OutlinedButton(
                        onClick = {
                            val ref = detail.reference
                            selectedItemForDetail = null
                            onReportSupportWithRef(ref)
                        }
                    ) {
                        Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Get Help")
                    }

                    if (detail is TransactionItem.Airtime && (detail.status == "PENDING" || detail.status == "PROCESSING")) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                isVerifyingNow = true
                                viewModel.verifyAirtimeTransaction(detail.reference) { _, _ ->
                                    isVerifyingNow = false
                                    selectedItemForDetail = null
                                }
                            },
                            enabled = !isVerifyingNow
                        ) {
                            Text("Verify")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItemForDetail = null }) {
                    Text("Close")
                }
            }
        )
    }
}
