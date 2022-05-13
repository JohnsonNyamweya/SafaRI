package com.johnsonnyamweya.safari;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private CircleImageView profileImageView;
    private EditText nameEditText, phoneEditText, driverCarName;
    private ImageView closeButton, saveButton;
    private TextView profileChangeBtn;
    private String getType;
    private String checker = "";

    private static final int galleryPick = 1;
    private Uri imageUri;
    private String myUrl = "";

    private StorageTask uploadTask;
    private StorageReference storageProfilePicsReference;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getType = getIntent().getStringExtra("type");
        Toast.makeText(this, getType, Toast.LENGTH_SHORT).show();

        mAuth = FirebaseAuth.getInstance();

        databaseReference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child(getType);

        storageProfilePicsReference = FirebaseStorage.getInstance().getReference()
                .child("Profile Pictures");


        profileImageView = findViewById(R.id.profile_image);
        nameEditText = findViewById(R.id.edt_name);
        phoneEditText = findViewById(R.id.edt_phone);
        driverCarName = findViewById(R.id.driver_car_name);

        if (getType.equals("Drivers")){

            driverCarName.setVisibility(View.VISIBLE);
        }

        closeButton = findViewById(R.id.close_button);
        saveButton = findViewById(R.id.save_button);
        profileChangeBtn = findViewById(R.id.change_profile_btn);


        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getType.equals("Drivers")){

                    startActivity(new Intent(SettingsActivity.this, DriverMapActivity.class));
                }
                else{
                    startActivity(new Intent(SettingsActivity.this, CustomerMapActivity.class));
                }
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checker.equals("clicked")){
                    validateControllers();
                }
                else{
                    validateAndSaveOnlyInformation();
                }
            }
        });

        profileChangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checker = "clicked";
                choosePhoto();
            }
        });

        getUserInformation();

    }


    private void choosePhoto() {

        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, galleryPick);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == galleryPick && resultCode == RESULT_OK && data != null){
            //imageUri == result
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
        }
        else {

            if (getType.equals("Drivers")){

                Toast.makeText(this, "Error: Try again.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SettingsActivity.this, DriverMapActivity.class));
            }
           else{
                Toast.makeText(this, "Error: Try again.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SettingsActivity.this, CustomerMapActivity.class));
            }
        }
    }

    private void validateControllers(){
        if (TextUtils.isEmpty(nameEditText.getText().toString())){
            Toast.makeText(this, "Please provide your name", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(phoneEditText.getText().toString())){
            Toast.makeText(this, "Please provide your phone number", Toast.LENGTH_SHORT).show();
        }
        else if (getType.equals("Drivers") && TextUtils.isEmpty(nameEditText.getText().toString())){
            Toast.makeText(this, "Please provide your car name", Toast.LENGTH_SHORT).show();
        }
        else if (checker.equals("clicked")){
            uploadProfilePicture();
        }
    }

    private void uploadProfilePicture() {

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Settings Account Information");
        progressDialog.setMessage("Please wait while we are updating your account information");
        progressDialog.show();

        if (imageUri != null){
            final StorageReference fileRef = storageProfilePicsReference
                    .child(mAuth.getCurrentUser().getUid() + ".jpg");
            uploadTask = fileRef.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return fileRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()){

                        Uri downloadUrl = task.getResult();
                        myUrl = downloadUrl.toString();

                        if (getType.equals("Drivers")){

                            HashMap <String, Object> userMap = new HashMap<>();
                            userMap.put("uid",mAuth.getCurrentUser().getUid());
                            userMap.put("name", nameEditText.getText().toString());
                            userMap.put("phone", phoneEditText.getText().toString());
                            userMap.put("image", myUrl);
                            userMap.put("car", driverCarName.getText().toString());

                            databaseReference.child(mAuth.getCurrentUser().getUid())
                                    .updateChildren(userMap)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        progressDialog.dismiss();

                                        Toast.makeText(SettingsActivity.this,
                                                "Information saved successfully", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SettingsActivity.this, DriverMapActivity.class));

                                    }
                                    else{
                                        Toast.makeText(SettingsActivity.this,
                                                "Error, please try again", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                        else{
                            HashMap <String, Object> driverMap = new HashMap<>();
                            driverMap.put("uid",mAuth.getCurrentUser().getUid());
                            driverMap.put("name", nameEditText.getText().toString());
                            driverMap.put("phone", phoneEditText.getText().toString());
                            driverMap.put("image", myUrl);

                            databaseReference.child(mAuth.getCurrentUser().getUid())
                                    .updateChildren(driverMap)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        progressDialog.dismiss();

                                        Toast.makeText(SettingsActivity.this,
                                                "Information saved successfully", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SettingsActivity.this, CustomerMapActivity.class));

                                    }
                                    else{
                                        Toast.makeText(SettingsActivity.this,
                                                "Error, please try again", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }
        else{
            Toast.makeText(this, "Image is not selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void validateAndSaveOnlyInformation() {

        if (TextUtils.isEmpty(nameEditText.getText().toString())){
            Toast.makeText(this, "Please provide your name", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(phoneEditText.getText().toString())){
            Toast.makeText(this, "Please provide your phone number", Toast.LENGTH_SHORT).show();
        }
        else if (getType.equals("Drivers") && TextUtils.isEmpty(nameEditText.getText().toString())){
            Toast.makeText(this, "Please provide your car name", Toast.LENGTH_SHORT).show();
        }
        else{

            HashMap <String, Object> userMap = new HashMap<>();
            userMap.put("uid",mAuth.getCurrentUser().getUid());
            userMap.put("name", nameEditText.getText().toString());
            userMap.put("phone", phoneEditText.getText().toString());

            if (getType.equals("Drivers")){
                userMap.put("car", driverCarName.getText().toString());
            }

            databaseReference.child(mAuth.getCurrentUser().getUid())
                    .updateChildren(userMap);


            if (getType.equals("Drivers")){

                startActivity(new Intent(SettingsActivity.this, DriverMapActivity.class));
            }
            else{

                startActivity(new Intent(SettingsActivity.this, CustomerMapActivity.class));
            }
        }
    }

    private void getUserInformation(){
        databaseReference.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0){
                    String name = snapshot.child("name").getValue().toString();
                    String phone = snapshot.child("phone").getValue().toString();

                    nameEditText.setText(name);
                    phoneEditText.setText(phone);

                    if (getType.equals("Drivers")){
                        String car = snapshot.child("car").getValue().toString();
                        driverCarName.setText(car);
                    }

                    if (snapshot.hasChild("image")){
                        String image = snapshot.child("image").getValue().toString();
                        //Picasso.get().load(image).into(profileImageView);
                        Glide.with(profileImageView.getContext()).load(image).fitCenter()
                                .centerCrop().into(profileImageView);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}