package com.h2play.canvas_magic.features.menu;

import javax.inject.Inject;

import com.h2play.canvas_magic.data.DataManager;
import com.h2play.canvas_magic.data.model.PackRef;
import com.h2play.canvas_magic.data.model.TutorialProgress;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.features.base.BasePresenter;
import com.h2play.canvas_magic.injection.ConfigPersistent;

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
    
    /**
     * 튜토리얼 다시하기 기능
     * 가이드를 활성화하고, 튜토리얼 시작
     */
    public void restartTutorial() {
        dataManager.setTutorialProgress(TutorialProgress.NOT_STARTED);
        getView().startTutorial();
    }

    public void cancelTutorial() {
        // Cancellation is resumable. Never convert an abandoned lesson into completion.
    }

    public boolean isGuideNeeded() {
        return dataManager.needGuide();
    }

    public int getTrickCount() {
        return dataManager.getTrickCount();
    }

    public int getLastShapeIndex() {
        return dataManager.getLastShapeIndex();
    }

    public ShapeInfo getLastShape() {
        PackRef pack = dataManager.getLastPackRef();
        return pack != null && pack.kind == PackRef.Kind.CUSTOM
                ? dataManager.getShapeByFileName(pack.id) : null;
    }

    public TutorialProgress getTutorialProgress() {
        return dataManager.getTutorialProgress();
    }

    public void setTutorialProgress(TutorialProgress progress) {
        dataManager.setTutorialProgress(progress);
    }

    public PackRef getLastPackRef() {
        return dataManager.getLastPackRef();
    }

    public boolean hasPersistedPackRef() {
        return dataManager.hasPersistedPackRef();
    }

    public void rememberBuiltInPack(String id) {
        dataManager.rememberPack(PackRef.builtIn(id));
    }

    public void rememberCustomPack(String fileName) {
        dataManager.rememberPack(PackRef.custom(fileName));
    }

    public int getShapeIndex(String fileName) {
        PackRef ref = PackRef.custom(fileName);
        return ref.findCustomIndex(dataManager.getShapeList());
    }
}
