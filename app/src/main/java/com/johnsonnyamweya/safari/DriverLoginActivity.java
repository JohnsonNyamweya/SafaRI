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

public class DriverLoginActivity extends AppCompatActivity {

    private EditText edtDriverEmail, edtDriverPassword;
    private Button driverLoginBtn;

    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        mAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);

        edtDriverEmail = (EditText) findViewById(R.id.driver_login_email);
        edtDriverPassword = (EditText) findViewById(R.id.driver_login_password);
        driverLoginBtn = (Button) findViewById(R.id.driver_login_btn);

        driverLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email = edtDriverEmail.getText().toString();
                String password = edtDriverPassword.getText().toString();

                if (TextUtils.isEmpty(email)){
                    Toast.makeText(DriverLoginActivity.this,
                            "Please write your email", Toast.LENGTH_SHORT).show();
                }
                else if (TextUtils.isEmpty(password)){
                    Toast.makeText(DriverLoginActivity.this,
                            "Please write your password", Toast.LENGTH_SHORT).show();
                }
                else{
                    loadingBar.setTitle("Driver Login");
                    loadingBar.setMessage("Please wait while we are checking your credentials");
                    loadingBar.show();

                    mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                loadingBar.dismiss();

                                Toast.makeText(DriverLoginActivity.this,
                                                "Logged in successfully", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(DriverLoginActivity.this, DriverMapActivity.class);
                                startActivity(intent);
                            }
                            else{
                                loadingBar.dismiss();
                                Toast.makeText(DriverLoginActivity.this,
                                        "Login Error: " + task.getException().getMessage() + "Please Register if not yet",
                                        Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(DriverLoginActivity.this, DriverRegisterActivity.class);
                                startActivity(intent);
                            }
                        }
                    });
                }

            }
        });

    }
}