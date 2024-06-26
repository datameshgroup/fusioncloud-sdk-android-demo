package au.com.dmg.fusioncloud.android.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    EditText txtSalesID;
    EditText txtPOIID;
    EditText txtKEK;
    EditText txtProviderIdentifiction;
    EditText txtApplicationName;
    EditText txtSoftwareVersion;
    EditText txtCertificationCode;
    Class previousClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        previousClass = getIntent().getClass();

        txtSalesID = findViewById(R.id.txtSalesID);
        txtPOIID = findViewById(R.id.txtPOIID);
        txtKEK = findViewById(R.id.txtKEK);
        txtProviderIdentifiction = findViewById(R.id.txtProviderIdentifiction);
        txtApplicationName = findViewById(R.id.txtApplicationName);
        txtSoftwareVersion = findViewById(R.id.txtSoftwareVersion);
        txtCertificationCode = findViewById(R.id.txtCertificationCode);
        getvalues();

        //disable editing for Static Sales Systems
        txtProviderIdentifiction.setFocusable(false);
        txtProviderIdentifiction.setClickable(false);
        txtApplicationName.setFocusable(false);
        txtApplicationName.setClickable(false);
        txtSoftwareVersion.setFocusable(false);
        txtSoftwareVersion.setClickable(false);
        txtCertificationCode.setFocusable(false);
        txtCertificationCode.setClickable(false);
    }

    private void getvalues() {
        txtSalesID.setText(Settings.getSaleId());
        txtPOIID.setText(Settings.getPoiId());
        txtKEK.setText(Settings.getKek());
        txtProviderIdentifiction.setText(Settings.getProviderIdentification());
        txtApplicationName.setText(Settings.getApplicationName());
        txtSoftwareVersion.setText(Settings.getSoftwareVersion());
        txtCertificationCode.setText(Settings.getCertificationCode());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.back, menu);
        getMenuInflater().inflate(R.menu.savesettings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.savesettings:
                savesettings();
                return true;

            case R.id.back:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

        private void savesettings () {
        try{
            Settings.setPoiId(txtPOIID.getText().toString());
            Settings.setSaleId(txtSalesID.getText().toString());
            Settings.setKek(txtKEK.getText().toString());

            Settings.setProviderIdentification(txtProviderIdentifiction.getText().toString());
            Settings.setApplicationName(txtApplicationName.getText().toString());;
            Settings.setSoftwareVersion(txtSoftwareVersion.getText().toString());
            Settings.setCertificationCode(txtCertificationCode.getText().toString());

            Toast.makeText(getApplicationContext(),"Settings Updated",Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_SHORT).show();
        }

        }
    }
