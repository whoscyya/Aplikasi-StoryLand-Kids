package com.astrantiabooks.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.astrantiabooks.databinding.ActivityDetailBukuBinding; // Pastikan ViewBinding aktif di build.gradle
import com.astrantiabooks.models.Buku;
import com.astrantiabooks.models.LocalData;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DetailBukuActivity extends AppCompatActivity {
    private ActivityDetailBukuBinding binding;
    private Buku currentBook;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBukuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String dbUrl = "https://astrantia-books-28ad6-default-rtdb.asia-southeast1.firebasedatabase.app/";
        mDatabase = FirebaseDatabase.getInstance(dbUrl).getReference("books");

        if (getIntent().hasExtra("extra_buku")) {
            currentBook = (Buku) getIntent().getSerializableExtra("extra_buku");
            updateUI(currentBook);
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnReadSynopsis.setOnClickListener(v -> {
            if (currentBook != null) {
                Intent intent = new Intent(this, IsiBukuActivity.class);
                intent.putExtra("EXTRA_SYNOPSIS", currentBook.getDescription());
                startActivity(intent);
            }
        });

        // Fitur Admin Only
        if (LocalData.currentUser != null && "admin".equals(LocalData.currentUser.getRole())) {
            binding.btnEdit.setVisibility(View.VISIBLE);
            binding.btnDelete.setVisibility(View.VISIBLE);

            binding.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddBookActivity.class);
                intent.putExtra("IS_EDIT_MODE", true);
                intent.putExtra("EXTRA_BOOK", currentBook);
                startActivity(intent);
                finish();
            });

            binding.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(this).setTitle("Hapus").setMessage("Yakin hapus?")
                        .setPositiveButton("Ya", (d,w) -> {
                            mDatabase.child(currentBook.getId()).removeValue();
                            finish();
                        }).setNegativeButton("Batal", null).show();
            });
        } else {
            binding.btnEdit.setVisibility(View.GONE);
            binding.btnDelete.setVisibility(View.GONE);
        }
    }

    private void updateUI(Buku buku) {
        binding.tvDetailTitle.setText(buku.getTitle());
        binding.tvDetailAuthor.setText(buku.getAuthor());
        binding.tvDetailCategory.setText(buku.getCategory());
        binding.tvDetailYear.setText("-");
        if (buku.getCoverUrl() != null && !buku.getCoverUrl().isEmpty()) {
            Glide.with(this).load(buku.getCoverUrl()).into(binding.imgDetailCover);
        }
    }
}