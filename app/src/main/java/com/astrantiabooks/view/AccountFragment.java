package com.astrantiabooks.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.astrantiabooks.R;
import com.astrantiabooks.controller.activity.LoginActivity; // Pastikan import LoginActivity
// import com.astrantiabooks.activities.WelcomeActivity; // HAPUS INI
import com.astrantiabooks.models.LocalData;
import com.astrantiabooks.models.PrefManager;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

public class AccountFragment extends Fragment {

    private CircleImageView imgProfile;
    private ActivityResultLauncher<Intent> profilePicLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        imgProfile = view.findViewById(R.id.imgProfile);
        TextView tvUsername = view.findViewById(R.id.tvAccUsername);
        TextView tvEmail = view.findViewById(R.id.tvAccEmail);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        if (LocalData.currentUser != null) {
            tvUsername.setText(LocalData.currentUser.getUsername());
            tvEmail.setText(LocalData.currentUser.getEmail());

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

        // --- UPDATE LOGOUT DISINI ---
        btnLogout.setOnClickListener(v -> {
            // 1. Logout Firebase
            FirebaseAuth.getInstance().signOut();

            // 2. Hapus Sesi Lokal
            if (getContext() != null) {
                PrefManager prefManager = new PrefManager(getContext());
                prefManager.logout();
            }

            // 3. Redirect ke LoginActivity (BUKAN WelcomeActivity)
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            // Membersihkan back stack agar user tidak bisa tekan 'Back' untuk kembali ke akun
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        // ----------------------------

        return view;
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
        // Gunakan URL default atau ambil dari google-services.json
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
                    }
                });
    }
}