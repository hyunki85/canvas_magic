package com.h2play.canvas_magic.features.menu;

import javax.inject.Inject;

import com.h2play.canvas_magic.data.DataManager;
import com.h2play.canvas_magic.features.base.BasePresenter;
import com.h2play.canvas_magic.injection.ConfigPersistent;
import com.h2play.canvas_magic.util.rx.scheduler.SchedulerUtils;

@ConfigPersistent
public class MenuPresenter extends BasePresenter<MenuMvpView> {

    private final DataManager dataManager;

    @Inject
    public MenuPresenter(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void attachView(MenuMvpView mvpView) {
        super.attachView(mvpView);
    }


    public void getShapeList() {
        if(dataManager.needGuide()) {
            getView().startTutorial();
        } else {
            getView().showShapeList(dataManager.getShapeList());
        }
    }

    public void checkNeedGuidE() {
        if(dataManager.needGuide()) {
            getView().showTutorial();
        }

    }
}
