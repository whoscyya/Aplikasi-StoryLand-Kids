package com.astrantiabooks.view;

import android.content.Context;
import android.content.Intent;
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
import com.astrantiabooks.controller.adapters.UserBukuAdapter;
import com.astrantiabooks.model.Buku;
import com.astrantiabooks.model.LocalData;
import com.astrantiabooks.model.Promotion;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminHomeFragment extends Fragment {

    // Views
    private ViewPager2 vpAdminSlider;
    private RecyclerView rvAdminBooksPreview;
    private LinearLayout layoutAdminIndicators;
    private TextView tvAdminGreeting;
    private ImageView imgAdminProfile;
    private ImageButton btnAdminSearch;

    // Search Views Baru
    private LinearLayout layoutHeaderNormal, layoutHeaderSearch;
    private EditText etSearchField;
    private ImageButton btnCloseSearch;

    // Adapters & Data
    private UserBukuAdapter bukuAdapter;
    private PromoAdapter promoAdapter;

    private List<Buku> listBukuMaster = new ArrayList<>();
    private List<Buku> listBukuDisplay = new ArrayList<>();
    private List<Promotion> listPromo = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_home, container, false);

        String dbUrl = "https://astrantia-books-28ad6-default-rtdb.asia-southeast1.firebasedatabase.app/";
        DatabaseReference ref = FirebaseDatabase.getInstance(dbUrl).getReference();

        // 1. INIT VIEWS
        imgAdminProfile = view.findViewById(R.id.imgAdminProfile);
        tvAdminGreeting = view.findViewById(R.id.tvAdminGreeting);
        btnAdminSearch = view.findViewById(R.id.btn_admin_search); // PERBAIKAN ID
        vpAdminSlider = view.findViewById(R.id.vpAdminSlider);
        layoutAdminIndicators = view.findViewById(R.id.layoutAdminIndicators);
        rvAdminBooksPreview = view.findViewById(R.id.rvAdminBooksPreview);

        // Init Search Views
        layoutHeaderNormal = view.findViewById(R.id.layoutHeaderNormal);
        layoutHeaderSearch = view.findViewById(R.id.layoutHeaderSearch);
        etSearchField = view.findViewById(R.id.et_search_field); // PERBAIKAN ID
        btnCloseSearch = view.findViewById(R.id.btn_close_search); // PERBAIKAN ID

        // 2. SETUP ADMIN INFO
        if (LocalData.currentUser != null) {
            tvAdminGreeting.setText("Halo Admin, " + LocalData.currentUser.getUsername() + "!");
            if (LocalData.currentUser.getProfileImageUrl() != null && !LocalData.currentUser.getProfileImageUrl().isEmpty())
                Glide.with(this).load(LocalData.currentUser.getProfileImageUrl()).into(imgAdminProfile);
        }

        // --- FITUR SEARCH BAR ---
        // Tambahkan pengecekan null sebelum memanggil setOnClickListener
        if (btnAdminSearch != null) btnAdminSearch.setOnClickListener(v -> openSearchBar());
        if (btnCloseSearch != null) btnCloseSearch.setOnClickListener(v -> closeSearchBar());

        // Tambahkan pengecekan null sebelum memanggil addTextChangedListener
        if (etSearchField != null) { // PERBAIKAN NPE di sini
            etSearchField.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterBySearch(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }


        // 3. SETUP SLIDER
        promoAdapter = new PromoAdapter(listPromo);
        vpAdminSlider.setAdapter(promoAdapter);

        // 4. SETUP BOOKS PREVIEW
        rvAdminBooksPreview.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        bukuAdapter = new UserBukuAdapter(getContext(), listBukuDisplay);
        rvAdminBooksPreview.setAdapter(bukuAdapter);

        // 5. LOAD DATA
        loadData(ref);

        return view;
    }

    private void openSearchBar() {
        layoutHeaderNormal.setVisibility(View.GONE);
        layoutHeaderSearch.setVisibility(View.VISIBLE);
        etSearchField.requestFocus();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etSearchField, InputMethodManager.SHOW_IMPLICIT);
    }

    private void closeSearchBar() {
        layoutHeaderSearch.setVisibility(View.GONE);
        layoutHeaderNormal.setVisibility(View.VISIBLE);
        etSearchField.setText("");
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearchField.getWindowToken(), 0);
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
        }
        bukuAdapter.notifyDataSetChanged();
    }

    private void loadData(DatabaseReference ref) {
        ref.child("promotions").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                listPromo.clear();
                for (DataSnapshot d : s.getChildren()) {
                    Promotion p = d.getValue(Promotion.class);
                    if (p != null) listPromo.add(p);
                }
                promoAdapter.notifyDataSetChanged();
                setupAdminSliderIndicators(listPromo.size());
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

        ref.child("books").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
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
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    // ... (Sisa kode setupSliderIndicators, PromoAdapter sama seperti sebelumnya)
    private void setupAdminSliderIndicators(int count) {
        if (getContext() == null) return;
        layoutAdminIndicators.removeAllViews();
        ImageView[] indicators = new ImageView[count];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(8, 0, 8, 0);

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(getContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.indicator_inactive));
            indicators[i].setLayoutParams(layoutParams);
            layoutAdminIndicators.addView(indicators[i]);
        }
        if (indicators.length > 0) indicators[0].setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.indicator_active));

        vpAdminSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
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
        PromoAdapter(List<Promotion> l) { list = l; }
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new Holder(LayoutInflater.from(getContext()).inflate(R.layout.item_promotion_banner, p, false));
        }
        @Override public void onBindViewHolder(@NonNull Holder h, int pos) {
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
        @Override public int getItemCount() { return list.size(); }
        class Holder extends RecyclerView.ViewHolder { TextView title, sub; ImageView bg; MaterialButton btn; Holder(View v) { super(v); title=v.findViewById(R.id.tvPromoTitle); sub=v.findViewById(R.id.tvPromoSubtitle); bg=v.findViewById(R.id.imgPromoBg); btn=v.findViewById(R.id.btnReadPromo); } }
    }
}