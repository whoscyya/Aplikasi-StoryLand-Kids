package com.astrantiabooks.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.astrantiabooks.R;
import com.astrantiabooks.controller.activity.SearchActivity;
import com.astrantiabooks.controller.adapters.UserBukuAdapter;
import com.astrantiabooks.models.Buku;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment {

    private RecyclerView rvCategories, rvGrid;
    private UserBukuAdapter bukuAdapter;
    private ExploreCategoryAdapter categoryAdapter;

    private List<Buku> listBukuMaster = new ArrayList<>();
    private List<Buku> listBukuDisplay = new ArrayList<>();

    // Kategori fungsional
    private final String[] CATEGORIES = {"Semua", "Edukasi", "Budaya", "Petualang", "Misteri", "Super Hero"};
    private int selectedCategoryIndex = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);

        ImageButton btnSearch = view.findViewById(R.id.btnOpenSearch);
        rvCategories = view.findViewById(R.id.rvExploreCategories);
        rvGrid = view.findViewById(R.id.rvExploreGrid);

        // 1. SETUP TOMBOL SEARCH -> Buka SearchActivity (Overlay)
        btnSearch.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), SearchActivity.class));
        });

        // 2. SETUP GRID BUKU
        rvGrid.setLayoutManager(new GridLayoutManager(getContext(), 2));
        bukuAdapter = new UserBukuAdapter(getContext(), listBukuDisplay);
        rvGrid.setAdapter(bukuAdapter);

        // 3. SETUP KATEGORI (CHIPS)
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new ExploreCategoryAdapter();
        rvCategories.setAdapter(categoryAdapter);

        // 4. LOAD DATA
        loadBooks();

        return view;
    }

    private void loadBooks() {
        String dbUrl = "https://astrantia-books-28ad6-default-rtdb.asia-southeast1.firebasedatabase.app/";
        DatabaseReference ref = FirebaseDatabase.getInstance(dbUrl).getReference("books");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listBukuMaster.clear();
                listBukuDisplay.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Buku buku = data.getValue(Buku.class);
                    if (buku != null) {
                        listBukuMaster.add(buku);
                        // Filter awal berdasarkan kategori yang terpilih
                        if (selectedCategoryIndex == 0 || (buku.getCategory() != null &&
                                buku.getCategory().equalsIgnoreCase(CATEGORIES[selectedCategoryIndex]))) {
                            listBukuDisplay.add(buku);
                        }
                    }
                }
                bukuAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void filterByCategory(int index) {
        selectedCategoryIndex = index;
        String category = CATEGORIES[index];

        listBukuDisplay.clear();
        if (index == 0) { // "Semua"
            listBukuDisplay.addAll(listBukuMaster);
        } else {
            for (Buku b : listBukuMaster) {
                if (b.getCategory() != null && b.getCategory().equalsIgnoreCase(category)) {
                    listBukuDisplay.add(b);
                }
            }
        }
        bukuAdapter.notifyDataSetChanged();
    }

    // --- ADAPTER KATEGORI (SUDAH DIPERBAIKI) ---
    class ExploreCategoryAdapter extends RecyclerView.Adapter<ExploreCategoryAdapter.Holder> {

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // 1. Buat TextView secara programatik
            TextView tv = new TextView(getContext());
            tv.setPadding(40, 20, 40, 20);
            tv.setTextSize(14f);

            // 2. ATUR MARGIN (Agar tidak berdempetan)
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            // Konversi 8dp ke pixel agar jaraknya pas di HP apapun
            int marginEnd = (int) (8 * getResources().getDisplayMetrics().density);
            lp.setMargins(0, 0, marginEnd, 0);

            tv.setLayoutParams(lp);

            return new Holder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            String cat = CATEGORIES[position];
            holder.tv.setText(cat);

            // Logic Ganti Warna (Kuning vs Putih/Abu)
            if (selectedCategoryIndex == position) {
                // Aktif: Warna Kuning, Teks Putih
                holder.tv.setBackgroundResource(R.drawable.category_active);
                holder.tv.setTextColor(Color.WHITE);
            } else {
                // Tidak Aktif: Outline / Putih, Teks Hitam
                holder.tv.setBackgroundResource(R.drawable.bg_button_rounded); // Pastikan drawable ini ada (putih border abu)
                holder.tv.setTextColor(Color.BLACK);
            }

            holder.itemView.setOnClickListener(v -> {
                int oldIndex = selectedCategoryIndex;
                selectedCategoryIndex = position;
                notifyItemChanged(oldIndex);
                notifyItemChanged(selectedCategoryIndex);
                filterByCategory(position);
            });
        }

        @Override public int getItemCount() { return CATEGORIES.length; }

        class Holder extends RecyclerView.ViewHolder {
            TextView tv;
            Holder(View v) { super(v); tv = (TextView) v; }
        }
    }
}