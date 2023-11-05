package au.com.dmg.fusioncloud.android.demo

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.applicationName
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.certificationCode
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.kek
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.poiId
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.providerIdentification
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.saleId
import au.com.dmg.fusioncloud.android.demo.utils.Configuration.softwareVersion

class ConfigurationActivity : AppCompatActivity() {
    var txtSalesID: EditText? = null
    var txtPOIID: EditText? = null
    var txtKEK: EditText? = null
    lateinit var txtProviderIdentifiction: EditText
    lateinit var txtApplicationName: EditText
    lateinit var txtSoftwareVersion: EditText
    lateinit var txtCertificationCode: EditText
    var previousClass: Class<*>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.configuration_activity)
        previousClass = intent.javaClass
        txtSalesID = findViewById(R.id.txtSalesID)
        txtPOIID = findViewById(R.id.txtPOIID)
        txtKEK = findViewById(R.id.txtKEK)
        txtProviderIdentifiction = findViewById(R.id.txtProviderIdentifiction)
        txtApplicationName = findViewById(R.id.txtApplicationName)
        txtSoftwareVersion = findViewById(R.id.txtSoftwareVersion)
        txtCertificationCode = findViewById(R.id.txtCertificationCode)
        getvalues()

        //disable editing for Static Sales Systems
        txtProviderIdentifiction.setFocusable(false)
        txtProviderIdentifiction.setClickable(false)
        txtApplicationName.setFocusable(false)
        txtApplicationName.setClickable(false)
        txtSoftwareVersion.setFocusable(false)
        txtSoftwareVersion.setClickable(false)
        txtCertificationCode.setFocusable(false)
        txtCertificationCode.setClickable(false)
    }

    private fun getvalues() {
        txtSalesID!!.setText(saleId)
        txtPOIID!!.setText(poiId)
        txtKEK!!.setText(kek)
        txtProviderIdentifiction!!.setText(providerIdentification)
        txtApplicationName!!.setText(applicationName)
        txtSoftwareVersion!!.setText(softwareVersion)
        txtCertificationCode!!.setText(certificationCode)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.back, menu)
        menuInflater.inflate(R.menu.savesettings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.savesettings -> {
                savesettings()
                true
            }

            R.id.back -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun savesettings() {
        try {
            poiId = txtPOIID!!.text.toString()
            saleId = txtSalesID!!.text.toString()
            kek = txtKEK!!.text.toString()
            providerIdentification = txtProviderIdentifiction!!.text.toString()
            applicationName = txtApplicationName!!.text.toString()
            softwareVersion = txtSoftwareVersion!!.text.toString()
            certificationCode = txtCertificationCode!!.text.toString()
            Toast.makeText(applicationContext, "Settings Updated", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
        }
    }
}