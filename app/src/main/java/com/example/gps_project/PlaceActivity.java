package com.example.gps_project;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class PlaceActivity extends AppCompatActivity {

    String dong1 = "", dong2 = "", dongRemain = "", local = "", adminArea = "", fullAddress = "";

    EditText etSource;
    ImageView btn_Exit;

    double lat = 0, lng = 0; //위도, 경도

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place);

        etSource = findViewById(R.id.et_source);
        btn_Exit = findViewById(R.id.btn_Exit);

        Places.initialize(getApplicationContext(), "AIzaSyAgnSlexNT4vgP5A2GI3kHB1XRvlAXQMDM");

        etSource.setFocusable(false);
        etSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Initialize place field list
                List<Place.Field> fields = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG);
                //Create intent
                Intent intent = new Autocomplete.IntentBuilder(
                        AutocompleteActivityMode.OVERLAY, fields).build(getApplicationContext());
                //Start activity result
                startActivityForResult(intent, 100);
            }
        });

        btn_Exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    } //onCreate()

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Check condition
        if (requestCode == 100 && resultCode == RESULT_OK) {
            //When success
            //Initialize place
            Place place = Autocomplete.getPlaceFromIntent(data);
            //Check condition
            //Set address on edit text
            etSource.setText(place.getAddress());
            //Get latitude and longitude
            String sSource = String.valueOf(place.getLatLng());
            sSource = sSource.replaceAll("lat/lng: ", "");
            sSource = sSource.replace("(", "");
            sSource = sSource.replace(")", "");
            String[] split = sSource.split(",");
            lat = Double.parseDouble(split[0]);
            lng = Double.parseDouble(split[1]);
        }

        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(lat, lng, 10);
        } catch (IOException e) {
            Log.d("test", "입출력 오류");
        }
        if (addresses != null) {
            if (addresses.size() == 0) {
                Toast.makeText(getApplicationContext(), "주소찾기 오류", Toast.LENGTH_SHORT).show();
            } else {
                for (Address add : addresses) {
                    if (add.getThoroughfare() != null && add.getThoroughfare().length() > 0) {
                        if (dong1.equals("")) {
                            dong1 = add.getThoroughfare();
                        } else if (dong2.equals("")) {
                            dong2 = add.getThoroughfare();
                        } else {
                            dongRemain = add.getThoroughfare();
                        }
                        Log.d("dong", dongRemain);
                    }

                    if (add.getAdminArea() != null && add.getAdminArea().length() > 0) {
                        adminArea = add.getAdminArea();
                        if (adminArea.equals("광주광역시") || adminArea.equals("대구광역시") || adminArea.equals("대전광역시") ||
                                adminArea.equals("부산광역시") || adminArea.equals("서울특별시") || adminArea.equals("울산광역시") ||
                                adminArea.equals("인천광역시")) {
                            if (add.getSubLocality() != null && add.getSubLocality().length() > 0) {
                                local = add.getSubLocality();
                                fullAddress = adminArea + " " + add.getSubLocality();
                                Log.d("결과1", fullAddress);
                            }
                        } else if (adminArea.equals("강원도") || adminArea.equals("경기도") || adminArea.equals("경상남도") ||
                                adminArea.equals("경상북도") || adminArea.equals("전라남도") || adminArea.equals("전라북도") ||
                                adminArea.equals("충청남도") || adminArea.equals("충청북도") || adminArea.equals("제주특별자치도")) {
                            if (add.getLocality() != null && add.getLocality().length() > 0) {
                                local = add.getLocality();
                                fullAddress = adminArea + " " + add.getLocality();
                                Log.d("결과2", fullAddress);
                            }
                        }
                    }
                }

                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                String userid = firebaseUser.getUid();
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);

                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("locality", local);
                hashMap.put("nowAddress", fullAddress);
                hashMap.put("thoroughfare", dong2);

                reference.updateChildren(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            finish();
                        }
                    }
                });
            }
        }

    }
}