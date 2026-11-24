package com.h2play.canvas_magic.features.share;


import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.data.model.response.ShapeOnline;
import com.h2play.canvas_magic.util.FabricView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.h2play.canvas_magic.util.FabricView.BACKGROUND_STYLE_GRAPH_PAPER;
import static com.h2play.canvas_magic.util.FabricView.BACKGROUND_STYLE_NOTEBOOK_PAPER;
import static com.h2play.canvas_magic.util.FabricView.DRAW_MODE;
import static com.h2play.canvas_magic.util.FabricView.LOCKED_MODE;


public class ShapeAdapter extends RecyclerView.Adapter<ShapeAdapter.ShapeViewHolder> {

    private List<ShapeOnline> shapeOnlines;
    private ClickListener mClickListener;
    private Subject<Object> subject = PublishSubject.create();
    private int lastSize;

    @Inject
    public ShapeAdapter() {
        shapeOnlines = Collections.emptyList();
    }

    public void setShapeOnlines(List<ShapeOnline> shapeOnlines) {
        if (shapeOnlines != null && shapeOnlines.size() <= lastSize) {
            lastSize = 0;
        }
        this.shapeOnlines = shapeOnlines == null ? Collections.emptyList() : shapeOnlines;
    }

    public void setClickListener(ClickListener clickListener) {
        mClickListener = clickListener;
    }

    public Subject<Object> getNextPage() {
        return subject;
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
        ShapeOnline shapeOnline = shapeOnlines.get(position);
        holder.shapeOnline = shapeOnline;
        holder.nameText.setText(shapeOnline.name == null ? "" : shapeOnline.name);
        DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
        holder.dateText.setText(shapeOnline.date == null ? "" : format.format(shapeOnline.date));
        holder.starText.setText(String.valueOf(shapeOnline.star));

        holder.starImageView.setSelected(shapeOnline.alreadyStar);
        holder.fabricView.setBackgroundMode(BACKGROUND_STYLE_GRAPH_PAPER);
        holder.fabricView.cleanPage();
        holder.fabricView.post(new Runnable() {
            @Override
            public void run() {
                if (holder.shapeOnline != shapeOnline) return;
                int page = com.h2play.canvas_magic.util.DrawingPreviewData.firstDrawable(
                        shapeOnline.json, shapeOnline.count);
                holder.itemView.findViewById(R.id.preview_empty).setVisibility(
                        page < 0 ? View.VISIBLE : View.GONE);
                drawShape(holder.fabricView, shapeOnline.json, page);
            }
        });

        holder.itemView.post(() -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION
                    && currentPosition == shapeOnlines.size() - 1
                    && lastSize != shapeOnlines.size()) {
                lastSize = shapeOnlines.size();
                subject.onNext(new Object());
            }
        });

    }

    public void drawShape(FabricView view, String json, int index) {
        com.h2play.canvas_magic.util.DrawingPreviewRenderer.draw(view, json, index);
    }

    @Override
    public int getItemCount() {
        return shapeOnlines.size();
    }

    public void updateItem(ShapeOnline shapeOnline) {
        int index = shapeOnlines.indexOf(shapeOnline);
        if (index < 0) return;
        shapeOnlines.set(index,shapeOnline);
        notifyItemChanged(index);
    }

    public interface ClickListener {
        void onShapeClick(ShapeOnline shapeOnline);
        void onLikeClick(ShapeOnline shapeOnline);
    }

    class ShapeViewHolder extends RecyclerView.ViewHolder {

        ShapeOnline shapeOnline;
        TextView nameText;
        TextView dateText;
        TextView starText;
        ImageView starImageView;
        FabricView fabricView;

        ShapeViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.txt_name);
            dateText = itemView.findViewById(R.id.txt_date);
            starText = itemView.findViewById(R.id.txt_star);
            starImageView = itemView.findViewById(R.id.img_like);
            fabricView = itemView.findViewById(R.id.fabricView1);
            
            itemView.setOnClickListener(v -> {
                if (mClickListener != null) mClickListener.onShapeClick(shapeOnline);
            });

            starImageView.setOnClickListener(v -> {
                if (mClickListener != null) mClickListener.onLikeClick(shapeOnline);
            });
        }
    }

}
