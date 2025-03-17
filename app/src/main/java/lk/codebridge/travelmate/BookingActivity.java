package lk.codebridge.travelmate;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import es.dmoral.toasty.Toasty;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BookingActivity extends AppCompatActivity {

    private JsonObject jsonObjectResponseData;

    private int totalPrice;

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
        setContentView(R.layout.activity_booking);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Gson gson = new Gson();

        Intent intent = getIntent();
        String data = intent.getStringExtra("data");

        String url = getString(R.string.url);

        JsonObject dataObject = gson.fromJson(data, JsonObject.class);

        OkHttpClient okHttpClient = new OkHttpClient();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Request request = new Request.Builder().url(url + "/travelmate/package/getPackageFromId/" + dataObject.get("id").getAsString()).build();

                try {
                    Response response = okHttpClient.newCall(request).execute();

                    String responseData = response.body().string();

                    if (response.isSuccessful()) {

                        jsonObjectResponseData = gson.fromJson(responseData, JsonObject.class);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                TextView packageName_checkOut = findViewById(R.id.packageName_checkOut);
                                TextView daysCount_checkOut = findViewById(R.id.daysCount_checkOut);
                                TextView nightsCount_checkOut = findViewById(R.id.nightsCount_checkOut);
                                TextView price_checkOut = findViewById(R.id.price_checkOut);
                                ImageView imageView_checkOut = findViewById(R.id.imageView_checkOut);

                                packageName_checkOut.setText(jsonObjectResponseData.get("packageName").getAsString());
                                daysCount_checkOut.setText(jsonObjectResponseData.get("days").getAsString());
                                nightsCount_checkOut.setText(jsonObjectResponseData.get("nights").getAsString());
                                price_checkOut.setText(jsonObjectResponseData.get("pricePerPerson").getAsString());


                                Bitmap bitmap = loadImageFromInternalStorage(jsonObjectResponseData.get("id").getAsString() + "_image.png");

                                // Set the Bitmap to an ImageView
                                if (bitmap != null) {
                                    imageView_checkOut.setImageBitmap(bitmap);
                                } else {
                                    Log.e("ImageLoad", "Failed to load image from storage");
                                }


                            }
                        });


                    } else {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                Toasty.error(BookingActivity.this, "Something went wrong. Please try again later", Toasty.LENGTH_SHORT, true).show();

                            }
                        });


                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }).start();

        EditText datePicker = findViewById(R.id.datePicker);

        // Set click listener for the date picker EditText
//        datePicker.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showDatePickerDialog(datePicker);
//            }
//        });

        datePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog(datePicker);
            }
        });


        EditText editTextPersonCount = findViewById(R.id.personCount_checkout);
        editTextPersonCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                TextView totalPrice_checkout = findViewById(R.id.totalPrice_checkout);

                float PP_price = jsonObjectResponseData.get("pricePerPerson").getAsFloat();

                String personCountTxt = charSequence.toString();

                if (personCountTxt.isEmpty() || personCountTxt == null) {

                    totalPrice_checkout.setText("0");

                } else {

                    int persons = Integer.parseInt(personCountTxt);

                    totalPrice = (int) (persons * PP_price);

                    totalPrice_checkout.setText(String.valueOf(totalPrice));
                }


            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        Button checkout = findViewById(R.id.checkoutButton_checkout);

        checkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                EditText dateTxt = findViewById(R.id.datePicker);
                EditText personCountTxt = findViewById(R.id.personCount_checkout);

                String date = dateTxt.getText().toString();
                String persons = personCountTxt.getText().toString();

                Log.i("date",date);
                Log.i("persons",persons);

                if (date.isEmpty() || date.trim().isEmpty() || date.isBlank()) {

                    Toasty.error(BookingActivity.this, "Insert your booking date.", Toasty.LENGTH_SHORT, true).show();

                } else if (persons.equals("0") || persons.isEmpty() || persons.trim().isEmpty()) {

                    Toasty.error(BookingActivity.this, "Insert your person count.", Toasty.LENGTH_SHORT, true).show();

                } else {

                    LayoutInflater layoutInflater = LayoutInflater.from(BookingActivity.this);
                    View alert = layoutInflater.inflate(R.layout.custom_alert_dialog, null, false);

                    TextView textView = alert.findViewById(R.id.totalPriceValue); // Access from alert view
                    textView.setText(String.valueOf(totalPrice));

                    TextView textView1 = alert.findViewById(R.id.personCountValue); // Access from alert view
                    textView1.setText(persons);

                    AlertDialog.Builder builder = new AlertDialog.Builder(BookingActivity.this);
                    builder.setView(alert);
                    AlertDialog dialog = builder.create();
                    dialog.show();

                    Button onConfirm = alert.findViewById(R.id.confirmButton);
                    onConfirm.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            Intent intentCheckout = new Intent(BookingActivity.this, CheckoutActivity.class);
                            intentCheckout.putExtra("packageId",jsonObjectResponseData.get("id").getAsString());
                            intentCheckout.putExtra("persons",persons);
                            intentCheckout.putExtra("total",String.valueOf(totalPrice));
                            intentCheckout.putExtra("name",jsonObjectResponseData.get("packageName").getAsString());
                            intentCheckout.putExtra("date",date);
                            startActivity(intentCheckout);


                        }
                    });

                    Button onCancel = alert.findViewById(R.id.cancelButton);
                    onCancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            dialog.dismiss();

                        }
                    });

                }

            }
        });

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


    private void showDatePickerDialog(EditText datePicker) {
        // Get current date
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Format the selected date and set it to the EditText
                    String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    datePicker.setText(selectedDate);
                },
                year, month, day
        );

        // Set minimum date to today + 2 days (avoid today and tomorrow)
        calendar.add(Calendar.DAY_OF_MONTH, 2); // Add 2 days to today
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());

        // Show the dialog
        datePickerDialog.show();
    }
}