package com.h2play.canvas_magic.features.list;

import java.util.List;

import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.features.base.MvpView;

public interface ShapeListMvpView extends MvpView {


    void showShape(String name, String fileName, int count);

    void showShapeList(List<ShapeInfo> shapeInfos);

    void setShapeList(List<ShapeInfo> shapeInfos);

    void showProgress(boolean show);

    void showError(Throwable error);

    void showImpossibleDelete();
}
