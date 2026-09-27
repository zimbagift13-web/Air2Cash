package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeUserById(id: Int): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE referralCode = :code LIMIT 1")
    suspend fun getUserByReferralCode(code: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE fullName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun searchUsers(query: String): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateUserStatus(userId: Int, status: String)

    @Query("UPDATE users SET passwordHash = :passwordHash, salt = :salt WHERE id = :userId")
    suspend fun updatePassword(userId: Int, passwordHash: String, salt: String)

    @Query("UPDATE users SET pinHash = :pinHash WHERE id = :userId")
    suspend fun updatePin(userId: Int, pinHash: String)
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets WHERE userId = :userId LIMIT 1")
    suspend fun getWalletByUserId(userId: Int): WalletEntity?

    @Query("SELECT * FROM wallets WHERE userId = :userId LIMIT 1")
    fun observeWalletByUserId(userId: Int): Flow<WalletEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity): Long

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    @Query("UPDATE wallets SET availableBalance = :available, pendingBalance = :pending, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateBalances(userId: Int, available: Double, pending: Double, updatedAt: Long)
}

@Dao
interface WalletLedgerDao {
    @Query("SELECT * FROM wallet_ledger WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeLedgerForUser(userId: Int): Flow<List<WalletLedgerEntity>>

    @Query("SELECT * FROM wallet_ledger WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentLedgerForUser(userId: Int, limit: Int): List<WalletLedgerEntity>

    @Query("SELECT * FROM wallet_ledger WHERE reference = :reference LIMIT 1")
    suspend fun getByReference(reference: String): WalletLedgerEntity?

    @Query("SELECT * FROM wallet_ledger ORDER BY createdAt DESC")
    fun observeAllLedger(): Flow<List<WalletLedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLedgerEntry(entry: WalletLedgerEntity): Long
}

@Dao
interface AirtimeTransactionDao {
    @Query("SELECT * FROM airtime_transactions WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeTransactionsForUser(userId: Int): Flow<List<AirtimeTransactionEntity>>

    @Query("SELECT * FROM airtime_transactions WHERE reference = :reference LIMIT 1")
    suspend fun getByReference(reference: String): AirtimeTransactionEntity?

    @Query("SELECT * FROM airtime_transactions WHERE reference = :reference LIMIT 1")
    fun observeByReference(reference: String): Flow<AirtimeTransactionEntity?>

    @Query("SELECT * FROM airtime_transactions WHERE verificationStatus = 'PENDING' OR verificationStatus = 'PROCESSING' ORDER BY createdAt ASC")
    suspend fun getPendingTransactions(): List<AirtimeTransactionEntity>

    @Query("SELECT * FROM airtime_transactions ORDER BY createdAt DESC")
    fun observeAllTransactions(): Flow<List<AirtimeTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: AirtimeTransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: AirtimeTransactionEntity)

    @Query("UPDATE airtime_transactions SET verificationStatus = :status, verifiedAt = :verifiedAt, adminNotes = :notes WHERE reference = :reference")
    suspend fun updateStatus(reference: String, status: String, verifiedAt: Long?, notes: String?)
}

@Dao
interface WithdrawalDao {
    @Query("SELECT * FROM withdrawals WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeWithdrawalsForUser(userId: Int): Flow<List<WithdrawalEntity>>

    @Query("SELECT * FROM withdrawals WHERE reference = :reference LIMIT 1")
    suspend fun getByReference(reference: String): WithdrawalEntity?

    @Query("SELECT * FROM withdrawals WHERE reference = :reference LIMIT 1")
    fun observeByReference(reference: String): Flow<WithdrawalEntity?>

    @Query("SELECT * FROM withdrawals WHERE status = 'PENDING' OR status = 'PROCESSING' ORDER BY createdAt ASC")
    suspend fun getPendingWithdrawals(): List<WithdrawalEntity>

    @Query("SELECT * FROM withdrawals ORDER BY createdAt DESC")
    fun observeAllWithdrawals(): Flow<List<WithdrawalEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWithdrawal(withdrawal: WithdrawalEntity): Long

    @Update
    suspend fun updateWithdrawal(withdrawal: WithdrawalEntity)

    @Query("UPDATE withdrawals SET status = :status, completedAt = :completedAt, failureReason = :failureReason WHERE reference = :reference")
    suspend fun updateStatus(reference: String, status: String, completedAt: Long?, failureReason: String?)
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM conversion_rates WHERE isActive = 1")
    fun observeActiveConversionRates(): Flow<List<ConversionRateEntity>>

    @Query("SELECT * FROM conversion_rates WHERE network = :network LIMIT 1")
    suspend fun getRateForNetwork(network: String): ConversionRateEntity?

    @Query("SELECT * FROM conversion_rates ORDER BY network ASC")
    fun observeAllConversionRates(): Flow<List<ConversionRateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRate(rate: ConversionRateEntity)

    @Query("SELECT * FROM fee_configs")
    fun observeFeeConfigs(): Flow<List<FeeConfigEntity>>

    @Query("SELECT * FROM fee_configs WHERE feeType = :feeType LIMIT 1")
    suspend fun getFeeConfig(feeType: String): FeeConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateFeeConfig(config: FeeConfigEntity)
}

@Dao
interface ReferralDao {
    @Query("SELECT * FROM referral_rewards WHERE referrerUserId = :userId ORDER BY createdAt DESC")
    fun observeRewardsForReferrer(userId: Int): Flow<List<ReferralRewardEntity>>

    @Query("SELECT COUNT(*) FROM users WHERE referredByCode = :referralCode")
    fun observeReferredCount(referralCode: String): Flow<Int>

    @Query("SELECT * FROM referral_rewards WHERE refereeUserId = :refereeUserId LIMIT 1")
    suspend fun getRewardForReferee(refereeUserId: Int): ReferralRewardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReward(reward: ReferralRewardEntity): Long

    @Update
    suspend fun updateReward(reward: ReferralRewardEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeNotificationsForUser(userId: Int): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun observeUnreadCount(userId: Int): Flow<Int>

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: Int)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long
}

@Dao
interface SupportTicketDao {
    @Query("SELECT * FROM support_tickets WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeTicketsForUser(userId: Int): Flow<List<SupportTicketEntity>>

    @Query("SELECT * FROM support_tickets ORDER BY createdAt DESC")
    fun observeAllTickets(): Flow<List<SupportTicketEntity>>

    @Query("SELECT * FROM support_tickets WHERE ticketNumber = :ticketNumber LIMIT 1")
    suspend fun getTicketByNumber(ticketNumber: String): SupportTicketEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTicket(ticket: SupportTicketEntity): Long

    @Update
    suspend fun updateTicket(ticket: SupportTicketEntity)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentAuditLogs(limit: Int): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long
}
