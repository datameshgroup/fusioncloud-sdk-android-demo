package au.com.dmg.fusioncloud.android.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
import au.com.dmg.fusion.data.MessageClass;
import au.com.dmg.fusion.data.MessageType;
import au.com.dmg.fusion.data.PaymentInstrumentType;
import au.com.dmg.fusion.data.PaymentType;
import au.com.dmg.fusion.data.SaleCapability;
import au.com.dmg.fusion.data.TerminalEnvironment;
import au.com.dmg.fusion.data.UnitOfMeasure;
import au.com.dmg.fusion.exception.FusionException;
import au.com.dmg.fusion.request.Request;
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
import au.com.dmg.fusion.response.paymentresponse.PaymentResponse;
import au.com.dmg.fusion.response.paymentresponse.PaymentResult;
import au.com.dmg.fusion.securitytrailer.SecurityTrailer;
import au.com.dmg.fusion.util.MessageHeaderUtil;
import au.com.dmg.fusion.util.SecurityTrailerUtil;
//TODO: Fix - Timer is not starting if there's no connection
public class PaymentActivity extends AppCompatActivity {

    ExecutorService executorService;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");//
    private long pressedTime;

    String mockHostProductCode = "DMGTC44857"; //Update this for Mock Host Testing
    String providerIdentification = "Company A"; // test environment only - replace for production
    String applicationName = "POS Retail"; // test environment only - replace for production
    String softwareVersion = "01.00.00"; // test environment only - replace for production
    String certificationCode = "98cf9dfc-0db7-4a92-8b8cb66d4d2d7169"; // test environment only - replace for production
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
    MessageCategory currentTransaction = MessageCategory.Login; // TODO: currentTransaction validation
    String currentServiceID;
    String referenceServiceID;

    EditText inputRequestedAmount;
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
    EditText respPaymentType;
    EditText respTransactionID;
    EditText respServiceID;


    Button btnLogin;
    Button btnPurchase;
    Button btnRefund;

    FusionClient fusionClient;
    Button btnCancel;
    ProgressBar progressCircle;


    void initUI (){
        inputRequestedAmount = findViewById(R.id.input_requested_amount);
        inputTipAmount = findViewById(R.id.input_tip_amount);
        inputProductCode = findViewById(R.id.input_product_code);

        jsonLogs = findViewById(R.id.edit_text_json_logs);
        jsonLogs.setMovementMethod(new ScrollingMovementMethod());


        respUiHeader = findViewById(R.id.text_view_ui_header);
        respUiDetail = findViewById(R.id.text_view_ui_details);
        respReceipt = findViewById(R.id.text_view_receipt);

        respAuthourizedAmount = findViewById(R.id.response_authorize_amount_value);
        respTipAmount = findViewById(R.id.response_tip_amount_value);
        respSurchargeAmount = findViewById(R.id.response_surcharge_amount_value);
        respMaskedPAN = findViewById(R.id.response_masked_pan);
        respPaymentBrand = findViewById(R.id.response_payment_brand);
        respEntryMode = findViewById(R.id.response_entry_mode);
        respPaymentType = findViewById(R.id.response_payment_type);
        respTransactionID = findViewById(R.id.response_transaction_id);
        respServiceID = findViewById(R.id.response_service_id);

        timer = findViewById(R.id.text_timer);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //these config values need to be configurable in POS
        saleID = "VA POS"; // Replace with your test SaleId provided by DataMesh
        poiID = "DMGVA002"; // Replace with your test POIID provided by DataMesh

        this.fusionClient = new FusionClient(useTestEnvironment); //need to override this in production
        kek = "44DACB2A22A4A752ADC1BBFFE6CEFB589451E0FFD83F8B21"; //for dev only, need to be replaced with prod value in prod
        this.fusionClient.setSettings(saleID, poiID, kek); // replace with the Sale ID provided by DataMesh


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
//        btnRefund.setOnClickListener(v -> doRefund());

        btnCancel = findViewById(R.id.btnCancel);
        progressCircle = findViewById(R.id.progressCircle);
        initUI();
    }


    /* UI Thread to display connection progress */
    Handler handler = new Handler();

    void Listen(){
        try {
            //Update timer text
            prevSecond = computeSecondsRemaining(prevSecond);
            handler.post(() ->timer.setText(String.valueOf(secondsRemaining)));

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
                handler.post(() -> {
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
                        log("Ignoring Event below...");
                        log(spr.toJson());
                        log("Event Details: " + eventNotification.getEventDetails());
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
            e.printStackTrace();
        }
    }

    private void displayLoginResponseMessage(FusionMessageResponse fmr) {
        // TODO Clean Validations here.
        endTransactionUi();
        handler.post(() -> respUiHeader.setText(fmr.displayMessage));
        waitingForResponse=false;
    }

    private void displayPaymentResponseMessage(FusionMessageResponse fmr) {
        //add receipt logic
        endTransactionUi();
        handler.post(() ->{
//            if(fmr.isSuccessful)
//            {
//                respUiHeader.setfo;
//            }
            respUiHeader.setText(fmr.displayMessage);

//            respReceipt.setText(msg.rece);
            PaymentResponse paymentResponse = ((SaleToPOIResponse)fmr.saleToPOI).getPaymentResponse();
            PaymentResult paymentResult = paymentResponse.getPaymentResult();

            respAuthourizedAmount.setText(paymentResult.getAmountsResp().getAuthorizedAmount().toString());
            respTipAmount.setText(paymentResult.getAmountsResp().getTipAmount().toString());
            respSurchargeAmount.setText(paymentResult.getAmountsResp().getSurchargeAmount().toString());
            respMaskedPAN.setText(paymentResult.getPaymentInstrumentData().getCardData().getMaskedPAN());
            respPaymentBrand.setText(paymentResult.getPaymentInstrumentData().getCardData().getPaymentBrand().toString());
            respEntryMode.setText(paymentResult.getPaymentInstrumentData().getCardData().getEntryMode().toString());
            respPaymentType.setText(paymentResult.getPaymentType().toString());
            respTransactionID.setText(paymentResponse.getPoiData().getPOITransactionID().getTransactionID());
            respServiceID.setText(fmr.saleToPOI.getMessageHeader().getServiceID());
        });
        waitingForResponse=false;
    }

    //Currently only called from transactionstatus response
    private void displayPaymentResponseMessage(PaymentResponse pr, MessageHeader mh) {
        //add receipt logic
        endTransactionUi();
        handler.post(() ->{
            respUiHeader.setText("PAYMENT " + pr.getResponse().getResult().toString().toUpperCase());

//            respReceipt.setText(msg.rece);
            PaymentResult paymentResult = pr.getPaymentResult();

            respAuthourizedAmount.setText(paymentResult.getAmountsResp().getAuthorizedAmount().toString());
            respTipAmount.setText(paymentResult.getAmountsResp().getTipAmount().toString());
            respSurchargeAmount.setText(paymentResult.getAmountsResp().getSurchargeAmount().toString());
            respMaskedPAN.setText(paymentResult.getPaymentInstrumentData().getCardData().getMaskedPAN());
            respPaymentBrand.setText(paymentResult.getPaymentInstrumentData().getCardData().getPaymentBrand().toString());
            respEntryMode.setText(paymentResult.getPaymentInstrumentData().getCardData().getEntryMode().toString());
            respPaymentType.setText(paymentResult.getPaymentType().toString());
            respTransactionID.setText(pr.getPoiData().getPOITransactionID().getTransactionID());
            respServiceID.setText(mh.getServiceID());
        });
        waitingForResponse=false;
    }

    // Only called when there is no repeated message response on the transaction status
    private void displayTransactionResponseMessage(ErrorCondition errorCondition, String additionalResponse){
        endTransactionUi();
        handler.post(()->{
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

//                    Response paymentResponseBody = transactionStatusResponse
//                            .getRepeatedMessageResponse().getRepeatedResponseMessageBody()
//                            .getPaymentResponse().getResponse();
//                    log(String.format("Actual Payment Result: %s",
//                            paymentResponseBody.getResult()), true);
                    PaymentResponse paymentResponse = transactionStatusResponse.getRepeatedMessageResponse().getRepeatedResponseMessageBody().getPaymentResponse();
                    MessageHeader paymentMessageHeader = transactionStatusResponse.getRepeatedMessageResponse().getMessageHeader();
                    displayPaymentResponseMessage(paymentResponse, paymentMessageHeader);

                } else if (fmr.errorCondition == ErrorCondition.InProgress) {
                    log("Transaction in progress...");
                    if(secondsRemaining>10){
                        errorHandlingTimeout = (secondsRemaining - 10) * 1000; //decrement errorHandlingTimeout
                        log("Sending another transaction status request after 10 seconds...");
                        log("Remaining seconds until error handling timeout: " + secondsRemaining);
                        try {
                            TimeUnit.SECONDS.sleep(10);
                            checkTransactionStatus(currentServiceID, "");
                        } catch (InterruptedException e) {
                            log(e);
                        }
                    }

                } else {
                    transactionStatusResponse = ((SaleToPOIResponse)fmr.saleToPOI).getTransactionStatusResponse();
                    responseBody = transactionStatusResponse.getResponse();
                    log(String.format("Error Condition: %s, Additional Response: %s",
                            responseBody.getErrorCondition(), responseBody.getAdditionalResponse()), true);

                    displayTransactionResponseMessage(responseBody.getErrorCondition(), responseBody.getAdditionalResponse());
                }


    }

    private void doLogin() {
        currentServiceID = MessageHeaderUtil.generateServiceID();
        try {
            currentTransaction = MessageCategory.Login;
            startTransactionUi();

            //Set timeout
            prevSecond = System.currentTimeMillis();
            secondsRemaining = (int) (loginTimeout/1000);

            SaleToPOIRequest loginRequest = buildLoginRequest(currentServiceID);
            log("Sending message to websocket server: " + "\n" + loginRequest.toJson());
            fusionClient.connect();
            fusionClient.sendMessage(loginRequest);

            // Loop for Listener
            waitingForResponse = true;
            while(waitingForResponse) {
                Listen();
                if(secondsRemaining<1) {
                    log("Login Request Timeout...", true);
                    break;
                }
            }
        } catch (ConfigurationException e) {
            log(e);
        }
    }

    private void doPayment() {

        currentServiceID = MessageHeaderUtil.generateServiceID();

        //Preparing for Transaction Status Check
        String abortReason = "";

        try {
            // Show Cancel button
            currentTransaction = MessageCategory.Payment;
            startTransactionUi();
            clearLog();
            // Set timeout
            prevSecond = System.currentTimeMillis();
            secondsRemaining = (int) (paymentTimeout/1000);

            SaleToPOIRequest paymentRequest = buildPaymentRequest(currentServiceID);
            log("Sending message to websocket server: " + "\n" + paymentRequest.toJson());
            fusionClient.connect();
            fusionClient.sendMessage(paymentRequest);

            waitingForResponse = true;
            while(waitingForResponse) {
                Listen();
                if(secondsRemaining < 1) {
                    abortReason = "Timeout";
                    log("Payment Request Timeout...", true);
                    checkTransactionStatus(currentServiceID, abortReason);
                    break;
                }
            }
        } catch (ConfigurationException e) {
            log(String.format("Exception: %s", e.toString()), true);
            abortReason = "Other Exception";
            checkTransactionStatus(currentServiceID, abortReason);
        }
    }

    private void doAbort(String serviceID, String abortReason){
//        waitingForResponse=false;

        endTransactionUi();

        handler.post(()-> {
           respUiHeader.setText("ABORTING TRANSACTION");
        });
        SaleToPOIRequest abortTransactionPOIRequest = buildAbortRequest(serviceID, abortReason);

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
            handler.post(()-> {
                respUiHeader.setText("CHECKING TRANSACTION STATUS");
            });
            SaleToPOIRequest transactionStatusRequest = buildTransactionStatusRequest(serviceID);

            log("Sending transaction status request to check status of payment... " + "\n" + transactionStatusRequest.toJson());
            fusionClient.connect();
            fusionClient.sendMessage(transactionStatusRequest);

            // Set timeout
            prevSecond = System.currentTimeMillis();
            secondsRemaining = (int) (errorHandlingTimeout/1000);

            waitingForResponse = true;
            while(waitingForResponse) {
                Listen();
                if(secondsRemaining < 1) {
                    endTransactionUi();
                    handler.post(()->{
                        respUiHeader.setText("Time Out");
                        respUiDetail.setText("Please check Satellite Transaction History");
                        log("Transaction Status Request Timeout...", true);
                    });
                    break;
                }
            }
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private SaleToPOIRequest buildLoginRequest(String serviceID) throws ConfigurationException {
        // Login Request
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

        // Message Header
        MessageHeader messageHeader = new MessageHeader.Builder()//
                .messageClass(MessageClass.Service)//
                .messageCategory(MessageCategory.Login)//
                .messageType(MessageType.Request)//
                .serviceID(serviceID)//
                .saleID(saleID)//
                .POIID(poiID)//
                .build();

        SecurityTrailer securityTrailer = null;
        try {
            securityTrailer = SecurityTrailerUtil.generateSecurityTrailer(messageHeader, loginRequest,
                    useTestEnvironment);
        } catch(Exception e){
            System.out.println("securityTrailer ---- printStackTrace2" + e.toString());
        }

        SaleToPOIRequest saleToPOI = new SaleToPOIRequest.Builder()//
                .messageHeader(messageHeader)//
                .request(loginRequest)//
                .securityTrailer(securityTrailer)//
                .build();

        return saleToPOI;
    }

    private  SaleToPOIRequest buildPaymentRequest(String serviceID) throws ConfigurationException {
        String inputAmount = String.valueOf(inputRequestedAmount.getText());
        String inputTip = String.valueOf(inputTipAmount.getText());
        String productCode = String.valueOf(inputProductCode.getText());

        // Payment Request
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
                .requestedAmount(new BigDecimal(inputAmount))//
                .build();

        SaleItem saleItem = new SaleItem.Builder()//
                .itemID(0)//
                .productCode(productCode)//
                .unitOfMeasure(UnitOfMeasure.Other)//
                .quantity(new BigDecimal(1))//
                .unitPrice(new BigDecimal(100.00))//
                .itemAmount(new BigDecimal(inputAmount))//
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

        // Message Header
        MessageHeader messageHeader = new MessageHeader.Builder()//
                .messageClass(MessageClass.Service)//
                .messageCategory(MessageCategory.Payment)//
                .messageType(MessageType.Request)//
                .serviceID(serviceID)//
                .saleID(saleID)//
                .POIID(poiID)//
                .build();

        SecurityTrailer securityTrailer = generateSecurityTrailer(messageHeader, paymentRequest);

        SaleToPOIRequest saleToPOI = new SaleToPOIRequest.Builder()//
                .messageHeader(messageHeader)//
                .request(paymentRequest)//
                .securityTrailer(securityTrailer)//
                .build();

        return saleToPOI;
    }

    private SaleToPOIRequest buildTransactionStatusRequest(String serviceID) throws ConfigurationException {
        currentServiceID = MessageHeaderUtil.generateServiceID();
        referenceServiceID = serviceID;

        // Transaction Status Request
        MessageReference messageReference = new MessageReference.Builder()//
                .messageCategory(MessageCategory.Payment)//
                .POIID(poiID)//
                .saleID(saleID)//
                .serviceID(referenceServiceID)//
                .build();

        TransactionStatusRequest transactionStatusRequest = new TransactionStatusRequest(messageReference);

        // Message Header
        MessageHeader messageHeader = new MessageHeader.Builder()//
                .messageClass(MessageClass.Service)//
                .messageCategory(MessageCategory.TransactionStatus)//
                .messageType(MessageType.Request)//
                .serviceID(currentServiceID)//
                .saleID(saleID)//
                .POIID(poiID)//
                .build();

        SecurityTrailer securityTrailer = generateSecurityTrailer(messageHeader, transactionStatusRequest);

        SaleToPOIRequest saleToPOI = new SaleToPOIRequest.Builder()//
                .messageHeader(messageHeader)//
                .request(transactionStatusRequest)//
                .securityTrailer(securityTrailer)//
                .build();

        return saleToPOI;
    }

    private SaleToPOIRequest buildAbortRequest(String paymentServiceID, String abortReason) {
        currentServiceID = MessageHeaderUtil.generateServiceID();
        referenceServiceID = paymentServiceID;
        // Message Header
        MessageHeader messageHeader = new MessageHeader.Builder()//
                .messageClass(MessageClass.Service)//
                .messageCategory(MessageCategory.Abort)//
                .messageType(MessageType.Request)//
                .serviceID(currentServiceID)//
                .saleID(saleID)//
                .POIID(poiID)//
                .build();

        MessageReference messageReference = new MessageReference.Builder().messageCategory(MessageCategory.Abort)
                .serviceID(referenceServiceID).build();

        AbortTransactionRequest abortTransactionRequest = new AbortTransactionRequest(messageReference, abortReason);

        SecurityTrailer securityTrailer = generateSecurityTrailer(messageHeader, abortTransactionRequest);

        SaleToPOIRequest saleToPOI = new SaleToPOIRequest.Builder()//
                .messageHeader(messageHeader)//
                .request(abortTransactionRequest)//
                .securityTrailer(securityTrailer)//
                .build();

        return saleToPOI;
    }

    private SecurityTrailer generateSecurityTrailer(MessageHeader messageHeader, Request request){
        return SecurityTrailerUtil.generateSecurityTrailer(messageHeader, request, useTestEnvironment);
    }

    private void startTransactionUi(){
        handler.post(()->{
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
        handler.post(()->{
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
        handler.post(()->{
           if(doHide){
               btnCancel.setVisibility(View.INVISIBLE);
           }else{
               btnCancel.setOnClickListener(v->doAbort(currentServiceID, "User Cancelled"));
               btnCancel.setVisibility(View.VISIBLE);
           }
        });
    }

    private void hideProgressCircle(Boolean doHide){
        handler.post(()->{
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
            secondsRemaining--;
            start = currentTime;
        }
        return start;
    }

    private void log(Exception ex){
        log(ex.getMessage());
        waitingForResponse = false;
    }

    private void log(String logData, Boolean stopWaiting) {
        handler.post(() ->
            jsonLogs.append(
    //        System.out.println(
                    sdf.format(new Date(System.currentTimeMillis())) + ": \n" + logData + "\n\n") // 2021.03.24.16.34.26
        );
        if(stopWaiting){
            waitingForResponse = false;
        }
    }

    private void clearLog() {
        handler.post(() ->
                jsonLogs.setText("")
        );
    }
    private void log(String logData) {
        System.out.println(sdf.format(new Date(System.currentTimeMillis())) + ": " + logData); // 2021.03.24.16.34.26
        handler.post(() ->
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

}