package com.astrantiabooks.view;

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
import com.astrantiabooks.model.Buku;
import com.astrantiabooks.model.Promotion;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
        // PERBAIKAN ERROR KONSTRUKTOR: Melewatkan 'this' (AdminAddFragment) sebagai argumen ketiga
        bookAdapter = new AdminBukuAdapter(getContext(), listBuku, this);
        rvBooks.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvBooks.setAdapter(bookAdapter);

        // Melewatkan referensi Fragment untuk mengakses showEditWidgetDialog
        widgetAdapter = new AdminWidgetAdapter(getContext(), listWidget, this);
        rvWidgets.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvWidgets.setAdapter(widgetAdapter);

        // ==================== IMAGE PICKER ====================
        // Launcher ini sekarang bisa menangani gambar untuk BUKU maupun WIDGET
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() == getActivity().RESULT_OK && r.getData() != null) {
                tempUri = r.getData().getData();
                if (tempImgView != null) {
                    Glide.with(this).load(tempUri).into(tempImgView); // Tampilkan di ImageView dialog yang aktif
                }
            }
        });

        // ==================== LOAD DATA ====================
        loadData();

        // ==================== BUTTON ACTION ====================

        // UBAH: Sekarang btnAddBook memanggil Dialog untuk Tambah Buku (null = Add Mode)
        btnAddBook.setOnClickListener(v -> showBookDialog(null));

        btnAddWidget.setOnClickListener(v -> showAddWidgetDialog());

        return view;
    }

    // ========================================================
    // 1. FUNGSI UNIFIED: DIALOG TAMBAH/EDIT BUKU
    // ========================================================
    public void showEditBookDialog(Buku bookToEdit) {
        showBookDialog(bookToEdit);
    }

    private void showBookDialog(Buku bookToEdit) {
        Context context = getContext();
        if (context == null) return;
        boolean isEditMode = (bookToEdit != null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        // MENGGUNAKAN LAYOUT SEDERHANA: dialog_add_book_simple.xml
        View v = getLayoutInflater().inflate(R.layout.dialog_add_book_simple, null);
        builder.setView(v);
        AlertDialog dialog = builder.create();

        // Init Views dalam Dialog (menggunakan ID dari dialog_add_book_simple.xml)
        EditText etTitle = v.findViewById(R.id.etBookTitleSimple);
        EditText etAuthor = v.findViewById(R.id.etBookAuthorSimple);
        EditText etCategory = v.findViewById(R.id.etBookCategorySimple);
        EditText etDesc = v.findViewById(R.id.etBookDescSimple);
        ImageView imgPreview = v.findViewById(R.id.imgBookPreviewSimple);
        Button btnPick = v.findViewById(R.id.btnSelectBookImgSimple);
        Button btnSave = v.findViewById(R.id.btnSaveBookSimple);

        // Asumsikan ID ini ada di dialog_add_book_simple.xml
        TextView tvDialogTitle = (TextView) v.findViewById(R.id.tvDialogTitleSimple);

        // Atur teks dialog
        if (tvDialogTitle != null) {
            tvDialogTitle.setText(isEditMode ? "Edit Buku" : "Tambah Buku Baru");
        }
        btnSave.setText(isEditMode ? "Simpan Perubahan" : "Simpan Buku");

        // --- PRA-ISI DATA JIKA MODE EDIT ---
        if (isEditMode) {
            etTitle.setText(bookToEdit.getTitle());
            etAuthor.setText(bookToEdit.getAuthor());
            etCategory.setText(bookToEdit.getCategory());
            etDesc.setText(bookToEdit.getDescription());

            // Load gambar yang sudah ada
            if (bookToEdit.getCoverUrl() != null && !bookToEdit.getCoverUrl().isEmpty()) {
                Glide.with(this).load(bookToEdit.getCoverUrl()).into(imgPreview);
            }
        }

        // Reset variabel gambar
        tempUri = null;
        tempImgView = imgPreview;

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

            // Gunakan ID yang ada jika mode edit, buat baru jika mode tambah
            String bookId = isEditMode ? bookToEdit.getId() : mRef.child("books").push().getKey();
            // Pertahankan URL lama jika mode edit dan tidak ada gambar baru dipilih
            String existingImageUrl = isEditMode ? bookToEdit.getCoverUrl() : "";

            if (title.isEmpty() || author.isEmpty()) {
                Toast.makeText(context, "Judul dan Penulis wajib diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog pd = new ProgressDialog(context);
            pd.setMessage(isEditMode ? "Menyimpan Perubahan..." : "Menyimpan Buku...");
            pd.show();

            // Cek apakah ada gambar yang dipilih (tempUri != null)
            if (tempUri != null) {
                // Upload Gambar baru
                StorageReference storageRef = FirebaseStorage.getInstance().getReference("book_covers/" + UUID.randomUUID().toString() + ".jpg");
                storageRef.putFile(tempUri).addOnSuccessListener(task -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Setelah upload sukses, simpan data ke database dengan URL baru
                        saveBookToDatabase(bookId, title, author, category, desc, uri.toString(), pd, dialog);
                    });
                }).addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(context, "Gagal upload gambar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } else {
                // Simpan tanpa gambar baru (gunakan URL lama jika mode edit)
                saveBookToDatabase(bookId, title, author, category, desc, existingImageUrl, pd, dialog);
            }
        });

        dialog.show();
    }

    private void saveBookToDatabase(String id, String title, String author, String category, String desc, String imgUrl, ProgressDialog pd, AlertDialog dialog) {
        Buku buku = new Buku(id, title, author, category, desc, imgUrl); // Gunakan ID yang disediakan

        mRef.child("books").child(id).setValue(buku).addOnCompleteListener(task -> {
            pd.dismiss();
            dialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), (mRef.child("books").child(id).getKey().equals(id) ? "Buku Berhasil Diperbarui!" : "Buku Berhasil Ditambahkan!"), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Gagal menyimpan data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ========================================================
    // 2. FUNGSI DIALOG TAMBAH WIDGET (Panggilan ke showWidgetDialog)
    // ========================================================
    private void showAddWidgetDialog() {
        showWidgetDialog(null);
    }

    // ========================================================
    // 3. FUNGSI BARU: DIALOG EDIT WIDGET (API publik untuk Adapter)
    // ========================================================
    public void showEditWidgetDialog(Promotion promotionToEdit) {
        showWidgetDialog(promotionToEdit);
    }

    private void showWidgetDialog(Promotion promotionToEdit) {
        Context context = getContext();
        if (context == null) return;

        boolean isEditMode = (promotionToEdit != null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View v = getLayoutInflater().inflate(R.layout.dialog_add_widget, null);
        builder.setView(v);
        AlertDialog dialog = builder.create();

        // PERBAIKAN: Binding Views (etWidgetTitle, etWidgetSubtitle, dll.)
        EditText etTitle = v.findViewById(R.id.etWidgetTitle);
        EditText etSub = v.findViewById(R.id.etWidgetSubtitle);
        Spinner spinner = v.findViewById(R.id.spinnerTargetBook);
        ImageView img = v.findViewById(R.id.imgWidgetPreview);
        Button btnPick = v.findViewById(R.id.btnSelectWidgetImg);
        Button btnSave = v.findViewById(R.id.btnSaveWidget);

        tempUri = null;
        tempImgView = img;

        // Menggunakan R.id.textTitle yang diasumsikan sudah ditambahkan di XML dialog_add_widget.xml
        TextView tvTitleDialog = (TextView) v.findViewById(R.id.textTitle);
        tvTitleDialog.setText(isEditMode ? "Edit Widget Promo" : "Tambah Widget Promo");
        btnSave.setText(isEditMode ? "Simpan Perubahan" : "Simpan");

        // Isi spinner dari buku (sama seperti sebelumnya)
        List<String> titles = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        int selectedBookPosition = 0;

        if (listBuku.isEmpty()) {
            titles.add("Tidak ada buku");
            ids.add("");
        } else {
            for (Buku buku : listBuku) {
                titles.add(buku.getTitle());
                ids.add(buku.getId());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, titles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // PRA-ISI DATA JIKA MODE EDIT
        if (isEditMode) {
            etTitle.setText(promotionToEdit.getTitle());
            etSub.setText(promotionToEdit.getSubtitle());

            // Load gambar yang sudah ada
            if (promotionToEdit.getImageUrl() != null && !promotionToEdit.getImageUrl().isEmpty()) {
                Glide.with(this).load(promotionToEdit.getImageUrl()).into(img);
            }

            // Set buku target yang dipilih
            if (promotionToEdit.getTargetBookId() != null) {
                for (int i = 0; i < ids.size(); i++) {
                    if (ids.get(i).equals(promotionToEdit.getTargetBookId())) {
                        selectedBookPosition = i;
                        break;
                    }
                }
            }
            spinner.setSelection(selectedBookPosition);
        }


        btnPick.setOnClickListener(v1 -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            launcher.launch(intent);
        });

        btnSave.setOnClickListener(v1 -> {
            String title = etTitle.getText().toString();
            String sub = etSub.getText().toString();

            if (title.isEmpty() || (!isEditMode && tempUri == null)) {
                Toast.makeText(context, "Judul dan Gambar wajib diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog pd = new ProgressDialog(context);
            pd.setMessage(isEditMode ? "Memperbarui Widget..." : "Menyimpan Widget...");
            pd.show();

            // Jika ada gambar baru dipilih (tempUri != null)
            if (tempUri != null) {
                // Upload Gambar
                StorageReference storage = FirebaseStorage.getInstance().getReference("widget/" + UUID.randomUUID());
                storage.putFile(tempUri).addOnSuccessListener(task -> {
                    storage.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Simpan/Update data dengan URL baru
                        saveOrUpdateWidget(
                                isEditMode ? promotionToEdit.getId() : mRef.child("promotions").push().getKey(),
                                title, sub, uri.toString(), ids.get(spinner.getSelectedItemPosition()), pd, dialog
                        );
                    });
                }).addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(context, "Gagal upload: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } else {
                // Mode EDIT TANPA upload gambar baru, gunakan URL gambar lama
                String oldImageUrl = isEditMode ? promotionToEdit.getImageUrl() : "";
                saveOrUpdateWidget(
                        isEditMode ? promotionToEdit.getId() : mRef.child("promotions").push().getKey(),
                        title, sub, oldImageUrl, ids.get(spinner.getSelectedItemPosition()), pd, dialog
                );
            }
        });

        dialog.show();
    }

    private void saveOrUpdateWidget(String id, String title, String sub, String imageUrl, String bookId, ProgressDialog pd, AlertDialog dialog) {
        Promotion promo = new Promotion(id, title, sub, imageUrl, bookId);

        mRef.child("promotions").child(id).setValue(promo).addOnCompleteListener(task -> {
            pd.dismiss();
            dialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Widget Berhasil Disimpan!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Gagal menyimpan data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
        AdminAddFragment fragment;

        // Perbarui konstruktor
        public AdminWidgetAdapter(Context ctx, List<Promotion> list, AdminAddFragment fragment) {
            this.ctx = ctx;
            this.list = list;
            this.fragment = fragment;
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
            holder.subtitle.setText(p.getSubtitle());

            if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
                Glide.with(ctx).load(p.getImageUrl()).into(holder.img);
            } else {
                holder.img.setImageResource(R.drawable.ic_launcher_background);
            }

            // Tombol EDIT
            holder.btnEdit.setOnClickListener(v -> {
                // Panggil fungsi edit widget
                fragment.showEditWidgetDialog(p);
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
            ImageButton btnEdit, btnDelete;

            public Holder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvAdminWidgetTitle);
                subtitle = itemView.findViewById(R.id.tvAdminWidgetSubtitle);
                img = itemView.findViewById(R.id.imgAdminWidget);
                btnEdit = itemView.findViewById(R.id.btnEditWidget);
                btnDelete = itemView.findViewById(R.id.btnDeleteWidget);
            }
        }
    }
}