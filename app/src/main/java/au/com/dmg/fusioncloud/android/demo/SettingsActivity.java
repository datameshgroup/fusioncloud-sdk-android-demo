package au.com.dmg.fusioncloud.android.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

/*
    String certificateLocation = Settings.certificateLocation;
    String serverDomain = Settings.serverDomain;
    String socketProtocol = Settings.socketProtocol;

    String kekValue = Settings.kekValue;
    String keyIdentifier = Settings.keyIdentifier;
    String keyVersion = Settings.keyVersion;

    String providerIdentification = Settings.providerIdentification;
    String applicationName = Settings.applicationName;
    String softwareVersion = Settings.softwareVersion;
    String certificationCode = Settings.certificationCode;
*/

    EditText txtSalesID;
    EditText txtPOIID;
    EditText txtKEK;
    EditText txtProviderIdentifiction;
    EditText txtApplicationName;
    EditText txtSoftwareVersion;
    EditText txtCertificationCode;
    EditText txtNexoURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        txtSalesID = findViewById(R.id.txtSalesID);
        txtPOIID = findViewById(R.id.txtPOIID);
        txtKEK = findViewById(R.id.txtKEK);
        txtProviderIdentifiction = findViewById(R.id.txtProviderIdentifiction);
        txtApplicationName = findViewById(R.id.txtApplicationName);
        txtSoftwareVersion = findViewById(R.id.txtSoftwareVersion);
        txtCertificationCode = findViewById(R.id.txtCertificationCode);
        txtNexoURL = findViewById(R.id.txtNexoURL);

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
        txtSalesID.setText(Settings.saleId);
        txtPOIID.setText(Settings.poiId);
        txtKEK.setText(Settings.kekValue);
        txtProviderIdentifiction.setText(Settings.providerIdentification);
        txtApplicationName.setText(Settings.applicationName);
        txtSoftwareVersion.setText(Settings.softwareVersion);
        txtCertificationCode.setText(Settings.certificationCode);
        txtNexoURL.setText(Settings.serverDomain);
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
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

        private void savesettings () {
        try{
            Settings.poiId = txtPOIID.getText().toString();
            Settings.saleId = txtSalesID.getText().toString();
            Settings.kekValue = txtKEK.getText().toString();
            Settings.serverDomain = txtNexoURL.getText().toString();

            Settings.providerIdentification = txtProviderIdentifiction.getText().toString();
            Settings.applicationName = txtApplicationName.getText().toString();
            Settings.softwareVersion = txtSoftwareVersion.getText().toString();
            Settings.certificationCode = txtCertificationCode.getText().toString();

            Toast.makeText(getApplicationContext(),"Settings Updated",Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_SHORT).show();
        }

        }
    }
