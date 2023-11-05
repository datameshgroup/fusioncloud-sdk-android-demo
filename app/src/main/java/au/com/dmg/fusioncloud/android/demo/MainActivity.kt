package au.com.dmg.fusioncloud.android.demo

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var btnCart: Button? = null
    private var btnSatellite: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        btnCart = findViewById<View>(R.id.btnPayment) as Button
        btnCart!!.setOnClickListener { v: View? -> openPaymentActivity() }
        btnSatellite = findViewById<View>(R.id.btnSatellite) as Button
        btnSatellite!!.setOnClickListener { v: View? -> openActivitySattelite() }
    }

    fun openPaymentActivity() {
        val intent = Intent(this, PaymentActivity::class.java)
        startActivity(intent)
    }

    fun openActivitySattelite() {
        val intent = Intent(this, SatelliteActivity::class.java)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.back, menu);
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}