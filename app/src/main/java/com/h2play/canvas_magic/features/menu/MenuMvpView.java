package com.h2play.canvas_magic.features.menu;

import java.util.List;

import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.features.base.MvpView;

public interface MenuMvpView extends MvpView {


    void showShapeList(List<ShapeInfo> shapeInfos);

    void showProgress(boolean show);

    void showError(Throwable error);

    void showTutorial();
    void startTutorial();
}
