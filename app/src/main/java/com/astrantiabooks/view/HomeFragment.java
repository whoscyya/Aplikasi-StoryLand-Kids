package com.astrantiabooks.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.astrantiabooks.R;
import com.astrantiabooks.controller.activity.DetailBukuActivity;
import com.astrantiabooks.controller.activity.MainActivity;
import com.astrantiabooks.controller.adapters.UserBukuAdapter;
import com.astrantiabooks.model.Buku;
import com.astrantiabooks.model.LocalData;
import com.astrantiabooks.model.Promotion;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    // Views
    private ViewPager2 viewPagerSlider;
    private RecyclerView rvCategories, rvBooks;
    private LinearLayout layoutSliderIndicators;
    private TextView tvGreeting, tvSeeAll;
    private ImageView imgProfile;
    private ImageButton btnSearch;

    // Search Views Baru
    private LinearLayout layoutHeaderNormal, layoutHeaderSearch;
    private EditText etSearchField;
    private ImageButton btnCloseSearch;

    // Data & Adapters
    private UserBukuAdapter bukuAdapter;
    private PromoAdapter promoAdapter;
    private CategoryAdapter categoryAdapter;

    private List<Buku> listBukuMaster = new ArrayList<>();
    private List<Buku> listBukuDisplay = new ArrayList<>();
    private List<Promotion> listPromo = new ArrayList<>();

    private final String[] CATEGORIES = {"Semua", "Edukasi", "Budaya", "Petualang", "Misteri", "Super Hero"};
    private int selectedCategoryIndex = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        String dbUrl = "https://astrantia-books-28ad6-default-rtdb.asia-southeast1.firebasedatabase.app/";
        DatabaseReference ref = FirebaseDatabase.getInstance(dbUrl).getReference();

        // 1. INIT VIEWS
        imgProfile = view.findViewById(R.id.imgProfile);
        tvGreeting = view.findViewById(R.id.tvGreeting); // PERBAIKAN: Menggunakan ID baru
        viewPagerSlider = view.findViewById(R.id.viewPagerSlider);
        layoutSliderIndicators = view.findViewById(R.id.layoutSliderIndicators);
        rvCategories = view.findViewById(R.id.rvCategories);
        rvBooks = view.findViewById(R.id.rvPopularBooks);
        tvSeeAll = view.findViewById(R.id.tvSeeAll);

        // Init Search Views Baru
        layoutHeaderNormal = view.findViewById(R.id.layoutHeaderNormal);
        layoutHeaderSearch = view.findViewById(R.id.layoutHeaderSearch);
        etSearchField = view.findViewById(R.id.etSearchField);
        btnCloseSearch = view.findViewById(R.id.btn_close_search); // PERBAIKAN: Menggunakan ID baru

        // 2. SETUP USER INFO
        if (LocalData.currentUser != null) {
            tvGreeting.setText("Halo, " + LocalData.currentUser.getUsername() + "!");
            if (LocalData.currentUser.getProfileImageUrl() != null && !LocalData.currentUser.getProfileImageUrl().isEmpty()) {
                Glide.with(this).load(LocalData.currentUser.getProfileImageUrl()).into(imgProfile);
            }
        }

        // --- LOGIC SEARCH BAR (TOGGLE) ---
        // Penambahan pengecekan null, meskipun setelah perbaikan ID seharusnya tidak lagi null
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> openSearchBar()); // Line 103 (Fixed)
        }

        if (btnCloseSearch != null) {
            btnCloseSearch.setOnClickListener(v -> closeSearchBar());
        }

        etSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBySearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // --- FITUR LIHAT SEMUA ---
        tvSeeAll.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                BottomNavigationView nav = getActivity().findViewById(R.id.nav_view);
                nav.setSelectedItemId(R.id.nav_explore);
            }
        });

        // 3. SETUP SLIDER
        promoAdapter = new PromoAdapter(listPromo);
        viewPagerSlider.setAdapter(promoAdapter);

        // 4. SETUP CATEGORIES
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new CategoryAdapter();
        rvCategories.setAdapter(categoryAdapter);

        // 5. SETUP BOOKS
        rvBooks.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        bukuAdapter = new UserBukuAdapter(getContext(), listBukuDisplay);
        rvBooks.setAdapter(bukuAdapter);

        // 6. LOAD DATA
        loadData(ref);

        return view;
    }

    // --- FUNGSI BUKA TUTUP SEARCH BAR ---
    private void openSearchBar() {
        layoutHeaderNormal.setVisibility(View.GONE);
        layoutHeaderSearch.setVisibility(View.VISIBLE);
        etSearchField.requestFocus();

        // Tampilkan Keyboard
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etSearchField, InputMethodManager.SHOW_IMPLICIT);
    }

    private void closeSearchBar() {
        layoutHeaderSearch.setVisibility(View.GONE);
        layoutHeaderNormal.setVisibility(View.VISIBLE);
        etSearchField.setText(""); // Bersihkan text

        // Sembunyikan Keyboard
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearchField.getWindowToken(), 0);

        // Reset Filter
        filterBySearch("");
    }

    private void filterBySearch(String query) {
        listBukuDisplay.clear();
        String q = query.toLowerCase();
        if (q.isEmpty()) {
            listBukuDisplay.addAll(listBukuMaster);
        } else {
            for (Buku b : listBukuMaster) {
                if (b.getTitle().toLowerCase().contains(q) ||
                        b.getAuthor().toLowerCase().contains(q)) {
                    listBukuDisplay.add(b);
                }
            }
            // Reset kategori jadi 'Semua' visualnya
            selectedCategoryIndex = 0;
            if (categoryAdapter != null) categoryAdapter.notifyDataSetChanged();
        }
        bukuAdapter.notifyDataSetChanged();
    }

    private void filterByCategory(String category) {
        listBukuDisplay.clear();
        if (category.equals("Semua")) {
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

    private void loadData(DatabaseReference ref) {
        ref.child("promotions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                listPromo.clear();
                for (DataSnapshot d : s.getChildren()) {
                    Promotion p = d.getValue(Promotion.class);
                    if (p != null) listPromo.add(p);
                }
                promoAdapter.notifyDataSetChanged();
                setupSliderIndicators(listPromo.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        });

        ref.child("books").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                listBukuMaster.clear();
                listBukuDisplay.clear();
                for (DataSnapshot d : s.getChildren()) {
                    Buku b = d.getValue(Buku.class);
                    if (b != null) {
                        listBukuMaster.add(b);
                        listBukuDisplay.add(b);
                    }
                }
                bukuAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        });
    }

    // ... (Sisa kode setupSliderIndicators, PromoAdapter, CategoryAdapter SAMA PERSIS dengan sebelumnya)
    private void setupSliderIndicators(int count) {
        if (getContext() == null) return;
        layoutSliderIndicators.removeAllViews();
        ImageView[] indicators = new ImageView[count];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(8, 0, 8, 0);

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(getContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.indicator_inactive));
            indicators[i].setLayoutParams(layoutParams);
            layoutSliderIndicators.addView(indicators[i]);
        }
        if (indicators.length > 0)
            indicators[0].setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.indicator_active));

        viewPagerSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                for (int i = 0; i < count; i++) {
                    indicators[i].setImageDrawable(ContextCompat.getDrawable(getContext(),
                            (i == position) ? R.drawable.indicator_active : R.drawable.indicator_inactive));
                }
            }
        });
    }

    class PromoAdapter extends RecyclerView.Adapter<PromoAdapter.Holder> {
        List<Promotion> list;

        PromoAdapter(List<Promotion> l) {
            list = l;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new Holder(LayoutInflater.from(getContext()).inflate(R.layout.item_promotion_banner, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int pos) {
            Promotion p = list.get(pos);
            h.title.setText(p.getTitle());
            h.sub.setText(p.getSubtitle());
            Glide.with(getContext()).load(p.getImageUrl()).into(h.bg);
            h.btn.setOnClickListener(v -> {
                String dbUrl = "https://astrantia-books-28ad6-default-rtdb.asia-southeast1.firebasedatabase.app/";
                FirebaseDatabase.getInstance(dbUrl).getReference("books").child(p.getTargetBookId()).get().addOnSuccessListener(d -> {
                    Buku b = d.getValue(Buku.class);
                    if (b != null) {
                        Intent i = new Intent(getContext(), DetailBukuActivity.class);
                        i.putExtra("extra_buku", b);
                        startActivity(i);
                    }
                });
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView title, sub;
            ImageView bg;
            MaterialButton btn;

            Holder(View v) {
                super(v);
                title = v.findViewById(R.id.tvPromoTitle);
                sub = v.findViewById(R.id.tvPromoSubtitle);
                bg = v.findViewById(R.id.imgPromoBg);
                btn = v.findViewById(R.id.btnReadPromo);
            }
        }
    }

    class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(getContext());
            tv.setPadding(40, 20, 40, 20);
            tv.setTextSize(14f);

            // --- PERBAIKAN JARAK (MARGIN) ---
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);

            // Memberi jarak kanan 8dp antar item
            int marginEnd = (int) (8 * getResources().getDisplayMetrics().density);
            lp.setMargins(0, 0, marginEnd, 0);

            tv.setLayoutParams(lp);

            return new Holder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            String cat = CATEGORIES[position];
            holder.tv.setText(cat);

            if (selectedCategoryIndex == position) {
                // Style Aktif (Kuning)
                holder.tv.setBackgroundResource(R.drawable.category_active);
                holder.tv.setTextColor(Color.WHITE);
            } else {
                // Style Tidak Aktif (Outline/Abu)
                holder.tv.setBackgroundResource(R.drawable.category_inactive);
                holder.tv.setTextColor(Color.BLACK);
            }

            holder.itemView.setOnClickListener(v -> {
                int oldIndex = selectedCategoryIndex;
                selectedCategoryIndex = position;
                notifyItemChanged(oldIndex);
                notifyItemChanged(selectedCategoryIndex);

                // Panggil fungsi filter
                filterByCategory(cat);
            });
        }

        @Override
        public int getItemCount() {
            return CATEGORIES.length;
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView tv;

            Holder(View v) {
                super(v);
                tv = (TextView) v;
            }
        }
    }
}