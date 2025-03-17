package lk.codebridge.travelmate.ui.dashboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lk.codebridge.travelmate.HomeActivityNew;
import lk.codebridge.travelmate.R;
import lk.codebridge.travelmate.SingleTravelPackageActivity;
import lk.codebridge.travelmate.databinding.FragmentDashboardBinding;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        RecyclerView recyclerView = view.findViewById(R.id.myOrdersPast);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<JsonObject> orderList = new ArrayList<>();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Gson gson = new Gson();

                String url = getString(R.string.url);

                SharedPreferences sp = getActivity().getSharedPreferences("lk.codebridge.travelmate", Context.MODE_PRIVATE);

                String email = sp.getString("userEmail",null);

                OkHttpClient okHttpClient = new OkHttpClient();

                Request request = new Request.Builder().url(url+"/travelmate/packageOrdering/getOrderedDetailsByUser?email="+email).build();

                try {
                    Response response = okHttpClient.newCall(request).execute();

                    if (response.isSuccessful()){

                        String dataResponse = response.body().string();

                        JsonArray jsonArray = gson.fromJson(dataResponse, JsonArray.class);

                        for (JsonElement element: jsonArray) {

                            orderList.add(element.getAsJsonObject());

                        }

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                OrderPackageAdapter packageAdapter = new OrderPackageAdapter(orderList, getContext());
                                recyclerView.setAdapter(packageAdapter);
                            }
                        });


                    }else {


                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }).start();




    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


class OrderPackageViewHolder extends RecyclerView.ViewHolder {

    TextView tvPackageLabel;
    TextView tvStartDate;

    TextView tvEndDate;
    TextView tvPersons;
    TextView tvTotal;

    public OrderPackageViewHolder(@NonNull View itemView) {
        super(itemView);
        tvPackageLabel = itemView.findViewById(R.id.tvPackageLabel);
        tvStartDate = itemView.findViewById(R.id.tvStartDate);
        tvEndDate = itemView.findViewById(R.id.tvEndDate);
        tvPersons = itemView.findViewById(R.id.tvPersons);

        tvTotal = itemView.findViewById(R.id.tvTotal);
    }

}

class OrderPackageAdapter extends RecyclerView.Adapter<OrderPackageViewHolder> {

    private final Context context;
    private final List<JsonObject> packagesList;

    public OrderPackageAdapter(List<JsonObject> packagesList, Context context) {
        this.packagesList = packagesList;
        this.context = context;
    }


    @NonNull
    @Override
    public OrderPackageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.order_item, parent, false);
        return new OrderPackageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderPackageViewHolder holder, int position) {

        JsonObject jsonObject = packagesList.get(position);

        holder.tvPackageLabel.setText(jsonObject.get("travelPackage").getAsJsonObject().get("packageName").getAsString());
        holder.tvStartDate.setText(jsonObject.get("checkIn").getAsString());
        holder.tvEndDate.setText(jsonObject.get("checkout").getAsString());
        holder.tvPersons.setText(jsonObject.get("persons").getAsString());
        holder.tvTotal.setText(jsonObject.get("total").getAsString());


    }

    @Override
    public int getItemCount() {
        return packagesList.size();
    }
}