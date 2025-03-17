package lk.codebridge.travelmate;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;

import es.dmoral.toasty.Toasty;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SingleTravelPackageActivity extends AppCompatActivity {

    private JsonObject jsonObject;

    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private SensorEventListener proximityListener;

    //Check Network
    private NetworkReceiver networkReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_single_travel_package);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        ImageView imageViewBack = findViewById(R.id.imageView6);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                finish();

            }
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        // Proximity sensor listener
        proximityListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                    float distance = event.values[0]; // Get proximity distance

                    Log.d("ProximitySensor", "Distance: " + distance);

                    // If an object is close (distance < max range of the sensor), close the app
                    if (distance < proximitySensor.getMaximumRange()) {
//                        Toast.makeText(SingleTravelPackageActivity.this, "Object detected near! Closing app...", Toast.LENGTH_SHORT).show();

                        Log.d("ProximitySensor", "Object is near. Closing app...");

                        new Handler().postDelayed(() -> {
                            Log.d("ProximitySensor", "App is closing...");

//                            SingleTravelPackageActivity.this.finishAffinity();

                            addToWishList();

                        }, 1000); // Delay of 1 second to show the toast
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Not needed for this example
            }
        };

        Gson gson = new Gson();

        Intent intent = getIntent();
        String data = intent.getStringExtra("data");

        jsonObject = gson.fromJson(data, JsonObject.class);

        String id = jsonObject.get("id").getAsString();

        TextView tittleTxt = findViewById(R.id.titleText);
        TextView priceTxt = findViewById(R.id.priceTextReal);
        TextView location = findViewById(R.id.locationText);
        TextView descOneTxt = findViewById(R.id.descriptionTextOne);
        TextView nightsTxt = findViewById(R.id.nightCount);
        TextView daysTxt = findViewById(R.id.daysCount_modern);
        TextView additionalData = findViewById(R.id.additionalInfoText);
        ImageView imageView = findViewById(R.id.imageViewPackageImage);

        tittleTxt.setText(jsonObject.get("packageName").getAsString());
        priceTxt.setText(jsonObject.get("pricePerPerson").getAsString());
        location.setText(jsonObject.get("city").getAsString());
        descOneTxt.setText(jsonObject.get("descOne").getAsString());
        nightsTxt.setText(jsonObject.get("nights").getAsString());
        daysTxt.setText(jsonObject.get("days").getAsString());
        additionalData.setText(jsonObject.get("descTwo").getAsString());

        Bitmap bitmap = loadImageFromInternalStorage(id + "_image.png");

        // Set the Bitmap to an ImageView
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            Log.e("ImageLoad", "Failed to load image from storage");
        }


        ImageView locationView = findViewById(R.id.wifiIcon);

        locationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent1 = new Intent(SingleTravelPackageActivity.this,TravelLocationActivity.class);
                intent1.putExtra("data",jsonObject.get("location").getAsString());
                intent1.putExtra("city",jsonObject.get("city").getAsString());
                startActivity(intent1);

            }
        });

        Button buttonWhereIVisit = findViewById(R.id.whereToVisitButton);

        buttonWhereIVisit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intentWhereIVisit = new Intent(SingleTravelPackageActivity.this, AboutMoreWebViewActivity.class);
                intentWhereIVisit.putExtra("urlWeb", jsonObject.get("wikiUrl").getAsString());
                startActivity(intentWhereIVisit);

            }
        });

        Button buyPackage = findViewById(R.id.buyPackageButton);

        buyPackage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intentWhereIVisit = new Intent(SingleTravelPackageActivity.this, BookingActivity.class);
                intentWhereIVisit.putExtra("data", gson.toJson(jsonObject));
                startActivity(intentWhereIVisit);

            }
        });


        Button addToWish = findViewById(R.id.addToWishlistButton);
        addToWish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                addToWishList();
            }
        });

    }

    public void addToWishList() {
        String url = getString(R.string.url);

        new Thread(new Runnable() {
            @Override
            public void run() {

                OkHttpClient okHttpClient = new OkHttpClient();
                SharedPreferences sp = getSharedPreferences("lk.codebridge.travelmate", Context.MODE_PRIVATE);

                String email = sp.getString("userEmail", null);

                Request request = new Request.Builder().url(url + "/travelmate/addToWishlist?email=" + email + "&packageId=" + jsonObject.get("id").getAsString()).build();

                try {
                    Response response = okHttpClient.newCall(request).execute();

                    String responseString = response.body().string();

                    if (response.isSuccessful()) {

                        if (responseString.equals("Already in wishlist")) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toasty.info(SingleTravelPackageActivity.this, "This is already in wishlist", Toasty.LENGTH_SHORT, true).show();
                                }
                            });
                        } else {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toasty.success(SingleTravelPackageActivity.this, "Successfully added to wishlist.", Toasty.LENGTH_SHORT, true).show();
                                }
                            });

                        }


                    } else {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toasty.error(SingleTravelPackageActivity.this, "Something went wrong, Try again later.", Toasty.LENGTH_SHORT, true).show();
                            }
                        });

                    }

                } catch (IOException e) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toasty.error(SingleTravelPackageActivity.this, "Something went wrong, Try again later.", Toasty.LENGTH_SHORT, true).show();
                        }
                    });


                    throw new RuntimeException(e);
                }

            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (proximitySensor != null) {
            // Register the listener for the proximity sensor
            sensorManager.registerListener(proximityListener, proximitySensor, SensorManager.SENSOR_DELAY_UI);
        }
        networkReceiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (proximitySensor != null) {
            // Unregister the listener when the activity is paused
            sensorManager.unregisterListener(proximityListener);
        }
        if (networkReceiver != null) {
            unregisterReceiver(networkReceiver);
        }
    }

    private Bitmap loadImageFromInternalStorage(String fileName) {
        try {
            // Get the file from internal storage
            File file = new File(getFilesDir(), fileName);

            // Decode the file into a Bitmap
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ImageLoad", "Error loading image: " + e.getMessage());
            return null;
        }
    }
}