package lk.codebridge.travelmate;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import es.dmoral.toasty.Toasty;

public class TravelLocationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_travel_location);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        double latitude = 0.0;
        double longitude = 0.0;

        String input = getIntent().getStringExtra("data");
        String city = getIntent().getStringExtra("city");

        TextView textView = findViewById(R.id.LocationTxt);
        textView.setText(city);

        String[] parts = input.split(",");

        if (parts.length == 2) {
            String firstPart = parts[0];
            String secondPart = parts[1];

            try {
                latitude = Double.parseDouble(firstPart);
                longitude = Double.parseDouble(secondPart);
            } catch (NumberFormatException e) {
                System.out.println("Error parsing latitude or longitude.");
            }

            System.out.println("Latitude: " + latitude);
            System.out.println("Longitude: " + longitude);
        } else {
            System.out.println("The input doesn't have exactly two parts.");
        }

        if (latitude != 0.0 && longitude != 0.0) {

            SupportMapFragment mapFragment = new SupportMapFragment();

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.frame2, mapFragment);
            fragmentTransaction.commit();

            double finalLatitude = latitude;
            double finalLongitude = longitude;

            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(@NonNull GoogleMap googleMap) {

                    LatLng location = new LatLng(finalLatitude, finalLongitude);

                    BitmapDescriptor customIcon = BitmapDescriptorFactory.fromResource(R.drawable.google_maps);

                    googleMap.addMarker(new MarkerOptions()
                            .position(location)
                            .title("Marker")
                            .icon(customIcon));

                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
                }
            });

        } else {

            Toasty.warning(TravelLocationActivity.this, "Something went wrong in Location.", Toasty.LENGTH_SHORT, true).show();
            finish();
        }


        ImageView backfrom_map = findViewById(R.id.backfrom_map);
        backfrom_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}
