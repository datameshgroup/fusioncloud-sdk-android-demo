package au.com.dmg.fusioncloud.android.demo

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SatelliteActivity : AppCompatActivity() {
    private var btnReversalReq: Button? = null
    private var btnCashoutReq: Button? = null
    private var btnRefundReq: Button? = null
    private var btnPreauthReq: Button? = null
    private var btnCompletionReq: Button? = null
    private var btnTransactionStatusReq: Button? = null
    private var btnCardAcquisitionReq: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.satellite_activity)
        btnReversalReq = findViewById<View>(R.id.btnReversalReq) as Button
        btnCashoutReq = findViewById<View>(R.id.btnCashoutReq) as Button
        btnRefundReq = findViewById<View>(R.id.btnRefundReq) as Button
        btnPreauthReq = findViewById<View>(R.id.btnPreauthReq) as Button
        btnCompletionReq = findViewById<View>(R.id.btnCompletionReq) as Button
        btnTransactionStatusReq = findViewById<View>(R.id.btnTransactionStatusReq) as Button
        btnCardAcquisitionReq = findViewById<View>(R.id.btnCardAcquisitionReq) as Button
        btnRefundReq!!.setOnClickListener { v: View? -> openActivityRequests("refund") }
        btnCashoutReq!!.setOnClickListener { v: View? -> openActivityRequests("cashout") }
        btnReversalReq!!.setOnClickListener { v: View? -> openActivityRequests("reversal") }
        btnPreauthReq!!.setOnClickListener { v: View? -> openActivityRequests("preauth") }
        btnCompletionReq!!.setOnClickListener { v: View? -> openActivityRequests("completion") }
        btnTransactionStatusReq!!.setOnClickListener { v: View? -> openActivityRequests("txnstatus") }
        btnCardAcquisitionReq!!.setOnClickListener { v: View? -> openActivityRequests("cardacq") }
    }

    fun openActivityRequests(req: String?) {
//        Intent intent = new Intent(this, ActivityRequests.class);

//        intent.putExtra("requestName", req);
//
//        startActivity(intent);
    }
}