package com.h2play.canvas_magic.data;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.h2play.canvas_magic.data.local.PreferencesHelper;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;

/**
 * Created by shivam on 29/5/17.
 */
@Singleton
public class DataManager {


    public static final String SHAPE_LIST_JSON = "shape_list_json";
    public static final String FILE_INDEX = "FILE_INDEX";
    public static final String NEED_GUIDE = "need_guide";
    public static final String TRICK_COUNT = "trick_count";
    private PreferencesHelper preferencesHelper;

    @Inject
    public DataManager(PreferencesHelper preferencesHelper) {
        this.preferencesHelper = preferencesHelper;
    }

    public List<ShapeInfo> getShapeList() {

        ArrayList<ShapeInfo> arrayList = new ArrayList<ShapeInfo>();

        Gson gson = new Gson();
        List<ShapeInfo> list = gson.fromJson(
                preferencesHelper.getString(SHAPE_LIST_JSON),
                new TypeToken<List<ShapeInfo>>() {
                }.getType());
        if(list != null) {
            arrayList.addAll( list );
        }

        return arrayList;
    }

    public void writeListToJson(List<ShapeInfo> shapeInfos) {

        Gson gson = new Gson();
        String jsonStr = gson.toJson(shapeInfos);
        preferencesHelper.putString(SHAPE_LIST_JSON,jsonStr);
    }

    public void addFileList(String name, String fileName, int count) {
        List<ShapeInfo> shapeList = getShapeList();
        shapeList.add(new ShapeInfo(fileName,name,count));
        writeListToJson(shapeList);
    }

    public void deleteFile(String name) {
        List<ShapeInfo> shapeList = getShapeList();
        shapeList.remove(getFileIndexFromName(name));
        writeListToJson(shapeList);
    }

    public int getNewFileIndex() {

        int index =  preferencesHelper.getInt(FILE_INDEX);
        if(index == -1) {
            index = 1;
        }
        preferencesHelper.putInt(FILE_INDEX,index+1);
        return index;
    }


    public int getFileIndexFromName(String name) {
        List<ShapeInfo> shapeList = getShapeList();
        for (int i = 0; i < shapeList.size(); ++i) {
            if (shapeList.get(i).name.compareToIgnoreCase(name)==0) {
                return i;
            }
        }
        return -1;
    }

    public void renameFile(String oldName, String newName) {
        List<ShapeInfo> shapeList = getShapeList();
        int index = getFileIndexFromName(oldName);
        ShapeInfo shapeItem = shapeList.get(index);
        shapeItem.name = newName;
        shapeList.set(index,shapeItem);
        writeListToJson(shapeList);
    }

    public ShapeInfo getShapeFromName(String name) {
        List<ShapeInfo> shapeList = getShapeList();
        int index = getFileIndexFromName(name);
        return shapeList.get(index);
    }

    public boolean alreadyStarId(String id) {
        return preferencesHelper.hasKey("STAR_ID_" + id);
    }


    public void giveStarId(String id) {
        preferencesHelper.putBoolean("STAR_ID_" + id,true);
    }

    public boolean needGuide() {
        return !preferencesHelper.getBoolean(NEED_GUIDE);
    }

    /**
     * 마술을 실제로 1회 수행했을 때 호출. 누적 횟수를 반환한다.
     * 이 앱의 활성화 지표이자, 이후 평점 요청·알림 권한 요청의 트리거 기준이 된다.
     */
    public int recordTrickPerformed() {
        int next = getTrickCount() + 1;
        preferencesHelper.putInt(TRICK_COUNT, next);
        return next;
    }

    public int getTrickCount() {
        int count = preferencesHelper.getInt(TRICK_COUNT);
        return count < 0 ? 0 : count;
    }

    public void setNoMoreGuide() {
        preferencesHelper.putBoolean(NEED_GUIDE,true);
    }
    
    /**
     * 가이드를 다시 활성화하는 메서드
     * NEED_GUIDE 값을 false로 설정하여 needGuide() 메서드가 true를 반환하도록 함
     */
    public void enableGuide() {
        preferencesHelper.putBoolean(NEED_GUIDE, false);
    }
}
