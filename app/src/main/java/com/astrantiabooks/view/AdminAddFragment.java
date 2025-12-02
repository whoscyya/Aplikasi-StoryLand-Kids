package com.astrantiabooks.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.astrantiabooks.R;
import com.astrantiabooks.controller.adapters.AdminBukuAdapter;
import com.astrantiabooks.models.Buku;
import com.astrantiabooks.models.Promotion;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminAddFragment extends Fragment {

    // ==================== VIEWS ====================
    private RelativeLayout sectionBooks, sectionWidgets;
    private TextView btnSwitchBook, btnSwitchWidget;

    private RecyclerView rvBooks, rvWidgets;
    private FloatingActionButton btnAddBook, btnAddWidget;

    // ==================== DATA ====================
    private AdminBukuAdapter bookAdapter;
    private AdminWidgetAdapter widgetAdapter;
    private List<Buku> listBuku = new ArrayList<>();
    private List<Promotion> listWidget = new ArrayList<>();

    // Daftar Kategori
    private final String[] CATEGORIES = {"Edukasi", "Budaya", "Petualang", "Misteri", "Super Hero"};

    // ==================== FIREBASE ====================
    private DatabaseReference mRef;

    // PENTING: Variabel ini dipakai bergantian oleh Dialog Buku & Dialog Widget
    private Uri tempUri;
    private ImageView tempImgView; // Menyimpan referensi ImageView mana yang sedang diedit (Buku atau Widget)
    private ActivityResultLauncher<Intent> launcher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_add, container, false);

        String dbUrl = "https://astrantia-books-28ad6-default-rtdb.asia-southeast1.firebasedatabase.app/";
        mRef = FirebaseDatabase.getInstance(dbUrl).getReference();

        // ==================== INIT VIEW ====================
        sectionBooks = view.findViewById(R.id.sectionBooks);
        sectionWidgets = view.findViewById(R.id.sectionWidgets);

        btnSwitchBook = view.findViewById(R.id.cardEditorBook);
        btnSwitchWidget = view.findViewById(R.id.cardEditorWidget);

        rvBooks = view.findViewById(R.id.rvAdminBooks);
        rvWidgets = view.findViewById(R.id.rvAdminWidgets);

        btnAddBook = view.findViewById(R.id.btnAddBook);
        btnAddWidget = view.findViewById(R.id.btnAddWidget);

        // ==================== SETUP SWITCH ====================
        setupSwitcher();

        // ==================== RECYCLER VIEW ====================
        bookAdapter = new AdminBukuAdapter(getContext(), listBuku);
        rvBooks.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvBooks.setAdapter(bookAdapter);

        widgetAdapter = new AdminWidgetAdapter(getContext(), listWidget);
        rvWidgets.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvWidgets.setAdapter(widgetAdapter);

        // ==================== IMAGE PICKER ====================
        // Launcher ini sekarang bisa menangani gambar untuk BUKU maupun WIDGET
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() == getActivity().RESULT_OK && r.getData() != null) {
                tempUri = r.getData().getData();
                if (tempImgView != null) {
                    tempImgView.setImageURI(tempUri); // Tampilkan di ImageView dialog yang aktif
                }
            }
        });

        // ==================== LOAD DATA ====================
        loadData();

        // ==================== BUTTON ACTION ====================

        // UBAH: Sekarang btnAddBook memanggil Dialog, bukan pindah Activity
        btnAddBook.setOnClickListener(v -> showAddBookDialog());

        btnAddWidget.setOnClickListener(v -> showAddWidgetDialog());

        return view;
    }

    // ========================================================
    // 1. FUNGSI BARU: DIALOG TAMBAH BUKU
    // ========================================================
    private void showAddBookDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        // Inflate layout baru yang kita buat tadi
        View v = getLayoutInflater().inflate(R.layout.dialog_add_book_popup, null);
        builder.setView(v);
        AlertDialog dialog = builder.create();

        // Init Views dalam Dialog
        TextInputEditText etTitle = v.findViewById(R.id.etDialogBookTitle);
        TextInputEditText etAuthor = v.findViewById(R.id.etDialogBookAuthor);
        TextInputEditText etDesc = v.findViewById(R.id.etDialogBookDesc);
        AutoCompleteTextView etCategory = v.findViewById(R.id.etDialogBookCategory);
        ImageView imgPreview = v.findViewById(R.id.imgDialogBookPreview);
        Button btnPick = v.findViewById(R.id.btnDialogSelectImg);
        Button btnSave = v.findViewById(R.id.btnDialogSaveBook);

        // Reset variabel gambar
        tempUri = null;
        tempImgView = imgPreview; // Set target image view ke preview buku

        // Setup Dropdown Kategori
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        etCategory.setAdapter(adapter);
        etCategory.setOnClickListener(view -> etCategory.showDropDown());
        etCategory.setOnFocusChangeListener((view, hasFocus) -> { if(hasFocus) etCategory.showDropDown(); });

        // Klik Pilih Gambar
        btnPick.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            launcher.launch(intent);
        });

        // Klik Simpan Buku
        btnSave.setOnClickListener(view -> {
            String title = etTitle.getText().toString();
            String author = etAuthor.getText().toString();
            String category = etCategory.getText().toString();
            String desc = etDesc.getText().toString();

            if (title.isEmpty() || author.isEmpty()) {
                Toast.makeText(getContext(), "Judul dan Penulis wajib diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog pd = new ProgressDialog(getContext());
            pd.setMessage("Menyimpan Buku...");
            pd.show();

            // Cek apakah ada gambar yang dipilih
            if (tempUri != null) {
                // Upload Gambar dulu
                StorageReference storageRef = FirebaseStorage.getInstance().getReference("book_covers/" + UUID.randomUUID().toString() + ".jpg");
                storageRef.putFile(tempUri).addOnSuccessListener(task -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Setelah upload sukses, simpan data ke database
                        saveBookToDatabase(title, author, category, desc, uri.toString(), pd, dialog);
                    });
                }).addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(getContext(), "Gagal upload gambar", Toast.LENGTH_SHORT).show();
                });
            } else {
                // Simpan tanpa gambar (atau gambar default)
                saveBookToDatabase(title, author, category, desc, "", pd, dialog);
            }
        });

        dialog.show();
    }

    private void saveBookToDatabase(String title, String author, String category, String desc, String imgUrl, ProgressDialog pd, AlertDialog dialog) {
        String id = mRef.child("books").push().getKey();
        Buku buku = new Buku(id, title, author, category, desc, imgUrl);

        mRef.child("books").child(id).setValue(buku).addOnCompleteListener(task -> {
            pd.dismiss();
            dialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Buku Berhasil Ditambahkan!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Gagal menyimpan data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ========================================================
    // 2. FUNGSI DIALOG TAMBAH WIDGET (YANG LAMA)
    // ========================================================
    private void showAddWidgetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View v = getLayoutInflater().inflate(R.layout.dialog_add_widget, null);
        builder.setView(v);
        AlertDialog dialog = builder.create();

        EditText etTitle = v.findViewById(R.id.etWidgetTitle);
        EditText etSub = v.findViewById(R.id.etWidgetSubtitle);
        Spinner spinner = v.findViewById(R.id.spinnerTargetBook);
        ImageView img = v.findViewById(R.id.imgWidgetPreview);
        Button btnPick = v.findViewById(R.id.btnSelectWidgetImg);
        Button btnSave = v.findViewById(R.id.btnSaveWidget);

        tempUri = null;
        tempImgView = img; // PENTING: Set target ke preview widget

        // Isi spinner dari buku
        List<String> titles = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (Buku buku : listBuku) {
            titles.add(buku.getTitle());
            ids.add(buku.getId());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, titles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        btnPick.setOnClickListener(v1 -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            launcher.launch(intent);
        });

        btnSave.setOnClickListener(v1 -> {
            if (tempUri == null || etTitle.getText().toString().isEmpty()) {
                Toast.makeText(getContext(), "Data belum lengkap!", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog pd = new ProgressDialog(getContext());
            pd.setMessage("Menyimpan Widget...");
            pd.show();

            StorageReference storage = FirebaseStorage.getInstance().getReference("widget/" + UUID.randomUUID());
            storage.putFile(tempUri).addOnSuccessListener(task -> {
                storage.getDownloadUrl().addOnSuccessListener(uri -> {
                    String id = mRef.child("promotions").push().getKey();
                    String bookId = (!ids.isEmpty()) ? ids.get(spinner.getSelectedItemPosition()) : "";

                    Promotion promo = new Promotion(id, etTitle.getText().toString(), etSub.getText().toString(), uri.toString(), bookId);
                    mRef.child("promotions").child(id).setValue(promo);

                    pd.dismiss();
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Widget berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                pd.dismiss();
                Toast.makeText(getContext(), "Gagal upload", Toast.LENGTH_SHORT).show();
            });
        });

        dialog.show();
    }

    // ========================================================
    // LOGIKA LAIN (SWITCHER & LOAD DATA)
    // ========================================================
    private void setupSwitcher() {
        activateBookTab();
        btnSwitchBook.setOnClickListener(v -> activateBookTab());
        btnSwitchWidget.setOnClickListener(v -> activateWidgetTab());
    }

    private void activateBookTab() {
        sectionBooks.setVisibility(View.VISIBLE);
        sectionWidgets.setVisibility(View.GONE);
        btnSwitchBook.setBackgroundResource(R.drawable.indicator_active);
        btnSwitchWidget.setBackgroundResource(R.drawable.indicator_inactive);
    }

    private void activateWidgetTab() {
        sectionBooks.setVisibility(View.GONE);
        sectionWidgets.setVisibility(View.VISIBLE);
        btnSwitchWidget.setBackgroundResource(R.drawable.indicator_active);
        btnSwitchBook.setBackgroundResource(R.drawable.indicator_inactive);
    }

    private void loadData() {
        mRef.child("books").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listBuku.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    Buku b = s.getValue(Buku.class);
                    if (b != null) listBuku.add(b);
                }
                bookAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        mRef.child("promotions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listWidget.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    Promotion p = s.getValue(Promotion.class);
                    if (p != null) listWidget.add(p);
                }
                widgetAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    class AdminWidgetAdapter extends RecyclerView.Adapter<AdminWidgetAdapter.Holder> {

        Context ctx;
        List<Promotion> list;

        public AdminWidgetAdapter(Context ctx, List<Promotion> list) {
            this.ctx = ctx;
            this.list = list;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Pastikan layout yang di-inflate benar: item_admin_widget
            return new Holder(LayoutInflater.from(ctx)
                    .inflate(R.layout.item_admin_widget, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Promotion p = list.get(position);

            // Set Data ke View
            holder.title.setText(p.getTitle());
            holder.subtitle.setText(p.getSubtitle()); // Pastikan field subtitle ada di model Promotion

            if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
                Glide.with(ctx).load(p.getImageUrl()).into(holder.img);
            } else {
                holder.img.setImageResource(R.drawable.ic_launcher_background);
            }

            // Tombol Edit (Sementara Toast dulu atau logika edit nanti)
            holder.btnEdit.setOnClickListener(v -> {
                Toast.makeText(ctx, "Edit Widget: " + p.getTitle(), Toast.LENGTH_SHORT).show();
                // TODO: Tambahkan logika buka dialog edit widget di sini jika diperlukan
            });

            // Tombol Hapus
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(ctx)
                        .setTitle("Hapus Widget?")
                        .setMessage("Yakin hapus widget '" + p.getTitle() + "'?")
                        .setPositiveButton("Hapus", (d, w) -> {
                            mRef.child("promotions").child(p.getId()).removeValue()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(ctx, "Widget Terhapus", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(ctx, "Gagal Hapus", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView title, subtitle;
            ImageView img;
            ImageButton btnEdit, btnDelete; // Menggunakan ImageButton

            public Holder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvAdminWidgetTitle);
                subtitle = itemView.findViewById(R.id.tvAdminWidgetSubtitle); // Tambahan subtitle
                img = itemView.findViewById(R.id.imgAdminWidget);
                btnEdit = itemView.findViewById(R.id.btnEditWidget);     // ID Baru
                btnDelete = itemView.findViewById(R.id.btnDeleteWidget); // ID Baru
            }
        }
    }

}