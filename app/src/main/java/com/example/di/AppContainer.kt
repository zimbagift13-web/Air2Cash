package com.example.di

import android.content.Context
import com.example.data.local.Air2CashDatabase
import com.example.data.provider.AirtimeProviderService
import com.example.data.provider.MobileMoneyProviderService
import com.example.data.repository.AdminRepository
import com.example.data.repository.AirtimeRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.ReferralRepository
import com.example.data.repository.SupportRepository
import com.example.data.repository.WalletRepository
import com.example.data.repository.WithdrawalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: Air2CashDatabase by lazy {
        com.example.data.local.DatabaseProvider.build(context, applicationScope)
    }

    val airtimeProviderService: AirtimeProviderService by lazy {
        AirtimeProviderService()
    }

    val mobileMoneyProviderService: MobileMoneyProviderService by lazy {
        MobileMoneyProviderService()
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(database)
    }

    val walletRepository: WalletRepository by lazy {
        WalletRepository(database)
    }

    val referralRepository: ReferralRepository by lazy {
        ReferralRepository(database)
    }

    val airtimeRepository: AirtimeRepository by lazy {
        AirtimeRepository(database, airtimeProviderService, walletRepository, referralRepository)
    }

    val withdrawalRepository: WithdrawalRepository by lazy {
        WithdrawalRepository(database, mobileMoneyProviderService, walletRepository, authRepository)
    }

    val notificationRepository: NotificationRepository by lazy {
        NotificationRepository(database)
    }

    val supportRepository: SupportRepository by lazy {
        SupportRepository(database)
    }

    val adminRepository: AdminRepository by lazy {
        AdminRepository(database)
    }
}
