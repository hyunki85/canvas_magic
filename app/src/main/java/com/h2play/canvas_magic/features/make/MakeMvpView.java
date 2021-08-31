package com.h2play.canvas_magic.features.make;

import java.util.List;

import com.h2play.canvas_magic.features.base.MvpView;

public interface MakeMvpView extends MvpView {


    void showProgress(boolean show);

    void showError(Throwable error);
}
