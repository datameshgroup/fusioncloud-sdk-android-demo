package au.com.dmg.fusioncloud.android.demo

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import au.com.dmg.fusion.MessageHeader
import au.com.dmg.fusion.SaleToPOI
import au.com.dmg.fusion.client.FusionClient
import au.com.dmg.fusion.data.ErrorCondition
import au.com.dmg.fusion.data.MessageCategory
import au.com.dmg.fusion.data.PaymentInstrumentType
import au.com.dmg.fusion.data.PaymentType
import au.com.dmg.fusion.data.SaleCapability
import au.com.dmg.fusion.data.TerminalEnvironment
import au.com.dmg.fusion.data.UnitOfMeasure
import au.com.dmg.fusion.exception.FusionException
import au.com.dmg.fusion.request.SaleTerminalData
import au.com.dmg.fusion.request.SaleToPOIRequest
import au.com.dmg.fusion.request.aborttransactionrequest.AbortTransactionRequest
import au.com.dmg.fusion.request.loginrequest.LoginRequest
import au.com.dmg.fusion.request.loginrequest.SaleSoftware
import au.com.dmg.fusion.request.paymentrequest.AmountsReq
import au.com.dmg.fusion.request.paymentrequest.PaymentData
import au.com.dmg.fusion.request.paymentrequest.PaymentInstrumentData
import au.com.dmg.fusion.request.paymentrequest.PaymentRequest
import au.com.dmg.fusion.request.paymentrequest.PaymentTransaction
import au.com.dmg.fusion.request.paymentrequest.SaleData
import au.com.dmg.fusion.request.paymentrequest.SaleItem
import au.com.dmg.fusion.request.paymentrequest.SaleTransactionID
import au.com.dmg.fusion.request.transactionstatusrequest.MessageReference
import au.com.dmg.fusion.request.transactionstatusrequest.TransactionStatusRequest
import au.com.dmg.fusion.response.Response
import au.com.dmg.fusion.response.SaleToPOIResponse
import au.com.dmg.fusion.response.TransactionStatusResponse
import au.com.dmg.fusion.response.paymentresponse.PaymentResponse
import au.com.dmg.fusion.util.MessageHeaderUtil
import au.com.dmg.fusioncloud.android.demo.utils.Configuration
import au.com.dmg.fusioncloud.android.demo.utils.FusionMessageHandler
import au.com.dmg.fusioncloud.android.demo.utils.FusionMessageResponse
import com.google.gson.GsonBuilder
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.naming.ConfigurationException

//TODO: Fix - Timer is not starting if there's no connection
//TODO: Add timer for building messages
class TestTransactionActivity : AppCompatActivity() {
    private lateinit var executorService: ExecutorService
    var sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss") //
    private var pressedTime: Long = 0
    var providerIdentification: String? = null
    var applicationName: String? = null
    var softwareVersion: String? = null
    var certificationCode: String? = null
    var saleID: String? = null
    var poiID: String? = null
    var kek: String? = null
    var useTestEnvironment = true

    //Timer settings; Update as needed.
    var loginTimeout: Long = 60000
    var paymentTimeout: Long = 60000 //60000
    var errorHandlingTimeout: Long = 90000 //90000
    var prevSecond // reference for counting second passed
            : Long = 0
    var waitingForResponse = false
    var secondsRemaining = 0
    var currentTransaction = MessageCategory.Login
    lateinit var currentServiceID: String
    lateinit var inputItemAmount: EditText
    lateinit var inputTipAmount: EditText
    lateinit var inputProductCode: EditText
    lateinit var jsonLogs: TextView
    lateinit var respUiHeader: TextView
    lateinit var respUiDetail: TextView
    lateinit var respReceipt: TextView
    lateinit var timer: TextView
    lateinit var respAuthourizedAmount: EditText
    lateinit var respTipAmount: EditText
    lateinit var respSurchargeAmount: EditText
    lateinit var respMaskedPAN: EditText
    lateinit var respPaymentBrand: EditText
    lateinit var respEntryMode: EditText
    lateinit var respServiceID: EditText
    lateinit var btnLogin: Button
    lateinit var btnPurchase: Button
    lateinit var btnRefund: Button
    lateinit var fusionClient: FusionClient
    lateinit var btnCancel: Button
    lateinit var progressCircle: ProgressBar
    var abortReason = ""

    fun initUI() {
        inputItemAmount = findViewById(R.id.input_item_amount)
        inputTipAmount = findViewById(R.id.input_tip_amount)
        inputProductCode = findViewById(R.id.input_product_code)
        jsonLogs = findViewById(R.id.edit_text_json_logs)
        jsonLogs.setTextIsSelectable(true)
        respUiHeader = findViewById(R.id.text_view_ui_header)
        respUiDetail = findViewById(R.id.text_view_ui_details)
        respReceipt = findViewById(R.id.text_view_receipt)
        respReceipt.setMovementMethod(ScrollingMovementMethod())
        respAuthourizedAmount = findViewById(R.id.response_authorize_amount_value)
        respTipAmount = findViewById(R.id.response_tip_amount_value)
        respSurchargeAmount = findViewById(R.id.response_surcharge_amount_value)
        respMaskedPAN = findViewById(R.id.response_masked_pan)
        respPaymentBrand = findViewById(R.id.response_payment_brand)
        respEntryMode = findViewById(R.id.response_entry_mode)
        respServiceID = findViewById(R.id.response_service_id)
        timer = findViewById(R.id.text_timer)
        progressCircle = findViewById(R.id.progressCircle)
    }

    private fun initFusionClient() {
        providerIdentification = Configuration.providerIdentification
        applicationName = Configuration.applicationName
        softwareVersion = Configuration.softwareVersion
        certificationCode = Configuration.certificationCode
        saleID = Configuration.saleId
        poiID = Configuration.poiId
        kek = Configuration.kek
        fusionClient = FusionClient(useTestEnvironment) //need to override this in production
        fusionClient.setSettings(
            saleID,
            poiID,
            kek
        ) // replace with the Sale ID provided by DataMesh
    }

    override fun onResume() {
        super.onResume()
        initFusionClient()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_transaction_activity)
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        initFusionClient()
        initUI()
        btnLogin = findViewById(R.id.btnLogin)
        btnLogin.setOnClickListener(View.OnClickListener { v: View? ->
            executorService = Executors.newSingleThreadExecutor()
            executorService.submit(Runnable { doLogin() })
        })
        btnPurchase = findViewById(R.id.btnPurchase)
        btnPurchase.setOnClickListener(View.OnClickListener { v: View? ->
            executorService = Executors.newSingleThreadExecutor()
            executorService.submit(Runnable { doPayment() })
        })
        btnRefund = findViewById(R.id.btnRefund)
        btnRefund.setOnClickListener(View.OnClickListener { v: View? ->
            executorService = Executors.newSingleThreadExecutor()
            executorService.submit(Runnable { doRefund() })
        })
        btnCancel = findViewById(R.id.btnCancel)
    }

    /* SaleToPOI Listener */
    private fun Listen() {
        try {
            //Update timer text
            prevSecond = computeSecondsRemaining(prevSecond)
            var saleToPOI: SaleToPOI? = null
            saleToPOI = fusionClient.readMessage()
            if (saleToPOI == null) return
            log("Response Received: ${prettyPrintJson(saleToPOI)}")
            val fmh = FusionMessageHandler()
            var fmr: FusionMessageResponse? = null
            if (saleToPOI is SaleToPOIRequest) {
                fmr = fmh.handle((saleToPOI as SaleToPOIRequest?)!!)

                /* DISPLAY SaleToPOIRequest*/
                val finalFmr: FusionMessageResponse = fmr
                runOnUiThread { respUiDetail!!.text = finalFmr.displayMessage }

                //Reset timer when a message is received (Not applicable to transaction status AKA error handling)
                if (currentTransaction == MessageCategory.Payment) {
                    secondsRemaining = (paymentTimeout / 1000).toInt() //Converting to seconds
                }
                waitingForResponse = true
            }
            if (saleToPOI is SaleToPOIResponse) {
                println("LISTEN SaleToPOIResponse")
                fmr = fmh.handle((saleToPOI as SaleToPOIResponse?)!!)

                //Ignore response if it's not the current transaction
                if (fmr.messageCategory != currentTransaction && fmr.messageCategory != MessageCategory.Event) {
                    log("Ignoring Response above... waiting for " + currentTransaction + ", received " + fmr.messageCategory)
                    return
                }
                when (fmr.messageCategory) {
                    MessageCategory.Event -> {
                        val spr = fmr.saleToPOI as SaleToPOIResponse?
                        val eventNotification = spr!!.eventNotification
                        log("Ignoring Event below... ${prettyPrintJson(spr)} Event Details: ${eventNotification!!.eventDetails}")
                    }

                    MessageCategory.Login -> displayLoginResponseMessage(fmr)
                    MessageCategory.Payment -> displayPaymentResponseMessage(fmr)
                    MessageCategory.TransactionStatus -> handleTransactionResponseMessage(fmr)
                    else -> {
                        //TODO Unknown MessageCategory
                    }
                }
            }
        } catch (e: FusionException) {
//            e.printStackTrace();
            // Should not loop if there's an error. e.g. Socket Disconnection
            endLog(String.format("Stopped listening to message. Reason:\n %s", e), true)
            if (currentTransaction != MessageCategory.TransactionStatus) {
                println("CURRENT SERVICE ID: " + currentServiceID)
                executorService!!.shutdownNow()
                executorService = Executors.newSingleThreadExecutor()
                executorService.submit(Runnable {
                    checkTransactionStatus(
                        currentServiceID,
                        "Websocket connection interrupted"
                    )
                })
            }
        }
    }

    private fun displayLoginResponseMessage(fmr: FusionMessageResponse) {
        endTransactionUi()
        runOnUiThread { respUiHeader!!.text = fmr.displayMessage }
        waitingForResponse = false
    }

    private fun displayPaymentResponseMessage(fmr: FusionMessageResponse) {
        endTransactionUi()
        val paymentResponse = (fmr.saleToPOI as SaleToPOIResponse?)!!.paymentResponse
        val paymentType = paymentResponse!!.paymentResult!!.paymentType
        val paymentResult = paymentResponse.paymentResult
        runOnUiThread {
            if (paymentType == PaymentType.Normal) {
                respUiHeader!!.text = fmr.displayMessage
                respAuthourizedAmount!!.setText(paymentResult!!.amountsResp!!.authorizedAmount.toString())
                respTipAmount!!.setText(paymentResult.amountsResp!!.tipAmount.toString())
                respSurchargeAmount!!.setText(paymentResult.amountsResp!!.surchargeAmount.toString())
                respMaskedPAN!!.setText(paymentResult.paymentInstrumentData!!.cardData.maskedPAN)
                respPaymentBrand!!.setText(paymentResult.paymentInstrumentData!!.cardData.paymentBrand.toString())
                respEntryMode!!.setText(paymentResult.paymentInstrumentData!!.cardData.entryMode.toString())
                respServiceID!!.setText(fmr.saleToPOI!!.messageHeader.serviceID)
                // Receipt
                val paymentReceipt = paymentResponse.paymentReceipt!![0]
                val OutputXHTML = paymentReceipt.receiptContentAsHtml
                respReceipt!!.text = HtmlCompat.fromHtml(OutputXHTML, 0)
            } else if (paymentType == PaymentType.Refund) {
                respUiHeader!!.text = fmr.displayMessage
                respAuthourizedAmount!!.setText(paymentResult!!.amountsResp!!.authorizedAmount.toString())
                respTipAmount!!.setText("0.00")
                respSurchargeAmount!!.setText(paymentResult.amountsResp!!.surchargeAmount.toString())
                respMaskedPAN!!.setText(paymentResult.paymentInstrumentData!!.cardData.maskedPAN)
                respPaymentBrand!!.setText(paymentResult.paymentInstrumentData!!.cardData.paymentBrand.toString())
                respEntryMode!!.setText(paymentResult.paymentInstrumentData!!.cardData.entryMode.toString())
                respServiceID!!.setText(fmr.saleToPOI!!.messageHeader.serviceID)
                // Receipt
                val paymentReceipt = paymentResponse.paymentReceipt!![0]
                val OutputXHTML = paymentReceipt.receiptContentAsHtml
                respReceipt!!.text = HtmlCompat.fromHtml(OutputXHTML, 0)
            }
        }
        waitingForResponse = false
    }

    //Currently only called from transactionstatus response
    private fun displayPaymentResponseMessage(pr: PaymentResponse, mh: MessageHeader) {
        endTransactionUi()
        runOnUiThread {
            respUiHeader!!.text =
                "PAYMENT " + pr.response.result.toString().uppercase(Locale.getDefault())
            val paymentResult = pr.paymentResult
            respAuthourizedAmount!!.setText(paymentResult!!.amountsResp!!.authorizedAmount.toString())
            respTipAmount!!.setText(paymentResult.amountsResp!!.tipAmount.toString())
            respSurchargeAmount!!.setText(paymentResult.amountsResp!!.surchargeAmount.toString())
            respMaskedPAN!!.setText(paymentResult.paymentInstrumentData!!.cardData.maskedPAN)
            respPaymentBrand!!.setText(paymentResult.paymentInstrumentData!!.cardData.paymentBrand.toString())
            respEntryMode!!.setText(paymentResult.paymentInstrumentData!!.cardData.entryMode.toString())
            respServiceID!!.setText(mh.serviceID)

            //Receipt
            val paymentReceipt = pr.paymentReceipt!![0]
            val OutputXHTML = paymentReceipt.receiptContentAsHtml
            respReceipt!!.text = HtmlCompat.fromHtml(OutputXHTML, 0)
        }
        waitingForResponse = false
    }

    // Only called when there is no repeated message response on the transaction status
    private fun displayTransactionResponseMessage(
        errorCondition: ErrorCondition,
        additionalResponse: String
    ) {
        endTransactionUi()
        runOnUiThread { respUiHeader!!.text = "$errorCondition - $additionalResponse" }
    }

    private fun handleTransactionResponseMessage(fmr: FusionMessageResponse) {
        // TODO: handle transaction status response for others. currently designed for Payment only.
        var transactionStatusResponse: TransactionStatusResponse? = null
        var responseBody: Response? = null
        if (fmr.isSuccessful!!) {
            transactionStatusResponse =
                (fmr.saleToPOI as SaleToPOIResponse?)!!.transactionStatusResponse
            responseBody = transactionStatusResponse!!.response
            log(String.format("Transaction Status Result: %s ", responseBody.result))
            val paymentResponse =
                transactionStatusResponse.repeatedMessageResponse.repeatedResponseMessageBody.paymentResponse
            val paymentMessageHeader =
                transactionStatusResponse.repeatedMessageResponse.messageHeader
            displayPaymentResponseMessage(paymentResponse, paymentMessageHeader)
        } else if (fmr.errorCondition == ErrorCondition.InProgress) {
            log("Transaction in progress...")
            if (secondsRemaining > 10) {
                errorHandlingTimeout =
                    ((secondsRemaining - 10) * 1000).toLong() //decrement errorHandlingTimeout so it will not reset after waiting
                log(
                    """
    Sending another transaction status request after 10 seconds...
    Remaining seconds until error handling timeout: $secondsRemaining
    """.trimIndent()
                )
                try {
                    TimeUnit.SECONDS.sleep(10)
                    executorService!!.shutdownNow()
                    executorService = Executors.newSingleThreadExecutor()
                    executorService.submit(Runnable {
                        checkTransactionStatus(
                            currentServiceID,
                            ""
                        )
                    })
                } catch (e: InterruptedException) {
                    endLog(e)
                }
            }
        } else {
            transactionStatusResponse =
                (fmr.saleToPOI as SaleToPOIResponse?)!!.transactionStatusResponse
            responseBody = transactionStatusResponse!!.response
            endLog(
                String.format(
                    "Error Condition: %s, Additional Response: %s",
                    responseBody.errorCondition, responseBody.additionalResponse
                ), true
            )
            displayTransactionResponseMessage(
                responseBody.errorCondition,
                responseBody.additionalResponse
            )
        }
    }

    private fun doLogin() {
        try {
            currentServiceID = MessageHeaderUtil.generateServiceID()
            currentTransaction = MessageCategory.Login
            startTransactionUi()
            val loginRequest = buildLoginRequest()
            log(" Sending message to websocket server: ${prettyPrintJson(loginRequest)}")
            fusionClient.sendMessage(loginRequest, currentServiceID)

            //Set timeout
            prevSecond = System.currentTimeMillis()
            secondsRemaining = (loginTimeout / 1000).toInt()

            // Loop for Listener
            waitingForResponse = true
            while (waitingForResponse) {
                Listen()
                if (secondsRemaining < 1) {
                    endLog("Login Request Timeout...", true)
                    endTransactionUi()
                    break
                }
            }
        } catch (e: ConfigurationException) {
            endLog(e)
        }
    }

    private fun doPayment() {
        try {
            currentServiceID = MessageHeaderUtil.generateServiceID()
            currentTransaction = MessageCategory.Payment
            startTransactionUi()
            clearLog()
            val paymentRequest = buildPaymentRequest()
            log(
                """
    Sending message to websocket server: 
    ${prettyPrintJson(paymentRequest)}
    """.trimIndent()
            )
            fusionClient.sendMessage(paymentRequest, currentServiceID)

            // Set timeout
            prevSecond = System.currentTimeMillis()
            secondsRemaining = (paymentTimeout / 1000).toInt()
            waitingForResponse = true
            while (waitingForResponse) {
                Listen()
                if (secondsRemaining < 1) {
                    endLog("Payment Request Timeout...", true)
                    abortReason = "Timeout"
                    checkTransactionStatus(currentServiceID, abortReason)
                    break
                }
            }
        } catch (e: ConfigurationException) {
            endLog(String.format("Exception: %s", e), true)
            abortReason = "Other Exception"
            checkTransactionStatus(currentServiceID, abortReason)
        } catch (e: FusionException) {
            endLog(String.format("FusionException: %s. Resending the Request...", e), true)
            // Continue the timer
            paymentTimeout = secondsRemaining * 1000L
            doPayment()
        }
    }

    private fun doAbort(serviceID: String?, abortReason: String) {
        endTransactionUi()
        runOnUiThread {
            respUiHeader!!.text = "ABORTING TRANSACTION"
            respUiDetail!!.text = ""
        }
        hideProgressCircle(false)
        val abortTransactionPOIRequest = buildAbortRequest(serviceID, abortReason)
        log(
            """
    Sending abort message to websocket server: 
    ${prettyPrintJson(abortTransactionPOIRequest)}
    """.trimIndent()
        )
        fusionClient!!.sendMessage(abortTransactionPOIRequest)
    }

    private fun checkTransactionStatus(serviceID: String?, abortReason: String) {
        try {
            currentTransaction = MessageCategory.TransactionStatus
            startTransactionUi()
            hideCancelBtn(true)
            if (abortReason !== "") {
                doAbort(serviceID, abortReason)
            }
            runOnUiThread { respUiHeader!!.text = "CHECKING TRANSACTION STATUS" }
            val transactionStatusRequest = buildTransactionStatusRequest(serviceID)
            log(
                """
    Sending transaction status request to check status of payment... 
    ${prettyPrintJson(transactionStatusRequest)}
    """.trimIndent()
            )
            fusionClient.sendMessage(transactionStatusRequest)

            // Set timeout
            prevSecond = System.currentTimeMillis()
            secondsRemaining = (errorHandlingTimeout / 1000).toInt()
            waitingForResponse = true
            while (waitingForResponse) {
                Listen()
                if (secondsRemaining < 1) {
                    endTransactionUi()
                    runOnUiThread {
                        respUiHeader!!.text = "Time Out"
                        respUiDetail!!.text = "Please check Satellite Transaction History"
                        endLog("Transaction Status Request Timeout...", true)
                    }
                    break
                }
            }
        } catch (e: ConfigurationException) {
            throw RuntimeException(e)
        }
    }

    private fun doRefund() {
        try {
            currentServiceID = MessageHeaderUtil.generateServiceID()
            currentTransaction = MessageCategory.Payment
            startTransactionUi()
            clearLog()
            val refundRequest = buildRefundRequest()
            log(
                """
    Sending message to websocket server: 
    ${prettyPrintJson(refundRequest)}
    """.trimIndent()
            )
            fusionClient.sendMessage(refundRequest, currentServiceID)

            // Set timeout
            prevSecond = System.currentTimeMillis()
            secondsRemaining = (paymentTimeout / 1000).toInt()
            waitingForResponse = true
            while (waitingForResponse) {
                Listen()
                if (secondsRemaining < 1) {
                    abortReason = "Timeout"
                    endLog("Refund Request Timeout...", true)
                    checkTransactionStatus(currentServiceID, abortReason)
                    break
                }
            }
        } catch (e: ConfigurationException) {
            endLog(String.format("Exception: %s", e), true)
            abortReason = "Other Exception"
            checkTransactionStatus(currentServiceID, abortReason)
        } catch (e: FusionException) {
            endLog(String.format("FusionException: %s. Resending the Request...", e), true)
            // Continue the timer
            paymentTimeout = secondsRemaining * 1000L
            doPayment()
        }
    }

    @Throws(ConfigurationException::class)
    private fun buildLoginRequest(): LoginRequest {
        val saleSoftware = SaleSoftware.Builder() //
            .providerIdentification(providerIdentification) //
            .applicationName(applicationName) //
            .softwareVersion(softwareVersion) //
            .certificationCode(certificationCode) //
            .build()
        val saleTerminalData = SaleTerminalData.Builder() //
            .terminalEnvironment(TerminalEnvironment.SemiAttended) //
            .saleCapabilities(
                listOf(
                    SaleCapability.CashierStatus, SaleCapability.CustomerAssistance,
                    SaleCapability.PrinterReceipt
                )
            ) //
            .build()
        return LoginRequest.Builder() //
            .dateTime(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Date())) //
            .saleSoftware(saleSoftware) //
            .saleTerminalData(saleTerminalData) //
            .operatorLanguage("en") //
            .build()
    }

    @Throws(ConfigurationException::class)
    private fun buildPaymentRequest(): PaymentRequest {
        val inputAmount =
            BigDecimal(inputItemAmount!!.text.toString())
        val inputTip = BigDecimal(inputTipAmount!!.text.toString())
        //        BigDecimal inputTip = new BigDecimal(inputTipAmount.getText().toString() ? ); // TODO: if null
        val productCode = inputProductCode!!.text.toString()
        val requestedAmount = inputAmount.add(inputTip)
        val saleTransactionID = SaleTransactionID.Builder() //
            .transactionID(
                "transactionID" + SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                    .format(Date())
            ) ////
            .timestamp(Instant.now()).build()
        val saleData = SaleData.Builder() //
            // .operatorID("")//
            .operatorLanguage("en") //
            .saleTransactionID(saleTransactionID) //
            .build()
        val amountsReq = AmountsReq.Builder() //
            .currency("AUD") //
            .requestedAmount(requestedAmount) //
            .tipAmount(inputTip)
            .build()
        val saleItem = SaleItem.Builder() //
            .itemID(0) //
            .productCode(productCode) //
            .unitOfMeasure(UnitOfMeasure.Other) //
            .quantity(BigDecimal(1)) //
            .unitPrice(BigDecimal(100.00)) //
            .itemAmount(inputAmount) //
            .productLabel("Product Label") //
            .build()
        val paymentInstrumentData =
            PaymentInstrumentData.Builder() //
                .paymentInstrumentType(PaymentInstrumentType.Cash) //
                .build()
        val paymentData = PaymentData.Builder() //
            .paymentType(PaymentType.Normal) //
            .paymentInstrumentData(paymentInstrumentData) //
            .build()
        val paymentTransaction = PaymentTransaction.Builder() //
            .amountsReq(amountsReq) //
            .addSaleItem(saleItem) //
            .build()
        return PaymentRequest.Builder() //
            .paymentTransaction(paymentTransaction) //
            .paymentData(paymentData) //
            .saleData(saleData).build()
    }

    @Throws(ConfigurationException::class)
    private fun buildTransactionStatusRequest(serviceID: String?): TransactionStatusRequest {
        val messageReference = MessageReference.Builder() //
            .messageCategory(MessageCategory.Payment) //
            .POIID(poiID) //
            .saleID(saleID) //
            .serviceID(serviceID) //
            .build()
        return TransactionStatusRequest(messageReference)
    }

    private fun buildAbortRequest(
        referenceServiceID: String?,
        abortReason: String
    ): AbortTransactionRequest {
        val messageReference = MessageReference.Builder()
            .messageCategory(MessageCategory.Abort)
            .serviceID(referenceServiceID).build()
        return AbortTransactionRequest(messageReference, abortReason)
    }

    @Throws(ConfigurationException::class)
    private fun buildRefundRequest(): PaymentRequest {
        val inputAmount =
            BigDecimal(inputItemAmount!!.text.toString())
        val productCode = inputProductCode!!.text.toString()
        val saleTransactionID = SaleTransactionID.Builder() //
            .transactionID(
                "transactionID" + SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                    .format(Date())
            ) ////
            .timestamp(Instant.now()).build()
        val saleData = SaleData.Builder() //
            .operatorLanguage("en") //
            .saleTransactionID(saleTransactionID) //
            .build()
        val amountsReq = AmountsReq.Builder() //
            .currency("AUD") //
            .requestedAmount(inputAmount) //
            .build()
        val saleItem = SaleItem.Builder() //
            .itemID(0) //
            .productCode(productCode) //
            .unitOfMeasure(UnitOfMeasure.Other) //
            .quantity(BigDecimal(1)) //
            .unitPrice(BigDecimal(100.00)) //
            .itemAmount(inputAmount) //
            .productLabel("Product Label") //
            .build()
        val paymentInstrumentData =
            PaymentInstrumentData.Builder() //
                .paymentInstrumentType(PaymentInstrumentType.Cash) //
                .build()
        val refundData = PaymentData.Builder() //
            .paymentType(PaymentType.Refund) //
            .paymentInstrumentData(paymentInstrumentData) //
            .build()
        val paymentTransaction = PaymentTransaction.Builder() //
            .amountsReq(amountsReq) //
            .addSaleItem(saleItem) //
            .build()
        return PaymentRequest.Builder() //
            .paymentTransaction(paymentTransaction) //
            .paymentData(refundData) //
            .saleData(saleData).build()
    }

    private fun startTransactionUi() {
        runOnUiThread {
            if (currentTransaction == MessageCategory.Payment) {
                btnCancel!!.setOnClickListener {
                    doAbort(
                        currentServiceID,
                        "User Cancelled"
                    )
                }
                btnCancel!!.visibility = View.VISIBLE
            }
            progressCircle!!.visibility = View.VISIBLE
            respUiHeader!!.text =
                currentTransaction.toString().uppercase(Locale.getDefault()) + " IN PROGRESS"
            respUiDetail!!.text = "..."
            btnLogin!!.isEnabled = false
            btnPurchase!!.isEnabled = false
            btnRefund!!.isEnabled = false
        }
    }

    private fun endTransactionUi() {
        runOnUiThread {
            progressCircle.visibility = View.INVISIBLE
            btnCancel.visibility = View.INVISIBLE
            respUiDetail.text = ""
            timer.text = "0"
            btnLogin.isEnabled = true
            btnPurchase.isEnabled = true
            btnRefund.isEnabled = true
        }
    }

    private fun hideCancelBtn(doHide: Boolean) {
        runOnUiThread {
            if (doHide) {
                btnCancel!!.visibility = View.INVISIBLE
            } else {
                btnCancel!!.setOnClickListener {
                    doAbort(
                        currentServiceID,
                        "User Cancelled"
                    )
                }
                btnCancel!!.visibility = View.VISIBLE
            }
        }
    }

    private fun hideProgressCircle(doHide: Boolean) {
        runOnUiThread {
            if (doHide) {
                progressCircle!!.visibility = View.INVISIBLE
            } else {
                progressCircle!!.visibility = View.VISIBLE
            }
        }
    }

    fun computeSecondsRemaining(start: Long): Long {
        var start = start
        val currentTime = System.currentTimeMillis()
        val sec = (currentTime - start) / 1000
        if (sec == 1L) {
            runOnUiThread { timer!!.text = secondsRemaining--.toString() }
            start = currentTime
        }
        return start
    }

    private fun endLog(ex: Exception) {
        log(ex.message)
        waitingForResponse = false
    }

    private fun endLog(logData: String, stopWaiting: Boolean) {
        runOnUiThread {
            jsonLogs!!.append(
                """
                        ${sdf.format(Date(System.currentTimeMillis()))}: 
                        $logData
                        
                        
                        """.trimIndent()
            )
        } // 2021.03.24.16.34.26

        if (stopWaiting) {
            waitingForResponse = false
        }
    }

    private fun clearLog() {
        runOnUiThread { jsonLogs!!.text = "" }
    }

    private fun log(logData: String?) {
        println(sdf.format(Date(System.currentTimeMillis())) + ": " + logData) // 2021.03.24.16.34.26
        runOnUiThread {
            jsonLogs!!.append(
                """
    ${sdf.format(Date(System.currentTimeMillis()))}: 
    $logData
    
    
    """.trimIndent()
            )
        }
    }

    fun prettyPrintJson(json: Any?): String {
        val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        return gson.toJson(json)
    }

    override fun onBackPressed() {
        if (pressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
            finish()
        } else {
            Toast.makeText(baseContext, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
        pressedTime = System.currentTimeMillis()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.back, menu);
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}