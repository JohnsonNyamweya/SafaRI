package com.johnsonnyamweya.safari;

import static com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.Animator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.johnsonnyamweya.safari.databinding.ActivityCustomerMapBinding;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback ,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;
    private ActivityCustomerMapBinding binding;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;

    private Button customerCallCarButton;
    private FirebaseAuth mAuth;
    private DatabaseReference customerDatabaseRef, driverAvailableRef, driverRef, driverLocationRef;
    private String customerID;
    private LatLng customerPickUpLocation;
    private int radius = 1;
    private Boolean driverFound = false, requestType = false;
    private String driverFoundID;
    Marker driverMarker, pickUpMarker;

    private ValueEventListener driverLocationRefListener;
    private GeoQuery geoQuery;

    private TextView txtName, txtPhone, txtCarName;
    private CircleImageView profilePic;
    private RelativeLayout relativeLayout;
    private ImageView driverCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = ActivityCustomerMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mAuth = FirebaseAuth.getInstance();
        customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        customerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customers Request");
        driverAvailableRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");

        Button logoutCustomerButton = findViewById(R.id.customer_logout_button);
        Button settingsCustomerButton = findViewById(R.id.customer_settings_button);
        customerCallCarButton = findViewById(R.id.customer_call_car);

        txtName = findViewById(R.id.name_driver);
        txtPhone = findViewById(R.id.phone_driver);
        txtCarName = findViewById(R.id.car_name_driver);
        relativeLayout = findViewById(R.id.relative2_customer);
        profilePic = findViewById(R.id.profile_image_driver);
        driverCall = findViewById(R.id.driver_call);

        driverCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                        .child("Users").child("Drivers").child(driverFoundID);

                reference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()){
                            String phone = snapshot.child("phone").getValue().toString();

                            Intent intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(Uri.fromParts("tel", phone, null));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

        logoutCustomerButton.setOnClickListener(view -> {
            mAuth.signOut();

            Intent intent = new Intent(CustomerMapActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

            Toast.makeText(CustomerMapActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        });

        settingsCustomerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomerMapActivity.this, SettingsActivity.class);
                intent.putExtra("type", "Customers");
                startActivity(intent);
            }
        });

        customerCallCarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(requestType && driverFound == null){

                    requestType = false;
                    geoQuery.removeAllListeners();

                    driverLocationRef.removeEventListener(driverLocationRefListener);

                    if (driverFound != null){
                        driverRef = FirebaseDatabase.getInstance().getReference()
                                .child("Users").child("Drivers")
                                .child(driverFoundID).child("customerRideID");

                        driverRef.removeValue();

                        driverFoundID = null;
                    }

                    driverFound = false;
                    radius = 1;

                    customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    GeoFire geoFire = new GeoFire(customerDatabaseRef);

                    geoFire.removeLocation(customerID);

                    if (pickUpMarker != null){
                        pickUpMarker.remove();
                    }

                    if (driverMarker != null){
                        driverMarker.remove();
                    }

                    customerCallCarButton.setText("Call A Car");

                    relativeLayout.setVisibility(View.GONE);

                }
                else{
                    requestType = true;

                    GeoFire geoFire = new GeoFire(customerDatabaseRef);
                    geoFire.setLocation(customerID, new GeoLocation(lastLocation.getLatitude(),
                            lastLocation.getLongitude()));

                    customerPickUpLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(customerPickUpLocation)
                            .title("My Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                    customerCallCarButton.setText("Getting you A Driver...");

                    getClosestCarDriver();
                }


            }
        });

    }

    private void getClosestCarDriver() {
        GeoFire geoFire = new GeoFire(driverAvailableRef);

          GeoQuery geoQuery = geoFire
                .queryAtLocation(new GeoLocation(customerPickUpLocation.latitude,
                                customerPickUpLocation.longitude), radius);

        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestType){
                    driverFound = true;
                    driverFoundID = key;

                    driverRef = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Drivers").child(driverFoundID);

                    HashMap driverMap = new HashMap();
                    driverMap.put("customerRideID", customerID);
                    driverRef.updateChildren(driverMap);

                    gettingDriverLocation();

                    customerCallCarButton.setText("Looking For Driver Location");

                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound){
                    radius = radius + 1;
                    getClosestCarDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void gettingDriverLocation() {

       driverLocationRefListener = driverLocationRef.child(driverFoundID).child("l")
        .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && requestType){
                    List<Object> driverLocationMap = (List<Object>) snapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    customerCallCarButton.setText("Customer Found");

                    relativeLayout.setVisibility(View.VISIBLE);
                    getAssignedDriverInformation();

                    if (driverLocationMap.get(0) != null) {
                        locationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                    }
                    if (driverLocationMap.get(1) != null){
                        locationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                    }

                    LatLng driverLatLng = new LatLng(locationLat, locationLng);

                    if (driverMarker != null){
                        driverMarker.remove();
                    }

                    Location location1 = new Location("");
                    location1.setLatitude(customerPickUpLocation.latitude);
                    location1.setLongitude(customerPickUpLocation.longitude);

                    Location location2 = new Location("");
                    location2.setLatitude(driverLatLng.latitude);
                    location2.setLongitude(driverLatLng.longitude);

                    float distance = location1.distanceTo(location2);

                    if (distance < 90){
                        customerCallCarButton.setText("Driver Has Arrived");
                    }
                    else{
                        customerCallCarButton.setText("Driver Found" + String.valueOf(distance));
                    }

                    driverMarker = mMap.addMarker(new MarkerOptions()
                            .position(driverLatLng).title("Your Driver is Here")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 0);

            return;
        }

        buildGoogleApiClient();

        mMap.setMyLocationEnabled(true);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);

        locationRequest.setPriority(PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        LocationServices.FusedLocationApi
                .requestLocationUpdates(googleApiClient, locationRequest, this);

}

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        lastLocation = location;

        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
//        mMap.addMarker(new MarkerOptions().position(myLocation).title("My Location"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 12));

    }

    protected synchronized void buildGoogleApiClient(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    private void getAssignedDriverInformation(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverFoundID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0){
                    String name = snapshot.child("name").getValue().toString();
                    String phone = snapshot.child("phone").getValue().toString();
                    String car = snapshot.child("car").getValue().toString();

                    txtName.setText(name);
                    txtPhone.setText(phone);
                    txtCarName.setText(car);



                    if (snapshot.hasChild("image")){
                        String image = snapshot.child("image").getValue().toString();
                        //Picasso.get().load(image).into(profilePic);
                        Glide.with(profilePic.getContext()).load(image).fitCenter()
                                .centerCrop().into(profilePic);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}