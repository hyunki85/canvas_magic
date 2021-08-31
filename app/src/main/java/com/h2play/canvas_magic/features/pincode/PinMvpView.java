package com.h2play.canvas_magic.features.pincode;

import java.util.List;

import com.h2play.canvas_magic.features.base.MvpView;

public interface PinMvpView extends MvpView {

    void showProgress(boolean show);

    void showError(Throwable error);

    void showGuide();

}
