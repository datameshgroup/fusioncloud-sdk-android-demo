package au.com.dmg.fusioncloud.android.demo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import au.com.dmg.fusioncloud.android.demo.databinding.SettingsActivityBinding

class SettingsActivity: AppCompatActivity()  {

    private lateinit var binding: SettingsActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnViewConfig.setOnClickListener {
            val intent = Intent(this, ConfigurationActivity::class.java)
            startActivity(intent)
        }

        binding.btnPairTerminal.setOnClickListener {
            //TODO
            println("btnPairTerminal clicked")
            val intent = Intent(this, PairingActivity::class.java)
            startActivity(intent)
        }

        binding.btnUnpairTerminal.setOnClickListener {
            //TODO
            val intent = Intent(this, PairingActivity::class.java)
            startActivity(intent)
        }

        binding.btnExitSettings.setOnClickListener {
            finish()
        }
    }
}