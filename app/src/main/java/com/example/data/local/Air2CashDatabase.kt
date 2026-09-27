package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.AirtimeTransactionDao
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.ConfigDao
import com.example.data.local.dao.NotificationDao
import com.example.data.local.dao.ReferralDao
import com.example.data.local.dao.SupportTicketDao
import com.example.data.local.dao.UserDao
import com.example.data.local.dao.WalletDao
import com.example.data.local.dao.WalletLedgerDao
import com.example.data.local.dao.WithdrawalDao
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

@Database(
    entities = [
        UserEntity::class,
        WalletEntity::class,
        WalletLedgerEntity::class,
        AirtimeTransactionEntity::class,
        WithdrawalEntity::class,
        ConversionRateEntity::class,
        FeeConfigEntity::class,
        ReferralRewardEntity::class,
        NotificationEntity::class,
        SupportTicketEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class Air2CashDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun walletDao(): WalletDao
    abstract fun walletLedgerDao(): WalletLedgerDao
    abstract fun airtimeTransactionDao(): AirtimeTransactionDao
    abstract fun withdrawalDao(): WithdrawalDao
    abstract fun configDao(): ConfigDao
    abstract fun referralDao(): ReferralDao
    abstract fun notificationDao(): NotificationDao
    abstract fun supportTicketDao(): SupportTicketDao
    abstract fun auditLogDao(): AuditLogDao
}
