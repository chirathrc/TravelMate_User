package lk.codebridge.travelmate;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import es.dmoral.toasty.Toasty;
import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CheckoutActivity extends AppCompatActivity {

    int PAYHERE_REQUEST;

    String nameUser;
    String mobile;
    String email;

    String packageId;

    String date;

    String personsCount;

    String total;

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
        setContentView(R.layout.activity_checkout);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String url = getString(R.string.url);

        Intent intent = getIntent();
        packageId = intent.getStringExtra("packageId");
        personsCount = intent.getStringExtra("persons");
        total = intent.getStringExtra("total");
        String name = intent.getStringExtra("name");
        date = intent.getStringExtra("date");

        TextView textViewPackageName = findViewById(R.id.packageName_checkOutFinal);
        TextView totalPrice_checkoutFinal = findViewById(R.id.totalPrice_checkoutFinal);
        ImageView imageView = findViewById(R.id.imageView_checkOutFinal);
        TextView checkoutPersons = findViewById(R.id.checkoutPersons);
        TextView checkoutDateFinal = findViewById(R.id.checkoutDateFinal);

        TextView userName = findViewById(R.id.checkoutName);
        TextView userEmail = findViewById(R.id.checkoutEmail);
        TextView userMobile = findViewById(R.id.checkoutMobile);

        textViewPackageName.setText(name);
        totalPrice_checkoutFinal.setText(total);
        checkoutPersons.setText(personsCount);
        checkoutDateFinal.setText(date);


        Bitmap bitmap = loadImageFromInternalStorage(packageId + "_image.png");

        // Set the Bitmap to an ImageView
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            Log.e("ImageLoad", "Failed to load image from storage");
        }

        SQLiteOpenHelper sqLiteOpenHelper = LoadingActivity.sqLiteOpenHelper;

        new Thread(new Runnable() {
            @Override
            public void run() {
                SQLiteDatabase sqLiteDatabase = sqLiteOpenHelper.getReadableDatabase();
                String[] projection = new String[]{"name", "email", "mobile"};

                Cursor cursor = sqLiteDatabase.query("user", null, null, null, null, null, null);

                while (cursor.moveToNext()) {
                    nameUser = cursor.getString(1);
                    email = cursor.getString(2);
                    mobile = cursor.getString(3);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            userName.setText(nameUser);
                            userEmail.setText(email);
                            userMobile.setText(mobile);
                        }
                    });

                    Log.i("nameUser", nameUser);
                }
            }
        }).start();

        Button checkoutButton_checkoutFinal = findViewById(R.id.checkoutButton_checkoutFinal);
        checkoutButton_checkoutFinal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                InitRequest req = new InitRequest();
                req.setMerchantId("1229571");       // Merchant ID
                req.setCurrency("USD");             // Currency code LKR/USD/GBP/EUR/AUD
                req.setAmount(Double.parseDouble(total));             // Final Amount to be charged

                String uniqueOrderId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                req.setOrderId(uniqueOrderId);

                req.setItemsDescription(name);  // Item description title
                req.setCustom1("");
                req.setCustom2("");
                req.getCustomer().setFirstName(nameUser);
                req.getCustomer().setLastName("");
                req.getCustomer().setEmail(email);
                req.getCustomer().setPhone(mobile);
                req.getCustomer().getAddress().setAddress("");
                req.getCustomer().getAddress().setCity("");
                req.getCustomer().getAddress().setCountry("");

                PAYHERE_REQUEST = 12345;


                Log.d("Payment Params", "Merchant ID: " + req.getMerchantId());
                Log.d("Payment Params", "Amount: " + req.getAmount());
                Log.d("Payment Params", "Order ID: " + req.getOrderId());
                Log.d("Payment Params", "Customer Name: " + req.getCustomer().getFirstName() + " " + req.getCustomer().getLastName());


                req.setNotifyUrl(url + "/travelmate/pay/confirmation");


                Intent intent = new Intent(CheckoutActivity.this, PHMainActivity.class);
                intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
                PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL);
                startActivityForResult(intent, PAYHERE_REQUEST); //unique request ID e.g. "11001"

            }
        });


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        new Thread(new Runnable() {
            @Override
            public void run() {

                if (requestCode == PAYHERE_REQUEST && data != null && data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                    PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

                    if (response != null) {
                        Gson gson = new Gson();
                        JsonObject object = gson.fromJson(gson.toJson(response), JsonObject.class);
                        Log.e("PAYHERE_RESPONSE", "Full Response: " + gson.toJson(response));

                        if (object.get("status").getAsInt() == 1) {
                            Log.i("statusId", String.valueOf(object.get("status").getAsInt()));

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    updateData();
                                }
                            }).start();


                            // Check and request notification permission (for Android 13+)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(CheckoutActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(CheckoutActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                                } else {

                                    runOnUiThread(() -> {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(CheckoutActivity.this);
                                        builder.setTitle("Payment Successful").setMessage("We will contact you for further details.").setIcon(R.drawable.check) // Replace with a valid icon or remove this line
                                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        dialog.dismiss(); // Close the dialog

                                                        Intent intent1 = new Intent(CheckoutActivity.this, HomeActivityNew.class);
                                                        startActivity(intent1);
                                                    }
                                                }).setCancelable(false) // Prevents closing the dialog by tapping outside
                                                .show();
                                    });

                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            showPaymentNotification();
                                        }
                                    }).start();
                                }
                            } else {

                                runOnUiThread(() -> {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(CheckoutActivity.this);
                                    builder.setTitle("Payment Successful").setMessage("We will contact you for further details.").setIcon(R.drawable.check) // Replace with a valid icon or remove this line
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();

                                                    Intent intent1 = new Intent(CheckoutActivity.this, HomeActivityNew.class);
                                                    startActivity(intent1);
                                                }
                                            }).setCancelable(false).show();
                                });
                            }
                        } else {
                            errorMsg();
                        }

                        if (response.getData() != null) {
                            StatusResponse statusResponse = response.getData();
                            Log.e("status", String.valueOf(statusResponse.getMessage()));
                        } else {
                            Log.e("PAYHERE_ERROR", "Response data is null");
                        }
                    } else {
                        Log.e("PAYHERE_ERROR", "Null response received");
                        errorMsg();
                    }
                }


            }
        }).start();


    }


    public void updateData() {

        String url = getString(R.string.url);

        Gson gson = new Gson();

        JsonObject order = new JsonObject();
        order.addProperty("userEmail", email);
        order.addProperty("packageId", packageId);
        order.addProperty("date", date);
        order.addProperty("persons", personsCount);

        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody body = RequestBody.create(gson.toJson(order), MediaType.get("application/json"));

        Request request = new Request.Builder().url(url + "/travelmate/pay/confirmationInUI").post(body).build();

        try {

            Response response = okHttpClient.newCall(request).execute();

            new Thread(new Runnable() {
                @Override
                public void run() {

                    if (response.isSuccessful()) {

                        Log.i("DatabaseUpdate", "success");


                        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();

                        Map<String, Object> bookingData = new HashMap<>();
                        bookingData.put("userEmail", email);
                        bookingData.put("packageId", packageId);
                        bookingData.put("date", date);
                        bookingData.put("persons", personsCount);
                        bookingData.put("booked_date", LocalDate.now().toString());
                        bookingData.put("total", total);

                        firebaseFirestore.collection("order").add(bookingData).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {

                                Log.i("firebaseInsert", "Success");

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.i("firebaseInsert", "Failed");
                            }
                        });


                    } else {

                        Log.i("DatabaseUpdate", "ErrorInSuccess");

                    }

                }
            }).start();


        } catch (IOException e) {
            Log.i("DatabaseUpdate", "ErrorInCatch");
            throw new RuntimeException(e);
        }

    }


    public void errorMsg() {

        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(CheckoutActivity.this);
            builder.setTitle("Something Went Wrong").setMessage("Please try again.").setIcon(R.drawable.warning) // Replace with a valid error icon or remove this line
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss(); // Close the dialog

                            Intent intent1 = new Intent(CheckoutActivity.this, HomeActivityNew.class);
                            startActivity(intent1);
                        }
                    }).setCancelable(false) // Prevents closing the dialog by tapping outside
                    .show();
        });


    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showPaymentNotification();
            } else {
                Log.e("Notification", "Permission denied! Notifications will not be shown.");
            }
        }
    }

    // Method to show notification
    private void showPaymentNotification() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "C1";
            NotificationChannel notificationChannel = new NotificationChannel(channelId, "Payment Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(notificationChannel);

            Notification builder = new Notification.Builder(CheckoutActivity.this, channelId).setContentTitle("Payment").setContentText("Payment Successful").setSmallIcon(R.drawable.tr).setPriority(Notification.PRIORITY_DEFAULT).build();

            notificationManager.notify(1, builder);
            Log.i("Notification", "Payment notification sent successfully!");
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