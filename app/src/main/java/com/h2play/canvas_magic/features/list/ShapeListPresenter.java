package com.h2play.canvas_magic.features.list;

import android.content.Context;

import javax.inject.Inject;

import com.h2play.canvas_magic.data.DataManager;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.features.base.BasePresenter;
import com.h2play.canvas_magic.injection.ConfigPersistent;
import com.h2play.canvas_magic.util.FileUtil;
import com.h2play.canvas_magic.util.rx.scheduler.SchedulerUtils;

import io.reactivex.Observable;


@ConfigPersistent
public class ShapeListPresenter extends BasePresenter<ShapeListMvpView> {

    private final DataManager dataManager;

    @Inject
    public ShapeListPresenter(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void attachView(ShapeListMvpView mvpView) {
        super.attachView(mvpView);
    }

    public void getShapes() {
        checkViewAttached();
        getView().showProgress(true);
        Observable.just(dataManager
                .getShapeList())
                .compose(SchedulerUtils.ioToMain())
                .subscribe(
                        shapes -> {
                            getView().showProgress(false);
                            getView().showShapeList(shapes);
                        },
                        throwable -> {
                            getView().showProgress(false);
                            getView().showError(throwable);
                        });
    }

    public void addNewItem(Context context, String name, int count) {

        StringBuilder builder = new StringBuilder("{\"shapes\":[");
        for (int i = 0; i < count; i++) {
            if( i > 0) {
                builder.append(",");   
            }
            builder.append("[]");
        }
        builder.append(" ] }");

        int index = dataManager.getNewFileIndex();
        String newFileName = String.format("file%d.txt",index);
        FileUtil.writeFile(context,newFileName,builder.toString());
        dataManager.addFileList(name,newFileName,count);

        getView().showShape(name, newFileName,count);

    }

    public void deleteItem( String name) {
        if(dataManager.getShapeList().size() > 1) {
            dataManager.deleteFile(name);
            getShapes();
        } else {
            getView().showImpossibleDelete();
        }
    }

    public void renameItem( String oldName, String newName ) {
        dataManager.renameFile(oldName,newName);
        ShapeInfo item = dataManager.getShapeFromName(newName);
        getView().showShape(item.name,item.fileName,item.count);
        getView().setShapeList(dataManager.getShapeList());

    }
}
