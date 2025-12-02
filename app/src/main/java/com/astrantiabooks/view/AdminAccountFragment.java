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
import com.astrantiabooks.controller.activity.LoginActivity; // <--- PENTING: Import LoginActivity
import com.astrantiabooks.models.LocalData;
import com.astrantiabooks.models.PrefManager;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminAccountFragment extends Fragment {

    private CircleImageView imgProfile;
    private ActivityResultLauncher<Intent> profilePicLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_account, container, false);

        Button btnLogout = view.findViewById(R.id.btnAdminLogout);
        imgProfile = view.findViewById(R.id.imgAdminProfile);
        TextView tvEmail = view.findViewById(R.id.tvAdminEmail);

        // Load Data Awal dari LocalData (Hanya untuk tampilan)
        if (LocalData.currentUser != null) {
            if (tvEmail != null) tvEmail.setText(LocalData.currentUser.getEmail());
            String photoUrl = LocalData.currentUser.getProfileImageUrl();
            if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(this).load(photoUrl).placeholder(R.drawable.ic_account).into(imgProfile);
            }
        }

        // Setup Image Picker
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

            // 3. Kembali ke LoginActivity (BUKAN WelcomeActivity)
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            // Clear Task agar tidak bisa back ke halaman admin
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        // ----------------------------

        return view;
    }

    private void uploadProfilePicture(Uri uri) {
        // Ambil User Langsung dari Firebase Auth (Lebih Aman)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(getContext(), "Gagal: Sesi kadaluarsa, silakan login ulang.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Mengupload foto...", Toast.LENGTH_SHORT).show();

        String uid = user.getUid();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("profile_images").child(uid + ".jpg");

        storageRef.putFile(uri).addOnSuccessListener(task ->
                storageRef.getDownloadUrl().addOnSuccessListener(url ->
                        updateDB(uid, url.toString())
                )
        ).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Gagal Upload: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void updateDB(String uid, String url) {
        if (uid == null) return;

        String dbUrl = "https://astrantia-books-28ad6-default-rtdb.asia-southeast1.firebasedatabase.app/";
        FirebaseDatabase.getInstance(dbUrl).getReference("users")
                .child(uid)
                .child("profileImageUrl")
                .setValue(url)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Foto Profil Diperbarui!", Toast.LENGTH_SHORT).show();

                        // Update Tampilan
                        Glide.with(this).load(url).into(imgProfile);

                        // Update Data Lokal & Simpan Sesi Baru agar sinkron
                        if (LocalData.currentUser != null) {
                            LocalData.currentUser.setProfileImageUrl(url);
                            new PrefManager(getContext()).saveUser(LocalData.currentUser);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Gagal Update DB: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}