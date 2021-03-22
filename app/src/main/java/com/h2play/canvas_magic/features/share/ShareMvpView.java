package com.h2play.canvas_magic.features.share;

import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.data.model.response.ShapeOnline;
import com.h2play.canvas_magic.features.base.MvpView;

import java.util.List;

public interface ShareMvpView extends MvpView {

    void showShapes(List<ShapeOnline> pokemon);

    void showShapeList(List<ShapeInfo> shapeInfos);

    void onShareComplete();

    void showProgress(boolean show);

    void showError(Throwable error);

    void updateShape(ShapeOnline shapeOnline);
}
