package com.astrantiabooks.controller;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.astrantiabooks.R;
import com.astrantiabooks.models.Buku;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvResults;
    private SearchAdapter adapter;
    private List<Buku> listMaster = new ArrayList<>();
    private List<Buku> listDisplay = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etSearch = findViewById(R.id.etSearchInput);
        rvResults = findViewById(R.id.rvSearchResults);
        ImageView btnBack = findViewById(R.id.btnBackSearch);

        btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchAdapter(listDisplay);
        rvResults.setAdapter(adapter);

        // Load Data
        loadData();

        // Search Logic
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Focus ke Search bar langsung
        etSearch.requestFocus();
    }

    private void loadData() {
        String dbUrl = "https://astrantia-books-28ad6-default-rtdb.asia-southeast1.firebasedatabase.app/";
        FirebaseDatabase.getInstance(dbUrl).getReference("books")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listMaster.clear();
                        for (DataSnapshot s : snapshot.getChildren()) {
                            Buku b = s.getValue(Buku.class);
                            if (b != null) listMaster.add(b);
                        }
                        // Awalnya kosong atau tampilkan semua (opsional, disini saya kosongkan agar bersih)
                        // filter("");
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void filter(String query) {
        listDisplay.clear();
        if (query.isEmpty()) {
            // Jika kosong, jangan tampilkan apa-apa (atau tampilkan history)
        } else {
            String q = query.toLowerCase();
            for (Buku b : listMaster) {
                if (b.getTitle().toLowerCase().contains(q) || b.getAuthor().toLowerCase().contains(q)) {
                    listDisplay.add(b);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // --- INNER ADAPTER CLASS KHUSUS SEARCH ---
    class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.Holder> {
        List<Buku> list;
        public SearchAdapter(List<Buku> list) { this.list = list; }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Buku b = list.get(position);
            holder.title.setText(b.getTitle());
            holder.category.setText(b.getCategory());
            holder.author.setText(b.getAuthor());
            if (b.getCoverUrl() != null && !b.getCoverUrl().isEmpty())
                Glide.with(holder.itemView.getContext()).load(b.getCoverUrl()).into(holder.img);

            holder.itemView.setOnClickListener(v -> {
                Intent i = new Intent(SearchActivity.this, DetailBukuActivity.class);
                i.putExtra("extra_buku", b);
                startActivity(i);
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView title, category, author;
            ImageView img;
            public Holder(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.tvSearchTitle);
                category = v.findViewById(R.id.tvSearchCategory);
                author = v.findViewById(R.id.tvSearchAuthor);
                img = v.findViewById(R.id.imgSearchCover);
            }
        }
    }
}