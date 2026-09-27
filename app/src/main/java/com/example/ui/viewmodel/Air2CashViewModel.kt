package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AirtimeTransactionEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.ConversionRateEntity
import com.example.data.local.entity.FeeConfigEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.ReferralRewardEntity
import com.example.data.local.entity.SupportTicketEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.WalletEntity
import com.example.data.local.entity.WalletLedgerEntity
import com.example.data.local.entity.WithdrawalEntity
import com.example.data.model.MobileNetwork
import com.example.data.model.WithdrawalMethod
import com.example.data.repository.AdminRepository
import com.example.data.repository.AirtimeRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.ConversionCalculation
import com.example.data.repository.NotificationRepository
import com.example.data.repository.ReferralRepository
import com.example.data.repository.SupportRepository
import com.example.data.repository.WalletRepository
import com.example.data.repository.WithdrawalCalculation
import com.example.data.repository.WithdrawalRepository
import com.example.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class Air2CashViewModel(
    private val authRepo: AuthRepository,
    private val walletRepo: WalletRepository,
    private val airtimeRepo: AirtimeRepository,
    private val withdrawalRepo: WithdrawalRepository,
    private val referralRepo: ReferralRepository,
    private val notificationRepo: NotificationRepository,
    private val supportRepo: SupportRepository,
    private val adminRepo: AdminRepository
) : ViewModel() {

    // 1. Current Session State
    val currentUser: StateFlow<UserEntity?> = authRepo.currentUser

    // 2. Reactive Wallet State for Logged-in User
    val currentWallet: StateFlow<WalletEntity?> = currentUser
        .flatMapLatest { user ->
            if (user != null) walletRepo.observeWallet(user.id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 3. Reactive Ledger History for Logged-in User
    val userLedger: StateFlow<List<WalletLedgerEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) walletRepo.observeLedger(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Reactive Airtime Transactions for Logged-in User
    val userAirtimeTransactions: StateFlow<List<AirtimeTransactionEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) airtimeRepo.observeUserTransactions(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Reactive Withdrawals for Logged-in User
    val userWithdrawals: StateFlow<List<WithdrawalEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) withdrawalRepo.observeWithdrawals(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6. Reactive Rates & Fees
    val conversionRates: StateFlow<List<ConversionRateEntity>> = airtimeRepo.observeRates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7. Referral Info
    val referralRewards: StateFlow<List<ReferralRewardEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) referralRepo.observeRewards(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val referredCount: StateFlow<Int> = currentUser
        .flatMapLatest { user ->
            if (user != null) referralRepo.observeReferredCount(user.referralCode) else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // 8. Notifications
    val notifications: StateFlow<List<NotificationEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) notificationRepo.observeNotifications(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = currentUser
        .flatMapLatest { user ->
            if (user != null) notificationRepo.observeUnreadCount(user.id) else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // 9. Support Tickets
    val userTickets: StateFlow<List<SupportTicketEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) supportRepo.observeUserTickets(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 10. Admin Streams
    val allUsers: StateFlow<List<UserEntity>> = adminRepo.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAirtimeTransactions: StateFlow<List<AirtimeTransactionEntity>> = adminRepo.observeAllAirtimeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWithdrawals: StateFlow<List<WithdrawalEntity>> = adminRepo.observeAllWithdrawals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRates: StateFlow<List<ConversionRateEntity>> = adminRepo.observeAllRates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFeeConfigs: StateFlow<List<FeeConfigEntity>> = adminRepo.observeAllFeeConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditLogs: StateFlow<List<AuditLogEntity>> = adminRepo.observeAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSupportTickets: StateFlow<List<SupportTicketEntity>> = supportRepo.observeAllTickets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Convert Airtime Form State
    val selectedConvertNetwork = MutableStateFlow(MobileNetwork.AIRTEL)
    val convertAmountInput = MutableStateFlow("2000")
    val convertSenderPhone = MutableStateFlow("")
    val convertCalculation = MutableStateFlow<ConversionCalculation?>(null)
    val isConverting = MutableStateFlow(false)
    val convertErrorMessage = MutableStateFlow<String?>(null)

    // Withdraw Form State
    val selectedWithdrawMethod = MutableStateFlow(WithdrawalMethod.AIRTEL_MONEY)
    val withdrawAmountInput = MutableStateFlow("1500")
    val withdrawDestinationPhone = MutableStateFlow("")
    val withdrawRecipientName = MutableStateFlow("")
    val withdrawCalculation = MutableStateFlow<WithdrawalCalculation?>(null)
    val isWithdrawing = MutableStateFlow(false)
    val withdrawErrorMessage = MutableStateFlow<String?>(null)

    // General Action State / Messages
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            authRepo.initDefaultSession()
            // Set initial sender phone from user phone if available
            authRepo.currentUser.collect { user ->
                if (user != null) {
                    if (convertSenderPhone.value.isEmpty()) {
                        convertSenderPhone.value = user.phone
                    }
                    if (withdrawDestinationPhone.value.isEmpty()) {
                        withdrawDestinationPhone.value = user.phone
                    }
                    if (withdrawRecipientName.value.isEmpty()) {
                        withdrawRecipientName.value = user.fullName
                    }
                }
            }
        }
        recalculateConversion()
        recalculateWithdrawal()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // --- Authentication Actions ---
    fun login(phone: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authRepo.login(phone, pass)
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun register(
        phone: String,
        name: String,
        email: String,
        pass: String,
        referralCode: String?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = authRepo.register(phone, name, email, pass, referralCode)
            if (res.isSuccess) {
                onResult(true, res.getOrNull())
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun verifyOtp(phone: String, otp: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authRepo.verifyOtp(phone, otp)
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun resendOtp(phone: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authRepo.resendOtp(phone)
            if (res.isSuccess) {
                onResult(true, res.getOrNull())
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun resetPassword(phone: String, otp: String, newPass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authRepo.resetPassword(phone, otp, newPass)
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun updateProfile(name: String, email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authRepo.updateProfile(name, email)
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun setTransactionPin(pin: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authRepo.setTransactionPin(pin)
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun logout() {
        authRepo.logout()
    }

    fun switchToAdmin() {
        viewModelScope.launch {
            val admin = allUsers.value.firstOrNull { it.role == "ADMIN" }
            if (admin != null) {
                authRepo.switchUser(admin)
                _statusMessage.value = "Switched to Administrator session"
            }
        }
    }

    fun switchToDemoUser() {
        viewModelScope.launch {
            val demo = allUsers.value.firstOrNull { it.role == "USER" }
            if (demo != null) {
                authRepo.switchUser(demo)
                _statusMessage.value = "Switched to Demo User (Gift Zimba)"
            }
        }
    }

    // --- Convert Airtime Actions ---
    fun selectConvertNetwork(network: MobileNetwork) {
        selectedConvertNetwork.value = network
        recalculateConversion()
    }

    fun setConvertAmount(amountStr: String) {
        convertAmountInput.value = amountStr
        recalculateConversion()
    }

    fun recalculateConversion() {
        viewModelScope.launch {
            val amount = convertAmountInput.value.toDoubleOrNull() ?: 0.0
            val calc = airtimeRepo.calculateConversion(selectedConvertNetwork.value, amount)
            convertCalculation.value = calc
        }
    }

    fun submitAirtimeConversion(
        onInitiated: (AirtimeTransactionEntity) -> Unit
    ) {
        val user = currentUser.value ?: return
        val amount = convertAmountInput.value.toDoubleOrNull() ?: 0.0
        val senderPhone = convertSenderPhone.value

        isConverting.value = true
        convertErrorMessage.value = null

        viewModelScope.launch {
            val res = airtimeRepo.submitConversionRequest(
                userId = user.id,
                senderPhoneRaw = senderPhone,
                network = selectedConvertNetwork.value,
                airtimeAmount = amount
            )

            isConverting.value = false
            if (res.isSuccess) {
                val txn = res.getOrThrow()
                onInitiated(txn)
            } else {
                convertErrorMessage.value = res.exceptionOrNull()?.message
            }
        }
    }

    fun verifyAirtimeTransaction(
        reference: String,
        onVerified: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = airtimeRepo.verifyAirtimeTransaction(reference)
            if (res.isSuccess) {
                onVerified(true, null)
            } else {
                onVerified(false, res.exceptionOrNull()?.message)
            }
        }
    }

    // --- Withdraw Actions ---
    fun selectWithdrawMethod(method: WithdrawalMethod) {
        selectedWithdrawMethod.value = method
        recalculateWithdrawal()
    }

    fun setWithdrawAmount(amountStr: String) {
        withdrawAmountInput.value = amountStr
        recalculateWithdrawal()
    }

    fun recalculateWithdrawal() {
        viewModelScope.launch {
            val amount = withdrawAmountInput.value.toDoubleOrNull() ?: 0.0
            val calc = withdrawalRepo.calculateWithdrawal(selectedWithdrawMethod.value, amount)
            withdrawCalculation.value = calc
        }
    }

    fun submitWithdrawal(
        pin: String,
        onSuccess: (WithdrawalEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = currentUser.value ?: return
        val amount = withdrawAmountInput.value.toDoubleOrNull() ?: 0.0
        val destination = withdrawDestinationPhone.value
        val name = withdrawRecipientName.value

        isWithdrawing.value = true
        withdrawErrorMessage.value = null

        viewModelScope.launch {
            val res = withdrawalRepo.submitWithdrawal(
                userId = user.id,
                method = selectedWithdrawMethod.value,
                destinationRaw = destination,
                recipientName = name,
                amount = amount,
                pin = pin
            )

            isWithdrawing.value = false
            if (res.isSuccess) {
                val wdr = res.getOrThrow()
                // Process disbursement in background
                viewModelScope.launch {
                    withdrawalRepo.processWithdrawalDisbursement(wdr.reference)
                }
                onSuccess(wdr)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Withdrawal failed."
                withdrawErrorMessage.value = err
                onError(err)
            }
        }
    }

    // --- Notifications & Support ---
    fun markNotificationAsRead(id: Int) {
        viewModelScope.launch {
            notificationRepo.markAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            notificationRepo.markAllAsRead(user.id)
        }
    }

    fun submitSupportTicket(
        subject: String,
        category: String,
        transactionRef: String?,
        description: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = supportRepo.submitTicket(
                userId = user.id,
                userPhone = user.phone,
                subject = subject,
                category = category,
                transactionRef = transactionRef,
                description = description
            )
            if (res.isSuccess) {
                onResult(true, res.getOrNull()?.ticketNumber)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    // --- Admin Actions ---
    fun toggleUserStatus(userId: Int, currentStatus: String) {
        viewModelScope.launch {
            val newStatus = if (currentStatus == "ACTIVE") "SUSPENDED" else "ACTIVE"
            adminRepo.setUserStatus(userId, newStatus)
            _statusMessage.value = "User status updated to $newStatus"
        }
    }

    fun manualApproveAirtime(reference: String, notes: String) {
        viewModelScope.launch {
            airtimeRepo.verifyAirtimeTransaction(reference)
            _statusMessage.value = "Transaction $reference approved and credited"
        }
    }

    fun updateConversionRate(
        network: String,
        ratePercent: Double,
        feePercent: Double,
        min: Double,
        max: Double,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            adminRepo.updateConversionRate(network, ratePercent, feePercent, min, max, isActive)
            _statusMessage.value = "Rates for $network updated successfully"
        }
    }

    fun updateFeeConfig(
        feeType: String,
        fixedFee: Double,
        percentageFee: Double,
        min: Double,
        max: Double
    ) {
        viewModelScope.launch {
            adminRepo.updateFeeConfig(feeType, fixedFee, percentageFee, min, max)
            _statusMessage.value = "Fee configuration updated"
        }
    }

    fun replySupportTicket(ticketNumber: String, reply: String, status: String) {
        viewModelScope.launch {
            supportRepo.replyTicket(ticketNumber, reply, status)
            _statusMessage.value = "Reply sent to user for ticket $ticketNumber"
        }
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return Air2CashViewModel(
                        authRepo = container.authRepository,
                        walletRepo = container.walletRepository,
                        airtimeRepo = container.airtimeRepository,
                        withdrawalRepo = container.withdrawalRepository,
                        referralRepo = container.referralRepository,
                        notificationRepo = container.notificationRepository,
                        supportRepo = container.supportRepository,
                        adminRepo = container.adminRepository
                    ) as T
                }
            }
    }
}
