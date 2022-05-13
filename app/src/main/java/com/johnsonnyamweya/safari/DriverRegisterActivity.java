package com.johnsonnyamweya.safari;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverRegisterActivity extends AppCompatActivity {

    private EditText edtDriverEmail, edtDriverPassword;
    private Button driverRegisterBtn;
    private TextView driverLoginLink;

    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;
    private DatabaseReference driverRef;
    private String onlineDriverID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_register);

        mAuth = FirebaseAuth.getInstance();

        loadingBar = new ProgressDialog(this);

        edtDriverEmail = (EditText) findViewById(R.id.driver_register_email);
        edtDriverPassword = (EditText) findViewById(R.id.driver_register_password);
        driverRegisterBtn = (Button) findViewById(R.id.driver_register_btn);
        driverLoginLink = (TextView) findViewById(R.id.driver_login_link);

        driverRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email = edtDriverEmail.getText().toString();
                String password = edtDriverPassword.getText().toString();

                if (TextUtils.isEmpty(email)){
                    Toast.makeText(DriverRegisterActivity.this,
                            "Please write your email", Toast.LENGTH_SHORT).show();
                }
                else if (TextUtils.isEmpty(password)){
                    Toast.makeText(DriverRegisterActivity.this,
                            "Please write your password", Toast.LENGTH_SHORT).show();
                }
                else{
                    loadingBar.setTitle("Driver Registration");
                    loadingBar.setMessage("Please wait while we are registering your data");
                    loadingBar.show();

                    mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if (task.isSuccessful()){
                                loadingBar.dismiss();

                                onlineDriverID = mAuth.getCurrentUser().getUid();
                                driverRef = FirebaseDatabase.getInstance().getReference()
                                        .child("Users").child("Drivers")
                                        .child(onlineDriverID);

                                driverRef.setValue(true);

                                Toast.makeText(DriverRegisterActivity.this,
                                        "Registered successfully", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(DriverRegisterActivity.this, DriverLoginActivity.class);
                                startActivity(intent);
                            }
                            else{
                                loadingBar.dismiss();

                                Toast.makeText(DriverRegisterActivity.this,
                                        "Registration Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

            }
        });

        driverLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent driverIntent =
                        new Intent(DriverRegisterActivity.this, DriverLoginActivity.class);
                startActivity(driverIntent);
            }
        });

    }
}