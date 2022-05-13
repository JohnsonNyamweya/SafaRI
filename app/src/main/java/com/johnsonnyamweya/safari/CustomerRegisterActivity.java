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

public class CustomerRegisterActivity extends AppCompatActivity {

    private EditText edtCustomerEmail, edtCustomerPassword;
    private Button customerRegisterBtn;
    private TextView customerLoginLink;

    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;
    private DatabaseReference customerRef;
    private String onlineCustomerID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_register);

        mAuth = FirebaseAuth.getInstance();

        loadingBar = new ProgressDialog(this);

        edtCustomerEmail = (EditText) findViewById(R.id.customer_register_email);
        edtCustomerPassword = (EditText) findViewById(R.id.customer_register_password);
        customerRegisterBtn = (Button) findViewById(R.id.customer_register_btn);
        customerLoginLink = (TextView) findViewById(R.id.customer_login_link);

        customerLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent driverIntent =
                        new Intent(CustomerRegisterActivity.this, CustomerLoginActivity.class);
                startActivity(driverIntent);
            }
        });

        customerRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = edtCustomerEmail.getText().toString();
                String password = edtCustomerPassword.getText().toString();


                if (TextUtils.isEmpty(email)){
                    Toast.makeText(CustomerRegisterActivity.this,
                            "Please write your email", Toast.LENGTH_SHORT).show();
                }
                else if (TextUtils.isEmpty(password)){
                    Toast.makeText(CustomerRegisterActivity.this,
                            "Please write your password", Toast.LENGTH_SHORT).show();
                }
                else{
                    loadingBar.setTitle("Customer Registration");
                    loadingBar.setMessage("Please wait while we are registering your data");
                    loadingBar.show();

                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                loadingBar.dismiss();

                                onlineCustomerID = mAuth.getCurrentUser().getUid();
                                customerRef = FirebaseDatabase.getInstance().getReference()
                                        .child("Users").child("Customers")
                                        .child(onlineCustomerID);

                                customerRef.setValue(true);


                                Toast.makeText(CustomerRegisterActivity.this,
                                        "Registered successfully", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(CustomerRegisterActivity.this, CustomerLoginActivity.class);
                                startActivity(intent);
                            }
                            else{
                                loadingBar.dismiss();

                                Toast.makeText(CustomerRegisterActivity.this,
                                        "Registration Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

            }
        });

    }
}