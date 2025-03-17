package lk.codebridge.travelmate.ui.notifications;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;
import lk.codebridge.travelmate.R;
import lk.codebridge.travelmate.SingleTravelPackageActivity;
import lk.codebridge.travelmate.databinding.FragmentNotificationsBinding;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private String url;
    private Gson gson = new Gson();
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private List<JsonObject> objectList = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        url = getString(R.string.url);
        progressBar = view.findViewById(R.id.progressBarWish);
        recyclerView = view.findViewById(R.id.wishListRecycleView);

        X x = new X();
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(x);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("lk.codebridge.travelmate", Context.MODE_PRIVATE);
            String email = sharedPreferences.getString("userEmail", null);

            Request request = new Request.Builder().url(url + "/travelmate/getFromWishlist?email=" + email).build();

            try {
                Response response = client.newCall(request).execute();
                String responseString = response.body().string();

                if (response.isSuccessful()) {
                    Log.i("resData", responseString);
                    JsonArray jsonArray = gson.fromJson(responseString, JsonArray.class);
                    objectList.clear();

                    for (JsonElement element : jsonArray) {
                        objectList.add(element.getAsJsonObject());
                    }

                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        recyclerView.setAdapter(new WishlistAdapter(objectList));
                    });
                } else {
                    getActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                }
            } catch (IOException e) {
                e.printStackTrace();
                getActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            }
        }).start();
    }

    private class WishlistAdapter extends RecyclerView.Adapter<WishlistViewHolder> {
        private List<JsonObject> jsonList;

        public WishlistAdapter(List<JsonObject> jsonList) {
            this.jsonList = jsonList;
        }

        @NonNull
        @Override
        public WishlistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.wishlist_item, parent, false);
            return new WishlistViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull WishlistViewHolder holder, int position) {
            JsonObject dataOb = jsonList.get(position);
            if (dataOb.has("travelPackage")) {
                JsonObject travelPackage = dataOb.getAsJsonObject("travelPackage");

                holder.name.setText(travelPackage.get("packageName").getAsString());
                holder.price.setText(travelPackage.get("pricePerPerson").getAsString());
                holder.days.setText(travelPackage.get("days").getAsString());
                holder.nights.setText(travelPackage.get("nights").getAsString());

                holder.id = dataOb.get("id").getAsString();

                holder.card.setOnClickListener(view -> {
                    Intent intent = new Intent(getActivity(), SingleTravelPackageActivity.class);
                    intent.putExtra("data", gson.toJson(travelPackage));
                    startActivity(intent);
                });

                Bitmap bitmap = loadImageFromInternalStorage(travelPackage.get("id").getAsString() + "_image.png");
                if (bitmap != null) {
                    holder.packageImageWish.setImageBitmap(bitmap);
                }
            }
        }

        @Override
        public int getItemCount() {
            return jsonList.size();
        }
    }

    private class WishlistViewHolder extends RecyclerView.ViewHolder {
        TextView name, price, days, nights;
        CardView card;
        ImageView packageImageWish;

        String id;

        public WishlistViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.wishName);
            price = itemView.findViewById(R.id.wishPrice);
            days = itemView.findViewById(R.id.daysCountWish);
            nights = itemView.findViewById(R.id.nightsCountWish);
            card = itemView.findViewById(R.id.cardWish);
            packageImageWish = itemView.findViewById(R.id.packageImageWish);
        }
    }


    private class X extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(0, ItemTouchHelper.LEFT);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            WishlistViewHolder wishlistViewHolder = (WishlistViewHolder) viewHolder;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    OkHttpClient client = new OkHttpClient();

                    Request request = new Request.Builder().url(url + "/travelmate/removeFromWishlist?id=" + wishlistViewHolder.id).build();

                    try {
                        Response response = client.newCall(request).execute();

                        if (response.isSuccessful()) {

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toasty.success(getContext(),"Successful removed",Toasty.LENGTH_SHORT,true).show();
                                }
                            });

                        } else {

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toasty.warning(getContext(),"Something went wrong, try again later.",Toasty.LENGTH_SHORT,true).show();
                                }
                            });

                        }
                    } catch (IOException e) {

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toasty.warning(getContext(),"Something went wrong, try again later.",Toasty.LENGTH_SHORT,true).show();
                            }
                        });
                        throw new RuntimeException(e);
                    }
                }
            }).start();


        }
    }

    private Bitmap loadImageFromInternalStorage(String fileName) {
        try {
            File file = new File(getActivity().getFilesDir(), fileName);
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ImageLoad", "Error loading image: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
