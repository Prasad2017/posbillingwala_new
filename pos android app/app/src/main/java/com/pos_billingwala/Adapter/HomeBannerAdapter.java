package com.pos_billingwala.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Model.HomeBannerResponse;
import com.pos_billingwala.databinding.ItemHomeBannerBinding;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class HomeBannerAdapter extends RecyclerView.Adapter<HomeBannerAdapter.Holder> {

    private final List<HomeBannerResponse> banners = new ArrayList<>();

    public void submit(List<HomeBannerResponse> items) {
        banners.clear();
        if (items != null) {
            banners.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemHomeBannerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        HomeBannerResponse banner = banners.get(position);
        String url = banner != null ? banner.normalizedImageUrl() : null;
        if (url == null) {
            holder.binding.bannerImage.setImageDrawable(null);
            return;
        }
        Picasso.get()
                .load(url)
                .into(holder.binding.bannerImage);
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemHomeBannerBinding binding;

        Holder(ItemHomeBannerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
