package com.example.ui.screens.support

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import com.example.ui.components.Air2CashAppBar
import com.example.ui.components.Formatters
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.viewmodel.Air2CashViewModel

data class FaqItem(val question: String, val answer: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    initialTransactionRef: String? = null,
    viewModel: Air2CashViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(if (initialTransactionRef != null) 1 else 0) }
    val userTickets by viewModel.userTickets.collectAsState()

    // Ticket form state
    var subject by remember { mutableStateOf(if (initialTransactionRef != null) "Inquiry on $initialTransactionRef" else "") }
    var category by remember { mutableStateOf("CONVERSION") }
    var transactionRef by remember { mutableStateOf(initialTransactionRef ?: "") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submissionSuccess by remember { mutableStateOf<String?>(null) }
    var submissionError by remember { mutableStateOf<String?>(null) }

    val faqs = listOf(
        FaqItem(
            "How do I convert Airtel airtime to cash?",
            "Select Airtel Malawi on the Convert screen, enter your amount, and follow the transfer instructions by dialing *211# (Airtel Airtime Share) to transfer to our verified collection line. Then tap Verify."
        ),
        FaqItem(
            "How do I convert TNM airtime to cash?",
            "Select TNM Malawi, enter amount, dial *444# (TNM Me2U) to send the airtime to our collection line, and tap Verify. Our telecom gateway will credit your wallet automatically."
        ),
        FaqItem(
            "How long do withdrawals take?",
            "Airtel Money and TNM Mpamba withdrawals are processed instantly upon entering your 4-digit security PIN. Bank transfers through Malawi National Switch take 5 to 30 minutes."
        ),
        FaqItem(
            "What if my airtime was sent but not credited?",
            "Tap on the transaction in your History, or use the 'New Ticket' tab here to report the issue. Make sure your transaction reference number is attached so our Lilongwe support desk can verify it immediately."
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Air2CashAppBar(
            title = "Help & Support",
            canNavigateBack = true,
            onNavigateBack = onNavigateBack
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("FAQs", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("New Ticket", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("My Tickets (${userTickets.size})", fontWeight = FontWeight.Bold) }
            )
        }

        when (selectedTab) {
            0 -> {
                // FAQs
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Frequently Asked Questions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(faqs) { faq ->
                        var isExpanded by remember { mutableStateOf(false) }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = faq.question,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null
                                    )
                                }
                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = faq.answer,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Need further assistance?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Our support team is available 24/7 in Lilongwe & Blantyre. Open a ticket or contact us via WhatsApp: +265 991 234 567.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // New Ticket Form
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    item {
                        Text(
                            text = "Submit a Support Request",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Describe your issue and attach any transaction reference.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Subject") },
                            placeholder = { Text("e.g. Delayed Airtime Transfer") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("support_subject_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = transactionRef,
                            onValueChange = { transactionRef = it },
                            label = { Text("Transaction Reference (Optional)") },
                            placeholder = { Text("e.g. A2C-MW-AIR-...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("support_ref_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Detailed Description") },
                            placeholder = { Text("Explain what happened, your phone number, network, etc.") },
                            minLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("support_description_input")
                        )

                        AnimatedVisibility(visible = submissionError != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                Text(
                                    text = submissionError ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        AnimatedVisibility(visible = submissionSuccess != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                Text(
                                    text = "Ticket created: $submissionSuccess. Our support team has been notified!",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                isSubmitting = true
                                submissionError = null
                                submissionSuccess = null
                                viewModel.submitSupportTicket(
                                    subject = subject,
                                    category = category,
                                    transactionRef = transactionRef,
                                    description = description
                                ) { success, result ->
                                    isSubmitting = false
                                    if (success) {
                                        submissionSuccess = result
                                        subject = ""
                                        description = ""
                                        transactionRef = ""
                                    } else {
                                        submissionError = result ?: "Failed to submit ticket."
                                    }
                                }
                            },
                            enabled = !isSubmitting && subject.isNotBlank() && description.length >= 10,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_ticket_btn")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Submit Ticket", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            2 -> {
                // My Tickets
                if (userTickets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.QuestionAnswer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No support tickets submitted",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(userTickets) { ticket ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
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
                                        Text(text = ticket.ticketNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                        StatusBadge(status = ticket.status)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = ticket.subject, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = ticket.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (ticket.transactionRef != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Ref: ${ticket.transactionRef}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    if (ticket.adminReply != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(text = "Support Desk Response:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                Text(text = ticket.adminReply ?: "", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = Formatters.formatDate(ticket.createdAt), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
