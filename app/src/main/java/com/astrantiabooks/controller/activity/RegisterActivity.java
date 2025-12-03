package com.astrantiabooks.controller.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.astrantiabooks.R;
import com.astrantiabooks.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    // PERBAIKAN: Tambahkan etUsername
    private EditText etUsername, etEmail, etPassword, etConfirmPass;
    private Button btnRegister;
    private ImageView btnBack;
    private TextView txtSignIn;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        String dbUrl = "https://astrantia-books-28ad6-default-rtdb.asia-southeast1.firebasedatabase.app/";
        mDatabase = FirebaseDatabase.getInstance(dbUrl).getReference();

        // 1. Binding Views
        etUsername = findViewById(R.id.edtUsername); // BARU: Binding untuk Username
        etEmail = findViewById(R.id.edtEmail);
        etPassword = findViewById(R.id.edtPassword);
        etConfirmPass = findViewById(R.id.edtConfirmPass);
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);
        txtSignIn = findViewById(R.id.txtSignIn);

        // 2. Logic Tombol
        btnRegister.setOnClickListener(v -> registerUser());
        btnBack.setOnClickListener(v -> finish());
        txtSignIn.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        // PERBAIKAN: Ambil data username
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPass.getText().toString().trim();

        // PERBAIKAN: Tambahkan username di validasi
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Isi semua kolom!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cek Konfirmasi Password
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Kata Sandi dan Konfirmasi Kata Sandi tidak cocok.", Toast.LENGTH_SHORT).show();
            return;
        }

        // HAPUS: Logika pengambilan username dari email dihapus, diganti dengan input field

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        // PERBAIKAN: Kirim username yang sudah diinput
                        simpanKeRealtimeDB(firebaseUser.getUid(), email, username);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Gagal: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void simpanKeRealtimeDB(String uid, String email, String username) {
        User newUser = new User(uid, email, username, "user");

        mDatabase.child("users").child(uid).setValue(newUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Registrasi Berhasil! Silakan Masuk.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();

                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "DB Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}