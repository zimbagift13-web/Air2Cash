package com.example.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AirtimeTransactionEntity
import com.example.data.local.entity.ConversionRateEntity
import com.example.data.local.entity.FeeConfigEntity
import com.example.data.local.entity.SupportTicketEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.WithdrawalEntity
import com.example.data.model.MobileNetwork
import com.example.ui.components.Air2CashAppBar
import com.example.ui.components.Formatters
import com.example.ui.components.NetworkBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.viewmodel.Air2CashViewModel

@Composable
fun AdminPortalScreen(
    viewModel: Air2CashViewModel,
    onNavigateBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allAirtimeTxns by viewModel.allAirtimeTransactions.collectAsState()
    val allWithdrawals by viewModel.allWithdrawals.collectAsState()
    val allRates by viewModel.allRates.collectAsState()
    val allFeeConfigs by viewModel.allFeeConfigs.collectAsState()
    val allAuditLogs by viewModel.allAuditLogs.collectAsState()
    val allTickets by viewModel.allSupportTickets.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Overview", "Users", "Airtime Requests", "Withdrawals", "Rates & Fees", "Support Desk", "Audit Logs")

    // Modals
    var editingRate by remember { mutableStateOf<ConversionRateEntity?>(null) }
    var replyingTicket by remember { mutableStateOf<SupportTicketEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Air2CashAppBar(
            title = "Admin Console",
            canNavigateBack = true,
            onNavigateBack = onNavigateBack
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Status banner
        if (statusMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.clearStatusMessage() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = statusMessage ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // Overview Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(text = "Malawi Operations Overview", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminStatCard("Registered Users", "${allUsers.size}", Icons.Default.People, Modifier.weight(1f))
                            val pendingAirtime = allAirtimeTxns.count { it.verificationStatus == "PENDING" || it.verificationStatus == "PROCESSING" }
                            AdminStatCard("Pending Airtime", "$pendingAirtime", Icons.Default.Receipt, Modifier.weight(1f))
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val totalConverted = allAirtimeTxns.filter { it.verificationStatus == "COMPLETED" }.sumOf { it.grossCash }
                            AdminStatCard("Total Converted", Formatters.formatMwk(totalConverted), Icons.Default.Tune, Modifier.weight(1f))
                            val totalDisbursed = allWithdrawals.filter { it.status == "COMPLETED" }.sumOf { it.netPayout }
                            AdminStatCard("Total Disbursed", Formatters.formatMwk(totalDisbursed), Icons.Default.Security, Modifier.weight(1f))
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Operational Controls", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "Active Telecom Providers:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    NetworkBadge(MobileNetwork.AIRTEL)
                                    NetworkBadge(MobileNetwork.TNM)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "All rate calculations and double-entry ledger transactions are enforced atomically on the database backend.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // Users Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(text = "Platform Users (${allUsers.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    items(allUsers) { u ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = u.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = if (u.role == "ADMIN") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = u.role,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "${u.phone} • Code: ${u.referralCode}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = if (u.status == "ACTIVE") Color(0xFFE8F8ED) else Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = u.status,
                                            color = if (u.status == "ACTIVE") EmeraldPrimaryLight else Color(0xFFDC2626),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (u.role != "ADMIN") {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedButton(
                                            onClick = { viewModel.toggleUserStatus(u.id, u.status) },
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(if (u.status == "ACTIVE") "Suspend" else "Activate", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Airtime Requests Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(text = "Airtime Liquidations (${allAirtimeTxns.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    items(allAirtimeTxns) { txn ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = txn.reference, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    StatusBadge(status = txn.verificationStatus)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    NetworkBadge(MobileNetwork.fromString(txn.network))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "From: ${txn.senderPhone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Airtime: ${Formatters.formatMwk(txn.airtimeAmount)} @ ${"%.0f".format(txn.conversionRatePercent)}% → Net Cash: ${Formatters.formatMwk(txn.netCash)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (txn.verificationStatus == "PENDING" || txn.verificationStatus == "PROCESSING") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.manualApproveAirtime(txn.reference, "Admin manual verification") },
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Approve & Credit", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // Withdrawals Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(text = "Disbursements & Payouts (${allWithdrawals.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    items(allWithdrawals) { wdr ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = wdr.reference, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    StatusBadge(status = wdr.status)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "${wdr.method.replace('_', ' ')} → ${wdr.destinationNumber} (${wdr.recipientName})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Payout: ${Formatters.formatMwk(wdr.netPayout)} (Fee: ${Formatters.formatMwk(wdr.fee)})",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (wdr.providerTransactionId != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "Provider ID: ${wdr.providerTransactionId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            4 -> {
                // Rates & Fees Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(text = "Airtime Conversion Rates", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    items(allRates) { rate ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "${rate.network} Malawi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "Payout: ${"%.0f".format(rate.ratePercent)}% • Active: ${rate.isActive}", fontSize = 12.sp)
                                    Text(text = "Min: ${Formatters.formatMwk(rate.minAmount)} | Max: ${Formatters.formatMwk(rate.maxAmount)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { editingRate = rate }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Rate")
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Withdrawal Fees Config", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    items(allFeeConfigs) { fee ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = fee.feeType.replace('_', ' '), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "Percentage: ${fee.percentageFee}% | Fixed Fee: ${Formatters.formatMwk(fee.fixedFee)}", fontSize = 12.sp)
                                Text(text = "Limits: Min ${Formatters.formatMwk(fee.minAmount)} • Max ${Formatters.formatMwk(fee.maxAmount)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            5 -> {
                // Support Desk Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(text = "Customer Support Tickets (${allTickets.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    items(allTickets) { ticket ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "${ticket.ticketNumber} • ${ticket.userPhone}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    StatusBadge(status = ticket.status)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = ticket.subject, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = ticket.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (ticket.adminReply != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Reply: ${ticket.adminReply}", fontSize = 12.sp, color = EmeraldPrimaryLight, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { replyingTicket = ticket },
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reply & Update", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            6 -> {
                // Audit Logs Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Text(text = "System Security & Audit Trail", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    items(allAuditLogs) { log ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = log.action, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(text = Formatters.formatDate(log.timestamp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = log.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Rate Dialog
    if (editingRate != null) {
        val rate = editingRate!!
        var ratePercent by remember { mutableStateOf(rate.ratePercent.toString()) }
        var minAmt by remember { mutableStateOf(rate.minAmount.toString()) }
        var maxAmt by remember { mutableStateOf(rate.maxAmount.toString()) }

        AlertDialog(
            onDismissRequest = { editingRate = null },
            title = { Text("Edit ${rate.network} Rate") },
            text = {
                Column {
                    OutlinedTextField(
                        value = ratePercent,
                        onValueChange = { ratePercent = it },
                        label = { Text("Payout Percentage (%)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = minAmt,
                        onValueChange = { minAmt = it },
                        label = { Text("Min Amount (MWK)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = maxAmt,
                        onValueChange = { maxAmt = it },
                        label = { Text("Max Amount (MWK)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rp = ratePercent.toDoubleOrNull() ?: rate.ratePercent
                        val min = minAmt.toDoubleOrNull() ?: rate.minAmount
                        val max = maxAmt.toDoubleOrNull() ?: rate.maxAmount
                        viewModel.updateConversionRate(rate.network, rp, rate.feePercent, min, max, rate.isActive)
                        editingRate = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingRate = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reply Ticket Dialog
    if (replyingTicket != null) {
        val ticket = replyingTicket!!
        var replyText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { replyingTicket = null },
            title = { Text("Reply to ${ticket.ticketNumber}") },
            text = {
                Column {
                    Text("Subject: ${ticket.subject}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Admin Reply") },
                        placeholder = { Text("Enter response to user...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.replySupportTicket(ticket.ticketNumber, replyText, "RESOLVED")
                        replyingTicket = null
                    },
                    enabled = replyText.isNotBlank()
                ) {
                    Text("Send & Resolve")
                }
            },
            dismissButton = {
                TextButton(onClick = { replyingTicket = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
