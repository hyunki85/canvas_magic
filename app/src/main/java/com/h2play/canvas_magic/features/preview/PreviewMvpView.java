package com.h2play.canvas_magic.features.preview;

import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.features.base.MvpView;

import java.util.List;

public interface PreviewMvpView extends MvpView {


    void showProgress(boolean show);

    void showError(Throwable error);

    void onSaveComplete();
}
