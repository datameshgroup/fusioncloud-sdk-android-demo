package au.com.dmg.fusioncloud.android.demo

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import au.com.dmg.fusion.exception.FusionException
import au.com.dmg.fusion.response.LoginResponse
import au.com.dmg.fusioncloud.android.demo.databinding.PairingActivityBinding
import au.com.dmg.fusioncloud.android.demo.databinding.PairingResultActivityBinding
import au.com.dmg.fusioncloud.android.demo.utils.ParsingUtils

class PairingResultActivity: AppCompatActivity() {
    private lateinit var binding: PairingResultActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PairingResultActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val isSuccessful = intent.getBooleanExtra("isSuccessful", false)

        if(isSuccessful){
            val receivedPOI = intent.getStringExtra("receivedPOI")
            binding.lblPairingResult.apply {
                text = "Pairing Successful"
                setBackgroundColor(Color.parseColor("#4CAF50"))
            }
            binding.txtPairingDetail.apply {
                text = "Paired to POI: $receivedPOI"
                setBackgroundColor(Color.parseColor("#4CAF50"))
            }
        }else{
            val errorCondition = intent.getStringExtra("errorCondition")
            val additionalResponse = intent.getStringExtra("additionalResponse")

            binding.lblPairingResult.apply {
                text = "Pairing Failed"
                setBackgroundColor(Color.parseColor("#E91E63"))
            }
            binding.txtPairingDetail.apply {
                text = "$errorCondition : $additionalResponse"
                setBackgroundColor(Color.parseColor("#E91E63"))
            }
        }

        binding.btnNext.setOnClickListener{
            finish()
        }



    }
}