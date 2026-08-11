package com.h2play.canvas_magic.features.menu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.widget.ImageViewCompat;
import androidx.cardview.widget.CardView;
import android.content.res.ColorStateList;

import com.h2play.canvas_magic.R;
import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.h2play.canvas_magic.features.menu.config.MenuItemConfig;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import androidx.core.graphics.ColorUtils;

/**
 * JSON 기반 동적 메뉴 렌더링 어댑터 (그리드)
 */
public class DynamicMenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnItemClickListener { void onItemClick(MenuItemConfig item); }

    private final LayoutInflater inflater;
    private final OnItemClickListener listener;
    private final List<MenuItemConfig> items = new ArrayList<>();

    private static final int TYPE_DEFAULT = 0;
    private static final int TYPE_PROMO = 1;
    private static final int TYPE_SMALL = 2;

    public DynamicMenuAdapter(Context ctx, OnItemClickListener listener) {
        this.inflater = LayoutInflater.from(ctx);
        this.listener = listener;
    }

    public void submit(List<MenuItemConfig> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @Override @NonNull public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_PROMO) {
            View v = inflater.inflate(R.layout.item_dynamic_menu_promo, parent, false);
            return new PromoVH(v);
        }
        if (viewType == TYPE_SMALL) {
            View v = inflater.inflate(R.layout.item_dynamic_menu_small, parent, false);
            return new VH(v);
        }
        View v = inflater.inflate(R.layout.item_dynamic_menu, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MenuItemConfig item = items.get(position);
        if (holder instanceof PromoVH) {
            bindPromo((PromoVH) holder, item, position);
        } else if (holder instanceof VH) {
            bindDefault((VH) holder, item, position);
        }
    }

    private void bindDefault(@NonNull VH h, MenuItemConfig item, int position) {
        h.title.setText(resolveText(h.itemView.getContext(), item.title));
        // Background and content colors
        int content = Color.WHITE;
        if (h.card != null) {
            int bg = resolveBackgroundColor(item, position);
            h.card.setCardBackgroundColor(bg);
            content = ColorUtils.calculateLuminance(bg) < 0.5 ? Color.WHITE : Color.parseColor("#111111");
            h.title.setTextColor(content);
        }
        // Description
        if (item.description != null && !item.description.isEmpty()) {
            h.description.setText(resolveText(h.itemView.getContext(), item.description));
            h.description.setTextColor(content);
            h.description.setVisibility(View.VISIBLE);
        } else {
            h.description.setVisibility(View.GONE);
        }
        boolean shown = false;
        final int iconTint = content;
        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            String url = item.imageUrl;
            try {
                if (url.startsWith("gs://")) {
                    StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(url);
                    ref.getDownloadUrl().addOnSuccessListener(u -> {
                        h.icon.setVisibility(View.VISIBLE);
                        ImageViewCompat.setImageTintList(h.icon, null);
                        Glide.with(h.itemView).load(u).into(h.icon);
                    }).addOnFailureListener(e -> h.icon.setVisibility(View.GONE));
                    shown = true;
                } else if (url.startsWith("http")) {
                    h.icon.setVisibility(View.VISIBLE);
                    ImageViewCompat.setImageTintList(h.icon, null);
                    Glide.with(h.itemView).load(url).into(h.icon);
                    shown = true;
                }
            } catch (Exception ignored) { }
        }
        if (!shown) {
            if (item.icon != null) {
                int resId = h.itemView.getContext().getResources().getIdentifier(item.icon, "drawable", h.itemView.getContext().getPackageName());
                if (resId != 0) {
                    h.icon.setImageResource(resId);
                    ColorStateList tint = ColorStateList.valueOf(iconTint);
                    ImageViewCompat.setImageTintList(h.icon, tint);
                    h.icon.setVisibility(View.VISIBLE);
                    shown = true;
                }
            }
        }
        if (!shown) {
            h.icon.setVisibility(View.GONE);
        }
        h.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    private void bindPromo(@NonNull PromoVH h, MenuItemConfig item, int position) {
        h.title.setText(resolveText(h.itemView.getContext(), item.title));
        int bg = resolveBackgroundColor(item, position);
        h.card.setCardBackgroundColor(bg);
        int content = ColorUtils.calculateLuminance(bg) < 0.5 ? Color.WHITE : Color.parseColor("#111111");
        h.title.setTextColor(content);
        // Description
        if (item.description != null && !item.description.isEmpty()) {
            h.description.setText(resolveText(h.itemView.getContext(), item.description));
            h.description.setVisibility(View.VISIBLE);
        } else {
            h.description.setVisibility(View.GONE);
        }

        boolean shown = false;
        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            String url = item.imageUrl;
            try {
                if (url.startsWith("gs://")) {
                    StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(url);
                    ref.getDownloadUrl().addOnSuccessListener(u -> {
                        h.image.setVisibility(View.VISIBLE);
                        Glide.with(h.itemView).load(u).centerCrop().into(h.image);
                    }).addOnFailureListener(e -> h.image.setVisibility(View.GONE));
                    shown = true;
                } else if (url.startsWith("http")) {
                    h.image.setVisibility(View.VISIBLE);
                    Glide.with(h.itemView).load(url).centerCrop().into(h.image);
                    shown = true;
                }
            } catch (Exception ignored) { }
        }
        if (!shown) {
            if (item.icon != null) {
                int resId = h.itemView.getContext().getResources().getIdentifier(item.icon, "drawable", h.itemView.getContext().getPackageName());
                if (resId != 0) {
                    h.image.setImageResource(resId);
                    h.image.setVisibility(View.VISIBLE);
                    shown = true;
                }
            }
        }
        if (!shown) {
            h.image.setVisibility(View.GONE);
        }
        h.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    private int resolveBackgroundColor(MenuItemConfig item, int position) {
        if (item.bgColor != null && !item.bgColor.isEmpty()) {
            try { return Color.parseColor(item.bgColor); } catch (Exception ignored) {}
        }
        // palette fallback (app colors)
        int[] palette = new int[] {
                Color.parseColor("#3F51B5"), // primary
                Color.parseColor("#009688"),
                Color.parseColor("#FF5722"),
                Color.parseColor("#795548"),
                Color.parseColor("#607D8B"),
                Color.parseColor("#9C27B0")
        };
        return palette[position % palette.length];
    }

    private CharSequence resolveText(Context context, String value) {
        if (value == null) {
            return "";
        }
        if (!value.startsWith("@string/")) {
            return value;
        }
        String resourceName = value.substring("@string/".length());
        int resId = context.getResources().getIdentifier(resourceName, "string", context.getPackageName());
        if (resId == 0) {
            return value;
        }
        return context.getString(resId);
    }

    public MenuItemConfig getItem(int position) {
        if (position < 0 || position >= items.size()) {
            return null;
        }
        return items.get(position);
    }

    @Override public int getItemCount() { return items.size(); }

    @Override public int getItemViewType(int position) {
        MenuItemConfig item = items.get(position);
        int span = item.span <= 0 ? 1 : item.span;
        if (item.promo || span >= 2) return TYPE_PROMO;
        if (item.small) return TYPE_SMALL;
        return TYPE_DEFAULT;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title; TextView description; ImageView icon; CardView card;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            description = itemView.findViewById(R.id.tv_description);
            icon = itemView.findViewById(R.id.iv_icon);
            card = itemView instanceof CardView ? (CardView) itemView : null;
        }
    }

    static class PromoVH extends RecyclerView.ViewHolder {
        TextView title; TextView description; ImageView image; CardView card;
        PromoVH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            description = itemView.findViewById(R.id.tv_description);
            image = itemView.findViewById(R.id.iv_image);
            card = (CardView) itemView;
        }
    }
}
