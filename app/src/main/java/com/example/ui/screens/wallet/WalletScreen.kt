package com.example.ui.screens.wallet

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.WalletLedgerEntity
import com.example.ui.components.Air2CashAppBar
import com.example.ui.components.BalanceCard
import com.example.ui.components.Formatters
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.Air2CashViewModel

@Composable
fun WalletScreen(
    viewModel: Air2CashViewModel,
    onNavigateToConvert: () -> Unit,
    onNavigateToWithdraw: () -> Unit,
    onSelectLedgerEntry: (WalletLedgerEntity) -> Unit
) {
    val wallet by viewModel.currentWallet.collectAsState()
    val ledger by viewModel.userLedger.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredLedger = remember(ledger, selectedFilter) {
        when (selectedFilter) {
            "CREDIT" -> ledger.filter { it.entryType == "CREDIT" }
            "DEBIT" -> ledger.filter { it.entryType == "DEBIT" }
            "REFERRAL" -> ledger.filter { it.category == "REFERRAL_BONUS" }
            else -> ledger
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Air2CashAppBar(
            title = "My Wallet",
            canNavigateBack = false
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("wallet_screen_column")
        ) {
            // Balance Card
            item {
                Spacer(modifier = Modifier.height(10.dp))
                BalanceCard(
                    availableBalance = wallet?.availableBalance ?: 0.0,
                    pendingBalance = wallet?.pendingBalance ?: 0.0,
                    onConvertClick = onNavigateToConvert,
                    onWithdrawClick = onNavigateToWithdraw
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Ledger Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All Ledger") }
                    )
                    FilterChip(
                        selected = selectedFilter == "CREDIT",
                        onClick = { selectedFilter = "CREDIT" },
                        label = { Text("Cash In (+)") }
                    )
                    FilterChip(
                        selected = selectedFilter == "DEBIT",
                        onClick = { selectedFilter = "DEBIT" },
                        label = { Text("Withdrawals (-)") }
                    )
                    FilterChip(
                        selected = selectedFilter == "REFERRAL",
                        onClick = { selectedFilter = "REFERRAL" },
                        label = { Text("Bonuses") }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Ledger Entries Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ledger Audit Records",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${filteredLedger.size} entries",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (filteredLedger.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No wallet ledger entries yet",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Converted airtime and withdrawals create permanent immutable ledger records.",
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(filteredLedger) { entry ->
                    val isCredit = entry.entryType == "CREDIT"
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelectLedgerEntry(entry) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = CircleShape,
                                    color = if (isCredit) Color(0xFFE8F8ED) else Color(0xFFF1F5F9)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (entry.category == "REFERRAL_BONUS") Icons.Default.CardGiftcard
                                            else if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (entry.category == "REFERRAL_BONUS") GoldAccent
                                            else if (isCredit) EmeraldPrimaryLight else Color(0xFF475569),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = entry.description,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "${Formatters.formatDate(entry.createdAt)} • Ref: ${entry.reference.takeLast(8)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = (if (isCredit) "+" else "-") + Formatters.formatMwk(entry.amount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (isCredit) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Bal: ${Formatters.formatMwk(entry.balanceAfter)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
