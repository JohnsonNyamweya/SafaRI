package com.johnsonnyamweya.safari;

import static com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
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
import com.johnsonnyamweya.safari.databinding.ActivityDriverMapBinding;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private ActivityDriverMapBinding binding;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference driverAvailabilityDatabaseRef, driverWorkingRef,
            assignCustomerRef, assignedCustomerPickUpRef;
    private Boolean currentLogoutDriverStatus = false;
    private String driverID, customerID = "";
    Marker pickUpMarker;
    private ValueEventListener assignedCustomerPickUpRefListener;

    private TextView txtName, txtPhone;
    private CircleImageView profilePic;
    private RelativeLayout relativeLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = ActivityDriverMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mAuth = FirebaseAuth.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        driverID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        driverAvailabilityDatabaseRef =  FirebaseDatabase.getInstance().getReference()
                .child("Drivers Available");
        driverWorkingRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");


        Button logoutDriverButton = findViewById(R.id.driver_logout_button);
        Button settingsDriverButton = findViewById(R.id.driver_settings_button);

        txtName = findViewById(R.id.name_customer);
        txtPhone = findViewById(R.id.phone_customer);
        relativeLayout = findViewById(R.id.relative2_driver);
        profilePic = findViewById(R.id.profile_image_customer);
        ImageView customerCall = findViewById(R.id.customer_call);

        customerCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                        .child("Users").child("Customers").child(customerID);

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

        logoutDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                currentLogoutDriverStatus = true;
                disconnectTheDriver();

                mAuth.signOut();

                Intent welcomeIntent = new Intent(DriverMapActivity.this, WelcomeActivity.class);
                welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(welcomeIntent);
                finish();

                Toast.makeText(DriverMapActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            }
        });

        settingsDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(DriverMapActivity.this, SettingsActivity.class);
                intent.putExtra("type", "Drivers");
                startActivity(intent);

            }
        });

        getAssignedCustomerRequest();
    }

    private void getAssignedCustomerRequest() {

        assignCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Drivers").child(driverID).child("customerRideID");

        assignCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){

                    customerID = snapshot.getValue().toString();

                    getAssignedCustomerPickUpLocation();

                    relativeLayout.setVisibility(View.VISIBLE);

                    getAssignedCustomerInformation();

                }
                else{
                    customerID = "";

                    if (pickUpMarker != null){
                        pickUpMarker.remove();
                    }
                    if (assignedCustomerPickUpRefListener != null){
                        assignedCustomerPickUpRef.removeEventListener(assignedCustomerPickUpRefListener);
                    }

                    relativeLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void getAssignedCustomerPickUpLocation() {
        assignedCustomerPickUpRef = FirebaseDatabase.getInstance().getReference()
        .child("Customer Request").child(customerID).child("l");

       assignedCustomerPickUpRefListener =  assignedCustomerPickUpRef
               .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){

                    List<Object> customerLocationMap = (List<Object>) snapshot.getValue();

                    double locationLat = 0;
                    double locationLng = 0;

                    if (customerLocationMap.get(0) != null) {
                        locationLat = Double.parseDouble(customerLocationMap.get(0).toString());
                    }
                    if (customerLocationMap.get(1) != null){
                        locationLng = Double.parseDouble(customerLocationMap.get(1).toString());
                    }

                    LatLng driverLatLng = new LatLng(locationLat, locationLng);

                    mMap.addMarker(new MarkerOptions().position(driverLatLng)
                            .title("Customer PickUp Location").icon(BitmapDescriptorFactory
                                    .fromResource(R.drawable.user)));



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

        buildGoogleApiClient();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 0);

            return;
        }
        mMap.setMyLocationEnabled(true);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);

        locationRequest.setPriority(PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
       if (getApplicationContext() != null){
           lastLocation = location;

           LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
           mMap.addMarker(new MarkerOptions().position(myLocation).title("My Location"));
           mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 13));


           GeoFire geoFireDriverAvailability = new GeoFire(driverAvailabilityDatabaseRef);


           GeoFire geoFireDriverWorking = new GeoFire(driverWorkingRef);


           switch (customerID){
               case "":
               geoFireDriverWorking.removeLocation(driverID);

                   geoFireDriverAvailability.setLocation(driverID, new GeoLocation(location.getLatitude(),
                           location.getLongitude()));
                   break;

               default:
                   geoFireDriverAvailability.removeLocation(driverID);

                   geoFireDriverWorking.setLocation(driverID, new GeoLocation(location.getLatitude(),
                           location.getLongitude()));

                   break;
           }

       }

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

        if (!currentLogoutDriverStatus){
            disconnectTheDriver();
        }

    }

    private void disconnectTheDriver() {

        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        final DatabaseReference driverAvailabilityRef = FirebaseDatabase.getInstance().getReference()
                .child("Drivers Available");

        GeoFire geoFire = new GeoFire(driverAvailabilityRef);
        geoFire.removeLocation(userID);
    }

    private void getAssignedCustomerInformation(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Customers").child(customerID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0){
                    String name = snapshot.child("name").getValue().toString();
                    String phone = snapshot.child("phone").getValue().toString();

                    txtName.setText(name);
                    txtPhone.setText(phone);


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