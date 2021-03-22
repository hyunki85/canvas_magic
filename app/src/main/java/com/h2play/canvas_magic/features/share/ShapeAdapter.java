package com.h2play.canvas_magic.features.share;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.data.model.response.ShapeOnline;

import java.text.DateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;


public class ShapeAdapter extends RecyclerView.Adapter<ShapeAdapter.ShapeViewHolder> {

    private List<ShapeOnline> shapes;
    private ClickListener mClickListener;

    @Inject
    public ShapeAdapter() {
        shapes = Collections.emptyList();
    }

    public void setShapes(List<ShapeOnline> pokemon) {
        shapes = pokemon;
    }

    public void setClickListener(ClickListener clickListener) {
        mClickListener = clickListener;
    }

    @Override
    public ShapeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_shape_online, parent, false);
        return new ShapeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ShapeViewHolder holder, int position) {
        ShapeOnline shapeOnline = shapes.get(position);
        holder.shapeOnline = shapeOnline;
        holder.nameText.setText(String.format("%s", shapeOnline.name));
        DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
        holder.dateText.setText(format.format(shapeOnline.date));
        holder.starText.setText(String.valueOf(shapeOnline.star));

        holder.starImageView.setSelected(shapeOnline.alreadyStar);

    }

    @Override
    public int getItemCount() {
        return shapes.size();
    }

    public void updateItem(ShapeOnline shapeOnline) {
        int index = shapes.indexOf(shapeOnline);
        shapes.set(index,shapeOnline);
        notifyDataSetChanged();
    }

    public interface ClickListener {
        void onShapeClick(ShapeOnline shapeOnline);
        void onLikeClick(ShapeOnline shapeOnline);
    }

    class ShapeViewHolder extends RecyclerView.ViewHolder {

        ShapeOnline shapeOnline;
        @BindView(R.id.txt_name)
        TextView nameText;

        @BindView(R.id.txt_date)
        TextView dateText;

        @BindView(R.id.txt_star)
        TextView starText;

        @BindView(R.id.img_like)
        ImageView starImageView;

        ShapeViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(v -> {
                if (mClickListener != null) mClickListener.onShapeClick(shapeOnline);
            });

            starImageView.setOnClickListener(v -> {
                if (mClickListener != null) mClickListener.onLikeClick(shapeOnline);
            });
        }
    }

}