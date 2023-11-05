package au.com.dmg.fusioncloud.android.demo

import android.graphics.Color
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import au.com.dmg.fusion.SaleToPOI
import au.com.dmg.fusion.client.FusionClient
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
import au.com.dmg.fusion.response.SaleToPOIResponse
import au.com.dmg.fusion.util.MessageHeaderUtil
import au.com.dmg.fusioncloud.android.demo.data.RequestData
import au.com.dmg.fusioncloud.android.demo.databinding.TransactionProgressActivityBinding
import au.com.dmg.fusioncloud.android.demo.utils.Configuration
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.applicationName
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.certificationCode
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.kek
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.poiId
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.providerIdentification
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.saleId
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.softwareVersion
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.useTestEnvironment
import au.com.dmg.fusioncloud.android.demo.utils.FusionMessageHandler
import au.com.dmg.fusioncloud.android.demo.utils.FusionMessageResponse
import au.com.dmg.fusioncloud.android.demo.utils.ParsingUtils.Companion.log
import au.com.dmg.fusioncloud.android.demo.utils.ParsingUtils.Companion.prettyPrintJson
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.naming.ConfigurationException


class TransactionProgressActivity: AppCompatActivity() {
    private lateinit var binding: TransactionProgressActivityBinding
    private lateinit var executorService: ExecutorService
    lateinit var fusionClient: FusionClient
    lateinit var requestData: RequestData
    lateinit var currentTransaction:MessageCategory

    private var currentServiceID = ""
    private var abortReason = ""

    //Timer settings; Update as needed.
    var loginTimeout: Long = 60000
    var paymentTimeout: Long = 60000 //60000
    private var errorHandlingTimeout: Long = 90000 //90000
    private var prevSecond: Long = 0
    private var waitingForResponse = false
    private var secondsRemaining = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TransactionProgressActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        initFusionClient()
        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            requestData = bundle.get("requestData") as RequestData

            currentTransaction = requestData.MessageCategory

            println("currentTransaction ----- $currentTransaction")

            when (currentTransaction){
                MessageCategory.Login -> {
                    executorService = Executors.newSingleThreadExecutor()
                    executorService.submit { doLogin() }
                }
                MessageCategory.Payment -> {
                    executorService = Executors.newSingleThreadExecutor()
                    if(requestData.PaymentType == PaymentType.Normal){
                        executorService.submit { doPayment() }
                    }else{
//                        executorService.submit { doRefund() }
                    }

                }
                else -> {}
            }

        }

//        putExtra("requestedAmount", requestedAmount)
//        putExtra("tipAmount", tipAmount)
//        // for testing only
//        putExtra("productCode", productCode)


        //MessageHeader currentServiceID category
//        currentServiceID = MessageHeaderUtil.generateServiceID()??
//        currentTransaction = MessageCategory.Payment

    }

    private fun initFusionClient() {
        fusionClient = FusionClient(useTestEnvironment) //need to override this in production
        fusionClient.setSettings(
            saleId,
            poiId,
            kek
        )
    }

    //#region UI STUFF
    fun initUI() {
        runOnUiThread {
            if (currentTransaction == MessageCategory.Payment) {
                binding.btnCancel.setOnClickListener {
                    doAbort(currentServiceID,"User Cancelled")
                }
                binding.btnCancel.visibility = View.VISIBLE
            }
            hideProgressCircle(false)
            binding.textViewUiHeader.text =
                currentTransaction.toString().uppercase(Locale.getDefault()) + " IN PROGRESS"
            binding.textViewUiDetails.text = ""
        }
    }
    fun finalUI() {
        runOnUiThread {
            hideProgressCircle(true)
            hideCancelBtn(true)
            binding.textTimer.text = "0"
            binding.textTimer.visibility=View.INVISIBLE

            binding.btnOK.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    finish()
                    //TODO Call intents here
                }
            }
        }
        waitingForResponse = false
        executorService.shutdownNow()
    }
    fun hideProgressCircle(doHide: Boolean) {
        runOnUiThread {
            if (doHide) {
                binding.progressCircle.visibility = View.INVISIBLE
            } else {
                binding.progressCircle.visibility = View.VISIBLE
            }
        }
    }
    private fun hideCancelBtn(doHide: Boolean) {
        runOnUiThread {
            if (doHide) {
                binding.btnCancel.visibility = View.INVISIBLE
            } else {
                binding.btnCancel.setOnClickListener {
                    doAbort(currentServiceID,"User Cancelled")
                }
                binding.btnCancel.visibility = View.GONE
            }
        }
    }

    private fun handleResponseMessage(fmr: FusionMessageResponse, mc: MessageCategory) {
        when(mc){
            MessageCategory.Login -> {
                finalUI()
            }
            (MessageCategory.Payment) -> {
                if(requestData.PaymentType==PaymentType.Normal){
                    //set intent send here
                }
                else{

                }
            }
            else -> {}
        }
        finalUI()
        runOnUiThread {
            if(fmr.isSuccessful==true){
                binding.textViewUiHeader.apply {
//                    setBackgroundColor(Color.parseColor("#4CAF50"))
                    text = fmr.displayMessage
                }
                binding.trHeader.setBackgroundColor(Color.parseColor("#4CAF50"))

                binding.textViewUiDetails.apply {
//                    setBackgroundColor(Color.parseColor("#4CAF50"))
                    text = ""
                }
            }else{

                binding.textViewUiHeader.apply {
//                    setBackgroundColor(Color.parseColor("#E91E63"))
                    text = fmr.errorCondition.toString()
                }

                binding.trHeader.setBackgroundColor(Color.parseColor("#E91E63"))

                binding.textViewUiDetails.apply {
//                    setBackgroundColor(Color.parseColor("#E91E63"))
                    text = fmr.additionalResponse
                }
            }
        }


        val paymentResponse = (fmr.saleToPOI as SaleToPOIResponse?)!!.paymentResponse
        val paymentType = paymentResponse!!.paymentResult!!.paymentType
        val paymentResult = paymentResponse.paymentResult
        runOnUiThread {
            if (paymentType == PaymentType.Normal) {
//                respUiHeader!!.text = fmr.displayMessage
//                respAuthourizedAmount!!.setText(paymentResult!!.amountsResp!!.authorizedAmount.toString())
//                respTipAmount!!.setText(paymentResult.amountsResp!!.tipAmount.toString())
//                respSurchargeAmount!!.setText(paymentResult.amountsResp!!.surchargeAmount.toString())
//                respMaskedPAN!!.setText(paymentResult.paymentInstrumentData!!.cardData.maskedPAN)
//                respPaymentBrand!!.setText(paymentResult.paymentInstrumentData!!.cardData.paymentBrand.toString())
//                respEntryMode!!.setText(paymentResult.paymentInstrumentData!!.cardData.entryMode.toString())
//                respServiceID!!.setText(fmr.saleToPOI!!.messageHeader.serviceID)
//                // Receipt
//                val paymentReceipt = paymentResponse.paymentReceipt!![0]
//                val OutputXHTML = paymentReceipt.receiptContentAsHtml
//                respReceipt!!.text = HtmlCompat.fromHtml(OutputXHTML, 0)
            } else if (paymentType == PaymentType.Refund) {
//                respUiHeader!!.text = fmr.displayMessage
//                respAuthourizedAmount!!.setText(paymentResult!!.amountsResp!!.authorizedAmount.toString())
//                respTipAmount!!.setText("0.00")
//                respSurchargeAmount!!.setText(paymentResult.amountsResp!!.surchargeAmount.toString())
//                respMaskedPAN!!.setText(paymentResult.paymentInstrumentData!!.cardData.maskedPAN)
//                respPaymentBrand!!.setText(paymentResult.paymentInstrumentData!!.cardData.paymentBrand.toString())
//                respEntryMode!!.setText(paymentResult.paymentInstrumentData!!.cardData.entryMode.toString())
//                respServiceID!!.setText(fmr.saleToPOI!!.messageHeader.serviceID)
//                // Receipt
//                val paymentReceipt = paymentResponse.paymentReceipt!![0]
//                val OutputXHTML = paymentReceipt.receiptContentAsHtml
//                respReceipt!!.text = HtmlCompat.fromHtml(OutputXHTML, 0)
            }
        }
    }
    //#endregion
    //#region REQUEST SENDERS
    fun doAbort(serviceID: String?, abortReason: String) {
        runOnUiThread {
            binding.textViewUiHeader.text = "ABORTING TRANSACTION"
            binding.textViewUiDetails.text = ""
        }
        hideProgressCircle(false)
        val abortTransactionPOIRequest = buildAbortRequest(serviceID, abortReason)
        log("Sending abort message to websocket server: ${prettyPrintJson(abortTransactionPOIRequest)}")
        try{
            fusionClient.sendMessage(abortTransactionPOIRequest)
        }catch (e:FusionException){
            println("FusionException doAbort")
        }

    }
    private fun doLogin() {
        try {
            currentServiceID = MessageHeaderUtil.generateServiceID()
            currentTransaction = MessageCategory.Login
            initUI()
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
                    log("Login Request Timeout...")
                    finalUI()
                    break
                }
            }
        } catch (e: ConfigurationException) {
            log(e.toString())
            finalUI()
        }
    }
    private fun doPayment() {
        try {
            currentServiceID = requestData.serviceID
            initUI()
            val paymentRequest = buildPaymentRequest()
            log("Sending message to websocket server: ${prettyPrintJson(paymentRequest)}")
            fusionClient.sendMessage(paymentRequest, currentServiceID)

            // Set timeout
            prevSecond = System.currentTimeMillis()
            secondsRemaining = (paymentTimeout / 1000).toInt()
            waitingForResponse = true
            while (waitingForResponse) {
                Listen()
                if (secondsRemaining < 1) {
                    log("Payment Request Timeout...")
                    finalUI()
                    abortReason = "Timeout"
                    checkTransactionStatus(currentServiceID, abortReason)
                    break
                }
            }
        } catch (ce: ConfigurationException) {
            log(String.format("Exception: %s", ce))
            finalUI()
            abortReason = "Other Exception"
            checkTransactionStatus(currentServiceID, abortReason)
        } catch (fe: FusionException) {
            log(String.format("FusionException: %s. Resending the Request...", fe))
            finalUI()
            // Continue the timer
            paymentTimeout = secondsRemaining * 1000L

            doPayment()
        }
    }
    private fun checkTransactionStatus(serviceID: String?, abortReason: String) {
        try {
            currentTransaction = MessageCategory.TransactionStatus
            initUI();
            hideCancelBtn(true)
            if (abortReason !== "") {
                doAbort(serviceID, abortReason)
            }
            runOnUiThread {
                binding.textViewUiHeader.text = "CHECKING TRANSACTION STATUS"
            }
            val transactionStatusRequest = buildTransactionStatusRequest(serviceID)
            log("Sending transaction status request to check status of payment...\n${prettyPrintJson(transactionStatusRequest)}")
            fusionClient.sendMessage(transactionStatusRequest)

            // Set timeout
            prevSecond = System.currentTimeMillis()
            secondsRemaining = (errorHandlingTimeout / 1000).toInt()
            waitingForResponse = true
            while (waitingForResponse) {
                Listen()
                if (secondsRemaining < 1) {
//                    endTransactionUi()
                    runOnUiThread {
                        binding.textViewUiHeader.text = "Time Out"
                        binding.textViewUiDetails.text = "Please check Satellite Transaction History"
                        log("Transaction Status Request Timeout...")
                        finalUI()
                    }
                    break
                }
            }
        } catch (e: ConfigurationException) {
            throw RuntimeException(e)
        }
    }
    //#endregion

    //#region REQUEST BUILDERS
    private fun buildAbortRequest(
        referenceServiceID: String?,
        abortReason: String
    ): AbortTransactionRequest {
        val messageReference = MessageReference.Builder()
            .messageCategory(MessageCategory.Abort)
            .serviceID(referenceServiceID).build()
        return AbortTransactionRequest(messageReference, abortReason)
    }


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

    private fun buildPaymentRequest(): PaymentRequest {

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
            .requestedAmount(requestData.requestedAmount) //
            .tipAmount(requestData.tipAmount)
            .build()
        //TODO Update this, if productCode is not empty, just 1. else array
        val saleItem = SaleItem.Builder() //
            .itemID(0) //
            .productCode(requestData.productCode) //
            .unitOfMeasure(UnitOfMeasure.Other) //
            .quantity(BigDecimal(1)) //
            .unitPrice(BigDecimal(100.00)) //
            .itemAmount(requestData.requestedAmount) //
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

    private fun buildTransactionStatusRequest(serviceID: String?): TransactionStatusRequest {
        val messageReference = MessageReference.Builder() //
            .messageCategory(MessageCategory.Payment) //
            .POIID(poiId) //
            .saleID(saleId) //
            .serviceID(serviceID) //
            .build()
        return TransactionStatusRequest(messageReference)
    }
    //#endregion

    //#region SaleToPOI Listener
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
                runOnUiThread {
                    binding.textViewUiDetails.text = finalFmr.displayMessage
                }

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

                    MessageCategory.Login -> handleResponseMessage(fmr, MessageCategory.Login)
                    MessageCategory.Payment -> handleResponseMessage(fmr , MessageCategory.Payment)
                    MessageCategory.TransactionStatus -> handleResponseMessage(fmr , MessageCategory.TransactionStatus)
                    else -> {
                        //TODO Unknown MessageCategory
                    }
                }
            }
        } catch (e: FusionException) {
//            e.printStackTrace();
            // Should not loop if there's an error. e.g. Socket Disconnection
            log(String.format("Stopped listening to message. Reason:\n %s", e))
            finalUI()
            if (currentTransaction != MessageCategory.TransactionStatus) {
                println("CURRENT SERVICE ID: " + currentServiceID)
                executorService.shutdownNow()
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

    //#endregion

    fun computeSecondsRemaining(start: Long): Long {
        var start = start
        val currentTime = System.currentTimeMillis()
        val sec = (currentTime - start) / 1000
        if (sec == 1L) {
            runOnUiThread {
                binding.textTimer.text = secondsRemaining--.toString()
            }
            start = currentTime
        }
        return start
    }
}