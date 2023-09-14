package com.payment.pt_sdk_app

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.paytabssamplewithsamsungpay.databinding.ActivityMainBinding
import com.payment.paymentsdk.PaymentSdkActivity
import com.payment.paymentsdk.PaymentSdkConfigBuilder
import com.payment.paymentsdk.QuerySdkActivity
import com.payment.paymentsdk.integrationmodels.*
import com.payment.paymentsdk.save_cards.entities.PaymentSDKSavedCardInfo
import com.payment.paymentsdk.sharedclasses.interfaces.CallbackPaymentInterface
import com.payment.paymentsdk.sharedclasses.interfaces.CallbackQueryInterface
import com.payment.paymentsdk.sharedclasses.model.response.TransactionResponseBody
import com.samsung.android.sdk.samsungpay.v2.PartnerInfo
import com.samsung.android.sdk.samsungpay.v2.SamsungPay
import com.samsung.android.sdk.samsungpay.v2.SpaySdk
import com.samsung.android.sdk.samsungpay.v2.StatusListener
import com.samsung.android.sdk.samsungpay.v2.payment.CardInfo
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo
import com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountBoxControl
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountConstants
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet


const val TAG_PAYMENT_SDK = "payment_sdk_Logs"

    @SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity(), CallbackPaymentInterface {

    companion object {
        private const val MID = "100695"
        private const val SERVER_KEY = "SRJNLKHHKN-JDT6ZKJKWN-HHWBJMBB2K"
        private const val CLIENT_KEY = "CTKMDV-V6Q76D-7G9K6K-9PGBTR"
        private const val AMOUNT = "20"

        //
        private const val CUSTOMER_EMAIL = "shiping_email@test.com"
        private const val CUSTOMER_STREET = "Shipping Address Street"
        private const val CUSTOMER_CITY = "Dubai"
        private const val CUSTOMER_STATE = "3510"
        private const val CUSTOMER_COUNTRY = "AE"
        private const val CUSTOMER_POSTAL_CODE = "1"

        //
        private const val TRANSACTION_TITLE = "Test SDK"
        private const val ORDER_ID = "123456"

        //
        private const val TOKEN = "2C4652BC67A3E531C6B490FE6C827EBB"
        private const val TRANSACTION_REF = "TST2310101555557"
        private const val MASKED_CARD_NUMBER = "4111 11## #### 1111"
        private const val CARD_SCHEME = "visa"
    }

    private var lastPaymentSdkTransactionDetails: PaymentSdkTransactionDetails? = null

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding!!.root)
        with(binding) {
            //
            mid.setText(MID)
            serverKey.setText(SERVER_KEY)
            clienttKey.setText(CLIENT_KEY)
            amount.setText(AMOUNT)
            //
            customerEmail.setText(CUSTOMER_EMAIL)
            customerStreet.setText(CUSTOMER_STREET)
            customerCity.setText(CUSTOMER_CITY)
            customerState.setText(CUSTOMER_STATE)
            customerCountry.setText(CUSTOMER_COUNTRY)
            customerPostalCode.setText(CUSTOMER_POSTAL_CODE)
            //
            cardToken.setText(TOKEN)
            transactionReference.setText(TRANSACTION_REF)
            //
            savedCardInfoMaskedNumber.setText(MASKED_CARD_NUMBER)
            savedCardInfoCardType.setText(CARD_SCHEME)
            //
            transactionClass.setSelection(1)
            tokenisation.setSelection(1)
            language.setSelection(1)
            regions.setSelection(2)
            currency.setSelection(6)
            //
            savedCardInfoLl.visibility = View.GONE
            tokenisationDataLl.visibility = View.GONE
            setSavedCardInfo.setOnCheckedChangeListener { _, isChecked ->
                savedCardInfoLl.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
            setTokenisationData.setOnCheckedChangeListener { _, isChecked ->
                tokenisationDataLl.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
            showLastTransactionInfo.setOnClickListener { showLastTransactionInfo() }
            pay.setOnClickListener { startCardPayment() }
            payRecurring.setOnClickListener { startRecurringCardPayment() }
            start3dsRecurring.setOnClickListener { start3DSRecurringCardPayment() }
            checkSamPay.setOnClickListener { handleSam() }
            payWithSavedCard.setOnClickListener { startCardPaymentWithSavedCards() }
            payAlt.setOnClickListener { startAlternativePaymentMethods() }
            query.setOnClickListener { queryTrx() }
            binding.samPay.setOnClickListener { startInAppPayWithCustomSheet() }
        }
    }

    private lateinit var partnerInfo: PartnerInfo

    private fun handleSam() {
        if (binding.samServiceId.text?.isBlank() == true) {
            Toast.makeText(this, "Please enter Samsung service id", Toast.LENGTH_SHORT).show()
            return
        }
        val bundle = Bundle()
        bundle.putString(SpaySdk.PARTNER_SERVICE_TYPE, SpaySdk.ServiceType.INAPP_PAYMENT.toString())
        partnerInfo = PartnerInfo(binding.samServiceId.text.toString(), bundle)
        updateSamsungPayButton()
    }

    private fun doActivateSamsungPay(serviceType: String) {
        if (binding.samServiceId.text?.isBlank() == true) {
            Toast.makeText(this, "Please enter Samsung service id", Toast.LENGTH_SHORT).show()
            return
        }
        val bundle = Bundle()
        bundle.putString(SamsungPay.PARTNER_SERVICE_TYPE, serviceType)
        val partnerInfo = PartnerInfo(binding.samServiceId.text.toString(), bundle)
        val samsungPay = SamsungPay(this, partnerInfo)
        samsungPay.activateSamsungPay()
    }

    private fun updateSamsungPayButton() {
        val samsungPay = SamsungPay(this, partnerInfo)
        samsungPay.getSamsungPayStatus(object : StatusListener {
            override fun onSuccess(status: Int, bundle: Bundle) {
                when (status) {
                    SpaySdk.SPAY_READY -> {
                        binding.samPay.visibility = View.VISIBLE
                        binding.samPay.isEnabled = true
                        Toast.makeText(this@MainActivity, "Samsung Pay supported", Toast.LENGTH_SHORT).show()
                        // Perform your operation.
                    }

                    SpaySdk.SPAY_NOT_READY -> {
                        // Samsung Pay is supported but not fully ready.

                        // If EXTRA_ERROR_REASON is ERROR_SPAY_APP_NEED_TO_UPDATE,
                        // Call goToUpdatePage().

                        // If EXTRA_ERROR_REASON is ERROR_SPAY_SETUP_NOT_COMPLETED,
                        // Call activateSamsungPay().
                        // If EXTRA_ERROR_REASON is ERROR_SPAY_SETUP_NOT_COMPLETED,
                        // Call activateSamsungPay().
                        val extraError = bundle.getInt(SamsungPay.EXTRA_ERROR_REASON)
                        if (extraError == SamsungPay.ERROR_SPAY_SETUP_NOT_COMPLETED) {
                            doActivateSamsungPay(SpaySdk.ServiceType.INAPP_PAYMENT.toString())
                        }

                        binding.samPay.isEnabled = false
                        Toast.makeText(this@MainActivity, "Samsung Pay not supported", Toast.LENGTH_SHORT).show()
                    }

                    SpaySdk.SPAY_NOT_ALLOWED_TEMPORALLY -> {
                        // If EXTRA_ERROR_REASON is ERROR_SPAY_CONNECTED_WITH_EXTERNAL_DISPLAY,
                        // guide user to disconnect it.
                        binding.samPay.isEnabled = false
                        Toast.makeText(this@MainActivity, "Samsung Pay not supported", Toast.LENGTH_SHORT).show()
                    }

                    SpaySdk.SPAY_NOT_SUPPORTED -> {
                        binding.samPay.isEnabled = false
                        Toast.makeText(this@MainActivity, "Samsung Pay not supported", Toast.LENGTH_SHORT).show()
                    }

                    else -> binding.samPay.visibility = View.INVISIBLE
                }
            }

            override fun onFail(errorCode: Int, bundle: Bundle) {
                binding.samPay.visibility = View.INVISIBLE
                Toast.makeText(applicationContext, "getSamsungPayStatus fail", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun makeTransactionDetailsWithSheet(): CustomSheetPaymentInfo? {
        val brandList = brandList

        val extraPaymentInfo = Bundle()
        val customSheet = CustomSheet()

        customSheet.addControl(makeAmountControl())
        return CustomSheetPaymentInfo.Builder().setMerchantId("123456").setMerchantName("Sample Merchant")
            .setOrderNumber("AMZ007MAR")
            // If you want to enter address, please refer to the javaDoc :
            // reference/com/samsung/android/sdk/samsungpay/v2/payment/sheet/AddressControl.html
            .setAddressInPaymentSheet(CustomSheetPaymentInfo.AddressInPaymentSheet.DO_NOT_SHOW).setAllowedCardBrands(brandList)
            .setCardHolderNameEnabled(true).setRecurringEnabled(false).setCustomSheet(customSheet)
            .setExtraPaymentInfo(extraPaymentInfo).build()
    }

    private fun makeAmountControl(): AmountBoxControl {
        val amountBoxControl = AmountBoxControl(binding.amount.text?.toString(), binding.currency.selectedItem.toString())
        //amountBoxControl.addItem(binding.productItemId.text.toString(), "Item", 990.99, "")
        //amountBoxControl.addItem(binding.productTaxId.text.toString(), "Tax", 5.0, "")
        amountBoxControl.setAmountTotal(
            binding.amount.text?.toString()?.toDouble() ?: 0.0, AmountConstants.FORMAT_TOTAL_PRICE_ONLY
        )
        return amountBoxControl
    }

    private val brandList: ArrayList<SpaySdk.Brand>
        get() {
            val brandList = ArrayList<SpaySdk.Brand>()
            brandList.add(SpaySdk.Brand.VISA)
            brandList.add(SpaySdk.Brand.MASTERCARD)
            brandList.add(SpaySdk.Brand.AMERICANEXPRESS)
            brandList.add(SpaySdk.Brand.DISCOVER)

            return brandList
        }
    private lateinit var paymentManager: PaymentManager

    /*
     * PaymentManager.startInAppPayWithCustomSheet is a method to request online(in-app) payment with Samsung Pay.
     * Partner app can use this method to make in-app purchase using Samsung Pay from their
     * application with custom payment sheet.
     */
    private fun startInAppPayWithCustomSheet() {
        if (binding.samServiceId.text?.isBlank() == true) {
            return
        }
        if (!::partnerInfo.isInitialized) {
            val bundle = Bundle()
            bundle.putString(SpaySdk.PARTNER_SERVICE_TYPE, SpaySdk.ServiceType.INAPP_PAYMENT.toString())
            partnerInfo = PartnerInfo(binding.samServiceId.text.toString(), bundle)
            updateSamsungPayButton()
            return
        }
        paymentManager = PaymentManager(applicationContext, partnerInfo)
        paymentManager.startInAppPayWithCustomSheet(
            makeTransactionDetailsWithSheet(), transactionInfoListener
        )
    }


    /*
     * CustomSheetTransactionInfoListener is for listening callback events of online (in-app) custom sheet payment.
     * This is invoked when card is changed by the user on the custom payment sheet,
     * and also with the success or failure of online (in-app) payment.
     */
    private val transactionInfoListener: PaymentManager.CustomSheetTransactionInfoListener =
        object : PaymentManager.CustomSheetTransactionInfoListener {
            // This callback is received when the user changes card on the custom payment sheet in Samsung Pay.
            override fun onCardInfoUpdated(selectedCardInfo: CardInfo, customSheet: CustomSheet) {/*
                 * Called when the user changes card in Samsung Pay.
                 * Newly selected cardInfo is passed and partner app can update transaction amount based on new card (if needed).
                 * Call updateSheet() method. This is mandatory.
                 */
                paymentManager.updateSheet(customSheet)
            }

            override fun onSuccess(
                response: CustomSheetPaymentInfo, paymentCredential: String, extraPaymentData: Bundle
            ) {/*
                 * You will receive the payloads shown below in paymentCredential parameter
                 * The output paymentCredential structure varies depending on the PG you're using and the integration model (direct, indirect) with Samsung.
                 */
                PaymentSdkActivity.startSamsungPayment(
                    this@MainActivity, getPaymentSdkConfigurationDetails(), paymentCredential, this@MainActivity
                )
            }

            // This callback is received when the online payment transaction has failed.
            override fun onFailure(errorCode: Int, errorData: Bundle?) {
                Toast.makeText(applicationContext, "onFailure() ", Toast.LENGTH_SHORT).show()
            }
        }

    private fun queryTrx() {
        QuerySdkActivity.queryTransaction(this, PaymentSDKQueryConfiguration(
            SERVER_KEY, CLIENT_KEY, getMerchantCountryCode(), MID, "TST223270139010"
        ), object : CallbackQueryInterface {
            override fun onError(error: PaymentSdkError) {
                Toast.makeText(this@MainActivity, error.msg, Toast.LENGTH_SHORT).show()
            }

            override fun onCancel() {
                Toast.makeText(this@MainActivity, "Cancel query", Toast.LENGTH_SHORT).show()
            }

            override fun onResult(transactionResponseBody: TransactionResponseBody) {
                Toast.makeText(this@MainActivity, transactionResponseBody.paymentResult?.responseMessage, Toast.LENGTH_SHORT)
                    .show()
            }

        })
    }

    private fun getMerchantCountryCode(): String {
        return binding.regions.selectedItem.toString()
    }


    private fun startCardPayment() {
        val configData = getPaymentSdkConfigurationDetails()
        PaymentSdkActivity.startCardPayment(context = this, ptConfigData = configData, callback = this)
    }

    private fun startRecurringCardPayment() {
        val configData = getPaymentSdkConfigurationDetails()
        PaymentSdkActivity.startTokenizedCardPayment(
            context = this,
            ptConfigData = configData,
            token = "${binding.cardToken.text}",
            transactionRef = "${binding.transactionReference.text}",
            callback = this
        )
    }

    private fun start3DSRecurringCardPayment() {
        val configData = getPaymentSdkConfigurationDetails()
        PaymentSdkActivity.start3DSecureTokenizedCardPayment(
            context = this, ptConfigData = configData, savedCardInfo = PaymentSDKSavedCardInfo(
                maskedCard = "${binding.savedCardInfoMaskedNumber.text}", cardType = "${binding.savedCardInfoCardType.text}"
            ), token = "${binding.cardToken.text}", callback = this
        )
    }

    private fun startCardPaymentWithSavedCards() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val configData = getPaymentSdkConfigurationDetails()
            PaymentSdkActivity.startPaymentWithSavedCards(
                context = this, ptConfigData = configData, support3DS = binding.request3ds.isChecked, callback = this
            )
        } else {
            Toast.makeText(this@MainActivity, "Not supported below Android M API 23", Toast.LENGTH_LONG).show()
        }

    }

    private fun startAlternativePaymentMethods() {
        val configData = getPaymentSdkConfigurationDetails()
        PaymentSdkActivity.startAlternativePaymentMethods(this, configData, this)
    }

    private fun getPaymentSdkConfigurationDetails(): PaymentSdkConfigurationDetails {
        val locale = getLocale()
        val cartDesc = "${binding.cartDesc.text}"
        val amount = "${binding.amount.text}".toDoubleOrNull()
        val billingData = PaymentSdkBillingDetails(
            city = "${binding.customerCity.text}",
            countryCode = "${binding.customerCountry.text}",
            email = "billing_mail@domain.com",
            name = "FirstName LastName",
            phone = "+201063209340",
            state = "121321",
            addressLine = "Billing Address Line",
            zip = "13512"
        )
        val shippingData = PaymentSdkShippingDetails(
            city = "${binding.customerCity.text}",
            countryCode = "${binding.customerCountry.text}",
            email = "${binding.customerEmail.text}",
            name = "FirstName LastName",
            phone = "+201063209340",
            state = "${binding.customerState.text}",
            addressLine = "${binding.customerStreet.text}",
            zip = "${binding.customerPostalCode.text}"
        )
        return PaymentSdkConfigBuilder(
            profileId = "${binding.mid.text}",
            serverKey = "${binding.serverKey.text}",
            clientKey = "${binding.clienttKey.text}",
            amount = amount ?: 0.0,
            currencyCode = binding.currency.selectedItem.toString()
        ).setCartDescription(cartDesc).setLanguageCode(locale).setBillingData(billingData)
            .setMerchantCountryCode(getMerchantCountryCode())
            .setTransactionType(PaymentSdkTransactionType.SALE).setShippingData(shippingData).setTokenise(getPaymentSdkTokenise())
            .setTransactionClass(getTransactionClass()).setCartId(ORDER_ID).showBillingInfo(true).showShippingInfo(false)
            .forceShippingInfo(false).setScreenTitle(TRANSACTION_TITLE).linkBillingNameWithCard(false)
            .setAlternativePaymentMethods(listOf(PaymentSdkApms.AMAN))
            .setCallback("https://webhook.site/a7f1e773-055d-41a5-9612-d204e91aea28").build()
    }

    private fun getPaymentSdkTokenise() = when (binding.tokenisation.selectedItemPosition) {
        0 -> PaymentSdkTokenise.NONE
        1 -> PaymentSdkTokenise.NONE
        2 -> PaymentSdkTokenise.MERCHANT_MANDATORY
        3 -> PaymentSdkTokenise.USER_MANDATORY
        4 -> PaymentSdkTokenise.USER_MANDATORY
        else -> PaymentSdkTokenise.NONE
    }

    private fun getTransactionClass() = when (binding.transactionClass.selectedItemPosition) {
        0 -> PaymentSdkTransactionClass.ECOM
        1 -> PaymentSdkTransactionClass.ECOM
        2 -> PaymentSdkTransactionClass.RECURRING
        else -> PaymentSdkTransactionClass.ECOM
    }

    private fun getLocale() =
        if (binding.language.selectedItemPosition == 1) PaymentSdkLanguageCode.EN else PaymentSdkLanguageCode.AR

    private fun showLastTransactionInfo() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Click To Copy")
        val info = arrayOf(
            "token = ${lastPaymentSdkTransactionDetails?.token ?: "EMPTY"}",
            "cardScheme = ${lastPaymentSdkTransactionDetails?.paymentInfo?.cardScheme ?: "EMPTY"}",
            "cardType = ${lastPaymentSdkTransactionDetails?.paymentInfo?.cardType ?: "EMPTY"}",
            "transactionType = ${lastPaymentSdkTransactionDetails?.transactionType ?: "EMPTY"}",
            "transactionReference = ${lastPaymentSdkTransactionDetails?.transactionReference ?: "EMPTY"}",
            "maskedCardNumber = ${lastPaymentSdkTransactionDetails?.paymentInfo?.paymentDescription ?: "EMPTY"}",
        )
        builder.setItems(info) { _, which ->
            val key = info[which].split(" = ").first()
            val value = info[which].split(" = ").last()
            val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(key, value)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "$key copied to clipboard", Toast.LENGTH_LONG).show()
        }
        val dialog = builder.create()
        dialog.show()
    }

    /**
     * hide billing
     *  link  cardHolder          unlink  billing
     * show billing
     *  link cardHolder           unlink billing
     */
    override fun onError(error: PaymentSdkError) {
        Log.d(TAG_PAYMENT_SDK, "onError: $error")
        Toast.makeText(this, "${error.msg}", Toast.LENGTH_SHORT).show()
    }

    override fun onPaymentFinish(paymentSdkTransactionDetails: PaymentSdkTransactionDetails) {
        lastPaymentSdkTransactionDetails = paymentSdkTransactionDetails
        Toast.makeText(this, "${paymentSdkTransactionDetails.paymentResult?.responseMessage}", Toast.LENGTH_SHORT).show()
        Log.d(TAG_PAYMENT_SDK, "onPaymentFinish: $paymentSdkTransactionDetails")
    }

    override fun onPaymentCancel() {
        Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
        Log.d(TAG_PAYMENT_SDK, "onPaymentCancel:")
    }
}