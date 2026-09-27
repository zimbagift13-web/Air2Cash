package com.example.ui.screens.withdraw

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.WithdrawalEntity
import com.example.data.model.WithdrawalMethod
import com.example.ui.components.Air2CashAppBar
import com.example.ui.components.Formatters
import com.example.ui.components.SecurityPinDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AirtelRed
import com.example.ui.theme.AirtelRedLight
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.TnmGreen
import com.example.ui.theme.TnmGreenLight
import com.example.ui.viewmodel.Air2CashViewModel

@Composable
fun WithdrawScreen(
    viewModel: Air2CashViewModel,
    onWithdrawSuccess: (WithdrawalEntity) -> Unit,
    onNavigateBack: () -> Unit
) {
    val wallet by viewModel.currentWallet.collectAsState()
    val selectedMethod by viewModel.selectedWithdrawMethod.collectAsState()
    val amountInput by viewModel.withdrawAmountInput.collectAsState()
    val destinationPhone by viewModel.withdrawDestinationPhone.collectAsState()
    val recipientName by viewModel.withdrawRecipientName.collectAsState()
    val calculation by viewModel.withdrawCalculation.collectAsState()
    val isWithdrawing by viewModel.isWithdrawing.collectAsState()
    val errorMessage by viewModel.withdrawErrorMessage.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Air2CashAppBar(
            title = "Withdraw Cash",
            canNavigateBack = true,
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Available Balance Reminder
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available to Withdraw:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = Formatters.formatMwk(wallet?.availableBalance ?: 0.0),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Method Selection
            Text(
                text = "1. Select Mobile Money / Bank Service",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Method cards
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Airtel Money
                val isAirtel = selectedMethod == WithdrawalMethod.AIRTEL_MONEY
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAirtel) AirtelRedLight else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isAirtel) androidx.compose.foundation.BorderStroke(2.dp, AirtelRed) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectWithdrawMethod(WithdrawalMethod.AIRTEL_MONEY) }
                        .testTag("method_airtel_money")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(AirtelRed)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Airtel Money Malawi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("1.5% network fee • Instant delivery", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // TNM Mpamba
                val isTnm = selectedMethod == WithdrawalMethod.TNM_MPAMBA
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTnm) TnmGreenLight else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isTnm) androidx.compose.foundation.BorderStroke(2.dp, TnmGreen) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectWithdrawMethod(WithdrawalMethod.TNM_MPAMBA) }
                        .testTag("method_tnm_mpamba")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(TnmGreen)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("TNM Mpamba", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("1.5% network fee • Instant delivery", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Bank Transfer
                val isBank = selectedMethod == WithdrawalMethod.BANK_TRANSFER
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBank) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isBank) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectWithdrawMethod(WithdrawalMethod.BANK_TRANSFER) }
                        .testTag("method_bank")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Malawi Commercial Bank", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Flat MWK 500 fee • National Switch (Standard, National, FDH)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Amount Input
            Text(
                text = "2. Amount to Withdraw",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amountInput,
                onValueChange = { viewModel.setWithdrawAmount(it) },
                label = { Text("Amount (MWK)") },
                prefix = { Text("MK ", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("withdraw_amount_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Destination Account Details
            Text(
                text = "3. Recipient Account Details",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = destinationPhone,
                onValueChange = { viewModel.withdrawDestinationPhone.value = it },
                label = {
                    Text(
                        if (selectedMethod == WithdrawalMethod.BANK_TRANSFER) "Bank Account Number"
                        else "Mobile Money Number"
                    )
                },
                placeholder = {
                    Text(
                        if (selectedMethod == WithdrawalMethod.AIRTEL_MONEY) "e.g. +265 991 234 567"
                        else if (selectedMethod == WithdrawalMethod.TNM_MPAMBA) "e.g. +265 881 234 567"
                        else "e.g. 1002345678"
                    )
                },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("withdraw_destination_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = recipientName,
                onValueChange = { viewModel.withdrawRecipientName.value = it },
                label = { Text("Recipient Full Name") },
                placeholder = { Text("Registered name on account") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Fee & Net Payout Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Withdrawal Summary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Requested Amount", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = Formatters.formatMwk(amountInput.toDoubleOrNull() ?: 0.0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Disbursement Fee", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = Formatters.formatMwk(calculation?.fee ?: 0.0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Amount You Receive",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = Formatters.formatMwk(calculation?.netPayout ?: 0.0),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimaryLight,
                            modifier = Modifier.testTag("net_payout_text")
                        )
                    }
                }
            }

            AnimatedVisibility(visible = calculation?.isValid == false) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = calculation?.errorMessage ?: "Invalid withdrawal request",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val currentBal = wallet?.availableBalance ?: 0.0
            val requestedAmt = amountInput.toDoubleOrNull() ?: 0.0
            val isBalanceSufficient = currentBal >= requestedAmt && requestedAmt > 0

            Button(
                onClick = { showPinDialog = true },
                enabled = !isWithdrawing && (calculation?.isValid == true) && isBalanceSufficient && destinationPhone.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("request_withdrawal_btn")
            ) {
                if (isWithdrawing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Authorize Withdrawal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!isBalanceSufficient && requestedAmt > 0) {
                Text(
                    text = "Insufficient balance. You need ${Formatters.formatMwk(requestedAmt)}, available is ${Formatters.formatMwk(currentBal)}.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    // Security PIN dialog
    SecurityPinDialog(
        isOpen = showPinDialog,
        onDismiss = { showPinDialog = false },
        onConfirmPin = { pin ->
            showPinDialog = false
            viewModel.submitWithdrawal(
                pin = pin,
                onSuccess = { wdr ->
                    onWithdrawSuccess(wdr)
                },
                onError = { /* handled in viewModel.withdrawErrorMessage */ }
            )
        }
    )
}

@Composable
fun WithdrawalConfirmationScreen(
    reference: String,
    viewModel: Air2CashViewModel,
    onDone: () -> Unit
) {
    val withdrawals by viewModel.userWithdrawals.collectAsState()
    val wdr = withdrawals.firstOrNull { it.reference == reference }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val isCompleted = wdr?.status == "COMPLETED"
        val isPending = wdr?.status == "PENDING" || wdr?.status == "PROCESSING"

        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = if (isCompleted) Color(0xFFE8F8ED) else MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isPending) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldPrimaryLight,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isCompleted) "Withdrawal Successful!" else "Processing Disbursement",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isCompleted)
                "Funds sent to ${wdr?.destinationNumber} via ${wdr?.method?.replace('_', ' ')}."
            else
                "Connecting with Malawi mobile money switch. Your funds will arrive in seconds.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Reference", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = reference, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Amount Payout", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatMwk(wdr?.netPayout ?: 0.0),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimaryLight
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Status", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusBadge(status = wdr?.status ?: "PROCESSING")
                }
                if (wdr?.providerTransactionId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Provider Txn ID", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = wdr.providerTransactionId ?: "", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDone,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
