package lk.codebridge.travelmate;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import es.dmoral.toasty.Toasty;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class RegisterActivity extends AppCompatActivity {

    private Button buttonRegister;

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
        setContentView(R.layout.activity_register);
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

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        Button buttonBackToSignIn = findViewById(R.id.backToSignin);

        buttonBackToSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intentBackToSignIn = new Intent(RegisterActivity.this, SignInActivity.class);

                startActivity(intentBackToSignIn);

            }
        });


        buttonRegister = findViewById(R.id.RegButton);

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                buttonRegister.setEnabled(false);
                registerUser();
            }
        });

    }

    //Register user
    public void registerUser() {

        TextView textViewName = findViewById(R.id.regName);
        TextView textViewEmail = findViewById(R.id.regEmail);
        TextView textViewMobile = findViewById(R.id.regMobile);
        TextView textViewPassword = findViewById(R.id.regPassword);


        String name = textViewName.getText().toString();
        String email = textViewEmail.getText().toString();
        String mobile = textViewMobile.getText().toString();
        String password = textViewPassword.getText().toString();


        if (name.isEmpty()) {

            Toasty.error(RegisterActivity.this, "Your name is empty.", Toast.LENGTH_SHORT).show();
            buttonRegister.setEnabled(true);

        } else if (email.isEmpty()) {

            Toasty.error(RegisterActivity.this, "Your email is empty.", Toast.LENGTH_SHORT).show();
            buttonRegister.setEnabled(true);

        } else if (mobile.isEmpty()) {

            Toasty.error(RegisterActivity.this, "Your mobile is empty.", Toast.LENGTH_SHORT).show();
            buttonRegister.setEnabled(true);

        } else if (password.isEmpty()) {

            Toasty.error(RegisterActivity.this, "Your password is empty.", Toast.LENGTH_SHORT).show();
            buttonRegister.setEnabled(true);

        } else if (!mobile.matches("(?:\\+94|94|0)(7[0-9]{8})")) {

            Toasty.error(RegisterActivity.this, "Invalid mobile number.", Toast.LENGTH_SHORT).show();
            buttonRegister.setEnabled(true);

        } else if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {

            Toasty.error(RegisterActivity.this, "Invalid email address.", Toast.LENGTH_SHORT).show();
            buttonRegister.setEnabled(true);

        } else {

            sendrequestForregister(name, email, mobile, password);
//            Button buttonRegister = findViewById(R.id.RegButton);
//            buttonRegister.setEnabled(false);

        }
    }

    public void sendrequestForregister(String name, String email, String mobile, String password) {

        Thread threadOne = new Thread() {

            @Override
            public void run() {

                try {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("name", name);
                    jsonObject.addProperty("email", email);
                    jsonObject.addProperty("mobile", mobile);
                    jsonObject.addProperty("password", password);

                    Gson gson = new Gson();
                    RequestBody body = RequestBody.create(gson.toJson(jsonObject), MediaType.get("application/json"));

                    String url = getString(R.string.url);

                    Request request = new Request.Builder()
                            .url(url + "/travelmate/userRegister")
                            .post(body)
                            .build();

                    OkHttpClient okHttpClient = new OkHttpClient();
                    try (Response response = okHttpClient.newCall(request).execute()) {

                        String responseBody = response.body().string();


                        if (response.isSuccessful()) {


                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    Log.i("error", responseBody);
                                    Toasty.success(RegisterActivity.this, "Your registration is successful.", Toast.LENGTH_SHORT).show();

                                    Intent intentAfterRegister = new Intent(RegisterActivity.this, OtpVerificationActivity.class);
                                    intentAfterRegister.putExtra("toPath", "SignIn");
                                    intentAfterRegister.putExtra("user", "OTP is send to " + email);
                                    intentAfterRegister.putExtra("email", email);
                                    startActivity(intentAfterRegister);
                                }
                            });

                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.i("error", responseBody);
                                    Toasty.error(RegisterActivity.this, responseBody, Toast.LENGTH_SHORT).show();
                                    Button buttonRegister = findViewById(R.id.RegButton);
                                    buttonRegister.setEnabled(true);
                                }
                            });

                        }
                    }
                } catch (Exception e) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toasty.error(RegisterActivity.this, "Something went wrong, Try again later.", Toasty.LENGTH_SHORT, true).show();
                        }
                    });

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            buttonRegister.setEnabled(true);
                        }
                    });
                    e.printStackTrace();

                }


            }
        };

        threadOne.start();

    }
}