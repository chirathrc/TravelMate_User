package lk.codebridge.travelmate;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeActivityNew extends AppCompatActivity {


    private PackageAdapter packageAdapter;
    private List<JsonObject> allPackagesList = new ArrayList<>();
    private List<JsonObject> filteredPackagesList = new ArrayList<>();
    private ProgressBar progressBar; // Add ProgressBar reference

    private SliderAdapter adapter;
    private Handler handler = new Handler();
    private Runnable runnable;
    private int currentPage = 0;
    private ViewPager2 viewPager2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_new);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        ImageView imageViewProfile = findViewById(R.id.avatarImageView);
        imageViewProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent profile = new Intent(HomeActivityNew.this, UserProfileActivity.class);
                startActivity(profile);
            }
        });

        TextView aboutUs = findViewById(R.id.aboutUs);
        aboutUs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent aboutUs = new Intent(HomeActivityNew.this, AboutUsActivity.class);
                startActivity(aboutUs);
            }
        });


        allPackagesList.clear();
        filteredPackagesList.clear();

        progressBar = findViewById(R.id.progressBarOne); // Initialize ProgressBar
        RecyclerView recyclerView = findViewById(R.id.packges_list_recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(HomeActivityNew.this));

        packageAdapter = new PackageAdapter(filteredPackagesList, this);
        recyclerView.setAdapter(packageAdapter);

        String urlText = getString(R.string.url);

        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterPackages(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterPackages(newText); // Perform real-time filtering
                return true;
            }
        });

        loadPackages(urlText); // Load packages


        viewPager2 = findViewById(R.id.imageSlider);

        List<Integer> imageList = Arrays.asList(
                R.drawable.slider_1,
                R.drawable.slider_2,
                R.drawable.slider_3
        );

        adapter = new SliderAdapter(this, imageList);
        viewPager2.setAdapter(adapter);
        startAutoSlider();

    }

    private NetworkReceiver networkReceiver;



    private void startAutoSlider() {
        runnable = new Runnable() {
            @Override
            public void run() {
                if (currentPage == adapter.getItemCount()) {
                    currentPage = 0;
                }
                viewPager2.setCurrentItem(currentPage++, true);
                handler.postDelayed(runnable, 3000); // 3 seconds delay for auto slide
            }
        };

        handler.postDelayed(runnable, 3000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(runnable);

        if (networkReceiver != null) {
            unregisterReceiver(networkReceiver);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(runnable, 3000); // Restart auto slider when activity is resumed

        networkReceiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
    }


    private void loadPackages(String url) {
        progressBar.setVisibility(View.VISIBLE); // Show ProgressBar

        Thread threadReq = new Thread() {
            @Override
            public void run() {

                OkHttpClient okHttpClient = new OkHttpClient();
                String apiUrl = url + "/travelmate/package/getAllPackagesForUser";

                Log.i("url", apiUrl);
                Request request = new Request.Builder().url(apiUrl).build();

                try {
                    Response response = okHttpClient.newCall(request).execute();

                    if (response.body() != null) {
                        String data = response.body().string();
                        Log.i("responseData", data);

                        Gson gson = new Gson();
                        Log.d("API_RESPONSE", data); // Debugging response

                        JsonArray jsonArray = gson.fromJson(data, JsonArray.class);

                        allPackagesList.clear();
                        filteredPackagesList.clear();
                        for (JsonElement element : jsonArray) {
                            JsonObject packageObject = element.getAsJsonObject();
                            Log.i("name", packageObject.get("packageName").getAsString());
                            allPackagesList.add(packageObject); // Store all packages
                        }

                        filteredPackagesList.addAll(allPackagesList); // Show all packages initially

                        runOnUiThread(() -> {
                            packageAdapter.notifyDataSetChanged();
                            progressBar.setVisibility(View.GONE); // Hide ProgressBar
                        });
                    } else {
                        Log.e("API Error", "Response body is null");

                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE); // Hide ProgressBar
                        });

                    }
                } catch (IOException e) {

                    Log.e("Network Error", "Failed to fetch data: " + e.getMessage(), e);

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE); // Hide ProgressBar

                        new AlertDialog.Builder(HomeActivityNew.this)
                                .setTitle("Connection Error")
                                .setMessage("Something went wrong with your connection. Please try again.")
                                .setCancelable(false)  // Prevent dismissing by clicking outside
                                .setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        String getUrl = getString(R.string.url);
                                        loadPackages(getUrl);
                                    }
                                })
                                .show();
                    });


                }
            }
        };

        threadReq.start();
    }

    private void filterPackages(String query) {
        filteredPackagesList.clear();

        if (query.isEmpty()) {
            // Show all packages when query is empty
            filteredPackagesList.addAll(allPackagesList);
        } else {
            // Filter the list
            for (JsonObject packageObject : allPackagesList) {
                String packageName = packageObject.get("packageName").getAsString();
                if (packageName.toLowerCase().contains(query.toLowerCase())) {
                    filteredPackagesList.add(packageObject);
                }
            }
        }

        // Notify adapter about dataset changes
        if (packageAdapter != null) {
            packageAdapter.notifyDataSetChanged();
        }
    }


}


class PackageViewHolder extends RecyclerView.ViewHolder {

    TextView textViewPackageName;
    TextView textViewPackagePrice;
    LinearLayout cardViewPackage;

    TextView dayCount;
    TextView nightCount;

    ImageView imageViewPackage;

    public PackageViewHolder(@NonNull View itemView) {
        super(itemView);
        textViewPackageName = itemView.findViewById(R.id.packageName_modern);
        textViewPackagePrice = itemView.findViewById(R.id.price_modern);
        cardViewPackage = itemView.findViewById(R.id.linear_modern);
        imageViewPackage = itemView.findViewById(R.id.imageView_modern);
        dayCount = itemView.findViewById(R.id.daysCount_modern);
        nightCount = itemView.findViewById(R.id.nightsCount_modern);
    }

}

class PackageAdapter extends RecyclerView.Adapter<PackageViewHolder> {

    private final Context context;
    private final List<JsonObject> packagesList;

    public PackageAdapter(List<JsonObject> packagesList, Context context) {
        this.packagesList = packagesList;
        this.context = context;
    }

    @NonNull
    @Override
    public PackageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.product_modern_design, parent, false);
        return new PackageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PackageViewHolder holder, int position) {
        JsonObject packageObject = packagesList.get(position);

        String base64Image = packageObject.get("resource").getAsString();
        String packgaeID = packageObject.get("id").getAsString();
        if (base64Image != null && !base64Image.isEmpty()) {
            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            holder.imageViewPackage.setImageBitmap(bitmap);
        } else {
            holder.imageViewPackage.setImageResource(R.drawable.back_home02);
        }

        holder.dayCount.setText(packageObject.get("days").getAsString());
        holder.nightCount.setText(packageObject.get("nights").getAsString());
        holder.textViewPackageName.setText(packageObject.get("packageName").getAsString());
        holder.textViewPackagePrice.setText(packageObject.get("pricePerPerson").getAsString());
        holder.textViewPackageName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                packageObject.addProperty("resource", "");
                Intent intent = new Intent(context, SingleTravelPackageActivity.class);
                intent.putExtra("data", new Gson().toJson(packageObject));
                context.startActivity(intent);
            }
        });


        new Thread(new Runnable() {
            @Override
            public void run() {
                // Decode the Base64 string into a Bitmap
                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                // Save the Bitmap to internal storage
                saveImageToInternalStorage(bitmap, packgaeID + "_image.png");

            }
        }).start();

    }


    private void saveImageToInternalStorage(Bitmap bitmap, String fileName) {
        try {
            // Get the internal storage directory for your app
            File file = new File(context.getFilesDir(), fileName);

            // Write the Bitmap to the file
            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                Log.d("ImageSave", "Image saved to: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("ImageSave", "Error saving image: " + e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return packagesList.size();
    }
}


class SliderAdapter extends RecyclerView.Adapter<SliderAdapter.SliderViewHolder> {
    private List<Integer> imageList;
    private Context context;

    public SliderAdapter(Context context, List<Integer> imageList) {
        this.context = context;
        this.imageList = imageList;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.slider_item, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        holder.imageView.setImageResource(imageList.get(position));
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    public static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.sliderImage);
        }
    }
}