package com.astrantiabooks.controller.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.astrantiabooks.R;
import com.astrantiabooks.controller.activity.DetailBukuActivity;
import com.astrantiabooks.model.Buku;
import com.bumptech.glide.Glide;

import java.util.List;

public class UserBukuAdapter extends RecyclerView.Adapter<UserBukuAdapter.Holder> {

    private Context context;
    private List<Buku> listBuku;

    public UserBukuAdapter(Context context, List<Buku> listBuku) {
        this.context = context;
        this.listBuku = listBuku;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Menggunakan layout item_book_card.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_buku_card, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Buku buku = listBuku.get(position);

        holder.tvTitle.setText(buku.getTitle());
        holder.tvAuthor.setText(buku.getAuthor());

        // Hapus: Tidak ada lagi penanganan rating

        // LOAD GAMBAR (Menggunakan getCoverUrl sesuai model Anda)
        if (buku.getCoverUrl() != null && !buku.getCoverUrl().isEmpty()) {
            Glide.with(context)
                    .load(buku.getCoverUrl())
                    .placeholder(R.drawable.img_cover_placeholder)
                    .into(holder.imgCover);
        } else {
            holder.imgCover.setImageResource(R.drawable.img_cover_placeholder);
        }

        // KLIK ITEM -> KE DETAIL
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailBukuActivity.class);
            intent.putExtra("extra_buku", buku);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return listBuku.size();
    }

    public static class Holder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle, tvAuthor;

        public Holder(@NonNull View itemView) {
            super(itemView);
            // PERBAIKAN: Menggunakan ID yang BENAR dari item_buku_card.xml
            imgCover = itemView.findViewById(R.id.img_book_cover);
            tvTitle = itemView.findViewById(R.id.tv_book_title);
            tvAuthor = itemView.findViewById(R.id.tv_book_author);
            // Catatan: tv_book_genre tidak dipetakan karena tidak ada variabelnya di Holder
        }
    }
}