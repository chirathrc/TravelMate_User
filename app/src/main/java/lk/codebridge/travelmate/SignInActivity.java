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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignInActivity extends AppCompatActivity {

//    public static SQLiteOpenHelper sqLiteOpenHelper;


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
        setContentView(R.layout.activity_sign_in);
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

        Button buttonRegisterNavigate = findViewById(R.id.signInButton2);
        buttonRegisterNavigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intentOne = new Intent(SignInActivity.this, RegisterActivity.class);

                startActivity(intentOne);

            }
        });

        Button signInButton = findViewById(R.id.signInButton);

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                TextView emailTextView = findViewById(R.id.signEmail);
                TextView passwordTextView = findViewById(R.id.signPassword);

                String email = emailTextView.getText().toString();
                String password = passwordTextView.getText().toString();

                Thread threadSignIn = new Thread() {
                    @Override
                    public void run() {

                        Log.i("data", email + password);

                        JsonObject userLoginDetails = new JsonObject();
                        userLoginDetails.addProperty("email", email);
                        userLoginDetails.addProperty("password", password);

                        Gson gson = new Gson();

                        String url = getString(R.string.url);

                        RequestBody body = RequestBody.create(gson.toJson(userLoginDetails), MediaType.get("application/json"));

                        String reqUrl = url + "/travelmate/userSignIn";

                        Log.i("SignIn_url",reqUrl);
                        Request request = new Request.Builder().url(reqUrl).post(body).build();

                        OkHttpClient okHttpClient = new OkHttpClient();

                        try {

                            Response response = okHttpClient.newCall(request).execute();
                            String responseBody = response.body().string();

                            if (response.isSuccessful()) {


                                if (responseBody.equals("OTP not submitted")) {

                                    Intent intentAfterRegister = new Intent(SignInActivity.this, OtpVerificationActivity.class);
                                    intentAfterRegister.putExtra("user", "OTP is send to " + email);
                                    intentAfterRegister.putExtra("toPath", "Home");
                                    intentAfterRegister.putExtra("email", email);
                                    startActivity(intentAfterRegister);

                                } else {

                                    JsonObject userDetailsAfterLogin = gson.fromJson(responseBody, JsonObject.class);

                                    Log.i("user",responseBody);

                                    SharedPreferences sp = getSharedPreferences("lk.codebridge.travelmate", Context.MODE_PRIVATE);

                                    SharedPreferences.Editor editor = sp.edit();
//                                    editor.putString("userName", userDetailsAfterLogin.get("name").getAsString());
                                    editor.putString("userEmail", userDetailsAfterLogin.get("email").getAsString());
//                                    editor.putString("userMobile", userDetailsAfterLogin.get("password").getAsString());
                                    editor.putBoolean("isLogin", true);

                                    editor.apply();

                                    SQLiteDatabase sqLiteDatabase = LoadingActivity.sqLiteOpenHelper.getWritableDatabase();
                                    ContentValues contentValues = new ContentValues();
                                    contentValues.put("name",userDetailsAfterLogin.get("name").getAsString());
                                    contentValues.put("email",userDetailsAfterLogin.get("email").getAsString());
                                    contentValues.put("mobile",userDetailsAfterLogin.get("mobile").getAsString());
                                    contentValues.put("password",password);

                                    Long idInsert = sqLiteDatabase.insert("user",null,contentValues);
                                    Log.i("idInsert",String.valueOf(idInsert));


                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            Toasty.success(SignInActivity.this, "Login Successful", Toasty.LENGTH_SHORT).show();
                                            Intent intentHome = new Intent(SignInActivity.this, HomeActivityNew.class);
                                            startActivity(intentHome);

                                        }
                                    });


                                }


                            } else {

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        Toasty.error(SignInActivity.this, "Invalid details", Toasty.LENGTH_SHORT).show();

                                    }
                                });

                            }

                        } catch (IOException e) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toasty.error(SignInActivity.this, "Something went wrong, Try again later.", Toasty.LENGTH_SHORT, true).show();
                                }
                            });

                            throw new RuntimeException(e);
                        }
                    }
                };

                threadSignIn.start();


            }
        });
    }
}