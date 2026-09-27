package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.ConversionRateEntity
import com.example.data.local.entity.FeeConfigEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.WalletEntity
import com.example.data.local.entity.WalletLedgerEntity
import com.example.data.security.SecurityHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseProvider {
    @Volatile
    private var instance: Air2CashDatabase? = null

    fun build(context: Context, scope: CoroutineScope): Air2CashDatabase {
        return instance ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                Air2CashDatabase::class.java,
                "air2cash_malawi.db"
            )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(sqliteDb: SupportSQLiteDatabase) {
                        super.onCreate(sqliteDb)
                        instance?.let { database ->
                            scope.launch(Dispatchers.IO) {
                                seedInitialData(database)
                            }
                        }
                    }
                })
                .build()
            instance = db
            db
        }
    }

    suspend fun seedInitialData(db: Air2CashDatabase) {
        val configDao = db.configDao()
        val userDao = db.userDao()
        val walletDao = db.walletDao()
        val ledgerDao = db.walletLedgerDao()
        val notificationDao = db.notificationDao()
        val auditLogDao = db.auditLogDao()

        // 1. Initial Conversion Rates (Airtel: 78%, TNM: 75%)
        configDao.insertOrUpdateRate(
            ConversionRateEntity(
                network = "AIRTEL",
                ratePercent = 78.0,
                feePercent = 0.0,
                minAmount = 500.0,
                maxAmount = 250000.0,
                isActive = true
            )
        )
        configDao.insertOrUpdateRate(
            ConversionRateEntity(
                network = "TNM",
                ratePercent = 75.0,
                feePercent = 0.0,
                minAmount = 500.0,
                maxAmount = 250000.0,
                isActive = true
            )
        )

        // 2. Initial Fee Configs for Withdrawals
        configDao.insertOrUpdateFeeConfig(
            FeeConfigEntity(
                feeType = "WITHDRAWAL_AIRTEL_MONEY",
                fixedFee = 0.0,
                percentageFee = 1.5,
                minAmount = 1000.0,
                maxAmount = 500000.0
            )
        )
        configDao.insertOrUpdateFeeConfig(
            FeeConfigEntity(
                feeType = "WITHDRAWAL_MPAMBA",
                fixedFee = 0.0,
                percentageFee = 1.5,
                minAmount = 1000.0,
                maxAmount = 500000.0
            )
        )
        configDao.insertOrUpdateFeeConfig(
            FeeConfigEntity(
                feeType = "WITHDRAWAL_BANK",
                fixedFee = 500.0,
                percentageFee = 0.0,
                minAmount = 5000.0,
                maxAmount = 1000000.0
            )
        )

        // 3. Seed Administrator Account
        val adminSalt = SecurityHelper.generateSalt()
        val adminPasswordHash = SecurityHelper.hashPassword("admin123", adminSalt)
        val adminUserId = userDao.insertUser(
            UserEntity(
                phone = "+265999000001",
                fullName = "Air2Cash Administrator",
                email = "admin@air2cash.mw",
                passwordHash = adminPasswordHash,
                salt = adminSalt,
                pinHash = SecurityHelper.hashPin("1234"),
                referralCode = "A2C-ADMIN",
                isPhoneVerified = true,
                role = "ADMIN",
                status = "ACTIVE"
            )
        ).toInt()

        walletDao.insertWallet(
            WalletEntity(
                userId = adminUserId,
                availableBalance = 1500000.0,
                pendingBalance = 0.0
            )
        )

        // 4. Seed Demo User (Gift Zimba, Lilongwe, Malawi)
        val demoSalt = SecurityHelper.generateSalt()
        val demoPasswordHash = SecurityHelper.hashPassword("user123", demoSalt)
        val demoUserId = userDao.insertUser(
            UserEntity(
                phone = "+265991234567",
                fullName = "Gift Zimba",
                email = "zimbagift13@gmail.com",
                passwordHash = demoPasswordHash,
                salt = demoSalt,
                pinHash = SecurityHelper.hashPin("2026"),
                referralCode = "GIFT265",
                isPhoneVerified = true,
                role = "USER",
                status = "ACTIVE"
            )
        ).toInt()

        val demoWalletId = walletDao.insertWallet(
            WalletEntity(
                userId = demoUserId,
                availableBalance = 3900.0,
                pendingBalance = 0.0
            )
        ).toInt()

        // Seed Ledger entry for demo user
        val initialLedgerRef = SecurityHelper.generateTransactionRef("LED")
        ledgerDao.insertLedgerEntry(
            WalletLedgerEntity(
                walletId = demoWalletId,
                userId = demoUserId,
                reference = initialLedgerRef,
                entryType = "CREDIT",
                category = "AIRTIME_CONVERSION",
                amount = 3900.0,
                fee = 0.0,
                balanceBefore = 0.0,
                balanceAfter = 3900.0,
                description = "Airtel MWK 5,000 Airtime converted @ 78%",
                status = "COMPLETED"
            )
        )

        // Seed Welcome Notification
        notificationDao.insertNotification(
            NotificationEntity(
                userId = demoUserId,
                title = "Welcome to Air2Cash Malawi!",
                message = "Your account is verified. You can now convert Airtel & TNM airtime directly to cash and withdraw to Airtel Money or Mpamba.",
                type = "SYSTEM"
            )
        )

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorUserId = adminUserId,
                actorRole = "SYSTEM",
                action = "SYSTEM_INITIALIZED",
                entityType = "DATABASE",
                entityId = "air2cash_malawi",
                details = "Platform initialized with Malawi rates (Airtel 78%, TNM 75%) and initial fee structures."
            )
        )
    }
}
