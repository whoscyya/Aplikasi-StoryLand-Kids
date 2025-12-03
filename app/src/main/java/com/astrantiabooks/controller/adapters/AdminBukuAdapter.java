package com.astrantiabooks.controller.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.astrantiabooks.R;
// import com.astrantiabooks.controller.activity.AddBookActivity; // Hapus import yang tidak digunakan
import com.astrantiabooks.model.Buku;
import com.astrantiabooks.view.AdminAddFragment; // BARU: Import AdminAddFragment
import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class AdminBukuAdapter extends RecyclerView.Adapter<AdminBukuAdapter.ViewHolder> {
    private Context context;
    private List<Buku> listBuku;
    private AdminAddFragment fragment; // FIELD BARU: Menyimpan referensi Fragment

    // KONSTRUKTOR DIPERBARUI: Menerima 3 argumen
    public AdminBukuAdapter(Context context, List<Buku> listBuku, AdminAddFragment fragment) {
        this.context = context;
        this.listBuku = listBuku;
        this.fragment = fragment; // INISIALISASI
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_buku_admin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Buku buku = listBuku.get(position);

        // Update set text sesuai layout baru
        holder.tvTitle.setText(buku.getTitle());
        holder.tvAuthor.setText(buku.getAuthor());
        holder.tvGenre.setText(buku.getCategory()); // Tambahan Genre

        if (buku.getCoverUrl() != null && !buku.getCoverUrl().isEmpty()) {
            Glide.with(context).load(buku.getCoverUrl()).into(holder.imgBuku);
        } else {
            holder.imgBuku.setImageResource(R.drawable.ic_launcher_background);
        }

        // PERBAIKAN: Tombol Edit sekarang memanggil dialog di fragment
        holder.btnEdit.setOnClickListener(v -> {
            if (fragment != null) {
                // Panggil fungsi dialog edit di dalam AdminAddFragment
                fragment.showEditBookDialog(buku);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context).setTitle("Hapus Buku").setMessage("Hapus " + buku.getTitle() + "?")
                    .setPositiveButton("Ya", (d, w) -> {
                        String dbUrl = "https://astrantia-books-28ad6-default-rtdb.asia-southeast1.firebasedatabase.app/";
                        FirebaseDatabase.getInstance(dbUrl).getReference("books").child(buku.getId()).removeValue();
                        Toast.makeText(context, "Terhapus", Toast.LENGTH_SHORT).show();
                    }).setNegativeButton("Batal", null).show();
        });
    }

    @Override public int getItemCount() { return listBuku.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAuthor, tvGenre;
        ImageView imgBuku;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // UPDATE ID SESUAI LAYOUT XML BARU
            tvTitle = itemView.findViewById(R.id.tv_book_title);
            tvAuthor = itemView.findViewById(R.id.tv_book_author);
            tvGenre = itemView.findViewById(R.id.tv_book_genre);
            imgBuku = itemView.findViewById(R.id.img_book_cover);

            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}