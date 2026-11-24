package com.h2play.canvas_magic.features.main;

import javax.inject.Inject;

import com.h2play.canvas_magic.data.DataManager;
import com.h2play.canvas_magic.data.model.PackRef;
import com.h2play.canvas_magic.data.model.TutorialProgress;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.features.base.BasePresenter;
import com.h2play.canvas_magic.injection.ConfigPersistent;

import java.util.List;


@ConfigPersistent
public class MainPresenter extends BasePresenter<MainMvpView> {

    private final DataManager dataManager;

    @Inject
    public MainPresenter(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void attachView(MainMvpView mvpView) {
        super.attachView(mvpView);
    }

    public void getShape(int shapeIndex) {
        checkViewAttached();
        getView().showProgress(true);
        List<ShapeInfo> shapeList = dataManager.getShapeList();
        if (shapeList.isEmpty()) {
            getView().showProgress(false);
            return;
        }

        int safeIndex = Math.max(0, Math.min(shapeIndex, shapeList.size() - 1));
        dataManager.setLastShapeIndex(safeIndex);
        getView().setShapeFileName(shapeList.get(safeIndex));
        getView().showProgress(false);
    }

    public void getShape(String fileName) {
        checkViewAttached();
        getView().showProgress(true);
        ShapeInfo shape = dataManager.getShapeByFileName(fileName);
        if (shape == null) shape = dataManager.getLastShape();
        if (shape != null) {
            dataManager.rememberPack(PackRef.custom(shape.fileName));
            getView().setShapeFileName(shape);
        }
        getView().showProgress(false);
    }

    public void checkNeedGuide() {
        if(dataManager.needGuide()) {
            getView().showLongPressGuide();
        }
    }

    public boolean isGuideMode() {
        return dataManager.needGuide();
    }

    public void skipGuide() {
        dataManager.setTutorialProgress(TutorialProgress.COMPLETED);
    }

    public void resumeGuide() {
        if (dataManager.getTutorialProgress().isComplete()) {
            dataManager.setTutorialProgress(TutorialProgress.DRAWING_GUIDE);
        }
    }

    public TutorialProgress getTutorialProgress() {
        return dataManager.getTutorialProgress();
    }

    public void setTutorialProgress(TutorialProgress progress) {
        dataManager.setTutorialProgress(progress);
    }

    public void completeTutorial() {
        dataManager.setTutorialProgress(TutorialProgress.COMPLETED);
    }

    public void rememberBuiltInPack(String id) {
        dataManager.rememberPack(PackRef.builtIn(id));
    }

    /** 마술 1회 수행 기록. 누적 횟수를 반환한다. */
    public int recordTrickPerformed() {
        return dataManager.recordTrickPerformed();
    }

}
