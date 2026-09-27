package com.example.data.model

enum class MobileNetwork(
    val displayName: String,
    val shortCode: String,
    val prefixHelp: String,
    val ussdInstruction: String,
    val transferNumber: String
) {
    AIRTEL(
        displayName = "Airtel Malawi",
        shortCode = "AIRTEL",
        prefixHelp = "e.g. 099xxxxxxx or 098xxxxxxx",
        ussdInstruction = "Dial *211# or use Airtel Airtime Share to transfer airtime to our collection number",
        transferNumber = "+265 991 234 567"
    ),
    TNM(
        displayName = "TNM Malawi",
        shortCode = "TNM",
        prefixHelp = "e.g. 088xxxxxxx or 089xxxxxxx",
        ussdInstruction = "Dial *444# or use TNM Me2U to transfer airtime to our collection number",
        transferNumber = "+265 881 234 567"
    );

    companion object {
        fun fromString(value: String): MobileNetwork {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AIRTEL
        }
    }
}

enum class WithdrawalMethod(
    val displayName: String,
    val providerName: String,
    val feeDescription: String
) {
    AIRTEL_MONEY(
        displayName = "Airtel Money",
        providerName = "Airtel Malawi",
        feeDescription = "1.5% network fee (Min MWK 100)"
    ),
    TNM_MPAMBA(
        displayName = "TNM Mpamba",
        providerName = "TNM Malawi",
        feeDescription = "1.5% network fee (Min MWK 100)"
    ),
    BANK_TRANSFER(
        displayName = "Malawi Commercial Bank",
        providerName = "National / Standard / FDH",
        feeDescription = "Flat MWK 500 processing fee"
    );

    companion object {
        fun fromString(value: String): WithdrawalMethod {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AIRTEL_MONEY
        }
    }
}

enum class TransactionStatus(val label: String) {
    PENDING("Pending Verification"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled"),
    REVERSED("Reversed");

    companion object {
        fun fromString(value: String): TransactionStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}

enum class LedgerEntryType {
    CREDIT,
    DEBIT
}

enum class LedgerCategory(val displayName: String) {
    AIRTIME_CONVERSION("Airtime Conversion"),
    WITHDRAWAL("Cash Withdrawal"),
    REFERRAL_BONUS("Referral Bonus"),
    FEE("Network Service Fee"),
    REFUND("Transaction Refund"),
    ADMIN_ADJUSTMENT("System Balance Adjustment")
}

enum class UserRole {
    USER,
    ADMIN
}

enum class UserStatus {
    ACTIVE,
    SUSPENDED
}

enum class TicketStatus(val label: String) {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    CLOSED("Closed")
}

enum class TicketCategory(val displayName: String) {
    CONVERSION("Airtime Conversion Issue"),
    WITHDRAWAL("Withdrawal Delay / Failure"),
    ACCOUNT("Account / Phone Number Change"),
    OTHER("General Inquiry")
}

enum class NotificationType(val titlePrefix: String) {
    TRANSACTION("Transaction Update"),
    WALLET("Wallet Notification"),
    SECURITY("Security Alert"),
    PROMOTION("Air2Cash Rewards"),
    SYSTEM("System Notice")
}
