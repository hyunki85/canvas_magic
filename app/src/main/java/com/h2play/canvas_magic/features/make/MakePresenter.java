package com.h2play.canvas_magic.features.make;

import javax.inject.Inject;

import com.h2play.canvas_magic.data.DataManager;
import com.h2play.canvas_magic.features.base.BasePresenter;
import com.h2play.canvas_magic.injection.ConfigPersistent;
import com.h2play.canvas_magic.util.rx.scheduler.SchedulerUtils;

@ConfigPersistent
public class MakePresenter extends BasePresenter<MakeMvpView> {

    private final DataManager dataManager;

    @Inject
    public MakePresenter(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void attachView(MakeMvpView mvpView) {
        super.attachView(mvpView);
    }

}
