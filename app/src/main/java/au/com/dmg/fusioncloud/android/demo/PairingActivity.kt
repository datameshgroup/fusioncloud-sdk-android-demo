package au.com.dmg.fusioncloud.android.demo

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.StrictMode
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import au.com.dmg.fusion.SaleToPOI
import au.com.dmg.fusion.client.FusionClient
import au.com.dmg.fusion.data.MessageCategory
import au.com.dmg.fusion.data.SaleCapability
import au.com.dmg.fusion.data.TerminalEnvironment
import au.com.dmg.fusion.exception.FusionException
import au.com.dmg.fusion.request.SaleTerminalData
import au.com.dmg.fusion.request.SaleToPOIRequest
import au.com.dmg.fusion.request.loginrequest.LoginRequest
import au.com.dmg.fusion.request.loginrequest.SaleSoftware
import au.com.dmg.fusion.response.SaleToPOIResponse
import au.com.dmg.fusion.util.PairingData
import au.com.dmg.fusioncloud.android.demo.databinding.PairingActivityBinding
import au.com.dmg.fusioncloud.android.demo.utils.Configuration
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.useTestEnvironment
import au.com.dmg.fusioncloud.android.demo.utils.FusionMessageHandler
import au.com.dmg.fusioncloud.android.demo.utils.FusionMessageResponse
import au.com.dmg.fusioncloud.android.demo.utils.ParsingUtils.Companion.log
import au.com.dmg.fusioncloud.android.demo.utils.ParsingUtils.Companion.prettyPrintJson
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.squareup.moshi.Moshi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class PairingActivity: AppCompatActivity() {
    private lateinit var binding: PairingActivityBinding
    private lateinit var executorService: ExecutorService
    var waitingForResponse = true

    var receivedPOIID: String? = null
    //SaleID, unique to the POS instance. Autogenerate this once per POS instance.
    var s:String = ""
    //PairingPOIID. This will be populated on the pairing response
    var p:String = ""
    //KEK. Autogenerate this once per POS instance.
    var k:String = ""
    var currentServiceID = ""
    lateinit var fusionClient: FusionClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PairingActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        fusionClient = FusionClient(useTestEnvironment)

        k =PairingData.CreateKEK();
        s = UUID.randomUUID().toString()
        p = UUID.randomUUID().toString() //PairingPOIID

        println("Generated KEK:$k")
        println("Generated SaleID:$s")
        println("Generated POIID:$p")

        fusionClient.poiID = p
        fusionClient.saleID = s //Set MessageHeader.SaleID to the pairing QR code SaleID value
        fusionClient.kek = k

        binding.progressCircle.isInvisible = true
        binding.txtPairingStatus.isInvisible = true

        generateQRCode()

        binding.btnEnterManually.setOnClickListener {
            val intent = Intent(this, ConfigurationActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnCancel.setOnClickListener{
            finish()
        }

        binding.btnNext.setOnClickListener {
            //TODO connect, then dologin
            runOnUiThread {
                binding.progressCircle.isInvisible = false
                binding.btnNext.isEnabled = false
                binding.btnCancel.isEnabled = false
            }
//            try {
                executorService = Executors.newSingleThreadExecutor()
                executorService.submit { doLogin() }
//            }catch (e: FusionException) {
//
//                log(String.format("Cannot connect to FusionClient. Reason:\n %s", e))
//                waitingForResponse = false
//                startActivityFailure("FusionClient Connection Failed", e.toString())
//            }
        }

    }

    private fun doLogin() {
        runOnUiThread {
            binding.progressCircle.isInvisible=true
            binding.txtPairingStatus.isInvisible = false
            binding.txtPairingStatus.text = "Logging in..."
        }
        currentServiceID = UUID.randomUUID().toString()

        val saleSoftware = SaleSoftware.Builder()
            .providerIdentification(Configuration.providerIdentification)
            .applicationName(Configuration.applicationName)
            .softwareVersion(Configuration.softwareVersion)
            .certificationCode(Configuration.certificationCode)
            .build()

        val saleTerminalData = SaleTerminalData.Builder()
            .terminalEnvironment(TerminalEnvironment.Attended)
            .saleCapabilities(
                listOf(
                    SaleCapability.CashierStatus, SaleCapability.CustomerAssistance,
                    SaleCapability.PrinterReceipt
                )
            ) //
            .build()

        val loginRequest = LoginRequest.Builder()
            .dateTime(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Date()))
            .operatorLanguage("en")
            .pairing(true)         //New for QR Pairing
            .saleTerminalData(saleTerminalData)
            .saleSoftware(saleSoftware)
            .build()
        try {
            fusionClient.sendMessage(loginRequest, currentServiceID)

        }catch (e: FusionException) {
            log(String.format("Login Failed. Reason:\n %s", e))
            waitingForResponse = false
            startActivityFailure("FusionClient Login Failed", "${e.toString()}")
        }


        waitingForResponse = true
        while (waitingForResponse) {
            listenForResponse()
        }

    }

    private fun listenForResponse() {
        try {
            var saleToPOI: SaleToPOI? = null
            saleToPOI = fusionClient.readMessage()

            if (saleToPOI == null) return

            log("Response Received: ${prettyPrintJson(saleToPOI)}")

            val fmh = FusionMessageHandler()
            var fmr: FusionMessageResponse? = null

            if (saleToPOI is SaleToPOIRequest) {
                fmr = fmh.handle((saleToPOI as SaleToPOIRequest?)!!)

                log("SaleToPOIRequest received on Pairing Login (Ignored): ${prettyPrintJson(fmr)}")
                waitingForResponse = true
                return
            }
            if (saleToPOI is SaleToPOIResponse) {
                println("SaleToPOIResponse received on Pairing Login")
                fmr = fmh.handle((saleToPOI as SaleToPOIResponse?)!!)

                //Ignore response if it's not Login Response
                if (fmr.messageCategory != MessageCategory.Login && fmr.messageCategory != MessageCategory.Event) {
                    log("Ignoring Response above... waiting for Login, received " + fmr.messageCategory)
                    return
                }
                when (fmr.messageCategory) {
                    MessageCategory.Event -> {
                        val spr = fmr.saleToPOI as SaleToPOIResponse?
                        val eventNotification = spr!!.eventNotification
                        log("Ignoring Event below... ${prettyPrintJson(spr)} Event Details: ${eventNotification!!.eventDetails}")
                    }
                    MessageCategory.Login -> {

                        handleLoginResponse(fmr)

                    }
                    else -> {
                        //TODO Unknown MessageCategory
                    }
                }
            }

        }catch (e: FusionException) {
            log(String.format("Stopped listening to message. Reason:\n %s", e))
            waitingForResponse = false
            startActivityFailure("FusionClient Connection Failed", "Stopped listening to message. Reason:\n ${e.toString()}")
        }

    }

    private fun handleLoginResponse(fmr: FusionMessageResponse) {
        runOnUiThread { binding.txtPairingStatus.text = fmr.displayMessage }
        waitingForResponse = false

        receivedPOIID = fmr.saleToPOI?.messageHeader?.poiID.toString()

        val loginResponse = (fmr.saleToPOI as SaleToPOIResponse?)!!.loginResponse
        val response = loginResponse?.response
        var errorCondition = "Unknown"
        var additionalResponse = "Invalid Response <NULL>"

        var intent = Intent(this, PairingResultActivity::class.java).apply {

            putExtra("isSuccessful", fmr.isSuccessful)
            putExtra("receivedPOI",receivedPOIID)
            if (response != null) {
                errorCondition = response.errorCondition.toString()
                additionalResponse = response.additionalResponse.toString()
            }
            putExtra("errorCondition",errorCondition)
            putExtra("additionalResponse",additionalResponse)
        }
        startActivity(intent)
        finish()
    }

    private fun generateQRCode() {
        println("Generating QR Code...")

        val c = Configuration.certificationCode //CertificationCode
        val n = Configuration.applicationName //POS display name with at most 30 characters.

        val newPairingData = createPairingData(
            s,
            p,
            k,
            n,
            c,
        )

        val moshi = Moshi.Builder().build()
        val jsonAdapter = moshi.adapter(PairingData::class.java)
        val json = jsonAdapter.toJson(newPairingData)
        println(json)
        val qrCodeValue = genQRCode(json)

        binding.ivQrCode.setImageBitmap(qrCodeValue)

    }

    fun genQRCode(input: String): Bitmap? {
        val writer = QRCodeWriter()
        try {
            val bitMatrix: BitMatrix = writer.encode(input, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            return bmp
        } catch (e: WriterException) {
            e.printStackTrace()
        }
        return null
    }

    private fun createPairingData(saleID: String?, pairingPOIID: String?, kek: String?, posName: String?, certificationCode: String?, version: Int = 1): PairingData? {
        if (certificationCode == null) {
            println("Invalid pairing request. certificationCode is null empty")
            return null
        }

        return PairingData.Builder()
            .saleID(saleID)
            .pairingPOIID(pairingPOIID)
            .kek(kek)
            .certificationCode(certificationCode)
            .posName(posName)
            .version(version)
            .build()
    }

    fun startActivityFailure(errorCondition:String, additionalResponse:String){
        var intent = Intent(this, PairingResultActivity::class.java).apply {
            putExtra("isSuccessful", false)
            putExtra("errorCondition",errorCondition)
            putExtra("additionalResponse",additionalResponse)
        }
        startActivity(intent)
        finish()
    }

}