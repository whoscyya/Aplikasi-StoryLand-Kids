package com.astrantiabooks.controller.activity;

import android.content.Intent;
import android.os.Bundle;
// Import Imageview untuk tombol back baru
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView; // Import baru untuk btnBack
import androidx.appcompat.app.AppCompatActivity;

import com.astrantiabooks.R;
import com.astrantiabooks.controller.activity.RegisterActivity; // PERBAIKAN: Tambahkan baris import ini
import com.astrantiabooks.model.LocalData;
import com.astrantiabooks.model.PrefManager;
import com.astrantiabooks.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    // Mengganti etEmail dengan edtUsername
    private EditText edtEmail, edtPassword;
    private Button btnLogin;
    // Mengganti tvRegister dengan txtSignup
    private TextView txtSignup;
    // Menambahkan ImageView untuk tombol back
    private ImageView btnBack;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inisialisasi Firebase
        mAuth = FirebaseAuth.getInstance();
        String dbUrl = "https://astrantia-books-28ad6-default-rtdb.asia-southeast1.firebasedatabase.app/";
        mDatabase = FirebaseDatabase.getInstance(dbUrl).getReference();

        // Binding Views (Sesuai XML baru)
        edtEmail = findViewById(R.id.edtEmail); // ID BARU
        edtPassword = findViewById(R.id.edtPassword); // ID BARU
        btnLogin = findViewById(R.id.btnLogin);
        txtSignup = findViewById(R.id.txtSignup); // ID BARU
        btnBack = findViewById(R.id.btnBack); // ID BARU

        // Jika user sudah login (Auto Login), langsung masuk
        if (mAuth.getCurrentUser() != null) {
            cekRoleDanRedirect(mAuth.getCurrentUser().getUid());
        }

        // Logic Tombol Back baru
        btnBack.setOnClickListener(v -> finish());

        btnLogin.setOnClickListener(v -> prosesLogin());

        // Logic Tombol Daftar/Sign-up baru
        txtSignup.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            // Jangan finish(), agar user bisa back ke login jika salah pencet
        });
    }

    private void prosesLogin() {
        String email = edtEmail.getText().toString().trim();
        String pass = edtPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Nama Pengguna dan Kata Sandi harus diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Loading...");

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        cekRoleDanRedirect(uid);
                    } else {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Masuk");
                        Toast.makeText(LoginActivity.this, "Masuk Gagal: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void cekRoleDanRedirect(String uid) {
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Masuk");

                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        user.setUid(uid);
                        // Update variable global
                        LocalData.currentUser = user;
                    }

                    // Simpan sesi
                    PrefManager prefManager = new PrefManager(LoginActivity.this);
                    prefManager.saveUser(user);

                    // Redirect
                    Intent intent;
                    if (user != null && "admin".equals(user.getRole())) {
                        intent = new Intent(LoginActivity.this, AdminMainActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, MainActivity.class);
                    }

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Data pengguna tidak ditemukan!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Masuk");
            }
        });
    }
}