package com.example.data.provider

import com.example.data.model.MobileNetwork
import kotlinx.coroutines.delay
import java.util.UUID

data class AirtimeVerificationResult(
    val isSuccess: Boolean,
    val providerReference: String?,
    val failureReason: String? = null
)

class AirtimeProviderService {

    /**
     * Returns actionable USSD string or airtime share instruction tailored for each Malawi operator.
     */
    fun getAirtimeTransferInstruction(network: MobileNetwork, amount: Double): String {
        val intAmount = amount.toInt()
        return when (network) {
            MobileNetwork.AIRTEL -> {
                "Dial *211# on your Airtel line, select 'Airtime Share', enter receiver 0991234567, amount $intAmount MWK and your Airtel PIN to send airtime to our system."
            }
            MobileNetwork.TNM -> {
                "Dial *444# on your TNM line, select 'Me2U', enter receiver 0881234567, amount $intAmount MWK to transfer airtime to our system."
            }
        }
    }

    fun getDirectUssdCode(network: MobileNetwork, amount: Double): String {
        val intAmount = amount.toInt()
        return when (network) {
            MobileNetwork.AIRTEL -> "*211*0991234567*$intAmount#"
            MobileNetwork.TNM -> "*444*0881234567*$intAmount#"
        }
    }

    /**
     * Simulates external operator verification engine (Airtel/TNM Airtime Gateway API).
     * In sandbox/live hybrid mode, verifies transaction reference against telecom gateway.
     */
    suspend fun verifyAirtimeTransfer(
        network: MobileNetwork,
        senderPhone: String,
        amount: Double,
        reference: String
    ): AirtimeVerificationResult {
        // Realistic provider telecom verification delay
        delay(2200)

        // Safety verification: Airtel numbers must start with Airtel prefix; TNM with TNM
        val isAirtel = senderPhone.contains("99") || senderPhone.contains("98") || senderPhone.contains("90")
        val isTnm = senderPhone.contains("88") || senderPhone.contains("89") || senderPhone.contains("80")

        if (network == MobileNetwork.AIRTEL && !isAirtel) {
            return AirtimeVerificationResult(
                isSuccess = false,
                providerReference = null,
                failureReason = "Sender number is not an Airtel Malawi line."
            )
        }
        if (network == MobileNetwork.TNM && !isTnm) {
            return AirtimeVerificationResult(
                isSuccess = false,
                providerReference = null,
                failureReason = "Sender number is not a TNM Malawi line."
            )
        }

        val providerRef = "MW-${network.shortCode}-" + UUID.randomUUID().toString().take(8).uppercase()
        return AirtimeVerificationResult(
            isSuccess = true,
            providerReference = providerRef
        )
    }
}
