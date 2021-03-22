package com.h2play.canvas_magic.features.main;

import java.util.List;

import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.features.base.MvpView;

public interface MainMvpView extends MvpView {

    void showLongPressGuide();

    void showPokemon(List<String> pokemon);

    void showProgress(boolean show);

    void showError(Throwable error);

    void setShapeFileName(ShapeInfo shapeInfo);
}
