package com.shruti.lofo;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
    private static final String USJ_EMAIL_DOMAIN = "@net.usj.edu.lb";
    private static final int MIN_PASSWORD_LENGTH = 6;

    EditText signupName, signupPhone, signupEmail, signupPassword;
    TextView loginRedirectText;
    Button signupButton;
    private Toast toast;
    private Handler handler = new Handler();
    FirebaseDatabase database;
    DatabaseReference reference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        signupName = findViewById(R.id.signup_name);
        signupEmail = findViewById(R.id.signup_email);
        signupPhone = findViewById(R.id.signup_phone);
        signupPassword = findViewById(R.id.signup_password);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        signupButton = findViewById(R.id.signup_button);
        signupEmail.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String email = s.toString().trim();
                if (!isUsjEmail(email)) {
                    signupEmail.setError("Use your USJ email (@net.usj.edu.lb)");
                    signupEmail.setTextColor(Color.RED);
                } else {
                    signupEmail.setError(null);
                    signupEmail.setTextColor(Color.BLACK);
                }
            }
        });
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String name = signupName.getText().toString().trim();
                String email = signupEmail.getText().toString().trim();
                String phone = signupPhone.getText().toString().trim();
                String password = signupPassword.getText().toString().trim();

                if (!isUsjEmail(email)) {
                    Toast.makeText(Register.this, "Please sign up with @net.usj.edu.lb email.", Toast.LENGTH_SHORT).show();
                    signupEmail.requestFocus();
                    return;
                }
                if (password.length() < MIN_PASSWORD_LENGTH) {
                    signupPassword.setError("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
                    signupPassword.requestFocus();
                    return;
                }
                if (phone.isEmpty()) {
                    signupPhone.setError("Phone is required");
                    signupPhone.requestFocus();
                    return;
                }

                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(Register.this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    user.sendEmailVerification()
                                            .addOnCompleteListener(task1 -> {
                                                if (task1.isSuccessful()) {
                                                    Toast.makeText(Register.this, "Verification email sent. Please verify before logging in.", Toast.LENGTH_LONG).show();
                                                } else {
                                                    Log.e(TAG, "sendEmailVerification", task1.getException());
                                                }
                                            });
                                }

                                FirebaseFirestore db = FirebaseFirestore.getInstance();

                                Map<String, Object> user1 = new HashMap<>();
                                user1.put("name", name);
                                user1.put("email", email);
                                user1.put("phone", phone);
                                user1.put("password", password);

                                db.collection("users")
                                        .add(user1)
                                        .addOnSuccessListener(documentReference -> {
                                            Toast.makeText(Register.this, "You have signed up successfully!", Toast.LENGTH_SHORT).show();
                                            mAuth.signOut();
                                            Intent intent = new Intent(Register.this, Login.class);
                                            startActivity(intent);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to save user profile", e);
                                            mAuth.signOut();
                                        });
                            } else {
                                Exception e = task.getException();
                                Log.e(TAG, "Registration failed", e);
                                String message = (e != null && e.getMessage() != null)
                                        ? e.getMessage()
                                        : "Registration failed. Check email, password, and network.";
                                Toast.makeText(Register.this, message, Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Register.this, Login.class);
                startActivity(intent);
            }
        });
    }

    private boolean isUsjEmail(String email) {
        return email != null && email.toLowerCase().endsWith(USJ_EMAIL_DOMAIN);
    }
}
