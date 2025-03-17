package lk.codebridge.travelmate.ui.home;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import es.dmoral.toasty.Toasty;
import lk.codebridge.travelmate.HomeActivityNew;
import lk.codebridge.travelmate.LoadingActivity;
import lk.codebridge.travelmate.MapActivity;
import lk.codebridge.travelmate.R;
import lk.codebridge.travelmate.SignInActivity;
import lk.codebridge.travelmate.databinding.FragmentHomeBinding;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private SQLiteDatabase sqLiteDatabase;

    private static final int REQUEST_CALL_PERMISSION = 1;



    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        FloatingActionButton floatingActionButton2 = view.findViewById(R.id.floatingActionButton2);

        floatingActionButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                getActivity().finish();
//                Intent intent1 = new Intent(getActivity(), HomeActivityNew.class);
//                startActivity(intent1);


            }
        });


        EditText nameTxt = view.findViewById(R.id.etName);
        EditText emailTxt = view.findViewById(R.id.etEmail);
        EditText mobileTxt = view.findViewById(R.id.etMobile);
        EditText passwordTxt = view.findViewById(R.id.etPassword);


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sqLiteDatabase = LoadingActivity.sqLiteOpenHelper.getReadableDatabase();
                    Cursor cursor = sqLiteDatabase.query("user", null, null, null, null, null, null);

                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            final String name = cursor.getString(1);
                            final String email = cursor.getString(2);
                            final String mobile = cursor.getString(3);
                            final String password = cursor.getString(4);

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (nameTxt != null && emailTxt != null && mobileTxt != null && passwordTxt != null) {
                                        nameTxt.setText(name);
                                        emailTxt.setText(email);
                                        mobileTxt.setText(mobile);
                                        passwordTxt.setText(password);
                                    }
                                }
                            });
                        }
                        cursor.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


        Button updateButton = view.findViewById(R.id.btnUpdate);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SpringAnimation springAnimX = new SpringAnimation(updateButton, SpringAnimation.SCALE_X, 1.1f);  // Zoom to 110%
                SpringAnimation springAnimY = new SpringAnimation(updateButton, SpringAnimation.SCALE_Y, 1.1f);  // Zoom to 110%

                // Set spring parameters for a little bounce
                springAnimX.getSpring().setStiffness(SpringForce.STIFFNESS_LOW)  // Low stiffness for a soft bounce
                        .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);  // Medium bounce for a subtle effect
                springAnimY.getSpring().setStiffness(SpringForce.STIFFNESS_LOW)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);

                // Start the spring animation for scaling up (zoom out effect)
                springAnimX.start();
                springAnimY.start();

                // After the animation finishes, return the button to normal size with a slight bounce back
                springAnimX.addEndListener((animation, canceled, value, velocity) -> {
                    SpringAnimation springAnimResetX = new SpringAnimation(updateButton, SpringAnimation.SCALE_X, 1f);  // Back to normal size
                    SpringAnimation springAnimResetY = new SpringAnimation(updateButton, SpringAnimation.SCALE_Y, 1f);  // Back to normal size
                    springAnimResetX.start();
                    springAnimResetY.start();
                });

                String name = nameTxt.getText().toString();
                String email = emailTxt.getText().toString();
                String mobile = mobileTxt.getText().toString();
                String password = passwordTxt.getText().toString();

                if (name.isEmpty() || name.trim().isEmpty()) {

                    Toasty.error(getContext(), "Insert your name.", Toasty.LENGTH_SHORT, true).show();
                    return;

                } else if (mobile.isEmpty() || mobile.trim().isEmpty() || !mobile.matches("^(?:\\+94|0)(7[01245678])\\d{7}$")) {

                    Toasty.error(getContext(), "Insert your mobile..", Toasty.LENGTH_SHORT, true).show();
                    return;

                } else if (password.isEmpty() || password.trim().isEmpty()) {

                    Toasty.error(getContext(), "Insert your password.", Toasty.LENGTH_SHORT, true).show();
                    return;

                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        JsonObject requestObject = new JsonObject();
                        requestObject.addProperty("name", name);
                        requestObject.addProperty("email", email);
                        requestObject.addProperty("mobile", mobile);
                        requestObject.addProperty("password", password);

                        Gson gson = new Gson();

                        String url = getString(R.string.url);
                        OkHttpClient okHttpClient = new OkHttpClient();
                        RequestBody body = RequestBody.create(gson.toJson(requestObject), MediaType.get("application/json"));

                        Request request = new Request.Builder().url(url + "/travelmate/updateUser").post(body).build();

                        try {
                            Response response = okHttpClient.newCall(request).execute();

                            if (response.isSuccessful()) {

                                String responseString = response.body().string();
                                JsonObject responseObject = gson.fromJson(responseString, JsonObject.class);

                                ContentValues contentValues = new ContentValues();
                                contentValues.put("name", responseObject.get("name").getAsString());
                                contentValues.put("mobile", responseObject.get("mobile").getAsString());
                                contentValues.put("password", responseObject.get("password").getAsString());


                                int count = sqLiteDatabase.update("user",
                                        contentValues,
                                        "`email`=?",
                                        new String[]{email});

                                Log.i("updateUser", count + " " + "Rows updated.");

                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toasty.success(getContext(), "Update Successfully.", Toasty.LENGTH_SHORT, true).show();
                                    }
                                });


                            } else {

                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toasty.error(getContext(), "Something went wrong, try again later.", Toasty.LENGTH_SHORT, true).show();
                                    }
                                });

                            }

                        } catch (IOException e) {

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toasty.warning(getContext(), "Something went wrong, try again later.", Toasty.LENGTH_SHORT, true).show();
                                }
                            });

                            throw new RuntimeException(e);
                        }


                    }
                }).start();


            }
        });


        FloatingActionButton logOut = view.findViewById(R.id.logout_floating);

        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {


                        SQLiteDatabase sqLiteDatabase1 = LoadingActivity.sqLiteOpenHelper.getWritableDatabase();

                        int row = sqLiteDatabase1.delete(
                                "user",
                                null,
                                null);

                        Log.i("LogOUt", row + " Deleted");

                        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("lk.codebridge.travelmate", Context.MODE_PRIVATE);

                        SharedPreferences.Editor editor = sharedPreferences.edit();

                        editor.remove("userEmail");
                        editor.putBoolean("isLogin", false);
                        editor.apply();

                        Intent intent = new Intent(getContext(), SignInActivity.class);
                        startActivity(intent);


                    }
                }).start();


            }
        });

        FloatingActionButton map_floating = view.findViewById(R.id.map_floating);
        map_floating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent1 = new Intent(view.getContext(), MapActivity.class);
                startActivity(intent1);

            }
        });


        FloatingActionButton call = view.findViewById(R.id.call_floating);
        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    Log.i("Permission", "Granted");
                    makePhoneCall(); // Call the function to make a phone call
                } else {
                    Log.i("Permission", "Requesting permission");
                    requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 100);
                }
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, make the call
                makePhoneCall();
            } else {
                // Permission denied, show a message
                Toast.makeText(getContext(), "Permission Denied! Cannot make a call.", Toast.LENGTH_SHORT).show();

                new AlertDialog.Builder(requireContext())
                        .setTitle("Permission Required")
                        .setMessage("You have denied the CALL permission permanently. Please enable it from settings.")
                        .setPositiveButton("Go to Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
    }

    private void makePhoneCall() {
        String phoneNumber = "0713828744"; // Replace with a real phone number

        if (phoneNumber.trim().length() > 0) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + phoneNumber)); // Correctly formatted URI
                startActivity(callIntent);
            } else {
                // Request permission again if not granted
                requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 100);
            }
        } else {
            Toast.makeText(requireContext(), "Phone number is invalid!", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}