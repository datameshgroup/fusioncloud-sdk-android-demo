package au.com.dmg.fusioncloud.android.demo

import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import au.com.dmg.fusioncloud.android.demo.databinding.ConfigurationActivityBinding
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.applicationName
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.certificationCode
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.kek
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.poiId
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.providerIdentification
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.saleId
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.softwareVersion

class ConfigurationActivity : AppCompatActivity() {
    private lateinit var binding: ConfigurationActivityBinding
    var previousClass: Class<*>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ConfigurationActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        previousClass = intent.javaClass

        getValues()

        //disable editing for Static Sales Systems
        binding.txtSalesID.isFocusable = false
        binding.txtProviderIdentifiction.isClickable = false
        binding.txtApplicationName.isFocusable= false
        binding.txtApplicationName.isClickable= false
        binding.txtSoftwareVersion.isFocusable= false
        binding.txtSoftwareVersion.isClickable= false
        binding.txtCertificationCode.isFocusable= false
        binding.txtCertificationCode.isClickable= false

        binding.btnExit.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            saveSettings()
        }
    }

    private fun getValues() {
        binding.txtSalesID.setText(saleId)
        binding.txtPOIID.setText(poiId)
        binding.txtKEK.setText(kek)
        binding.txtProviderIdentifiction.setText(providerIdentification)
        binding.txtApplicationName.setText(applicationName)
        binding.txtSoftwareVersion.setText(softwareVersion)
        binding.txtCertificationCode.setText(certificationCode)
    }


    private fun saveSettings() {
        try {
            poiId = binding.txtPOIID.text.toString()
            saleId = binding.txtSalesID!!.text.toString()
            kek = binding.txtKEK.text.toString()
            providerIdentification = binding.txtProviderIdentifiction.text.toString()
            applicationName = binding.txtApplicationName.text.toString()
            softwareVersion = binding.txtSoftwareVersion.text.toString()
            certificationCode = binding.txtCertificationCode.text.toString()
            Toast.makeText(applicationContext, "Settings Updated", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
        }
    }
}