package com.johnsonnyamweya.safari;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WelcomeActivity extends AppCompatActivity {

    private Button driverBtn, customerBtn;
    private FirebaseAuth mAuth;
    String button = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mAuth = FirebaseAuth.getInstance();

        driverBtn = (Button) findViewById(R.id.welcome_driver_btn);
        customerBtn = (Button) findViewById(R.id.welcome_customer_btn);

        driverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(WelcomeActivity.this, DriverRegisterActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();

                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                if(firebaseUser == null){
                    Intent driverIntent = new Intent(WelcomeActivity.this, DriverLoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(driverIntent);
                    finish();

                }
            }
        });

        customerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(WelcomeActivity.this, CustomerRegisterActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();

                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                if(firebaseUser == null){
                    Intent customerIntent = new Intent(WelcomeActivity.this, CustomerLoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(customerIntent);
                    finish();

                }
            }
        });

    }
}