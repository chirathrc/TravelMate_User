package lk.codebridge.travelmate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.Wave;

import java.util.List;

import es.dmoral.toasty.Toasty;

public class LoadingActivity extends AppCompatActivity {

    public static SQLiteOpenHelper sqLiteOpenHelper;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        ProgressBar progressBar = findViewById(R.id.spin_kitNew);
        Sprite doubleBounce = new Wave();
        progressBar.setIndeterminateDrawable(doubleBounce);

        // Initialize and register network receiver
        networkChangeReceiver = new NetworkChangeReceiver();
        registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));


        // Check network status on startup
        if (isNetworkAvailable()) {
            startApp();
        } else {
            Log.i("Network Status", "No Internet Connection");
//            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show();
        }

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(networkChangeReceiver);  // Unregister receiver to prevent leaks
    }

    private void showNoInternetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Internet Connection")
                .setMessage("You cannot proceed without an internet connection. Please check your network and try again.")
                .setCancelable(false) // Prevent dismissing by tapping outside
                .setPositiveButton("Retry", (dialog, which) -> {
                    if (isNetworkAvailable()) {
                        startApp(); // Retry starting the app if internet is back
                    } else {
                        showNoInternetDialog(); // Show the alert again
                    }
                })
                .setNegativeButton("Exit", (dialog, which) -> finishAffinity()); // Close app if user chooses

        AlertDialog alert = builder.create();
        alert.show();
    }

    private void startApp() {
        sqLiteOpenHelper = new SQLiteOpenHelper(LoadingActivity.this, "travelMate.db", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase sqLiteDatabase) {
                sqLiteDatabase.execSQL("CREATE TABLE user ( user_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, email TEXT NOT NULL, mobile TEXT NOT NULL, password TEXT NOT NULL );");
            }

            @Override
            public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            }
        };

        SharedPreferences sp = getSharedPreferences("lk.codebridge.travelmate", Context.MODE_PRIVATE);
        Intent intent = sp.getBoolean("isLogin", false) ?
                new Intent(this, HomeActivityNew.class) :
                new Intent(this, MainActivity.class);

        startActivity(intent);
        finish();
    }

    // NetworkChangeReceiver class inside LoadingActivity
    private class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isNetworkAvailable(context)) {
                startApp();
                Toasty.info(context, "Internet Connected", Toast.LENGTH_SHORT, true).show();
            } else {
                showNoInternetDialog();
//                Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show();
            }
        }

        private boolean isNetworkAvailable(Context context) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
            return false;
        }
    }
}
