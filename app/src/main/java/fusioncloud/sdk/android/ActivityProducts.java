package fusioncloud.sdk.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posonandroidva.R;

import java.util.ArrayList;
import java.util.List;

public class ActivityProducts extends AppCompatActivity {

    List<Model> modelList;
    RecyclerView recyclerView;
    OrderAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        // creating an arraylist

        modelList = new ArrayList<>();
        modelList.add(new Model("Green Tea", getString(R.string.greentea), R.drawable.greentea ));
        modelList.add(new Model("Latte", getString(R.string.latte), R.drawable.late));
        modelList.add(new Model("Orange Smoothie", getString(R.string.orangesmoothie), R.drawable.orange));
        modelList.add(new Model("Orange Vanilla", getString(R.string.orangevanilla), R.drawable.orangevanilla));
        modelList.add(new Model("Cappucino", getString(R.string.cappcuni), R.drawable.cappcunio));
        modelList.add(new Model("Thai Tea", getString(R.string.thaitea), R.drawable.thaitea));
        modelList.add(new Model("Tea", getString(R.string.tea), R.drawable.tea));
        modelList.add(new Model("Bubble Tea", getString(R.string.bubbletea), R.drawable.milk));
        modelList.add(new Model("Matcha", getString(R.string.match), R.drawable.match));

        // recyclerview
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(null));
        // adapter
        mAdapter = new OrderAdapter(this, modelList);
        recyclerView.setAdapter(mAdapter);


    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.back, menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            case R.id.back:
                Intent i=new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }
}