package com.astrantiabooks.view;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.astrantiabooks.R;
import com.astrantiabooks.controller.activity.LoginActivity;
import com.astrantiabooks.model.LocalData;
import com.astrantiabooks.model.PrefManager;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;
import java.util.HashMap;
import java.util.Map;

public class AccountFragment extends Fragment {

    private CircleImageView imgProfile;
    private TextView tvUsernameCard;
    private EditText etFormUsername, etFormEmail;
    private Button btnLogout;
    private Button btnSaveProfile;
    private ActivityResultLauncher<Intent> profilePicLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // BINDING VIEW BARU
        imgProfile = view.findViewById(R.id.imgProfile);
        tvUsernameCard = view.findViewById(R.id.tvUsernameCard);
        etFormUsername = view.findViewById(R.id.et_form_username);
        etFormEmail = view.findViewById(R.id.et_form_email);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile); // PERBAIKAN: ID sekarang ditemukan dari XML

        if (LocalData.currentUser != null) {
            String username = LocalData.currentUser.getUsername();
            String email = LocalData.currentUser.getEmail();

            // Tampilkan data
            if (tvUsernameCard != null) tvUsernameCard.setText(username);
            if (etFormUsername != null) {
                etFormUsername.setText(username);
                // PERBAIKAN: Username bisa diubah
                etFormUsername.setFocusableInTouchMode(true);
            }
            if (etFormEmail != null) {
                etFormEmail.setText(email);
                // Email tidak diubah
                etFormEmail.setFocusable(false);
            }

            String photoUrl = LocalData.currentUser.getProfileImageUrl();
            if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(this).load(photoUrl).placeholder(R.drawable.ic_account).into(imgProfile);
            }
        }

        profilePicLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        uploadProfilePicture(result.getData().getData());
                    }
                }
        );

        imgProfile.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            profilePicLauncher.launch(intent);
        });

        // LOGIKA SAVE USERNAME BARU
        if (btnSaveProfile != null) {
            btnSaveProfile.setOnClickListener(v -> {
                String newUsername = etFormUsername.getText().toString().trim();
                String currentEmail = etFormEmail.getText().toString().trim();

                if (newUsername.isEmpty()) {
                    Toast.makeText(getContext(), "Nama Pengguna tidak boleh kosong.", Toast.LENGTH_SHORT).show();
                    return;
                }
                updateUserData(newUsername, currentEmail);
            });
        }

        // LOGIKA LOGOUT
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            if (getContext() != null) {
                PrefManager prefManager = new PrefManager(getContext());
                prefManager.logout();
            }
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    // FUNGSI UPDATE DATA USERNAME DI DATABASE DAN SESI LOKAL
    private void updateUserData(String newUsername, String currentEmail) {
        if (LocalData.currentUser == null) return;
        if (getContext() == null) return;

        Toast.makeText(getContext(), "Menyimpan perubahan...", Toast.LENGTH_SHORT).show();

        String uid = LocalData.currentUser.getUid();
        String dbUrl = "https://astrantia-books-28ad6-default-rtdb.asia-southeast1.firebasedatabase.app/";
        DatabaseReference userRef = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(uid);

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", newUsername);

        userRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Data Profil Diperbarui!", Toast.LENGTH_SHORT).show();

                    // 1. Update Data Lokal
                    LocalData.currentUser.setUsername(newUsername);

                    // 2. Simpan Sesi Baru
                    new PrefManager(getContext()).saveUser(LocalData.currentUser);

                    // 3. Update Tampilan
                    if (tvUsernameCard != null) tvUsernameCard.setText(newUsername);
                    if (etFormUsername != null) etFormUsername.setText(newUsername);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Gagal Update Data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void uploadProfilePicture(Uri uri) {
        if (LocalData.currentUser == null) return;
        Toast.makeText(getContext(), "Mengupload foto...", Toast.LENGTH_SHORT).show();

        String uid = LocalData.currentUser.getUid();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("profile_images").child(uid + ".jpg");

        storageRef.putFile(uri).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                updateDatabaseProfileUrl(uid, downloadUrl.toString());
            });
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Gagal upload: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void updateDatabaseProfileUrl(String uid, String url) {
        String dbUrl = "https://astrantia-books-28ad6-default-rtdb.asia-southeast1.firebasedatabase.app/";

        FirebaseDatabase.getInstance(dbUrl).getReference("users")
                .child(uid)
                .child("profileImageUrl")
                .setValue(url)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Foto profil diperbarui!", Toast.LENGTH_SHORT).show();
                        Glide.with(this).load(url).into(imgProfile);

                        // Update Data Lokal & Sesi
                        LocalData.currentUser.setProfileImageUrl(url);
                        new PrefManager(getContext()).saveUser(LocalData.currentUser);

                        // Update form field display
                        if (tvUsernameCard != null) tvUsernameCard.setText(LocalData.currentUser.getUsername());
                        if (etFormUsername != null) etFormUsername.setText(LocalData.currentUser.getUsername());
                        if (etFormEmail != null) etFormEmail.setText(LocalData.currentUser.getEmail());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Gagal Update DB: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}