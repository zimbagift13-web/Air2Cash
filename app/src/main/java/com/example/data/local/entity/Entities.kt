package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["phone"], unique = true),
        Index(value = ["referralCode"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phone: String,
    val fullName: String,
    val email: String = "",
    val passwordHash: String,
    val salt: String,
    val pinHash: String = "", // 4-digit security transaction PIN
    val referralCode: String,
    val referredByCode: String? = null,
    val isPhoneVerified: Boolean = false,
    val role: String = "USER", // "USER" or "ADMIN"
    val status: String = "ACTIVE", // "ACTIVE" or "SUSPENDED"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "wallets",
    indices = [Index(value = ["userId"], unique = true)]
)
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val availableBalance: Double = 0.0,
    val pendingBalance: Double = 0.0,
    val currency: String = "MWK",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "wallet_ledger",
    indices = [
        Index(value = ["reference"], unique = true),
        Index(value = ["userId"]),
        Index(value = ["createdAt"])
    ]
)
data class WalletLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val walletId: Int,
    val userId: Int,
    val reference: String,
    val entryType: String, // "CREDIT", "DEBIT"
    val category: String, // "AIRTIME_CONVERSION", "WITHDRAWAL", "REFERRAL_BONUS", "FEE", "REFUND", "ADMIN_ADJUSTMENT"
    val amount: Double,
    val fee: Double = 0.0,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val description: String,
    val status: String = "COMPLETED", // "COMPLETED", "PENDING", "REVERSED"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "airtime_transactions",
    indices = [
        Index(value = ["reference"], unique = true),
        Index(value = ["userId"]),
        Index(value = ["verificationStatus"])
    ]
)
data class AirtimeTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reference: String, // e.g. A2C-MW-AIR-928172
    val userId: Int,
    val network: String, // "AIRTEL", "TNM"
    val senderPhone: String,
    val airtimeAmount: Double,
    val conversionRatePercent: Double,
    val grossCash: Double,
    val feeAmount: Double,
    val netCash: Double,
    val verificationStatus: String, // "PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED", "REVERSED"
    val providerReference: String? = null,
    val proofReference: String? = null,
    val adminNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long? = null
)

@Entity(
    tableName = "withdrawals",
    indices = [
        Index(value = ["reference"], unique = true),
        Index(value = ["userId"]),
        Index(value = ["status"])
    ]
)
data class WithdrawalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reference: String, // e.g. A2C-MW-WDR-510293
    val userId: Int,
    val method: String, // "AIRTEL_MONEY", "TNM_MPAMBA", "BANK_TRANSFER"
    val destinationNumber: String,
    val recipientName: String,
    val amount: Double,
    val fee: Double,
    val netPayout: Double,
    val status: String, // "PENDING", "PROCESSING", "COMPLETED", "FAILED", "REVERSED"
    val failureReason: String? = null,
    val providerTransactionId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(
    tableName = "conversion_rates",
    indices = [Index(value = ["network"], unique = true)]
)
data class ConversionRateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val network: String, // "AIRTEL", "TNM"
    val ratePercent: Double, // e.g., 78.0 for 78% payout
    val feePercent: Double = 0.0,
    val minAmount: Double = 500.0,
    val maxAmount: Double = 250000.0,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "fee_configs",
    indices = [Index(value = ["feeType"], unique = true)]
)
data class FeeConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val feeType: String, // "WITHDRAWAL_AIRTEL_MONEY", "WITHDRAWAL_MPAMBA", "WITHDRAWAL_BANK"
    val fixedFee: Double = 0.0,
    val percentageFee: Double = 1.5,
    val minAmount: Double = 1000.0,
    val maxAmount: Double = 500000.0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "referral_rewards",
    indices = [
        Index(value = ["referrerUserId"]),
        Index(value = ["refereeUserId"])
    ]
)
data class ReferralRewardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val referrerUserId: Int,
    val refereeUserId: Int,
    val refereePhone: String,
    val rewardAmount: Double = 500.0,
    val qualifyingTransactionRef: String? = null,
    val status: String = "PENDING", // "PENDING", "CREDITED"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["isRead"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val title: String,
    val message: String,
    val type: String, // "TRANSACTION", "WALLET", "SECURITY", "PROMOTION", "SYSTEM"
    val relatedReference: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "support_tickets",
    indices = [
        Index(value = ["ticketNumber"], unique = true),
        Index(value = ["userId"]),
        Index(value = ["status"])
    ]
)
data class SupportTicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticketNumber: String,
    val userId: Int,
    val userPhone: String,
    val subject: String,
    val category: String,
    val transactionRef: String? = null,
    val description: String,
    val status: String = "OPEN", // "OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"
    val adminReply: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["actorUserId"]),
        Index(value = ["timestamp"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actorUserId: Int,
    val actorRole: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
