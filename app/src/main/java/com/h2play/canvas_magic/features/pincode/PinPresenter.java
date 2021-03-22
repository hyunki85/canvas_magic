package com.h2play.canvas_magic.features.pincode;

import javax.inject.Inject;

import com.h2play.canvas_magic.data.DataManager;
import com.h2play.canvas_magic.features.base.BasePresenter;
import com.h2play.canvas_magic.injection.ConfigPersistent;
import com.h2play.canvas_magic.util.rx.scheduler.SchedulerUtils;

@ConfigPersistent
public class PinPresenter extends BasePresenter<PinMvpView> {

    private final DataManager dataManager;

    @Inject
    public PinPresenter(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void attachView(PinMvpView mvpView) {
        super.attachView(mvpView);
    }

    public void getPokemon(int limit) {
        checkViewAttached();
        getView().showProgress(true);
        dataManager
                .getPokemonList(limit)
                .compose(SchedulerUtils.ioToMain())
                .subscribe(
                        pokemons -> {
                            getView().showProgress(false);
                            getView().showPokemon(pokemons);
                        },
                        throwable -> {
                            getView().showProgress(false);
                            getView().showError(throwable);
                        });
    }

    public void needGuide() {
        if(dataManager.needGuide()) {
            getView().showGuide();
        }
    }

    public void noMoreGuide() {
        dataManager.setNoMoreGuide();

    }
}
