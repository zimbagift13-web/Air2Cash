package com.example.data.repository

import com.example.data.local.Air2CashDatabase
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
import com.example.data.provider.AirtimeProviderService
import com.example.data.provider.MobileMoneyProviderService
import com.example.data.security.SecurityHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class ConversionCalculation(
    val network: MobileNetwork,
    val airtimeAmount: Double,
    val ratePercent: Double,
    val grossCash: Double,
    val feeAmount: Double,
    val netCash: Double,
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class WithdrawalCalculation(
    val method: WithdrawalMethod,
    val amount: Double,
    val fee: Double,
    val netPayout: Double,
    val isValid: Boolean,
    val errorMessage: String? = null
)

class AuthRepository(
    private val db: Air2CashDatabase
) {
    private val userDao = db.userDao()
    private val walletDao = db.walletDao()
    private val notificationDao = db.notificationDao()
    private val auditDao = db.auditLogDao()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Temporary storage for OTP verification: Phone -> Pair(OtpCode, ExpiryMillis)
    private val pendingOtps = ConcurrentHashMap<String, Pair<String, Long>>()

    suspend fun initDefaultSession() = withContext(Dispatchers.IO) {
        // Automatically log in the demo user if not logged in
        if (_currentUser.value == null) {
            val demo = userDao.getUserByPhone("+265991234567")
            if (demo != null) {
                _currentUser.value = demo
            }
        }
    }

    suspend fun login(phoneRaw: String, passwordRaw: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val phone = SecurityHelper.normalizeMalawiPhone(phoneRaw)
        val user = userDao.getUserByPhone(phone)
            ?: return@withContext Result.failure(Exception("No account found with this phone number. Please register."))

        if (user.status == "SUSPENDED") {
            return@withContext Result.failure(Exception("This account has been suspended. Please contact customer support."))
        }

        val isValid = SecurityHelper.verifyPassword(passwordRaw, user.salt, user.passwordHash)
        if (!isValid) {
            return@withContext Result.failure(Exception("Incorrect password. Please try again or use Forgot Password."))
        }

        _currentUser.value = user
        auditDao.insertAuditLog(
            AuditLogEntity(
                actorUserId = user.id,
                actorRole = user.role,
                action = "USER_LOGIN",
                entityType = "USER",
                entityId = user.id.toString(),
                details = "User logged in successfully from $phone"
            )
        )
        Result.success(user)
    }

    suspend fun register(
        phoneRaw: String,
        fullName: String,
        email: String,
        passwordRaw: String,
        referralCodeInput: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        val phone = SecurityHelper.normalizeMalawiPhone(phoneRaw)
        if (!SecurityHelper.isValidMalawiPhone(phone)) {
            return@withContext Result.failure(Exception("Please enter a valid Malawi phone number (e.g. +265 99x xxx xxx or 088x xxx xxx)."))
        }

        if (fullName.trim().length < 3) {
            return@withContext Result.failure(Exception("Please enter your full official name."))
        }

        if (passwordRaw.length < 6) {
            return@withContext Result.failure(Exception("Password must be at least 6 characters long."))
        }

        val existing = userDao.getUserByPhone(phone)
        if (existing != null) {
            return@withContext Result.failure(Exception("An account already exists for $phone. Please log in instead."))
        }

        // Validate referral code if provided
        var referrer: UserEntity? = null
        val cleanRef = referralCodeInput?.trim()?.uppercase()
        if (!cleanRef.isNullOrEmpty()) {
            referrer = userDao.getUserByReferralCode(cleanRef)
            if (referrer == null) {
                return@withContext Result.failure(Exception("The referral code '$cleanRef' is invalid."))
            }
        }

        val salt = SecurityHelper.generateSalt()
        val passwordHash = SecurityHelper.hashPassword(passwordRaw, salt)
        val myReferralCode = SecurityHelper.generateReferralCode()

        val newUser = UserEntity(
            phone = phone,
            fullName = fullName.trim(),
            email = email.trim(),
            passwordHash = passwordHash,
            salt = salt,
            referralCode = myReferralCode,
            referredByCode = referrer?.referralCode,
            isPhoneVerified = false,
            role = "USER",
            status = "ACTIVE"
        )

        val userId = userDao.insertUser(newUser).toInt()

        // Create empty wallet
        walletDao.insertWallet(
            WalletEntity(
                userId = userId,
                availableBalance = 0.0,
                pendingBalance = 0.0
            )
        )

        // Generate OTP (Valid for 5 minutes)
        val otp = SecurityHelper.generateOtp()
        val expiry = System.currentTimeMillis() + (5 * 60 * 1000)
        pendingOtps[phone] = Pair(otp, expiry)

        notificationDao.insertNotification(
            NotificationEntity(
                userId = userId,
                title = "Verification Code",
                message = "Your Air2Cash verification code is: $otp. Valid for 5 minutes. Never share this code.",
                type = "SECURITY"
            )
        )

        auditDao.insertAuditLog(
            AuditLogEntity(
                actorUserId = userId,
                actorRole = "USER",
                action = "USER_REGISTERED",
                entityType = "USER",
                entityId = userId.toString(),
                details = "Registered with referral code: ${referrer?.referralCode ?: "NONE"}"
            )
        )

        Result.success(otp)
    }

    fun getPendingOtp(phoneRaw: String): String? {
        val phone = SecurityHelper.normalizeMalawiPhone(phoneRaw)
        val pair = pendingOtps[phone] ?: return null
        if (System.currentTimeMillis() > pair.second) {
            pendingOtps.remove(phone)
            return null
        }
        return pair.first
    }

    suspend fun verifyOtp(phoneRaw: String, inputOtp: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val phone = SecurityHelper.normalizeMalawiPhone(phoneRaw)
        val pair = pendingOtps[phone]
            ?: return@withContext Result.failure(Exception("Verification code expired or not requested. Please tap Resend."))

        if (System.currentTimeMillis() > pair.second) {
            pendingOtps.remove(phone)
            return@withContext Result.failure(Exception("Verification code has expired. Please request a new one."))
        }

        if (pair.first != inputOtp.trim()) {
            return@withContext Result.failure(Exception("Incorrect verification code. Please check and try again."))
        }

        pendingOtps.remove(phone)
        val user = userDao.getUserByPhone(phone)
            ?: return@withContext Result.failure(Exception("User not found."))

        val updated = user.copy(isPhoneVerified = true, updatedAt = System.currentTimeMillis())
        userDao.updateUser(updated)
        _currentUser.value = updated

        notificationDao.insertNotification(
            NotificationEntity(
                userId = user.id,
                title = "Phone Verified",
                message = "Your phone number $phone is successfully verified. Welcome to Air2Cash Malawi!",
                type = "SECURITY"
            )
        )

        Result.success(updated)
    }

    suspend fun resendOtp(phoneRaw: String): Result<String> = withContext(Dispatchers.IO) {
        val phone = SecurityHelper.normalizeMalawiPhone(phoneRaw)
        val user = userDao.getUserByPhone(phone)
            ?: return@withContext Result.failure(Exception("No account found for $phone."))

        val otp = SecurityHelper.generateOtp()
        val expiry = System.currentTimeMillis() + (5 * 60 * 1000)
        pendingOtps[phone] = Pair(otp, expiry)

        notificationDao.insertNotification(
            NotificationEntity(
                userId = user.id,
                title = "New Verification Code",
                message = "Your new Air2Cash verification code is: $otp. Valid for 5 minutes.",
                type = "SECURITY"
            )
        )

        Result.success(otp)
    }

    suspend fun resetPassword(phoneRaw: String, otpInput: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        val phone = SecurityHelper.normalizeMalawiPhone(phoneRaw)
        val user = userDao.getUserByPhone(phone)
            ?: return@withContext Result.failure(Exception("No account registered with this phone number."))

        val pair = pendingOtps[phone]
            ?: return@withContext Result.failure(Exception("Verification code expired or not requested."))

        if (System.currentTimeMillis() > pair.second) {
            pendingOtps.remove(phone)
            return@withContext Result.failure(Exception("Verification code has expired. Please request a new code."))
        }

        if (pair.first != otpInput.trim()) {
            return@withContext Result.failure(Exception("Incorrect verification code."))
        }

        if (newPassword.length < 6) {
            return@withContext Result.failure(Exception("Password must be at least 6 characters long."))
        }

        pendingOtps.remove(phone)
        val newSalt = SecurityHelper.generateSalt()
        val newHash = SecurityHelper.hashPassword(newPassword, newSalt)
        userDao.updatePassword(user.id, newHash, newSalt)

        notificationDao.insertNotification(
            NotificationEntity(
                userId = user.id,
                title = "Password Changed",
                message = "Your password has been successfully reset. If you did not make this change, contact support immediately.",
                type = "SECURITY"
            )
        )

        Result.success(Unit)
    }

    suspend fun updateProfile(fullName: String, email: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val current = _currentUser.value
            ?: return@withContext Result.failure(Exception("User not authenticated."))

        if (fullName.trim().length < 3) {
            return@withContext Result.failure(Exception("Name is too short."))
        }

        val updated = current.copy(
            fullName = fullName.trim(),
            email = email.trim(),
            updatedAt = System.currentTimeMillis()
        )
        userDao.updateUser(updated)
        _currentUser.value = updated
        Result.success(updated)
    }

    suspend fun setTransactionPin(pin: String): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value
            ?: return@withContext Result.failure(Exception("User not authenticated."))

        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            return@withContext Result.failure(Exception("PIN must be exactly 4 digits."))
        }

        val pinHash = SecurityHelper.hashPin(pin)
        userDao.updatePin(current.id, pinHash)
        val updated = current.copy(pinHash = pinHash)
        _currentUser.value = updated
        Result.success(Unit)
    }

    suspend fun verifyTransactionPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext false
        if (current.pinHash.isEmpty()) {
            // Default demo pin accepted if not set
            return@withContext pin == "1234" || pin == "2026"
        }
        val hashed = SecurityHelper.hashPin(pin)
        return@withContext hashed == current.pinHash
    }

    fun logout() {
        _currentUser.value = null
    }

    suspend fun switchUser(user: UserEntity) {
        _currentUser.value = user
    }
}

class WalletRepository(
    private val db: Air2CashDatabase
) {
    private val walletDao = db.walletDao()
    private val ledgerDao = db.walletLedgerDao()

    fun observeWallet(userId: Int): Flow<WalletEntity?> = walletDao.observeWalletByUserId(userId)

    fun observeLedger(userId: Int): Flow<List<WalletLedgerEntity>> = ledgerDao.observeLedgerForUser(userId)

    suspend fun getRecentLedger(userId: Int, limit: Int = 10): List<WalletLedgerEntity> = withContext(Dispatchers.IO) {
        ledgerDao.getRecentLedgerForUser(userId, limit)
    }

    suspend fun atomicCreditBalance(
        userId: Int,
        amount: Double,
        fee: Double = 0.0,
        category: String,
        reference: String,
        description: String
    ): Result<WalletLedgerEntity> = withContext(Dispatchers.IO) {
        // Enforce idempotency: check if ledger entry already exists
        val existing = ledgerDao.getByReference(reference)
        if (existing != null) {
            return@withContext Result.success(existing)
        }

        val wallet = walletDao.getWalletByUserId(userId)
            ?: return@withContext Result.failure(Exception("Wallet not found for user ID: $userId"))

        val balanceBefore = wallet.availableBalance
        val balanceAfter = balanceBefore + amount

        val ledgerEntry = WalletLedgerEntity(
            walletId = wallet.id,
            userId = userId,
            reference = reference,
            entryType = "CREDIT",
            category = category,
            amount = amount,
            fee = fee,
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter,
            description = description,
            status = "COMPLETED"
        )

        ledgerDao.insertLedgerEntry(ledgerEntry)
        walletDao.updateBalances(userId, available = balanceAfter, pending = wallet.pendingBalance, updatedAt = System.currentTimeMillis())

        Result.success(ledgerEntry)
    }

    suspend fun atomicDebitBalance(
        userId: Int,
        amount: Double,
        fee: Double,
        category: String,
        reference: String,
        description: String
    ): Result<WalletLedgerEntity> = withContext(Dispatchers.IO) {
        val totalDeduction = amount + fee
        val wallet = walletDao.getWalletByUserId(userId)
            ?: return@withContext Result.failure(Exception("Wallet not found."))

        if (wallet.availableBalance < totalDeduction) {
            return@withContext Result.failure(
                Exception("Insufficient funds. Required MWK ${"%.2f".format(totalDeduction)}, available MWK ${"%.2f".format(wallet.availableBalance)}")
            )
        }

        val balanceBefore = wallet.availableBalance
        val balanceAfter = balanceBefore - totalDeduction

        val ledgerEntry = WalletLedgerEntity(
            walletId = wallet.id,
            userId = userId,
            reference = reference,
            entryType = "DEBIT",
            category = category,
            amount = amount,
            fee = fee,
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter,
            description = description,
            status = "COMPLETED"
        )

        ledgerDao.insertLedgerEntry(ledgerEntry)
        walletDao.updateBalances(userId, available = balanceAfter, pending = wallet.pendingBalance, updatedAt = System.currentTimeMillis())

        Result.success(ledgerEntry)
    }
}

class AirtimeRepository(
    private val db: Air2CashDatabase,
    private val providerService: AirtimeProviderService,
    private val walletRepository: WalletRepository,
    private val referralRepository: ReferralRepository
) {
    private val airtimeDao = db.airtimeTransactionDao()
    private val configDao = db.configDao()
    private val notificationDao = db.notificationDao()
    private val auditDao = db.auditLogDao()

    fun observeRates(): Flow<List<ConversionRateEntity>> = configDao.observeActiveConversionRates()

    fun observeUserTransactions(userId: Int): Flow<List<AirtimeTransactionEntity>> =
        airtimeDao.observeTransactionsForUser(userId)

    fun observeTransaction(reference: String): Flow<AirtimeTransactionEntity?> =
        airtimeDao.observeByReference(reference)

    suspend fun calculateConversion(network: MobileNetwork, airtimeAmount: Double): ConversionCalculation = withContext(Dispatchers.IO) {
        val rateEntity = configDao.getRateForNetwork(network.name)
            ?: ConversionRateEntity(network = network.name, ratePercent = if (network == MobileNetwork.AIRTEL) 78.0 else 75.0)

        if (airtimeAmount < rateEntity.minAmount) {
            return@withContext ConversionCalculation(
                network = network,
                airtimeAmount = airtimeAmount,
                ratePercent = rateEntity.ratePercent,
                grossCash = 0.0,
                feeAmount = 0.0,
                netCash = 0.0,
                isValid = false,
                errorMessage = "Minimum conversion for ${network.displayName} is MWK ${"%.0f".format(rateEntity.minAmount)}"
            )
        }

        if (airtimeAmount > rateEntity.maxAmount) {
            return@withContext ConversionCalculation(
                network = network,
                airtimeAmount = airtimeAmount,
                ratePercent = rateEntity.ratePercent,
                grossCash = 0.0,
                feeAmount = 0.0,
                netCash = 0.0,
                isValid = false,
                errorMessage = "Maximum conversion for ${network.displayName} is MWK ${"%.0f".format(rateEntity.maxAmount)}"
            )
        }

        val gross = airtimeAmount * (rateEntity.ratePercent / 100.0)
        val fee = if (rateEntity.feePercent > 0) gross * (rateEntity.feePercent / 100.0) else 0.0
        val net = gross - fee

        ConversionCalculation(
            network = network,
            airtimeAmount = airtimeAmount,
            ratePercent = rateEntity.ratePercent,
            grossCash = gross,
            feeAmount = fee,
            netCash = net,
            isValid = true
        )
    }

    suspend fun submitConversionRequest(
        userId: Int,
        senderPhoneRaw: String,
        network: MobileNetwork,
        airtimeAmount: Double
    ): Result<AirtimeTransactionEntity> = withContext(Dispatchers.IO) {
        val senderPhone = SecurityHelper.normalizeMalawiPhone(senderPhoneRaw)
        if (!SecurityHelper.isValidMalawiPhone(senderPhone)) {
            return@withContext Result.failure(Exception("Please enter a valid Malawi phone number."))
        }

        val calc = calculateConversion(network, airtimeAmount)
        if (!calc.isValid) {
            return@withContext Result.failure(Exception(calc.errorMessage ?: "Invalid conversion parameters."))
        }

        val reference = SecurityHelper.generateTransactionRef("AIR")
        val entity = AirtimeTransactionEntity(
            reference = reference,
            userId = userId,
            network = network.name,
            senderPhone = senderPhone,
            airtimeAmount = airtimeAmount,
            conversionRatePercent = calc.ratePercent,
            grossCash = calc.grossCash,
            feeAmount = calc.feeAmount,
            netCash = calc.netCash,
            verificationStatus = "PENDING"
        )

        airtimeDao.insertTransaction(entity)

        notificationDao.insertNotification(
            NotificationEntity(
                userId = userId,
                title = "Airtime Conversion Initiated",
                message = "Order $reference submitted for MWK ${"%.0f".format(airtimeAmount)} (${network.displayName}). Please complete the airtime transfer.",
                type = "TRANSACTION",
                relatedReference = reference
            )
        )

        auditDao.insertAuditLog(
            AuditLogEntity(
                actorUserId = userId,
                actorRole = "USER",
                action = "AIRTIME_CONVERSION_INITIATED",
                entityType = "AIRTIME_TRANSACTION",
                entityId = reference,
                details = "User initiated conversion of MWK $airtimeAmount via ${network.name}"
            )
        )

        Result.success(entity)
    }

    suspend fun verifyAirtimeTransaction(reference: String): Result<AirtimeTransactionEntity> = withContext(Dispatchers.IO) {
        val txn = airtimeDao.getByReference(reference)
            ?: return@withContext Result.failure(Exception("Transaction $reference not found."))

        if (txn.verificationStatus == "COMPLETED") {
            return@withContext Result.success(txn)
        }

        // Mark as PROCESSING
        airtimeDao.updateStatus(reference, "PROCESSING", null, null)

        val network = MobileNetwork.fromString(txn.network)
        val verifyResult = providerService.verifyAirtimeTransfer(
            network = network,
            senderPhone = txn.senderPhone,
            amount = txn.airtimeAmount,
            reference = reference
        )

        if (!verifyResult.isSuccess) {
            airtimeDao.updateStatus(reference, "FAILED", null, verifyResult.failureReason)
            notificationDao.insertNotification(
                NotificationEntity(
                    userId = txn.userId,
                    title = "Conversion Failed",
                    message = "Could not verify airtime transfer for $reference: ${verifyResult.failureReason}",
                    type = "TRANSACTION",
                    relatedReference = reference
                )
            )
            return@withContext Result.failure(Exception(verifyResult.failureReason ?: "Verification failed."))
        }

        // Verified successfully! Credit user wallet atomically
        val creditResult = walletRepository.atomicCreditBalance(
            userId = txn.userId,
            amount = txn.netCash,
            category = "AIRTIME_CONVERSION",
            reference = reference,
            description = "${network.displayName} MWK ${"%.0f".format(txn.airtimeAmount)} Airtime converted @ ${"%.0f".format(txn.conversionRatePercent)}%"
        )

        if (creditResult.isFailure) {
            airtimeDao.updateStatus(reference, "FAILED", null, "Wallet credit error: ${creditResult.exceptionOrNull()?.message}")
            return@withContext Result.failure(creditResult.exceptionOrNull() ?: Exception("Failed to credit wallet."))
        }

        val completedTxn = txn.copy(
            verificationStatus = "COMPLETED",
            providerReference = verifyResult.providerReference,
            verifiedAt = System.currentTimeMillis()
        )
        airtimeDao.updateTransaction(completedTxn)

        notificationDao.insertNotification(
            NotificationEntity(
                userId = txn.userId,
                title = "Airtime Converted Successfully!",
                message = "MWK ${"%.2f".format(txn.netCash)} has been credited to your Air2Cash wallet. Ref: $reference.",
                type = "WALLET",
                relatedReference = reference
            )
        )

        auditDao.insertAuditLog(
            AuditLogEntity(
                actorUserId = txn.userId,
                actorRole = "SYSTEM",
                action = "AIRTIME_CONVERSION_COMPLETED",
                entityType = "AIRTIME_TRANSACTION",
                entityId = reference,
                details = "Credited MWK ${txn.netCash} to wallet. Provider ref: ${verifyResult.providerReference}"
            )
        )

        // Check if referee qualifies for referral bonus
        referralRepository.checkAndAwardReferral(txn.userId, reference)

        Result.success(completedTxn)
    }
}

class WithdrawalRepository(
    private val db: Air2CashDatabase,
    private val providerService: MobileMoneyProviderService,
    private val walletRepository: WalletRepository,
    private val authRepository: AuthRepository
) {
    private val withdrawalDao = db.withdrawalDao()
    private val configDao = db.configDao()
    private val notificationDao = db.notificationDao()
    private val auditDao = db.auditLogDao()

    fun observeWithdrawals(userId: Int): Flow<List<com.example.data.local.entity.WithdrawalEntity>> =
        withdrawalDao.observeWithdrawalsForUser(userId)

    fun observeWithdrawal(reference: String): Flow<com.example.data.local.entity.WithdrawalEntity?> =
        withdrawalDao.observeByReference(reference)

    suspend fun calculateWithdrawal(method: WithdrawalMethod, amount: Double): WithdrawalCalculation = withContext(Dispatchers.IO) {
        val feeType = when (method) {
            WithdrawalMethod.AIRTEL_MONEY -> "WITHDRAWAL_AIRTEL_MONEY"
            WithdrawalMethod.TNM_MPAMBA -> "WITHDRAWAL_MPAMBA"
            WithdrawalMethod.BANK_TRANSFER -> "WITHDRAWAL_BANK"
        }

        val config = configDao.getFeeConfig(feeType)
            ?: FeeConfigEntity(feeType = feeType, percentageFee = 1.5, minAmount = 1000.0, maxAmount = 500000.0)

        if (amount < config.minAmount) {
            return@withContext WithdrawalCalculation(
                method = method,
                amount = amount,
                fee = 0.0,
                netPayout = 0.0,
                isValid = false,
                errorMessage = "Minimum withdrawal for ${method.displayName} is MWK ${"%.0f".format(config.minAmount)}"
            )
        }

        if (amount > config.maxAmount) {
            return@withContext WithdrawalCalculation(
                method = method,
                amount = amount,
                fee = 0.0,
                netPayout = 0.0,
                isValid = false,
                errorMessage = "Maximum withdrawal for ${method.displayName} is MWK ${"%.0f".format(config.maxAmount)}"
            )
        }

        val fee = config.fixedFee + (amount * (config.percentageFee / 100.0))
        val netPayout = amount - fee

        WithdrawalCalculation(
            method = method,
            amount = amount,
            fee = fee,
            netPayout = netPayout,
            isValid = true
        )
    }

    suspend fun submitWithdrawal(
        userId: Int,
        method: WithdrawalMethod,
        destinationRaw: String,
        recipientName: String,
        amount: Double,
        pin: String
    ): Result<com.example.data.local.entity.WithdrawalEntity> = withContext(Dispatchers.IO) {
        // 1. PIN verification
        val isPinValid = authRepository.verifyTransactionPin(pin)
        if (!isPinValid) {
            return@withContext Result.failure(Exception("Invalid transaction security PIN. Please try again."))
        }

        val destination = SecurityHelper.normalizeMalawiPhone(destinationRaw)
        if (method != WithdrawalMethod.BANK_TRANSFER && !SecurityHelper.isValidMalawiPhone(destination)) {
            return@withContext Result.failure(Exception("Please enter a valid Malawi mobile money number."))
        }

        val calc = calculateWithdrawal(method, amount)
        if (!calc.isValid) {
            return@withContext Result.failure(Exception(calc.errorMessage ?: "Invalid withdrawal parameters."))
        }

        val reference = SecurityHelper.generateTransactionRef("WDR")

        // 2. Deduct from available balance first (atomic debit)
        val debitResult = walletRepository.atomicDebitBalance(
            userId = userId,
            amount = calc.netPayout,
            fee = calc.fee,
            category = "WITHDRAWAL",
            reference = reference,
            description = "Withdrawal to ${method.displayName} ($destination)"
        )

        if (debitResult.isFailure) {
            return@withContext Result.failure(debitResult.exceptionOrNull() ?: Exception("Insufficient balance."))
        }

        val withdrawalEntity = com.example.data.local.entity.WithdrawalEntity(
            reference = reference,
            userId = userId,
            method = method.name,
            destinationNumber = destination,
            recipientName = recipientName.ifEmpty { "Registered Account" },
            amount = amount,
            fee = calc.fee,
            netPayout = calc.netPayout,
            status = "PENDING"
        )

        withdrawalDao.insertWithdrawal(withdrawalEntity)

        notificationDao.insertNotification(
            NotificationEntity(
                userId = userId,
                title = "Withdrawal Submitted",
                message = "Withdrawal of MWK ${"%.2f".format(amount)} to ${method.displayName} ($destination) is submitted and processing. Ref: $reference",
                type = "WALLET",
                relatedReference = reference
            )
        )

        auditDao.insertAuditLog(
            AuditLogEntity(
                actorUserId = userId,
                actorRole = "USER",
                action = "WITHDRAWAL_SUBMITTED",
                entityType = "WITHDRAWAL",
                entityId = reference,
                details = "User requested MWK $amount to ${method.name} ($destination)"
            )
        )

        Result.success(withdrawalEntity)
    }

    suspend fun processWithdrawalDisbursement(reference: String): Result<com.example.data.local.entity.WithdrawalEntity> = withContext(Dispatchers.IO) {
        val wdr = withdrawalDao.getByReference(reference)
            ?: return@withContext Result.failure(Exception("Withdrawal record not found."))

        if (wdr.status == "COMPLETED") {
            return@withContext Result.success(wdr)
        }

        withdrawalDao.updateStatus(reference, "PROCESSING", null, null)

        val method = WithdrawalMethod.fromString(wdr.method)
        val disbursementResult = providerService.processDisbursement(
            method = method,
            destinationNumber = wdr.destinationNumber,
            amount = wdr.netPayout,
            reference = reference
        )

        if (!disbursementResult.isSuccess) {
            // Reverse funds back to user wallet
            walletRepository.atomicCreditBalance(
                userId = wdr.userId,
                amount = wdr.netPayout + wdr.fee,
                fee = 0.0,
                category = "REFUND",
                reference = SecurityHelper.generateTransactionRef("REV"),
                description = "Reversal for failed withdrawal $reference"
            )

            withdrawalDao.updateStatus(reference, "FAILED", null, disbursementResult.failureReason)

            notificationDao.insertNotification(
                NotificationEntity(
                    userId = wdr.userId,
                    title = "Withdrawal Failed - Refunded",
                    message = "Withdrawal $reference could not be completed: ${disbursementResult.failureReason}. MWK ${"%.2f".format(wdr.amount)} refunded to your wallet.",
                    type = "WALLET",
                    relatedReference = reference
                )
            )

            return@withContext Result.failure(Exception(disbursementResult.failureReason ?: "Disbursement failed."))
        }

        val updated = wdr.copy(
            status = "COMPLETED",
            providerTransactionId = disbursementResult.providerTransactionId,
            completedAt = System.currentTimeMillis()
        )
        withdrawalDao.updateWithdrawal(updated)

        notificationDao.insertNotification(
            NotificationEntity(
                userId = wdr.userId,
                title = "Withdrawal Successful!",
                message = "MWK ${"%.2f".format(wdr.netPayout)} sent to ${method.displayName} (${wdr.destinationNumber}). Provider Txn ID: ${disbursementResult.providerTransactionId}",
                type = "WALLET",
                relatedReference = reference
            )
        )

        auditDao.insertAuditLog(
            AuditLogEntity(
                actorUserId = wdr.userId,
                actorRole = "SYSTEM",
                action = "WITHDRAWAL_COMPLETED",
                entityType = "WITHDRAWAL",
                entityId = reference,
                details = "Completed payout of MWK ${wdr.netPayout}. Provider Txn: ${disbursementResult.providerTransactionId}"
            )
        )

        Result.success(updated)
    }
}

class ReferralRepository(
    private val db: Air2CashDatabase
) {
    private val userDao = db.userDao()
    private val referralDao = db.referralDao()
    private val walletDao = db.walletDao()
    private val ledgerDao = db.walletLedgerDao()
    private val notificationDao = db.notificationDao()

    fun observeRewards(userId: Int): Flow<List<ReferralRewardEntity>> = referralDao.observeRewardsForReferrer(userId)

    fun observeReferredCount(referralCode: String): Flow<Int> = referralDao.observeReferredCount(referralCode)

    suspend fun checkAndAwardReferral(refereeUserId: Int, qualifyingTxnRef: String) = withContext(Dispatchers.IO) {
        val referee = userDao.getUserById(refereeUserId) ?: return@withContext
        val refCode = referee.referredByCode ?: return@withContext

        val referrer = userDao.getUserByReferralCode(refCode) ?: return@withContext
        if (referrer.id == referee.id) return@withContext

        // Check if reward was already given
        val existing = referralDao.getRewardForReferee(referee.id)
        if (existing != null && existing.status == "CREDITED") return@withContext

        val rewardAmount = 500.0 // MWK 500 referral bonus per qualifying user conversion
        val rewardEntity = ReferralRewardEntity(
            referrerUserId = referrer.id,
            refereeUserId = referee.id,
            refereePhone = referee.phone,
            rewardAmount = rewardAmount,
            qualifyingTransactionRef = qualifyingTxnRef,
            status = "CREDITED"
        )
        referralDao.insertReward(rewardEntity)

        // Credit referrer wallet
        val referrerWallet = walletDao.getWalletByUserId(referrer.id) ?: return@withContext
        val balanceBefore = referrerWallet.availableBalance
        val balanceAfter = balanceBefore + rewardAmount
        val ref = SecurityHelper.generateTransactionRef("REF")

        ledgerDao.insertLedgerEntry(
            WalletLedgerEntity(
                walletId = referrerWallet.id,
                userId = referrer.id,
                reference = ref,
                entryType = "CREDIT",
                category = "REFERRAL_BONUS",
                amount = rewardAmount,
                fee = 0.0,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                description = "Referral reward: ${referee.fullName} completed first airtime conversion",
                status = "COMPLETED"
            )
        )
        walletDao.updateBalances(referrer.id, available = balanceAfter, pending = referrerWallet.pendingBalance, updatedAt = System.currentTimeMillis())

        notificationDao.insertNotification(
            NotificationEntity(
                userId = referrer.id,
                title = "Referral Reward Earned! 🎉",
                message = "You earned MWK 500.00 because ${referee.fullName} completed their first airtime conversion!",
                type = "PROMOTION",
                relatedReference = ref
            )
        )
    }
}

class NotificationRepository(
    private val db: Air2CashDatabase
) {
    private val notificationDao = db.notificationDao()

    fun observeNotifications(userId: Int): Flow<List<NotificationEntity>> =
        notificationDao.observeNotificationsForUser(userId)

    fun observeUnreadCount(userId: Int): Flow<Int> =
        notificationDao.observeUnreadCount(userId)

    suspend fun markAsRead(id: Int) = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllAsRead(userId: Int) = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead(userId)
    }

    suspend fun sendNotification(userId: Int, title: String, message: String, type: String, ref: String? = null) = withContext(Dispatchers.IO) {
        notificationDao.insertNotification(
            NotificationEntity(
                userId = userId,
                title = title,
                message = message,
                type = type,
                relatedReference = ref
            )
        )
    }
}

class SupportRepository(
    private val db: Air2CashDatabase
) {
    private val supportDao = db.supportTicketDao()
    private val notificationDao = db.notificationDao()

    fun observeUserTickets(userId: Int): Flow<List<SupportTicketEntity>> =
        supportDao.observeTicketsForUser(userId)

    fun observeAllTickets(): Flow<List<SupportTicketEntity>> =
        supportDao.observeAllTickets()

    suspend fun submitTicket(
        userId: Int,
        userPhone: String,
        subject: String,
        category: String,
        transactionRef: String?,
        description: String
    ): Result<SupportTicketEntity> = withContext(Dispatchers.IO) {
        if (subject.trim().length < 4) {
            return@withContext Result.failure(Exception("Please enter a subject."))
        }
        if (description.trim().length < 10) {
            return@withContext Result.failure(Exception("Please describe your problem with at least 10 characters."))
        }

        val ticketNum = "TCK-MW-" + (100000 + (System.currentTimeMillis() % 900000))
        val ticket = SupportTicketEntity(
            ticketNumber = ticketNum,
            userId = userId,
            userPhone = userPhone,
            subject = subject.trim(),
            category = category,
            transactionRef = transactionRef?.trim()?.ifEmpty { null },
            description = description.trim(),
            status = "OPEN"
        )
        supportDao.insertTicket(ticket)

        notificationDao.insertNotification(
            NotificationEntity(
                userId = userId,
                title = "Support Ticket Created",
                message = "Your support ticket $ticketNum has been submitted. Our Malawi support desk will respond shortly.",
                type = "SYSTEM"
            )
        )

        Result.success(ticket)
    }

    suspend fun replyTicket(ticketNumber: String, reply: String, status: String) = withContext(Dispatchers.IO) {
        val ticket = supportDao.getTicketByNumber(ticketNumber) ?: return@withContext
        val updated = ticket.copy(
            adminReply = reply,
            status = status,
            updatedAt = System.currentTimeMillis()
        )
        supportDao.updateTicket(updated)

        notificationDao.insertNotification(
            NotificationEntity(
                userId = ticket.userId,
                title = "Support Ticket Update: $ticketNumber",
                message = "Support response: $reply",
                type = "SYSTEM"
            )
        )
    }
}

class AdminRepository(
    private val db: Air2CashDatabase
) {
    private val userDao = db.userDao()
    private val airtimeDao = db.airtimeTransactionDao()
    private val withdrawalDao = db.withdrawalDao()
    private val configDao = db.configDao()
    private val auditDao = db.auditLogDao()

    fun observeAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsersFlow()

    fun observeAllAirtimeTransactions(): Flow<List<AirtimeTransactionEntity>> = airtimeDao.observeAllTransactions()

    fun observeAllWithdrawals(): Flow<List<com.example.data.local.entity.WithdrawalEntity>> = withdrawalDao.observeAllWithdrawals()

    fun observeAllRates(): Flow<List<ConversionRateEntity>> = configDao.observeAllConversionRates()

    fun observeAllFeeConfigs(): Flow<List<FeeConfigEntity>> = configDao.observeFeeConfigs()

    fun observeAuditLogs(): Flow<List<AuditLogEntity>> = auditDao.observeRecentAuditLogs(100)

    suspend fun setUserStatus(userId: Int, status: String) = withContext(Dispatchers.IO) {
        userDao.updateUserStatus(userId, status)
        auditDao.insertAuditLog(
            AuditLogEntity(
                actorUserId = 1,
                actorRole = "ADMIN",
                action = "USER_STATUS_CHANGE",
                entityType = "USER",
                entityId = userId.toString(),
                details = "User status changed to $status"
            )
        )
    }

    suspend fun updateConversionRate(
        network: String,
        ratePercent: Double,
        feePercent: Double,
        min: Double,
        max: Double,
        isActive: Boolean
    ) = withContext(Dispatchers.IO) {
        val entity = ConversionRateEntity(
            network = network,
            ratePercent = ratePercent,
            feePercent = feePercent,
            minAmount = min,
            maxAmount = max,
            isActive = isActive,
            updatedAt = System.currentTimeMillis()
        )
        configDao.insertOrUpdateRate(entity)
        auditDao.insertAuditLog(
            AuditLogEntity(
                actorUserId = 1,
                actorRole = "ADMIN",
                action = "UPDATE_RATE",
                entityType = "CONVERSION_RATE",
                entityId = network,
                details = "Updated rate for $network to $ratePercent% (Active: $isActive)"
            )
        )
    }

    suspend fun updateFeeConfig(
        feeType: String,
        fixedFee: Double,
        percentageFee: Double,
        min: Double,
        max: Double
    ) = withContext(Dispatchers.IO) {
        val entity = FeeConfigEntity(
            feeType = feeType,
            fixedFee = fixedFee,
            percentageFee = percentageFee,
            minAmount = min,
            maxAmount = max,
            updatedAt = System.currentTimeMillis()
        )
        configDao.insertOrUpdateFeeConfig(entity)
    }
}
