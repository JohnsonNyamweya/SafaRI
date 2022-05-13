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
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class CustomerLoginActivity extends AppCompatActivity {

    private EditText edtCustomerEmail, edtCustomerPassword;
    private Button customerLoginBtn;

    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login);

        mAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);

        edtCustomerEmail = (EditText) findViewById(R.id.customer_login_email);
        edtCustomerPassword = (EditText) findViewById(R.id.customer_login_password);
        customerLoginBtn = (Button) findViewById(R.id.customer_login_btn);

        customerLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email = edtCustomerEmail.getText().toString();
                String password = edtCustomerPassword.getText().toString();

                if (TextUtils.isEmpty(email)){
                    Toast.makeText(CustomerLoginActivity.this,
                            "Please write your email", Toast.LENGTH_SHORT).show();
                }
                else if (TextUtils.isEmpty(password)){
                    Toast.makeText(CustomerLoginActivity.this,
                            "Please write your password", Toast.LENGTH_SHORT).show();
                }
                else{
                    loadingBar.setTitle("Customer Login");
                    loadingBar.setMessage("Please wait while we are checking your credentials");
                    loadingBar.show();

                    mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                loadingBar.dismiss();

                                Toast.makeText(CustomerLoginActivity.this,
                                        "Logged in successfully", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(CustomerLoginActivity.this, CustomerMapActivity.class);
                                startActivity(intent);
                            }
                            else{
                                loadingBar.dismiss();
                                Toast.makeText(CustomerLoginActivity.this,
                                        "Login Error: " + task.getException().getMessage() + "Please Register if not yet"
                                        , Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(CustomerLoginActivity.this, CustomerRegisterActivity.class);
                                startActivity(intent);
                            }
                        }
                    });
                }

            }
        });

    }
}