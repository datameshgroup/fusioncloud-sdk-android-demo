package fusioncloud.sdk.android;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.posonandroidva.R;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.naming.ConfigurationException;
import javax.websocket.DeploymentException;

import au.com.dmg.fusion.MessageHeader;
import au.com.dmg.fusion.client.FusionClient;
import au.com.dmg.fusion.config.FusionClientConfig;
import au.com.dmg.fusion.config.KEKConfig;
import au.com.dmg.fusion.config.SaleSystemConfig;
import au.com.dmg.fusion.data.ErrorCondition;
import au.com.dmg.fusion.data.MessageCategory;
import au.com.dmg.fusion.data.MessageClass;
import au.com.dmg.fusion.data.MessageType;
import au.com.dmg.fusion.data.PaymentInstrumentType;
import au.com.dmg.fusion.data.PaymentType;
import au.com.dmg.fusion.data.SaleCapability;
import au.com.dmg.fusion.data.TerminalEnvironment;
import au.com.dmg.fusion.data.UnitOfMeasure;
import au.com.dmg.fusion.exception.NotConnectedException;
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
import au.com.dmg.fusion.response.Response;
import au.com.dmg.fusion.response.ResponseResult;
import au.com.dmg.fusion.response.SaleToPOIResponse;
import au.com.dmg.fusion.response.paymentresponse.PaymentReceipt;
import au.com.dmg.fusion.securitytrailer.SecurityTrailer;
import au.com.dmg.fusion.util.MessageHeaderUtil;
import au.com.dmg.fusion.util.SecurityTrailerUtil;

@RequiresApi(api = Build.VERSION_CODES.N)
public class ActivityLoading extends AppCompatActivity {
    private static final FusionClient fusionClient = new FusionClient();
    private TextView tvProgressMessage;
    private TextView tvProgressHeader;
    private Button btnCancelTxn;
    private TextView tvTimer;
    long cdtErrorHandlerLimit = 60000;
    long cdtPaymentLimit = 30000;


    public boolean connected;
    public boolean loginsuccess;
    public boolean paymentsuccess;
    public boolean reconnecting;
    public boolean errorhandling;
    public boolean retry;
    public boolean wait;


    private Thread mainThread;
    ExecutorService service;
    ExecutorService errorhandlingService;

    String serviceID;
    public static String SALE_ID;
    public static String POI_ID;

    public String displayMsg;
    public String displayHd;
    String txnType;
    String maintxnType;
    SaleToPOIResponse response = null;
    SaleToPOIRequest request = null;

    private String lastServiceID = null;
    BigDecimal bTotal = BigDecimal.valueOf(0);
    BigDecimal bDiscount = BigDecimal.valueOf(0);
    BigDecimal bTip = BigDecimal.valueOf(0);

    public void initValues()
    {
        connected=false;
        loginsuccess = false;
        paymentsuccess = false;
        reconnecting = false;
        errorhandling = false;
        retry = true;
        wait=true;

        service = null;
        errorhandlingService = null;
        mainThread = null;

        displayMsg = "Connecting to pinpad...";
        displayHd = "PAYMENT IN PROGRESS";
        txnType = "";
        response = null;

    }

    private static void initConfig() {
        SALE_ID = Settings.SALE_ID;
        POI_ID = Settings.POI_ID;
        String serverDomain = Settings.serverDomain;
        String socketProtocol = Settings.socketProtocol;

        String kekValue = Settings.kekValue;
        String keyIdentifier = Settings.keyIdentifier;
        String keyVersion = Settings.keyVersion;

        String providerIdentification = Settings.providerIdentification;
        String applicationName = Settings.applicationName;
        String softwareVersion = Settings.softwareVersion;
        String certificationCode = Settings.certificationCode;
        String ENV = Settings.ENV;

        try {
            FusionClientConfig.init(serverDomain, socketProtocol, ENV);
            KEKConfig.init(kekValue, keyIdentifier, keyVersion);
            SaleSystemConfig.init(providerIdentification, applicationName, softwareVersion, certificationCode);
        } catch (ConfigurationException e) {
            System.out.println("FusionClientConfig.init: ConfigurationException" + e); // Ensure all config fields have values
        }
    }

    private SaleToPOIRequest buildPaymentRequest(String txnType, String serviceID, BigDecimal Total, BigDecimal inputTip, BigDecimal inputDiscount) throws ConfigurationException {
        this.lastServiceID = serviceID;
        PaymentType paymentType = PaymentType.Normal;

        AmountsReq amountsReq;

        amountsReq = new AmountsReq.Builder()//
                .currency("AUD")//
                .requestedAmount(Total)//
                //.paidAmount(Total)
                .tipAmount(inputTip)
                .cashBackAmount(inputDiscount)
                .build();

        if (Objects.equals(txnType, "refund")){
            paymentType = PaymentType.Refund;
            amountsReq = new AmountsReq.Builder()//
                    .currency("AUD")//
                    .requestedAmount(Total)//
                    .build();

        }

        // Payment Request
        SaleTransactionID saleTransactionID = new SaleTransactionID.Builder()//
                .transactionID("transactionID" + new java.text.SimpleDateFormat("HH:mm:ss").format(new Date()))//
                .timestamp(Instant.now()).build();

        SaleData saleData = new SaleData.Builder()//
                // .operatorID("")//
                .operatorLanguage("en")//
                .saleTransactionID(saleTransactionID)//
                .build();

        //products here
        List<SaleItem> saleItems= new ArrayList<SaleItem>();
        saleItems.add( new SaleItem.Builder()//
                .itemID(0)//
                .productCode("productCode")//
                .unitOfMeasure(UnitOfMeasure.Other)//
                .quantity(new BigDecimal(1))//
                .unitPrice(new BigDecimal(100.00))//
                .itemAmount(Total)//
                .productLabel("Product Label")//
                .build());

        PaymentTransaction paymentTransaction = new PaymentTransaction.Builder()//
                .amountsReq(amountsReq)//
                .addSaleItem(saleItems.get(0))//
                .build();


        PaymentInstrumentData paymentInstrumentData = new PaymentInstrumentData.Builder()//
                .paymentInstrumentType(PaymentInstrumentType.Cash)//
                .build();


        PaymentData paymentData = new PaymentData.Builder()//
                .paymentType(paymentType)//
                .paymentInstrumentData(paymentInstrumentData)//
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
                .saleID(SALE_ID)//
                .POIID(POI_ID)//
                .build();

        SecurityTrailer securityTrailer = null;
        try {
            securityTrailer = SecurityTrailerUtil.generateSecurityTrailer(messageHeader, paymentRequest,
                    KEKConfig.getInstance().getValue());
        } catch (InvalidKeyException | NoSuchPaddingException | InvalidAlgorithmParameterException
                | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException
                | IOException e) {
            e.printStackTrace(); System.out.println("securityTrailer");
        }

        SaleToPOIRequest saleToPOI = new SaleToPOIRequest.Builder()//
                .messageHeader(messageHeader)//
                .request(paymentRequest)//
                .securityTrailer(securityTrailer)//
                .build();

        return saleToPOI;
    }


    CountDownTimer cdtPayment = new CountDownTimer(cdtPaymentLimit, 100) {
        @Override
        public void onTick(long millisUntilFinished) {
            String time = String.valueOf(millisUntilFinished / 1000);
            tvTimer.setText(time);
            reconnecting = false;
            if (!fusionClient.isConnected()){
                System.out.println("Disconnected!");
                reconnecting = true;
                retry=false;
                checkTransactionStatus(serviceID);
            }
        }
        @Override
        public void onFinish() {
            System.out.println("Timeout");
            errorhandling = true;
            retry=false;
            wait=false;
            cdtErrorHandler.start();
            checkTransactionStatus(serviceID);
        }
    };

    CountDownTimer cdtErrorHandler = new CountDownTimer(cdtErrorHandlerLimit, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            String time = String.valueOf(millisUntilFinished / 1000);
            tvTimer.setText(time);
            if (!fusionClient.isConnected()){
                System.out.println("Disconnected!"); //will not try and reconnect
                handleResponse(null, "errorhandler");
            }
        }
        @Override
        public void onFinish() {
            System.out.println("Timeout");
//            errorhandling = true;
            reconnecting=false;
            wait=false;
            retry=false;
            handleResponse(null, "errorhandler");
        }
    };

    /* UI Thread to display connection progress */
    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            handler.removeMessages(0);
            synchronized (this) {
                cdtPayment.start();
                while (!mainThread.isInterrupted()) {
                    Optional<SaleToPOIRequest> optMessageRequest;
                    Optional<SaleToPOIResponse> optMessageResponse;
                    try {
                        /* DISPLAY */
                        optMessageRequest = Optional.ofNullable(fusionClient.inQueueRequest.poll(1000, TimeUnit.MILLISECONDS));
                        optMessageResponse = Optional.ofNullable(fusionClient.inQueueResponse.poll(1000, TimeUnit.MILLISECONDS));
                        if(optMessageRequest.isPresent() && Objects.equals(txnType, "payment")){
                            System.out.println(optMessageRequest);
                            SaleToPOIRequest x = optMessageRequest.get();
                            String[] disp = x.getDisplayRequest().getDisplayOutput().getOutputContent().getOutputText().getText().split("\\|");

                            displayHd = "PAYMENT IN PROGRESS";
                            displayMsg = disp[0];
                            reconnecting=false;
                            errorhandling=false;
                            cdtPayment.cancel();cdtPayment.start();
                        }
                        if(reconnecting) {
                            displayHd="RECONNECTING";
                            displayMsg="Please reconnect POS to internet";
                        }else if(errorhandling){
                            displayHd="CHECKING TRANSACTION STATUS";
                            displayMsg="Entering Error Handling";
                        }
                        if(Objects.equals(txnType, "cancel")){
                            displayHd="CANCELLING TRANSACTION";
                            displayMsg="...";
                        }
                        handler.post(() -> {
                            tvProgressMessage.setText(displayMsg);
                            if (errorhandling||reconnecting||Objects.equals(txnType, "cancel")) {
                                tvProgressHeader.setText(displayHd);
                                btnCancelTxn.setVisibility(View.GONE);
                            }
                        });


                        /* BACKGROUND */
                        boolean serviceIDMatchesRequest;

                        if (optMessageResponse.isPresent()) {
                            System.out.println(txnType + "-----background");
                            SaleToPOIResponse response = optMessageResponse.get();
                            System.out.println(txnType + " -- " + response.toJson());
                            System.out.println(response.toJson());

                            serviceIDMatchesRequest = response.getMessageHeader().getServiceID()
                                    .equalsIgnoreCase(request.getMessageHeader().getServiceID());

                            //login
                            switch(txnType){
                                case "login":
                                    if (serviceIDMatchesRequest && response.getLoginResponse() != null) {
                                        Response responseBody = response.getLoginResponse().getResponse();

                                        if (responseBody.getResult() != null) {
                                            System.out.printf("Login Result: %s %n", responseBody.getResult());
                                            wait=false;
                                            if (responseBody.getResult() != ResponseResult.Success) {
                                                System.out.printf("Error Condition: %s, Additional Response: %s%n",
                                                        responseBody.getErrorCondition(), responseBody.getAdditionalResponse());
                                                loginsuccess = false;
                                                handleResponse(response, txnType);
                                            }
                                            else
                                            {
                                                loginsuccess = true;
                                            }

                                        }
                                    }

                                case "payment":
                                case "refund":
                                    if (serviceIDMatchesRequest && response.getPaymentResponse()!=null) {
                                        boolean saleTransactionIDMatchesRequest = response.getPaymentResponse().getSaleData().getSaleTransactionID()
                                                .getTransactionID().equals(request.getPaymentRequest().getSaleData()
                                                        .getSaleTransactionID().getTransactionID());

                                        if (!saleTransactionIDMatchesRequest) {
                                            System.out.println("Unknown sale ID " + response.getPaymentResponse().getSaleData()
                                                    .getSaleTransactionID().getTransactionID());
                                            retry=false;
                                            handleResponse(null, txnType);
                                        }

                                        Response responseBody = response.getPaymentResponse().getResponse();

                                        if (responseBody.getResult() != null) {
                                            System.out.printf("Payment Result: %s%n", responseBody.getResult());

                                            if (responseBody.getResult() != ResponseResult.Success) {
                                                System.out.println(String.format("Error Condition: %s, Additional Response: %s",
                                                        responseBody.getErrorCondition(), responseBody.getAdditionalResponse()));
                                                txnType="cancel";
                                            }
                                            cdtPayment.cancel();
                                            retry=false;
                                            wait=false;
                                            handleResponse(response, txnType);
                                        }

                                    }

                                case "transactionstatus":
                                    if (serviceIDMatchesRequest && response.getTransactionStatusResponse() != null
                                            && response.getTransactionStatusResponse().getResponse() != null) {
                                        Response responseBody = response.getTransactionStatusResponse().getResponse();
                                        if (responseBody.getResult() != null) {
                                            System.out.println(String.format("Transaction Status Result: %s ", responseBody.getResult()));

                                            if (responseBody.getResult() == ResponseResult.Success) {
                                                Response paymentResponseBody = null;
                                                if (response.getTransactionStatusResponse().getRepeatedMessageResponse() != null
                                                        && response.getTransactionStatusResponse().getRepeatedMessageResponse()
                                                        .getRepeatedResponseMessageBody() != null
                                                        && response.getTransactionStatusResponse().getRepeatedMessageResponse()
                                                        .getRepeatedResponseMessageBody().getPaymentResponse() != null) {
                                                    paymentResponseBody = response.getTransactionStatusResponse()
                                                            .getRepeatedMessageResponse().getRepeatedResponseMessageBody()
                                                            .getPaymentResponse().getResponse();
                                                }
                                                if (paymentResponseBody != null) {
                                                    System.out.println(String.format("Payment Result: %s", paymentResponseBody.getResult()));

                                                    if (paymentResponseBody.getErrorCondition() != null || paymentResponseBody.getAdditionalResponse() != null) {
                                                        System.out.println(String.format("Error Condition: %s, Additional Response: %s",
                                                                responseBody.getErrorCondition(),
                                                                responseBody.getAdditionalResponse()));
                                                    }
                                                        handleResponse(response, "checktransaction");
                                                        errorhandling=false;
                                                        retry=false;
                                                        wait=false;
                                                }

                                            }
                                            else if (responseBody.getErrorCondition() == ErrorCondition.InProgress) {
                                                System.out.println("Payment in progress...");
                                                System.out.println(String.format("Error Condition: %s, Additional Response: %s",
                                                        responseBody.getErrorCondition(), responseBody.getAdditionalResponse()));
                                                wait=false;
                                                retry=true;
                                            }

                                        }
                                    }
                                case "cancel":
                                    if (serviceIDMatchesRequest && response.getPaymentResponse()!=null) {

                                        Response responseBody = response.getPaymentResponse().getResponse();

                                        if (responseBody.getResult() != null) {
                                            System.out.printf("Payment Result: %s%n", responseBody.getResult());

                                            if (responseBody.getResult() != ResponseResult.Success) {
                                                System.out.println(String.format("Error Condition: %s, Additional Response: %s",
                                                        responseBody.getErrorCondition(), responseBody.getAdditionalResponse()));
                                                txnType="cancel";
                                            }
                                            handleResponse(response, txnType);
                                        }

                                    }
                            }
                        }

                    } catch (InterruptedException e) {
                        System.out.println("Stop polling for display requests...");
                        break;
                    }
                }
            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        initValues();
        tvProgressMessage = findViewById(R.id.tvProgressMessage);
        tvProgressHeader = findViewById(R.id.tvProgressHeader);
        tvTimer = findViewById(R.id.tvTimer);

        Bundle bundle = getIntent().getExtras();

        this.maintxnType = bundle.getString("txnType");
        String Total = bundle.getString("Total");
        String Tip = bundle.getString("Tip");
        String Discount = bundle.getString("Discount");

        if (Total.isEmpty()){ Total = "0";}
        if (Tip.isEmpty()){ Tip = "0";}
        if (Discount.isEmpty()){ Discount = "0"; }

        bTotal = BigDecimal.valueOf(Double.parseDouble(Total));
        bTip = BigDecimal.valueOf(Double.parseDouble(Tip)); //van update for null
        bDiscount = BigDecimal.valueOf(Double.parseDouble(Discount));

        btnCancelTxn = findViewById(R.id.btnCancelTxn);

        btnCancelTxn.setOnClickListener(v -> {
            doCancel();
        });

        serviceID = MessageHeaderUtil.generateServiceID(10);



        mainThread = new Thread(runnable);
        service = Executors.newSingleThreadExecutor();
        if (!FusionClientConfig.isInitialised()) {
            service.execute(ActivityLoading::initConfig);
        }

        Future connect = service.submit(() -> {
            connected = doConnect();
        });
        Future login = service.submit(() -> {
            loginsuccess = doLogin();
        });
        Future pay = service.submit(() -> {
            paymentsuccess = doPayment();
        });

        try {
            System.out.println(connect.get());
            if(connected){
                System.out.println(login.get());
                if (loginsuccess) {
                    System.out.println(pay.get());
                }
            }
        } catch (ExecutionException | InterruptedException e){// | TimeoutException e) {
            System.out.println("ExecutionException | InterruptedException --- login mini" + e);
            e.printStackTrace();
        }
    }


    private void handleResponse(SaleToPOIResponse response, String txnType) {
        this.response = response;

        try {
            openActivityResult(response, txnType);
        } catch (Exception e) {
            openActivityResult(response, e.toString());
        }

    }

    public void openActivityResult(SaleToPOIResponse res, String x) {
        Intent intent = new Intent(this, ActivityResult.class);
        intent.putExtra("txnType", x);
        try {
            if(Objects.equals(x, "login")) {
                intent.putExtra("logindetails", res.getLoginResponse().getResponse().getAdditionalResponse());
            }
            else if(Objects.equals(x, "errorhandler")){
                //
            }
            else if(Objects.equals(x, "ConfigurationException")){
                //
            }
            else if(Objects.equals(x, "InterruptedException")){
                //
            }
            else if(Objects.equals(x, "NotConnectedException")){
                //
            }
            else if(Objects.equals(x, "checktransaction")){
                PaymentReceipt paymentreceipt = res.getTransactionStatusResponse().getRepeatedMessageResponse().getRepeatedResponseMessageBody().getPaymentResponse().getPaymentReceipt().get(0);

                String paymentresult = res.getTransactionStatusResponse().getRepeatedMessageResponse().getRepeatedResponseMessageBody().getPaymentResponse().getResponse().getResult().name();

                String receiptOutput = paymentreceipt.getReceiptContentAsHtml();
                intent.putExtra("OutputXHTML", receiptOutput);
                intent.putExtra("paymentresult", paymentresult);
                intent.putExtra("authorizedamount", res.getTransactionStatusResponse().getRepeatedMessageResponse().getRepeatedResponseMessageBody().getPaymentResponse().getPaymentResult().getAmountsResp().getAuthorizedAmount().toString());
                intent.putExtra("surcharge", res.getTransactionStatusResponse().getRepeatedMessageResponse().getRepeatedResponseMessageBody().getPaymentResponse().getPaymentResult().getAmountsResp().getSurchargeAmount().toString());
                intent.putExtra("tip", res.getTransactionStatusResponse().getRepeatedMessageResponse().getRepeatedResponseMessageBody().getPaymentResponse().getPaymentResult().getAmountsResp().getTipAmount().toString());
            }
            else{
                PaymentReceipt paymentreceipt = res.getPaymentResponse().getPaymentReceipt().get(0);

                String paymentresult = res.getPaymentResponse().getResponse().getResult().name();

                String receiptOutput = paymentreceipt.getReceiptContentAsHtml();
                intent.putExtra("OutputXHTML", receiptOutput);
                intent.putExtra("paymentresult", paymentresult);
                intent.putExtra("authorizedamount", res.getPaymentResponse().getPaymentResult().getAmountsResp().getAuthorizedAmount().toString());
                intent.putExtra("surcharge", res.getPaymentResponse().getPaymentResult().getAmountsResp().getSurchargeAmount().toString());
                intent.putExtra("tip", res.getPaymentResponse().getPaymentResult().getAmountsResp().getTipAmount().toString());
            }

        } catch (Exception e) {
            intent.putExtra("txnType", e.toString());
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void doCancel(){
        this.lastServiceID = serviceID;
        txnType = "cancel";
        service = Executors.newSingleThreadExecutor();
        service.execute(() -> {
            try {
                request = buildAbortRequest(this.lastServiceID);

                System.out.println("Sending message to websocket server: " + "\n" + request);
                fusionClient.sendMessage(request);
            } catch (Exception e) {
                System.out.println("doCancel Exception" + e);
            }

        });

    }

    private SaleToPOIRequest buildAbortRequest(String serviceID){
        // Abort Request
        // Transaction Status Request
        MessageReference messageReference = new MessageReference.Builder()//
                .messageCategory(MessageCategory.Abort)//
                .saleID(SALE_ID)//
                .POIID(POI_ID)//
                .serviceID(serviceID)
                .build();

        AbortTransactionRequest abortTransactionRequest = new AbortTransactionRequest(messageReference, "User Cancel");

        // Message Header
        MessageHeader messageHeader = new MessageHeader.Builder()//
                .messageClass(MessageClass.Service)//
                .messageCategory(MessageCategory.Abort)//
                .messageType(MessageType.Request)//
                .saleID(SALE_ID)//
                .POIID(POI_ID)//
                .serviceID(serviceID)
                .build();

        SecurityTrailer securityTrailer = null;
        try {
            securityTrailer = SecurityTrailerUtil.generateSecurityTrailer(messageHeader, abortTransactionRequest,
                    KEKConfig.getInstance().getValue());
        } catch (InvalidKeyException | NoSuchPaddingException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException | IOException | ConfigurationException e) {
            e.printStackTrace();
            System.out.println("securityTrailer ---- printStackTrace1");
        }

        SaleToPOIRequest saleToPOI = new SaleToPOIRequest.Builder()//
                .messageHeader(messageHeader)//
                .request(abortTransactionRequest)//
                .securityTrailer(securityTrailer)//
                .build();

        return saleToPOI;
    }

    private boolean doConnect(){
        if (!fusionClient.isConnected()) {
            try {
                fusionClient.connect(new URI(FusionClientConfig.getInstance().getServerDomain()));
                connected = true;
            } catch (DeploymentException | IOException | KeyManagementException | NoSuchAlgorithmException | CertificateException | KeyStoreException | ConfigurationException | URISyntaxException e) {
                e.printStackTrace();
                System.out.println("fusionClient.connect ---------" + e);
                connected=false;
            }
        }
        return connected;
    }

    private boolean doPayment(){
        request = null;
        try {
            txnType = maintxnType;
            request = buildPaymentRequest(txnType, serviceID, bTotal, bTip, bDiscount);
            System.out.println("Sending message to websocket server: " + "\n" + request);

            fusionClient.sendMessage(request);

            retry = true;
            while(retry){
            }


        } catch (ConfigurationException e) {
            System.out.println(e + "-----------ConfigurationException --- paymentrequest");
        } catch (NotConnectedException e) {
            System.out.println(e + "-----------NotConnectedException --- paymentrequest"); //here van
            txnType="payment";
            retry=true;
            doConnect();
        }
        return paymentsuccess;
    }

    private Boolean doLogin() {
        request = null;
        try {
            txnType = "login";
            request = buildLoginRequest();

            System.out.println("login request to websocket server: " + "\n" + request);

            fusionClient.sendMessage(request);
            mainThread.start();

            wait=true;
            while(wait){
            }

        } catch (ConfigurationException e) {
            System.out.println(e + "------ConfigurationException---dologin");
            handleResponse(null, "ConfigurationException");
        }catch ( NotConnectedException e){
            System.out.println(e + "------NotConnectedException---dologin");
            handleResponse(null, "NotConnectedException");
//        }catch (InterruptedException e) {
//            System.out.println(e + "------InterruptedException---dologin");
//            e.printStackTrace();
//            handleResponse(null, "InterruptedException");
        }
        return loginsuccess;
    }

    private static SaleToPOIRequest buildLoginRequest() throws ConfigurationException {
        // Login Request
        SaleSoftware saleSoftware = new SaleSoftware.Builder()//
                .providerIdentification(SaleSystemConfig.getInstance().getProviderIdentification())//
                .applicationName(SaleSystemConfig.getInstance().getApplicationName())//
                .softwareVersion(SaleSystemConfig.getInstance().getSoftwareVersion())//
                .certificationCode(SaleSystemConfig.getInstance().getCertificationCode())//
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
                .serviceID(MessageHeaderUtil.generateServiceID(10))//
                .saleID(SALE_ID)//
                .POIID(POI_ID)//
                .build();

        SecurityTrailer securityTrailer = null;
        try {
            securityTrailer = SecurityTrailerUtil.generateSecurityTrailer(messageHeader, loginRequest,
                    KEKConfig.getInstance().getValue());
        } catch (InvalidKeyException | NoSuchPaddingException | InvalidAlgorithmParameterException
                | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException
                | UnsupportedEncodingException | InvalidKeySpecException e) {
            e.printStackTrace();
            System.out.println("securityTrailer ---- printStackTrace2");
        }

        SaleToPOIRequest saleToPOI = new SaleToPOIRequest.Builder()//
                .messageHeader(messageHeader)//
                .request(loginRequest)//
                .securityTrailer(securityTrailer)//
                .build();

        return saleToPOI;
    }


    private static SaleToPOIRequest buildTransactionStatusRequest(String serviceID) throws ConfigurationException {
        // Transaction Status Request
        MessageReference messageReference = new MessageReference.Builder()//
                .messageCategory(MessageCategory.Payment)//
                .POIID(POI_ID)//
                .saleID(SALE_ID)//
                .serviceID(serviceID)//
                .build();

        TransactionStatusRequest transactionStatusRequest = new TransactionStatusRequest(messageReference);

        // Message Header
        MessageHeader messageHeader = new MessageHeader.Builder()//
                .messageClass(MessageClass.Service)//
                .messageCategory(MessageCategory.TransactionStatus)//
                .messageType(MessageType.Request)//
                .serviceID(MessageHeaderUtil.generateServiceID(10))//
                .saleID(SALE_ID)//
                .POIID(POI_ID)//
                .build();

        SecurityTrailer securityTrailer = null;
        try {
            securityTrailer = SecurityTrailerUtil.generateSecurityTrailer(messageHeader, transactionStatusRequest,
                    KEKConfig.getInstance().getValue());
        } catch (InvalidKeyException | NoSuchPaddingException | InvalidAlgorithmParameterException
                | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException
                | IOException e) {
            e.printStackTrace();
            System.out.println("securityTrailer ---- printStackTrace3");
        }

        SaleToPOIRequest saleToPOI = new SaleToPOIRequest.Builder()//
                .messageHeader(messageHeader)//
                .request(transactionStatusRequest)//
                .securityTrailer(securityTrailer)//
                .build();

        return saleToPOI;
    }

    private void checkTransactionStatus(String serviceID) {
            service.execute(()->{
            txnType = "transactionstatus";
            request=null;
            try {
                System.out.println("Reconnecting to FusionClient... ");
                fusionClient.connect(new URI(FusionClientConfig.getInstance().getServerDomain()));

                System.out.println("Sending transaction status request to check status of payment...");
                request = buildTransactionStatusRequest(serviceID);
                retry = true;

                while(retry){
                    System.out.println("Sending message to websocket server (checkTransactionStatus): " + "\n" + request);
                    fusionClient.sendMessage(request);
                    wait = true;
                    while(wait){
                        if(!fusionClient.isConnected()){
                            fusionClient.sendMessage(request);
                        }
                    }
                }


            } catch (ConfigurationException e) {
                System.out.println(e + "---------ConfigurationException --- outside checktransactionstatus");
            } catch (NotConnectedException | DeploymentException e) { //InterruptedException |
//                    System.out.println(e + " ------ NotConnectedException --- outside checktransactionstatus");
                retry=true;
            } catch ( URISyntaxException | IOException | KeyManagementException | NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
                e.printStackTrace();
                System.out.println(" ---- URISyntaxException--- outside checktransactionstatus");
            }
        });


    }
    @Override
    protected void onStop() {
        super.onStop();
        reconnecting = false;
        errorhandling = false;
        retry = false;
        wait=false;
        cdtPayment.cancel();
        cdtErrorHandler.cancel();
        if(!mainThread.isInterrupted()){ mainThread.interrupt(); System.out.println("OUT1");}
        if(service!=null){service.shutdown(); System.out.println("OUT2");}
        if(fusionClient.isConnected()){
            try {
                fusionClient.disconnect();
                System.out.println("OUT3");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        handler.removeMessages(0);
        finish();
    }


}
