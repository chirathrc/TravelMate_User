package lk.codebridge.travelmate;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import es.dmoral.toasty.Toasty;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OtpVerificationActivity extends AppCompatActivity {


    //Check Network
    private NetworkReceiver networkReceiver;

    //Check Network
    @Override
    protected void onResume() {
        super.onResume();
        networkReceiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
    }

    //Check Network
    @Override
    protected void onPause() {
        super.onPause();
        if (networkReceiver != null) {
            unregisterReceiver(networkReceiver);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp_verification);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        Intent intent = getIntent();
        String text = intent.getStringExtra("user");
        String email = intent.getStringExtra("email");
        String toPath = intent.getStringExtra("toPath");

        TextView textView = findViewById(R.id.emailSendTotext);
        textView.setText(text);

        Button otpSubmit = findViewById(R.id.sumbitOtpBtn);


        otpSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Thread thread = new Thread() {
                    @Override
                    public void run() {

                        EditText editText = findViewById(R.id.otpNumberText);

                        String otp = editText.getText().toString();

                        final OkHttpClient okHttpClient = new OkHttpClient();

                        String url = getString(R.string.url);

                        Request request = new Request.Builder().url(url + "/travelmate/otpSubmission?otp=" + otp + "&email=" + email).build();

                        try {
                            Response response = okHttpClient.newCall(request).execute();

                            String resString = response.body().string();

                            if (response.isSuccessful()) {

                                if (toPath.equals("Home")) {

                                    Log.i("response", resString);

                                    JsonObject jsonUserObject = new Gson().fromJson(resString, JsonObject.class);

                                    SharedPreferences sp = getSharedPreferences("lk.codebridge.travelmate", Context.MODE_PRIVATE);

                                    SharedPreferences.Editor editor = sp.edit();
                                    editor.putString("userEmail", jsonUserObject.get("email").getAsString());
                                    editor.putBoolean("isLogin", true);

                                    editor.apply();

                                    SQLiteDatabase sqLiteDatabase = LoadingActivity.sqLiteOpenHelper.getWritableDatabase();
                                    ContentValues contentValues = new ContentValues();
                                    contentValues.put("name", jsonUserObject.get("name").getAsString());
                                    contentValues.put("email", jsonUserObject.get("email").getAsString());
                                    contentValues.put("mobile", jsonUserObject.get("mobile").getAsString());
                                    contentValues.put("password", jsonUserObject.get("password").getAsString());

                                    Long idInsert = sqLiteDatabase.insert("user", null, contentValues);
                                    Log.i("idInsert", String.valueOf(idInsert));


                                    Intent intentToSignIn = new Intent(OtpVerificationActivity.this, HomeActivityNew.class);
                                    startActivity(intentToSignIn);


                                } else {

                                    Intent intentToSignIn = new Intent(OtpVerificationActivity.this, SignInActivity.class);
                                    startActivity(intentToSignIn);

                                }

                            } else {

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toasty.error(OtpVerificationActivity.this, "Invalid OTP").show();
                                    }
                                });

                            }

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }


                    }
                };

                thread.start();


            }
        });

    }
}