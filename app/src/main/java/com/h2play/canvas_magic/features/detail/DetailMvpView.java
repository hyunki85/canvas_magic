package com.h2play.canvas_magic.features.detail;

import com.h2play.canvas_magic.data.model.response.Statistic;
import com.h2play.canvas_magic.features.base.MvpView;

public interface DetailMvpView extends MvpView {

    void showStat(Statistic statistic);

    void showProgress(boolean show);

    void showError(Throwable error);
}
