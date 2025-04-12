package com.h2play.canvas_magic.features.detail.widget;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.data.model.response.Statistic;

public class StatisticView extends RelativeLayout {

    TextView nameText;
    ProgressBar statProgress;

    public StatisticView(Context context) {
        super(context);
        init();
    }

    public StatisticView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StatisticView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public StatisticView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.view_statistic, this);
        nameText = view.findViewById(R.id.text_name);
        statProgress = view.findViewById(R.id.progress_stat);
    }

    @SuppressLint("SetTextI18n")
    public void setStat(Statistic statistic) {
        nameText.setText(
                statistic.stat.name.substring(0, 1).toUpperCase()
                        + statistic.stat.name.substring(1));
        statProgress.setProgress(statistic.baseStat);
    }
}
