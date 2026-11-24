package com.h2play.canvas_magic.features.menu;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.data.model.PackRef;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.util.DrawingArtworkView;
import com.h2play.canvas_magic.util.DrawingCatalog;
import com.h2play.canvas_magic.util.DrawingPreviewRenderer;
import com.h2play.canvas_magic.util.FabricView;
import com.h2play.canvas_magic.util.FileUtil;

import java.util.ArrayList;
import java.util.List;

final class PackPickerBottomSheet {
    interface Listener { void onPackChosen(PackRef pack); }

    private PackPickerBottomSheet() { }

    static void show(Activity activity, List<ShapeInfo> customShapes, PackRef current,
                     Listener listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        FrameLayout inflationRoot = new FrameLayout(activity);
        View content = activity.getLayoutInflater().inflate(
                R.layout.bottom_sheet_pack_picker, inflationRoot, false);
        dialog.setContentView(content);

        List<Option> options = new ArrayList<>();
        if (customShapes != null) {
            for (ShapeInfo shape : customShapes) {
                if (shape != null && shape.fileName != null) options.add(Option.custom(shape));
            }
        }
        String[] titles = activity.getResources().getStringArray(R.array.drawing_pack_titles);
        for (int index = 0; index < DrawingCatalog.size(); index++) {
            options.add(Option.builtIn(DrawingCatalog.id(index), titles[index], index));
        }

        PackRef initial = contains(options, current) ? current
                : options.isEmpty() ? null : options.get(0).ref;
        Adapter adapter = new Adapter(activity, options, initial);
        RecyclerView list = content.findViewById(R.id.pack_picker_list);
        list.setLayoutManager(new LinearLayoutManager(activity));
        list.setAdapter(adapter);
        content.findViewById(R.id.pack_picker_start).setOnClickListener(v -> {
            PackRef selected = adapter.getSelected();
            if (selected != null) {
                listener.onPackChosen(selected);
                dialog.dismiss();
            }
        });

        dialog.setOnShowListener(ignored -> {
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                ViewGroup.LayoutParams params = sheet.getLayoutParams();
                params.height = Math.round(activity.getResources().getDisplayMetrics().heightPixels * .88f);
                sheet.setLayoutParams(params);
                BottomSheetBehavior.from(sheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        dialog.show();
    }

    private static boolean contains(List<Option> options, PackRef ref) {
        if (ref == null) return false;
        for (Option option : options) if (option.ref.equals(ref)) return true;
        return false;
    }

    private static final class Option {
        final PackRef ref;
        final String title;
        final int count;
        final int catalogIndex;
        final String fileName;

        private Option(PackRef ref, String title, int count, int catalogIndex, String fileName) {
            this.ref = ref;
            this.title = title;
            this.count = count;
            this.catalogIndex = catalogIndex;
            this.fileName = fileName;
        }

        static Option custom(ShapeInfo shape) {
            return new Option(PackRef.custom(shape.fileName), shape.name, shape.count, -1, shape.fileName);
        }

        static Option builtIn(String id, String title, int catalogIndex) {
            return new Option(PackRef.builtIn(id), title, 3, catalogIndex, null);
        }
    }

    private static final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        private final Context context;
        private final List<Option> options;
        private PackRef selected;

        Adapter(Context context, List<Option> options, PackRef selected) {
            this.context = context;
            this.options = options;
            this.selected = selected;
        }

        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pack_option, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            Option option = options.get(position);
            boolean checked = option.ref.equals(selected);
            holder.title.setText(option.title);
            holder.subtitle.setText(context.getString(option.catalogIndex >= 0
                    ? R.string.pack_picker_included_count : R.string.pack_picker_mine_count, option.count));
            holder.check.setVisibility(checked ? View.VISIBLE : View.GONE);
            holder.card.setStrokeColor(context.getColor(checked
                    ? R.color.primary_color : R.color.canvas_toolbar_border));
            holder.card.setStrokeWidth(dp(checked ? 2 : 1));
            holder.itemView.setContentDescription(context.getString(
                    checked ? R.string.pack_picker_selected_accessibility
                            : R.string.pack_picker_option_accessibility, option.title));
            holder.itemView.setOnClickListener(v -> {
                int newPosition = holder.getBindingAdapterPosition();
                if (newPosition == RecyclerView.NO_POSITION) return;
                int old = findSelected();
                selected = option.ref;
                if (old >= 0) notifyItemChanged(old);
                notifyItemChanged(newPosition);
            });
            bindPreviews(holder, option);
        }

        private void bindPreviews(Holder holder, Option option) {
            for (FrameLayout frame : holder.previews) frame.removeAllViews();
            if (option.catalogIndex >= 0) {
                for (int index = 0; index < holder.previews.length; index++) {
                    DrawingArtworkView preview = new DrawingArtworkView(context);
                    holder.previews[index].addView(preview, match());
                    preview.show(option.catalogIndex, index);
                }
                return;
            }
            String json = FileUtil.getJsonFromFile(context, option.fileName);
            for (int index = 0; index < holder.previews.length; index++) {
                FabricView preview = new FabricView(context, null);
                holder.previews[index].addView(preview, match());
                int drawingIndex = index;
                preview.post(() -> DrawingPreviewRenderer.draw(preview, json, drawingIndex));
            }
        }

        private int findSelected() {
            for (int index = 0; index < options.size(); index++) {
                if (options.get(index).ref.equals(selected)) return index;
            }
            return -1;
        }

        PackRef getSelected() {
            return selected;
        }

        private int dp(int value) {
            return Math.round(value * context.getResources().getDisplayMetrics().density);
        }

        private static FrameLayout.LayoutParams match() {
            return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }

        @Override public int getItemCount() { return options.size(); }

        static final class Holder extends RecyclerView.ViewHolder {
            final MaterialCardView card;
            final TextView title;
            final TextView subtitle;
            final TextView check;
            final FrameLayout[] previews;

            Holder(View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.pack_option_card);
                title = itemView.findViewById(R.id.pack_option_title);
                subtitle = itemView.findViewById(R.id.pack_option_subtitle);
                check = itemView.findViewById(R.id.pack_option_check);
                previews = new FrameLayout[] { itemView.findViewById(R.id.pack_preview_1),
                        itemView.findViewById(R.id.pack_preview_2), itemView.findViewById(R.id.pack_preview_3) };
            }
        }
    }
}
