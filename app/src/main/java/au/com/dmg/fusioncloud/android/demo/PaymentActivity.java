package au.com.dmg.fusioncloud.android.demo;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.naming.ConfigurationException;

import au.com.dmg.fusion.MessageHeader;
import au.com.dmg.fusion.SaleToPOI;
import au.com.dmg.fusion.client.FusionClient;
import au.com.dmg.fusion.data.ErrorCondition;
import au.com.dmg.fusion.data.MessageCategory;
import au.com.dmg.fusion.data.PaymentInstrumentType;
import au.com.dmg.fusion.data.PaymentType;
import au.com.dmg.fusion.data.SaleCapability;
import au.com.dmg.fusion.data.TerminalEnvironment;
import au.com.dmg.fusion.data.UnitOfMeasure;
import au.com.dmg.fusion.exception.FusionException;
import au.com.dmg.fusion.request.SaleTerminalData;
import au.com.dmg.fusion.request.SaleToPOIRequest;
import au.com.dmg.fusion.request.aborttransactionrequest.AbortTransactionRequest;
import au.com.dmg.fusion.request.loginrequest.LoginRequest;
import au.com.dmg.fusion.request.loginrequest.SaleSoftware;
import au.com.dmg.fusion.request.paymentrequest.AmountsReq;
import au.com.dmg.fusion.request.paymentrequest.PaymentData;
import au.com.dmg.fusion.request.paymentrequest.PaymentInstrumentData;
import au.com.dmg.fusion.request.paymentrequest.PaymentRequest;
import au.com.dmg.fusion.request.paymentrequest.PaymentTransaction;
import au.com.dmg.fusion.request.paymentrequest.SaleData;
import au.com.dmg.fusion.request.paymentrequest.SaleItem;
import au.com.dmg.fusion.request.paymentrequest.SaleTransactionID;
import au.com.dmg.fusion.request.transactionstatusrequest.MessageReference;
import au.com.dmg.fusion.request.transactionstatusrequest.TransactionStatusRequest;
import au.com.dmg.fusion.response.EventNotification;
import au.com.dmg.fusion.response.Response;
import au.com.dmg.fusion.response.SaleToPOIResponse;
import au.com.dmg.fusion.response.TransactionStatusResponse;
import au.com.dmg.fusion.response.paymentresponse.PaymentReceipt;
import au.com.dmg.fusion.response.paymentresponse.PaymentResponse;
import au.com.dmg.fusion.response.paymentresponse.PaymentResult;
import au.com.dmg.fusion.util.MessageHeaderUtil;

//TODO: Fix - Timer is not starting if there's no connection
public class PaymentActivity extends AppCompatActivity {

    ExecutorService executorService;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");//
    private long pressedTime;

    String providerIdentification;
    String applicationName;
    String softwareVersion;
    String certificationCode;
    String saleID;
    String poiID;
    String kek;
    boolean useTestEnvironment = true;

    //Timer settings; Update as needed.
    long loginTimeout = 60000;
    long paymentTimeout = 60000; //60000
    long errorHandlingTimeout = 90000; //90000
    long prevSecond; // reference for counting second passed

    boolean waitingForResponse;
    int secondsRemaining;
    MessageCategory currentTransaction = MessageCategory.Login;
    String currentServiceID;

    EditText inputItemAmount;
    EditText inputTipAmount;
    EditText inputProductCode;

    TextView jsonLogs;

    TextView respUiHeader;
    TextView respUiDetail;
    TextView respReceipt;
    TextView timer;

    EditText respAuthourizedAmount;
    EditText respTipAmount;
    EditText respSurchargeAmount;
    EditText respMaskedPAN;
    EditText respPaymentBrand;
    EditText respEntryMode;
    EditText respServiceID;


    Button btnLogin;
    Button btnPurchase;
    Button btnRefund;

    FusionClient fusionClient;
    Button btnCancel;
    ProgressBar progressCircle;

    String abortReason = "";

    void initUI (){
        inputItemAmount = findViewById(R.id.input_item_amount);
        inputTipAmount = findViewById(R.id.input_tip_amount);
        inputProductCode = findViewById(R.id.input_product_code);

        jsonLogs = findViewById(R.id.edit_text_json_logs);
        jsonLogs.setMovementMethod(new ScrollingMovementMethod());


        respUiHeader = findViewById(R.id.text_view_ui_header);
        respUiDetail = findViewById(R.id.text_view_ui_details);
        respReceipt = findViewById(R.id.text_view_receipt);
        respReceipt.setMovementMethod(new ScrollingMovementMethod());

        respAuthourizedAmount = findViewById(R.id.response_authorize_amount_value);
        respTipAmount = findViewById(R.id.response_tip_amount_value);
        respSurchargeAmount = findViewById(R.id.response_surcharge_amount_value);
        respMaskedPAN = findViewById(R.id.response_masked_pan);
        respPaymentBrand = findViewById(R.id.response_payment_brand);
        respEntryMode = findViewById(R.id.response_entry_mode);
        respServiceID = findViewById(R.id.response_service_id);

        timer = findViewById(R.id.text_timer);
    }

    private void initFusionClient() {
        providerIdentification = Settings.getProviderIdentification();
        applicationName = Settings.getApplicationName();
        softwareVersion = Settings.getSoftwareVersion();
        certificationCode = Settings.getCertificationCode();
        saleID = Settings.getSaleId();
        poiID = Settings.getPoiId();
        kek = Settings.getKek();

        this.fusionClient = new FusionClient(useTestEnvironment); //need to override this in production
        this.fusionClient.setSettings(saleID, poiID, kek); // replace with the Sale ID provided by DataMesh
    }

    @Override
    protected void onResume(){
        super.onResume();
        initFusionClient();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        initFusionClient();

        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(v -> {
            executorService = Executors.newSingleThreadExecutor();
            executorService.submit(()-> doLogin());
        });

        btnPurchase = findViewById(R.id.btnPurchase);
        btnPurchase.setOnClickListener(v -> {
            executorService = Executors.newSingleThreadExecutor();
            executorService.submit(()-> doPayment());
        });

        btnRefund = findViewById(R.id.btnRefund);
        btnRefund.setOnClickListener(v -> {
            executorService = Executors.newSingleThreadExecutor();
            executorService.submit(()-> doRefund());
        });

        btnCancel = findViewById(R.id.btnCancel);
        progressCircle = findViewById(R.id.progressCircle);
        initUI();
    }


    /* SaleToPOI Listener */
    void Listen(){
        try {
            //Update timer text
            prevSecond = computeSecondsRemaining(prevSecond);

            SaleToPOI saleToPOI = null;
            saleToPOI = fusionClient.readMessage();

            if(saleToPOI==null) return;

            log("Response Received: \n" + saleToPOI.toJson());

            FusionMessageHandler fmh = new FusionMessageHandler();
            FusionMessageResponse fmr = null;
            if(saleToPOI instanceof SaleToPOIRequest) {

                fmr = fmh.handle((SaleToPOIRequest) saleToPOI);

                /* DISPLAY SaleToPOIRequest*/
                FusionMessageResponse finalFmr = fmr;
                runOnUiThread(() -> {
                    respUiDetail.setText(finalFmr.displayMessage);
                });

                //Reset timeout (Not applicable to transaction status)
                if(currentTransaction.equals(MessageCategory.Payment)){
                    secondsRemaining = (int) (paymentTimeout/1000); //Converting to seconds
                }
                waitingForResponse=true;

            }

            if (saleToPOI instanceof SaleToPOIResponse) {
                System.out.println("LISTEN SaleToPOIResponse");
                fmr = fmh.handle((SaleToPOIResponse) saleToPOI);

                //Ignore response if it's not the current transaction
                if(fmr.messageCategory!=currentTransaction && fmr.messageCategory!= MessageCategory.Event) {
                    log("Ignoring Response above... waiting for " + currentTransaction +", received "+ fmr.messageCategory);
                    return;
                }

                switch (fmr.messageCategory){
                    case Event: // This only logs an Event
                        SaleToPOIResponse spr = (SaleToPOIResponse) fmr.saleToPOI;
                        EventNotification eventNotification = spr.getEventNotification();
                        log("Ignoring Event below...\n" +
                                "spr.toJson()\n" +
                                "Event Details: " + eventNotification.getEventDetails());
                        break;
                    case Login:
                        displayLoginResponseMessage(fmr);
                        break;
                    case Payment:
                        displayPaymentResponseMessage(fmr);
                        break;
                    case TransactionStatus:
                        handleTransactionResponseMessage(fmr);
                        break;
                }
            }
        } catch (FusionException e) {
//            e.printStackTrace();
            // Should not loop if there's an error. e.g. Socket Disconnection
            endLog(String.format("Stopped listening to message. Reason:\n %s", e), true);
            if(currentTransaction!=MessageCategory.TransactionStatus){
                System.out.println("CURRENT SERVICE ID: " + this.currentServiceID);
                executorService.shutdownNow();
                executorService = Executors.newSingleThreadExecutor();
                executorService.submit(()->
                        checkTransactionStatus(this.currentServiceID, "Websocket connection interrupted"));
            }
        }
    }

    private void displayLoginResponseMessage(FusionMessageResponse fmr) {
        endTransactionUi();
        runOnUiThread(() -> respUiHeader.setText(fmr.displayMessage));
        waitingForResponse=false;
    }

    private void displayPaymentResponseMessage(FusionMessageResponse fmr) {
        endTransactionUi();

        PaymentResponse paymentResponse = ((SaleToPOIResponse)fmr.saleToPOI).getPaymentResponse();
        PaymentType paymentType = paymentResponse.getPaymentResult().getPaymentType();
        PaymentResult paymentResult = paymentResponse.getPaymentResult();
        runOnUiThread(() ->{
        if (paymentType.equals(PaymentType.Normal)){

                respUiHeader.setText(fmr.displayMessage);

                respAuthourizedAmount.setText(paymentResult.getAmountsResp().getAuthorizedAmount().toString());
                respTipAmount.setText(paymentResult.getAmountsResp().getTipAmount().toString());
                respSurchargeAmount.setText(paymentResult.getAmountsResp().getSurchargeAmount().toString());
                respMaskedPAN.setText(paymentResult.getPaymentInstrumentData().getCardData().getMaskedPAN());
                respPaymentBrand.setText(paymentResult.getPaymentInstrumentData().getCardData().getPaymentBrand().toString());
                respEntryMode.setText(paymentResult.getPaymentInstrumentData().getCardData().getEntryMode().toString());
                respServiceID.setText(fmr.saleToPOI.getMessageHeader().getServiceID());
                // Receipt
                PaymentReceipt paymentReceipt = paymentResponse.getPaymentReceipt().get(0);
                String OutputXHTML = paymentReceipt.getReceiptContentAsHtml();
                respReceipt.setText(HtmlCompat.fromHtml(OutputXHTML, 0));

        } else if(paymentType.equals(PaymentType.Refund)){
            respUiHeader.setText(fmr.displayMessage);

            respAuthourizedAmount.setText(paymentResult.getAmountsResp().getAuthorizedAmount().toString());
            respTipAmount.setText("0.00");
            respSurchargeAmount.setText(paymentResult.getAmountsResp().getSurchargeAmount().toString());
            respMaskedPAN.setText(paymentResult.getPaymentInstrumentData().getCardData().getMaskedPAN());
            respPaymentBrand.setText(paymentResult.getPaymentInstrumentData().getCardData().getPaymentBrand().toString());
            respEntryMode.setText(paymentResult.getPaymentInstrumentData().getCardData().getEntryMode().toString());
            respServiceID.setText(fmr.saleToPOI.getMessageHeader().getServiceID());
            // Receipt
            PaymentReceipt paymentReceipt = paymentResponse.getPaymentReceipt().get(0);
            String OutputXHTML = paymentReceipt.getReceiptContentAsHtml();
            respReceipt.setText(HtmlCompat.fromHtml(OutputXHTML, 0));
        }

        });
        waitingForResponse=false;
    }

    //Currently only called from transactionstatus response
    private void displayPaymentResponseMessage(PaymentResponse pr, MessageHeader mh) {
        endTransactionUi();
        runOnUiThread(() ->{
            respUiHeader.setText("PAYMENT " + pr.getResponse().getResult().toString().toUpperCase());


            PaymentResult paymentResult = pr.getPaymentResult();

            respAuthourizedAmount.setText(paymentResult.getAmountsResp().getAuthorizedAmount().toString());
            respTipAmount.setText(paymentResult.getAmountsResp().getTipAmount().toString());
            respSurchargeAmount.setText(paymentResult.getAmountsResp().getSurchargeAmount().toString());
            respMaskedPAN.setText(paymentResult.getPaymentInstrumentData().getCardData().getMaskedPAN());
            respPaymentBrand.setText(paymentResult.getPaymentInstrumentData().getCardData().getPaymentBrand().toString());
            respEntryMode.setText(paymentResult.getPaymentInstrumentData().getCardData().getEntryMode().toString());
            respServiceID.setText(mh.getServiceID());

            //Receipt
            PaymentReceipt paymentReceipt = pr.getPaymentReceipt().get(0);
            String OutputXHTML = paymentReceipt.getReceiptContentAsHtml();
            respReceipt.setText(HtmlCompat.fromHtml(OutputXHTML, 0));
        });
        waitingForResponse=false;
    }

    // Only called when there is no repeated message response on the transaction status
    private void displayTransactionResponseMessage(ErrorCondition errorCondition, String additionalResponse){
        endTransactionUi();
        runOnUiThread(()->{
            respUiHeader.setText(errorCondition.toString() + " - " + additionalResponse);
        });
    }

    private void handleTransactionResponseMessage(FusionMessageResponse fmr) {
        // TODO: handle transaction status response for others. currently designed for Payment only.
        TransactionStatusResponse transactionStatusResponse = null;
        Response responseBody = null;

                if (fmr.isSuccessful) {
                    transactionStatusResponse = ((SaleToPOIResponse)fmr.saleToPOI).getTransactionStatusResponse();
                    responseBody = transactionStatusResponse.getResponse();
                    log(String.format("Transaction Status Result: %s ", responseBody.getResult()));

                    PaymentResponse paymentResponse = transactionStatusResponse.getRepeatedMessageResponse().getRepeatedResponseMessageBody().getPaymentResponse();
                    MessageHeader paymentMessageHeader = transactionStatusResponse.getRepeatedMessageResponse().getMessageHeader();
                    displayPaymentResponseMessage(paymentResponse, paymentMessageHeader);

                } else if (fmr.errorCondition == ErrorCondition.InProgress) {
                    log("Transaction in progress...");
                    if(secondsRemaining>10){
                        errorHandlingTimeout = (secondsRemaining - 10) * 1000; //decrement errorHandlingTimeout so it will not reset after waiting
                        log("Sending another transaction status request after 10 seconds...\n" +
                                "Remaining seconds until error handling timeout: " + secondsRemaining);
                        try {
                            TimeUnit.SECONDS.sleep(10);
                            this.executorService.shutdownNow();
                            executorService = Executors.newSingleThreadExecutor();
                            executorService.submit(()->checkTransactionStatus(currentServiceID, ""));
                        } catch (InterruptedException e) {
                            endLog(e);
                        }
                    }

                } else {
                    transactionStatusResponse = ((SaleToPOIResponse)fmr.saleToPOI).getTransactionStatusResponse();
                    responseBody = transactionStatusResponse.getResponse();
                    endLog(String.format("Error Condition: %s, Additional Response: %s",
                            responseBody.getErrorCondition(), responseBody.getAdditionalResponse()), true);

                    displayTransactionResponseMessage(responseBody.getErrorCondition(), responseBody.getAdditionalResponse());
                }


    }

    private void doLogin() {
        try {
            currentServiceID = MessageHeaderUtil.generateServiceID();
            currentTransaction = MessageCategory.Login;
            startTransactionUi();

            //Set timeout
            prevSecond = System.currentTimeMillis();
            secondsRemaining = (int) (loginTimeout/1000);

            LoginRequest loginRequest = buildLoginRequest();
            log("Sending message to websocket server: " + "\n" + loginRequest.toJson());
            fusionClient.sendMessage(loginRequest, currentServiceID);

            // Loop for Listener
            waitingForResponse = true;
            while(waitingForResponse) {
                Listen();
                if(secondsRemaining<1) {
                    endLog("Login Request Timeout...", true);
                    endTransactionUi();
                    break;
                }
            }
        } catch (ConfigurationException e) {
            endLog(e);
        }
    }

    private void doPayment() {
        try {
            currentServiceID = MessageHeaderUtil.generateServiceID();
            currentTransaction = MessageCategory.Payment;

            startTransactionUi();
            clearLog();
            // Set timeout
            prevSecond = System.currentTimeMillis();
            secondsRemaining = (int) (paymentTimeout/1000);

            PaymentRequest paymentRequest = buildPaymentRequest();
            log("Sending message to websocket server: " + "\n" + paymentRequest.toJson());
            fusionClient.sendMessage(paymentRequest, currentServiceID);

            waitingForResponse = true;
            while(waitingForResponse) {
                Listen();
                if(secondsRemaining < 1) {
                    abortReason = "Timeout";
                    endLog("Payment Request Timeout...", true);
                    checkTransactionStatus(currentServiceID, abortReason);
                    break;
                }
            }
        } catch (ConfigurationException e) {
            endLog(String.format("Exception: %s", e), true);
            abortReason = "Other Exception";
            checkTransactionStatus(currentServiceID, abortReason);
        } catch (FusionException e){
            endLog(String.format("FusionException: %s. Resending the Request...", e), true);
            // Continue the timer
            paymentTimeout = secondsRemaining * 1000L;
            doPayment();
        }
    }

    private void doAbort(String serviceID, String abortReason){
        endTransactionUi();

        runOnUiThread(()-> {
           respUiHeader.setText("ABORTING TRANSACTION");
           respUiDetail.setText("");
        });
        hideProgressCircle(false);
        AbortTransactionRequest abortTransactionPOIRequest = buildAbortRequest(serviceID, abortReason);

        log("Sending abort message to websocket server: " + "\n" + abortTransactionPOIRequest.toJson());
        fusionClient.sendMessage(abortTransactionPOIRequest);
    }

    private void checkTransactionStatus(String serviceID, String abortReason) {
        try {
            currentTransaction = MessageCategory.TransactionStatus;
            startTransactionUi();
            hideCancelBtn(true);
            if (abortReason != "") {
                doAbort(serviceID, abortReason);
            }
            runOnUiThread(()-> {
                respUiHeader.setText("CHECKING TRANSACTION STATUS");
            });
            TransactionStatusRequest transactionStatusRequest = buildTransactionStatusRequest(serviceID);

            log("Sending transaction status request to check status of payment... " + "\n" + transactionStatusRequest.toJson());
            fusionClient.sendMessage(transactionStatusRequest);

            // Set timeout
            prevSecond = System.currentTimeMillis();
            secondsRemaining = (int) (errorHandlingTimeout/1000);

            waitingForResponse = true;
            while(waitingForResponse) {
                Listen();
                if(secondsRemaining < 1) {
                    endTransactionUi();
                    runOnUiThread(()->{
                        respUiHeader.setText("Time Out");
                        respUiDetail.setText("Please check Satellite Transaction History");
                        endLog("Transaction Status Request Timeout...", true);
                    });
                    break;
                }
            }
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private void doRefund() {
        try {
            currentServiceID = MessageHeaderUtil.generateServiceID();
            currentTransaction = MessageCategory.Payment;

            startTransactionUi();
            clearLog();
            // Set timeout
            prevSecond = System.currentTimeMillis();
            secondsRemaining = (int) (paymentTimeout/1000);

            PaymentRequest refundRequest = buildRefundRequest();
            log("Sending message to websocket server: " + "\n" + refundRequest.toJson());
            fusionClient.sendMessage(refundRequest, currentServiceID);

            waitingForResponse = true;
            while(waitingForResponse) {
                Listen();
                if(secondsRemaining < 1) {
                    abortReason = "Timeout";
                    endLog("Refund Request Timeout...", true);
                    checkTransactionStatus(currentServiceID, abortReason);
                    break;
                }
            }
        } catch (ConfigurationException e) {
            endLog(String.format("Exception: %s", e), true);
            abortReason = "Other Exception";
            checkTransactionStatus(currentServiceID, abortReason);
        } catch (FusionException e){
            endLog(String.format("FusionException: %s. Resending the Request...", e), true);
            // Continue the timer
            paymentTimeout = secondsRemaining * 1000L;
            doPayment();
        }
    }

    private LoginRequest buildLoginRequest() throws ConfigurationException {
        SaleSoftware saleSoftware = new SaleSoftware.Builder()//
                .providerIdentification(providerIdentification)//
                .applicationName(applicationName)//
                .softwareVersion(softwareVersion)//
                .certificationCode(certificationCode)//
                .build();

        SaleTerminalData saleTerminalData = new SaleTerminalData.Builder()//
                .terminalEnvironment(TerminalEnvironment.SemiAttended)//
                .saleCapabilities(Arrays.asList(SaleCapability.CashierStatus, SaleCapability.CustomerAssistance,
                        SaleCapability.PrinterReceipt))//
                .build();

        LoginRequest loginRequest = new LoginRequest.Builder()//
                .dateTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()))//
                .saleSoftware(saleSoftware)//
                .saleTerminalData(saleTerminalData)//
                .operatorLanguage("en")//
                .build();

        return loginRequest;
    }

    private PaymentRequest buildPaymentRequest() throws ConfigurationException {
        BigDecimal inputAmount = new BigDecimal(inputItemAmount.getText().toString());
        BigDecimal inputTip = new BigDecimal(inputTipAmount.getText().toString());
        String productCode = String.valueOf(inputProductCode.getText());

        BigDecimal requestedAmount = inputAmount.add(inputTip);

         SaleTransactionID saleTransactionID = new SaleTransactionID.Builder()//
                .transactionID("transactionID" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()))////
                .timestamp(Instant.now()).build();

        SaleData saleData = new SaleData.Builder()//
                // .operatorID("")//
                .operatorLanguage("en")//
                .saleTransactionID(saleTransactionID)//
                .build();

        AmountsReq amountsReq = new AmountsReq.Builder()//
                .currency("AUD")//
                .requestedAmount(requestedAmount)//
                .tipAmount(inputTip)
                .build();

        SaleItem saleItem = new SaleItem.Builder()//
                .itemID(0)//
                .productCode(productCode)//
                .unitOfMeasure(UnitOfMeasure.Other)//
                .quantity(new BigDecimal(1))//
                .unitPrice(new BigDecimal(100.00))//
                .itemAmount(inputAmount)//
                .productLabel("Product Label")//
                .build();

        PaymentInstrumentData paymentInstrumentData = new PaymentInstrumentData.Builder()//
                .paymentInstrumentType(PaymentInstrumentType.Cash)//
                .build();

        PaymentData paymentData = new PaymentData.Builder()//
                .paymentType(PaymentType.Normal)//
                .paymentInstrumentData(paymentInstrumentData)//
                .build();

        PaymentTransaction paymentTransaction = new PaymentTransaction.Builder()//
                .amountsReq(amountsReq)//
                .addSaleItem(saleItem)//
                .build();

        PaymentRequest paymentRequest = new PaymentRequest.Builder()//
                .paymentTransaction(paymentTransaction)//
                .paymentData(paymentData)//
                .saleData(saleData).build();

        return paymentRequest;
    }

    private TransactionStatusRequest buildTransactionStatusRequest(String serviceID) throws ConfigurationException {
        MessageReference messageReference = new MessageReference.Builder()//
                .messageCategory(MessageCategory.Payment)//
                .POIID(poiID)//
                .saleID(saleID)//
                .serviceID(serviceID)//
                .build();

        TransactionStatusRequest transactionStatusRequest = new TransactionStatusRequest(messageReference);

        return transactionStatusRequest;
    }


    private AbortTransactionRequest buildAbortRequest(String referenceServiceID, String abortReason) {

        MessageReference messageReference = new MessageReference.Builder()
                .messageCategory(MessageCategory.Abort)
                .serviceID(referenceServiceID).build();

        AbortTransactionRequest abortTransactionRequest = new AbortTransactionRequest(messageReference, abortReason);

        return abortTransactionRequest;
    }

    private PaymentRequest buildRefundRequest() throws ConfigurationException {
        BigDecimal inputAmount = new BigDecimal(inputItemAmount.getText().toString());
        String productCode = String.valueOf(inputProductCode.getText());

        SaleTransactionID saleTransactionID = new SaleTransactionID.Builder()//
                .transactionID("transactionID" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()))////
                .timestamp(Instant.now()).build();

        SaleData saleData = new SaleData.Builder()//
                .operatorLanguage("en")//
                .saleTransactionID(saleTransactionID)//
                .build();

        AmountsReq amountsReq = new AmountsReq.Builder()//
                .currency("AUD")//
                .requestedAmount(inputAmount)//
                .build();

        SaleItem saleItem = new SaleItem.Builder()//
                .itemID(0)//
                .productCode(productCode)//
                .unitOfMeasure(UnitOfMeasure.Other)//
                .quantity(new BigDecimal(1))//
                .unitPrice(new BigDecimal(100.00))//
                .itemAmount(inputAmount)//
                .productLabel("Product Label")//
                .build();

        PaymentInstrumentData paymentInstrumentData = new PaymentInstrumentData.Builder()//
                .paymentInstrumentType(PaymentInstrumentType.Cash)//
                .build();

        PaymentData refundData = new PaymentData.Builder()//
                .paymentType(PaymentType.Refund)//
                .paymentInstrumentData(paymentInstrumentData)//
                .build();

        PaymentTransaction paymentTransaction = new PaymentTransaction.Builder()//
                .amountsReq(amountsReq)//
                .addSaleItem(saleItem)//
                .build();

        PaymentRequest refundRequest = new PaymentRequest.Builder()//
                .paymentTransaction(paymentTransaction)//
                .paymentData(refundData)//
                .saleData(saleData).build();

        return refundRequest;
    }

    private void startTransactionUi(){
        runOnUiThread(()->{
            if(currentTransaction==MessageCategory.Payment){
                btnCancel.setOnClickListener(v->doAbort(currentServiceID, "User Cancelled"));
                btnCancel.setVisibility(View.VISIBLE);
            }
            progressCircle.setVisibility(View.VISIBLE);
            respUiHeader.setText(currentTransaction.toString().toUpperCase() + " IN PROGRESS");
            respUiDetail.setText("...");
            btnLogin.setEnabled(false);
            btnPurchase.setEnabled(false);
            btnRefund.setEnabled(false);
        });
    }

    private void endTransactionUi(){
        runOnUiThread(()->{
            progressCircle.setVisibility(View.INVISIBLE);
            btnCancel.setVisibility(View.INVISIBLE);
            respUiDetail.setText("");
            timer.setText("0");

            btnLogin.setEnabled(true);
            btnPurchase.setEnabled(true);
            btnRefund.setEnabled(true);
        });
    }

    private void hideCancelBtn(Boolean doHide){
        runOnUiThread(()->{
           if(doHide){
               btnCancel.setVisibility(View.INVISIBLE);
           }else{
               btnCancel.setOnClickListener(v->doAbort(currentServiceID, "User Cancelled"));
               btnCancel.setVisibility(View.VISIBLE);
           }
        });
    }

    private void hideProgressCircle(Boolean doHide){
        runOnUiThread(()->{
            if(doHide){
                progressCircle.setVisibility(View.INVISIBLE);
            }else{
                progressCircle.setVisibility(View.VISIBLE);
            }
        });
    }

    public long computeSecondsRemaining(long start) {
        long currentTime = System.currentTimeMillis();
        long sec = (currentTime - start) / 1000;
        if(sec==1) {
            runOnUiThread(() ->timer.setText(String.valueOf(secondsRemaining--)));
            start = currentTime;
        }
        return start;
    }

    private void endLog(Exception ex){
        log(ex.getMessage());
        waitingForResponse = false;
    }

    private void endLog(String logData, Boolean stopWaiting) {
        runOnUiThread(() ->
            jsonLogs.append(
                    sdf.format(new Date(System.currentTimeMillis())) + ": \n" + logData + "\n\n") // 2021.03.24.16.34.26
        );
        if(stopWaiting){
            waitingForResponse = false;
        }
    }

    private void clearLog() {
        runOnUiThread(() ->
                jsonLogs.setText("")
        );
    }
    private void log(String logData) {
        System.out.println(sdf.format(new Date(System.currentTimeMillis())) + ": " + logData); // 2021.03.24.16.34.26
        runOnUiThread(() ->
                jsonLogs.append(sdf.format(new Date(System.currentTimeMillis())) + ": \n" + logData + "\n\n")
        );
    }

    @Override
    public void onBackPressed() {
        if (pressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
            finish();
        } else {
            Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
        pressedTime = System.currentTimeMillis();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.back, menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }
}