package com.instituto.bancodealimentos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

/**
 * ImageAdapter com loop "infinito" para ViewPager2.
 * Usa item_image_carousel.xml e uma lista de drawables (List<Integer>).
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.VH> {

    private final List<Integer> images;

    public ImageAdapter(List<Integer> images) {
        this.images = (images != null) ? images : Collections.emptyList();
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_carousel, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        // Evita crash se a lista estiver vazia
        if (images.isEmpty()) return;
        int realPos = position % images.size();
        holder.imageView.setImageResource(images.get(realPos));
    }

    @Override
    public int getItemCount() {
        return images.isEmpty() ? 0 : Integer.MAX_VALUE;
    }

    @Override
    public long getItemId(int position) {
        // Estável o suficiente para o ViewPager2 com looping
        return position;
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView imageView;
        VH(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.img_carousel);
        }
    }
}
