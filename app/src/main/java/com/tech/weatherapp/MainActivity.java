package com.tech.weatherapp;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;
import com.tech.weatherapp.Adapter.WeatherRVAdapter;
import com.tech.weatherapp.Model.WeatherRVModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout homeRL;
    private ProgressBar loadingPB;
    private TextView cityNameTV, temperatureTV, conditionTV;
    private TextInputEditText cityEdt;
    private ImageView backIV, iconIV, searchIV;
    private RecyclerView weatherRV;

    private ArrayList<WeatherRVModel> weatherRVModelArrayList;
    private WeatherRVAdapter weatherRVAdapter;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private int PERMISSION_CODE = 1;
    private String cityName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //we can't show status bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_main);

        homeRL = findViewById(R.id.idRLHome);
        loadingPB = findViewById(R.id.idPBLoading);
        cityNameTV = findViewById(R.id.idTVCityName);
        temperatureTV = findViewById(R.id.idTVTemperature);
        conditionTV = findViewById(R.id.idTVCondition);
        weatherRV = findViewById(R.id.idRVWeather);
        cityEdt = findViewById(R.id.idEdtCity);
        iconIV = findViewById(R.id.idTVIcon);
        backIV = findViewById(R.id.idTVBack);
        searchIV = findViewById(R.id.idTVSearch);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        weatherRVModelArrayList = new ArrayList<>();
        weatherRVAdapter = new WeatherRVAdapter(this, weatherRVModelArrayList);
        weatherRV.setAdapter(weatherRVAdapter);

        getLastLocation();

        searchIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String city = cityEdt.getText().toString();
                if (city.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter city Name", Toast.LENGTH_SHORT).show();
                } else {
                    cityNameTV.setText(cityName);
                    getWeatherInfo(city);
                }
            }
        });
    }

    private void getLastLocation() {

        Log.d("find", "ok1");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)  == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                       //Already user given the location access in app
            Log.d("find", "ok2");

            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    Log.d("find", "ok3");
                    if(location != null){

                        cityName = getCityName(location.getLatitude(),location.getLongitude());
                        Toast.makeText(MainActivity.this, cityName, Toast.LENGTH_SHORT).show();
                        getWeatherInfo(cityName);
                    }else{
                        Log.d("find", "okz");
                    }
                }
            });
        }else{
            Log.d("find", "ok4");
            //if user have not  access the location then ask Location permission
            askPermission();
        }
    }

    private void askPermission() {
        ActivityCompat.requestPermissions(MainActivity.this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        Log.d("find", "ok5");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            Log.d("find", "ok6");
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("find", "ok7");
                Toast.makeText(this, "Permission granted..", Toast.LENGTH_SHORT).show();
            } else {
                Log.d("find", "ok8");
                Toast.makeText(this, "Please provides the permissions", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    private String getCityName(double latitude, double longitude) {
        String cityName = "Not found";
        Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());

        try {
            List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1); //1,3,4,5,6

            for (Address address : addressList) {
                if (address != null) {
                    String city = address.getLocality();
                    if (city != null && !city.equals("")) {
                        cityName = city;
                    } else {
                        Log.e("TAG", "CITY NOT FOUND");
                        Toast.makeText(this, "User City Not Found..", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;
    }

    //api url  = http://api.weatherapi.com/v1/forecast.json?key=d9c6dbd55c1b42c8b2b80638221512&q=Gaya&days=1&aqi=yes&alerts=yes
    private void getWeatherInfo(String cityName) {
        String url = "https://api.weatherapi.com/v1/forecast.json?key=d9c6dbd55c1b42c8b2b80638221512&q=" + cityName + "&days=1&aqi=yes&alerts=yes";
        cityNameTV.setText(cityName);

        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                loadingPB.setVisibility(View.GONE);
                homeRL.setVisibility(View.VISIBLE);
                weatherRVModelArrayList.clear();

                try {
                    String temperature = response.getJSONObject("current").getString("temp_c");
                    temperatureTV.setText(temperature + "°c");

                    int isDay = response.getJSONObject("current").getInt("is_day");
                    String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    Picasso.get()
                            .load("https:".concat(conditionIcon))
                            .into(iconIV);
                    conditionTV.setText(condition);

                    if (isDay == 1) { // 1 mean Morning
                        Picasso.get()
                                .load("https://steemitimages.com/p/3HaJVvr6qfoEFgBir1jy4DeK1wwhDm7oNHzc1b8roaJY8MKWeEDPCwVBn1vrfmooEj72QpV15ga9ybUME7giSERTYCFk8QdJJLqPSNJ?format=match&mode=fit&width=1280").into(backIV);
                    } else {        // 0  mean Night
                        Picasso.get()
                                .load("https://w0.peakpx.com/wallpaper/725/98/HD-wallpaper-moon-clouds-cool-night-purple-weather.jpg").into(backIV);
                    }

                    JSONObject forecast = response.getJSONObject("forecast");
                    JSONObject foreCastDayArray = forecast.getJSONArray("forecastday").getJSONObject(0);
                    JSONArray hourArray = foreCastDayArray.getJSONArray("hour");

                    for (int i = 0; i < hourArray.length(); i++) {
                        JSONObject hourObj = hourArray.getJSONObject(i);
                        String time = hourObj.getString("time");
                        String temper = hourObj.getString("temp_c");
                        String img = hourObj.getJSONObject("condition").getString("icon");
                        String windSpeed = hourObj.getString("wind_kph");

                        weatherRVModelArrayList.add(new WeatherRVModel(time, temper, img, windSpeed));
                    }
                    weatherRVAdapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Please enter valid city Name", Toast.LENGTH_SHORT).show();
                Log.d("Api", error.getMessage());
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

}