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


public class ActivityCart extends AppCompatActivity {

    private TextView inputTotal;
    private TextView inputDiscount;
    private TextView inputTip;
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
        setContentView(R.layout.activity_cart);
        initObjects();

        final Button btnPay = findViewById(R.id.btnPay);
        btnPay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                initObjects();
                Intent intent = new Intent(ActivityCart.this, ActivityLoading.class);
                intent.putExtra("txnType", "payment");
                intent.putExtra("Total", inputTotal.getText().toString());
                intent.putExtra("Tip", inputDiscount.getText().toString());
                intent.putExtra("Discount", inputTip.getText().toString());
                startActivity(intent);
                finish();
            }
        });
    }


    void initObjects() {
        inputTotal = findViewById(R.id.inputTotal);
        inputDiscount = findViewById(R.id.inputDiscount);
        inputTip = findViewById(R.id.inputTip);
    }

}


