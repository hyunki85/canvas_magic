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

import butterknife.BindView;
import butterknife.ButterKnife;
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
    private Subject<ShapeOnline> subject = PublishSubject.create();
    private int lastSize;

    @Inject
    public ShapeAdapter() {
        shapeOnlines = Collections.emptyList();
    }

    public void setShapeOnlines(List<ShapeOnline> shapeOnlines) {
        this.shapeOnlines = shapeOnlines;
    }

    public void setClickListener(ClickListener clickListener) {
        mClickListener = clickListener;
    }

    public Subject<ShapeOnline> getNextPage() {
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
        holder.nameText.setText(String.format("%s", shapeOnline.name));
        DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
        holder.dateText.setText(format.format(shapeOnline.date));
        holder.starText.setText(String.valueOf(shapeOnline.star));

        holder.starImageView.setSelected(shapeOnline.alreadyStar);
        holder.fabricView.setBackgroundMode(BACKGROUND_STYLE_GRAPH_PAPER);
        holder.fabricView.cleanPage();
        holder.fabricView.post(new Runnable() {
            @Override
            public void run() {
                drawShape(holder.fabricView, shapeOnline.json,0);
            }
        });

        if((position == shapeOnlines.size() - 1) && lastSize != shapeOnlines.size()) {
            lastSize =  shapeOnlines.size();
            subject.onNext(shapeOnlines.get(position));
        }

    }

    public void drawShape(FabricView fabricView, String jsonText, int shapeIndex) {

        fabricView.cleanPage();
        fabricView.setColor(Color.BLACK);

        JsonObject assetJsonObject = new Gson().fromJson(jsonText, JsonObject.class);
        JsonArray shapes = assetJsonObject.get("shapes").getAsJsonArray();
        JsonArray actions = shapes.get(shapeIndex).getAsJsonArray();

        List<JsonObject> jsonObjects = new ArrayList<>();
        for (int i = 0; i < actions.size(); ++i ) {
            jsonObjects.add(actions.get(i).getAsJsonObject());
        }
        ;
        for (JsonObject jsonObject :  jsonObjects) {
            switch (jsonObject.get("action").getAsString()) {
                case "down": {
                    fabricView.actionDown(jsonObject.get("x").getAsFloat()*fabricView.getWidth()
                            ,jsonObject.get("y").getAsFloat()*fabricView.getHeight());
                    break;
                }

                case "up": {
                    fabricView.actionUp(jsonObject.get("x").getAsFloat()*fabricView.getWidth()
                            ,jsonObject.get("y").getAsFloat()*fabricView.getHeight());
                    break;
                }

                case "move": {
                    fabricView.actionMove(jsonObject.get("x1").getAsFloat()*fabricView.getWidth()
                            ,jsonObject.get("y1").getAsFloat()*fabricView.getHeight(),
                            jsonObject.get("x2").getAsFloat()*fabricView.getWidth(),
                            jsonObject.get("y2").getAsFloat()*fabricView.getHeight());
                    break;
                }
            }
        }
        fabricView.setInteractionMode(LOCKED_MODE);
    }

    @Override
    public int getItemCount() {
        return shapeOnlines.size();
    }

    public void updateItem(ShapeOnline shapeOnline) {
        int index = shapeOnlines.indexOf(shapeOnline);
        shapeOnlines.set(index,shapeOnline);
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

        @BindView(R.id.fabricView1)
        FabricView fabricView;



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