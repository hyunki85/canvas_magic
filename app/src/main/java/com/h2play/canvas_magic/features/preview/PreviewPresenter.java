package com.h2play.canvas_magic.features.preview;

import android.content.Context;

import com.h2play.canvas_magic.data.DataManager;
import com.h2play.canvas_magic.features.base.BasePresenter;
import com.h2play.canvas_magic.injection.ConfigPersistent;
import com.h2play.canvas_magic.util.FileUtil;

import javax.inject.Inject;


@ConfigPersistent
public class PreviewPresenter extends BasePresenter<PreviewMvpView> {

    private final DataManager dataManager;

    @Inject
    public PreviewPresenter(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void attachView(PreviewMvpView mvpView) {
        super.attachView(mvpView);
    }

    public void addNewItem(Context context, String name, String json) {

        int index = dataManager.getNewFileIndex();
        String newFileName = String.format("file%d.txt",index);
        FileUtil.writeFile(context,newFileName,json);
        dataManager.addFileList(name,newFileName);

        getView().onSaveComplete();

    }
}
