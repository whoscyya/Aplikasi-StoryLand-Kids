package com.astrantiabooks.controller.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter; // Import ini penting
import android.widget.AutoCompleteTextView; // Import ini penting
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.astrantiabooks.R;
import com.astrantiabooks.model.Buku;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.UUID;

public class AddBookActivity extends AppCompatActivity {
    private TextInputEditText etTitle, etAuthor, etDesc;
    private AutoCompleteTextView etCategory; // Ubah tipe jadi AutoCompleteTextView
    private ImageView imgPreview, btnBack;
    private Button btnSelectImg, btnSave;
    private Uri imageUri;
    private boolean isEdit = false;
    private Buku editBook;
    private DatabaseReference mDb;
    private StorageReference mStorage;

    // Daftar Kategori Tetap
    private final String[] CATEGORIES = {"Edukasi", "Budaya", "Petualang", "Misteri", "Super Hero"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_book);

        String dbUrl = "https://astrantia-books-28ad6-default-rtdb.asia-southeast1.firebasedatabase.app/";
        mDb = FirebaseDatabase.getInstance(dbUrl).getReference("books");
        mStorage = FirebaseStorage.getInstance().getReference("book_covers");

        initViews();
        setupCategoryDropdown(); // Setup dropdown kategori

        if (getIntent().getBooleanExtra("IS_EDIT_MODE", false)) {
            isEdit = true;
            editBook = (Buku) getIntent().getSerializableExtra("EXTRA_BOOK");
            fillData();
        }

        ActivityResultLauncher<Intent> launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        imgPreview.setImageURI(imageUri);
                    }
                });

        btnSelectImg.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            launcher.launch(i);
        });

        btnSave.setOnClickListener(v -> uploadAndSave());
        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        etTitle = findViewById(R.id.etJudul);
        etAuthor = findViewById(R.id.etPenulis);
        etCategory = findViewById(R.id.etKategori); // Pastikan di XML ini AutoCompleteTextView
        etDesc = findViewById(R.id.etDeskripsi);
        imgPreview = findViewById(R.id.imgPreview);
        btnSelectImg = findViewById(R.id.btnPilihGambar);
        btnSave = findViewById(R.id.btnSimpan);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        etCategory.setAdapter(adapter);
        // Agar dropdown muncul saat diklik
        etCategory.setOnClickListener(v -> etCategory.showDropDown());
        etCategory.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) etCategory.showDropDown();
        });
    }

    private void fillData() {
        etTitle.setText(editBook.getTitle());
        etAuthor.setText(editBook.getAuthor());
        etCategory.setText(editBook.getCategory());
        // Set text saja tidak cukup untuk memicu dropdown filter, tapi cukup untuk display
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        etCategory.setAdapter(adapter);

        etDesc.setText(editBook.getDescription());
        if (editBook.getCoverUrl() != null && !editBook.getCoverUrl().isEmpty())
            Glide.with(this).load(editBook.getCoverUrl()).into(imgPreview);
        btnSave.setText("Update Buku");
    }

    private void uploadAndSave() {
        String title = etTitle.getText().toString();
        String category = etCategory.getText().toString();

        if (title.isEmpty()) { Toast.makeText(this, "Judul wajib!", Toast.LENGTH_SHORT).show(); return; }

        // Validasi Kategori (Opsional, tapi bagus)
        boolean validCategory = false;
        for (String c : CATEGORIES) {
            if (c.equals(category)) { validCategory = true; break; }
        }
        if (!validCategory && !category.isEmpty()) {
            Toast.makeText(this, "Pilih kategori yang tersedia!", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Menyimpan...");
        pd.show();

        if (imageUri != null) {
            StorageReference ref = mStorage.child(UUID.randomUUID().toString() + ".jpg");
            ref.putFile(imageUri).addOnSuccessListener(t -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                saveData(uri.toString(), pd);
            }));
        } else {
            saveData(isEdit ? editBook.getCoverUrl() : "", pd);
        }
    }

    private void saveData(String url, ProgressDialog pd) {
        String id = isEdit ? editBook.getId() : mDb.push().getKey();
        Buku buku = new Buku(id, etTitle.getText().toString(), etAuthor.getText().toString(),
                etCategory.getText().toString(), etDesc.getText().toString(), url);
        mDb.child(id).setValue(buku).addOnCompleteListener(t -> {
            pd.dismiss();
            finish();
        });
    }
}