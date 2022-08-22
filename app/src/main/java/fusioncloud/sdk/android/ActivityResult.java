package fusioncloud.sdk.android;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import com.example.posonandroidva.R;

import java.util.Objects;

public class ActivityResult extends AppCompatActivity {

    String StatusMessage = "";
    String txnID = "";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        //GlobalClass globalClass = (GlobalClass) getApplicationContext();

        Bundle bundle = getIntent().getExtras();
        TextView tvReceipt = (TextView) findViewById(R.id.tvReceipt);
        TextView tvMessageHead = (TextView) findViewById(R.id.tvMessageHead);
        TextView tvMessageDetail = (TextView) findViewById(R.id.tvMessageDetail);

        String txntype = getIntent().getStringExtra("txnType");
        if (Objects.equals(txntype, "login")){
            String logindetails = getIntent().getStringExtra("logindetails");
            tvMessageHead.setText("Login Failed");
            tvMessageDetail.setText(logindetails + "\n");
            tvReceipt.setVisibility(View.GONE);
        }else if(Objects.equals(txntype, "NotConnectedException")) {
            tvMessageHead.setText("No Internet");
            tvMessageDetail.setText("Please make sure POS is connected to the internet");
            tvReceipt.setVisibility(View.GONE);
        }else if(Objects.equals(txntype, "timeout")){
            tvMessageHead.setText("Transaction Timeout");
            tvMessageDetail.setText("Transaction Aborted");
            tvReceipt.setVisibility(View.GONE);
        }else if(Objects.equals(txntype, "errorhandler")) {
            tvMessageHead.setText("Connection Timeout");
            tvMessageDetail.setText("Please check transaction history in on pinpad");
            tvReceipt.setVisibility(View.GONE);
        }
        else
        {
            String OutputXHTML = getIntent().getStringExtra("OutputXHTML");
            String paymentresult = getIntent().getStringExtra("paymentresult");

            String authorizedamount = getIntent().getStringExtra("authorizedamount");
            String surcharge = getIntent().getStringExtra("surcharge");
            String tip = getIntent().getStringExtra("tip");

            switch (txntype) {
                case "payment":
                case "checktransaction":
                    tvMessageHead.setText("Payment " + paymentresult);
                    tvMessageHead.setTextColor(Color.parseColor("#FF4CAF50"));

                    tvMessageDetail.setText("Authorized Amount: " + authorizedamount + "\n"
                            + "Surcharge: " + surcharge + "\n"
                            + "Tip: " + tip
                    );
                    tvReceipt.setText(HtmlCompat.fromHtml(OutputXHTML, 0));
                    break;

                case "refund":
                    tvMessageHead.setText("Refund " + paymentresult);
                    tvMessageHead.setTextColor(Color.parseColor("#FF4CAF50"));

                    tvMessageDetail.setText("Authorized Amount: " + authorizedamount + "\n");

                    tvReceipt.setText(HtmlCompat.fromHtml(OutputXHTML, 0));
                    break;
                case "cancel":
                    tvMessageHead.setText("Transaction Cancelled");
                    tvMessageHead.setTextColor(Color.parseColor("#FF4CAF50"));

                    tvMessageDetail.setText("Authorized Amount: " + authorizedamount + "\n"
                            + "Surcharge: " + surcharge + "\n"
                            + "Tip: " + tip
                    );

                    tvReceipt.setText(HtmlCompat.fromHtml(OutputXHTML, 0));
                    break;
                default:
                    tvMessageHead.setText("Error \n" + txntype);
                    break;
            }
        }


    }
}
