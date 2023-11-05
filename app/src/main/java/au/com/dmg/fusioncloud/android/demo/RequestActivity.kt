package au.com.dmg.fusioncloud.android.demo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import au.com.dmg.fusion.data.MessageCategory
import au.com.dmg.fusion.data.PaymentType
import au.com.dmg.fusioncloud.android.demo.data.RequestData
import au.com.dmg.fusioncloud.android.demo.databinding.RequestActivityBinding
import java.math.BigDecimal
import java.util.UUID

class RequestActivity: AppCompatActivity()  {
    private lateinit var binding:RequestActivityBinding
    var requestedAmount = ""
    var tipAmount = ""
    var productCode =""
    var requestData: RequestData? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RequestActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnLogin.setOnClickListener {
            requestData = RequestData(
                UUID.randomUUID().toString(),
                MessageCategory.Login,
                null,
                null,
                null,
                null,
                null
            )
            startTransactionProgressActivity()
        }
        binding.btnPurchase.setOnClickListener {
            requestedAmount = binding.inputItemAmount.text.toString()
            tipAmount = binding.inputTipAmount.text.toString()
            productCode = binding.inputProductCode.text.toString()
//test null and empty
            requestData = RequestData(
                UUID.randomUUID().toString(),
                MessageCategory.Payment,
                PaymentType.Normal,
                BigDecimal(requestedAmount),
                BigDecimal(tipAmount),
                "productCode",
            null
            )

            startTransactionProgressActivity()
        }

        binding.btnRefund.setOnClickListener {
            requestedAmount = binding.inputItemAmount.text.toString()

            requestData = RequestData(
                UUID.randomUUID().toString(),
                MessageCategory.Payment,
                PaymentType.Refund,
                BigDecimal(requestedAmount),
                null,
                null,
                null
            )

            startTransactionProgressActivity()
        }
    }

    private fun startTransactionProgressActivity(){
        val intent = Intent(this, TransactionProgressActivity::class.java)
        intent.putExtra("requestData", requestData)
        startActivity(intent)
        finish()
    }
}