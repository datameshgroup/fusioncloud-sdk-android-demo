package fusioncloud.sdk.android;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.posonandroidva.R;

import java.math.BigDecimal;

import au.com.dmg.fusion.response.SaleToPOIResponse;

public class ActivityRequests extends AppCompatActivity {

    BigDecimal bAmt = BigDecimal.valueOf(0);
    private String requestName;
    private TextView tvRequestTitle;
    private TextView txtAmountLabel;
    private TextView tvAmount;
    private Button btnSendReq;
    private SaleToPOIResponse response = null;
    private String lastTxid = null;
    private String lastServiceID = null;
    private long pressedTime;


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


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);


        //this.lastTxid = globalClass.getgLasttxnID();

        Bundle bundle = getIntent().getExtras();

        String requestName = bundle.getString("requestName");

        tvRequestTitle = (TextView) findViewById(R.id.tvRequestTitle);
        txtAmountLabel = (TextView) findViewById(R.id.txtAmountLabel);
        btnSendReq = (Button) findViewById(R.id.btnSendReq);
        tvAmount = (TextView) findViewById(R.id.tvAmount);

        switch (requestName) {

            case "refund":
                tvRequestTitle.setText("REFUND REQUEST");
                btnSendReq.setOnClickListener(v -> {
                    String total = tvAmount.getText().toString();
                    Intent intent = new Intent(ActivityRequests.this, ActivityLoading.class);
                    intent.putExtra("txnType", "refund");
                    intent.putExtra("Total", total);
                    intent.putExtra("Tip", "0");
                    intent.putExtra("Discount", "0");
                    startActivity(intent);
                    finish();
                });
                break;
            case "reversal":
                tvRequestTitle.setText("REVERSAL REQUEST");
                txtAmountLabel.setText("Last Transaction ID: ");
                tvAmount.setText(((lastTxid == null) ? "0" : lastTxid));
                //txtAmountLabel.setText("Last Transaction ID: " + ((lastTxid == null) ? "0" : lastTxid));
                //tvAmount.setVisibility(View.GONE);
//                btnSendReq.setOnClickListener(v -> sendReversal());
                break;
            case "cashout":
                tvRequestTitle.setText("CASHOUT REQUEST");
//                btnSendReq.setOnClickListener(v -> sendCashOut());
                break;
            case "preauth":
                tvRequestTitle.setText("PREAUTH REQUEST");
                txtAmountLabel.setText("ID: " + ((lastTxid == null) ? "0" : lastTxid));
                tvAmount.setVisibility(View.GONE);
//                btnSendReq.setOnClickListener(v -> sendPreAuth());
                break;
            case "completion":
                tvRequestTitle.setText("COMPLETION REQUEST");
                txtAmountLabel.setText("ID: " + ((lastTxid == null) ? "0" : lastTxid));
                tvAmount.setVisibility(View.GONE);
//                btnSendReq.setOnClickListener(v -> sendCompletion());
                break;
            case "txnstatus":
                tvRequestTitle.setText("TRANSACTION STATUS REQUEST");
                txtAmountLabel.setText("STATUS: " + ((lastTxid == null) ? "0" : lastTxid));
                tvAmount.setVisibility(View.GONE);
//                btnSendReq.setOnClickListener(v -> sendTransactionStatusRequest());
                break;
            case "cardacq":
                tvRequestTitle.setText("CARD ACQUISITION REQUEST");
                txtAmountLabel.setText("CARD: " + ((lastTxid == null) ? "0" : lastTxid));
                tvAmount.setVisibility(View.GONE);
//                btnSendReq.setOnClickListener(v -> sendCardAcquisitionRequest());
                break;
            default:
                tvRequestTitle.setText("no match");
                txtAmountLabel.setVisibility(View.GONE);
                tvAmount.setVisibility(View.GONE);
        }

    }
}