package com.astrantiabooks.adapters;

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
import com.astrantiabooks.activities.DetailBukuActivity;
import com.astrantiabooks.models.Buku;
import com.bumptech.glide.Glide;
import java.util.List;

public class UserBukuAdapter extends RecyclerView.Adapter<UserBukuAdapter.ViewHolder> {
    private Context context;
    private List<Buku> listBuku;

    public UserBukuAdapter(Context context, List<Buku> listBuku) {
        this.context = context;
        this.listBuku = listBuku;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_buku_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Buku buku = listBuku.get(position);
        holder.tvTitle.setText(buku.getTitle());
        holder.tvAuthor.setText(buku.getAuthor());
        holder.tvGenre.setText(buku.getCategory());

        if (buku.getCoverUrl() != null && !buku.getCoverUrl().isEmpty()) {
            Glide.with(context).load(buku.getCoverUrl()).placeholder(R.drawable.ic_launcher_background).into(holder.imgBuku);
        } else {
            holder.imgBuku.setImageResource(R.drawable.ic_launcher_background);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailBukuActivity.class);
            intent.putExtra("extra_buku", buku);
            context.startActivity(intent);
        });
    }

    @Override public int getItemCount() { return listBuku.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAuthor, tvGenre;
        ImageView imgBuku;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Sesuai dengan ID di item_buku_user.xml yang baru
            tvTitle = itemView.findViewById(R.id.tv_book_title);
            tvAuthor = itemView.findViewById(R.id.tv_book_author);
            tvGenre = itemView.findViewById(R.id.tv_book_genre);
            imgBuku = itemView.findViewById(R.id.img_book_cover);
        }
    }
}