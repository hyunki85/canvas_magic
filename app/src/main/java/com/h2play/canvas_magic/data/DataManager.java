package com.h2play.canvas_magic.data;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.h2play.canvas_magic.data.local.PreferencesHelper;
import com.h2play.canvas_magic.data.model.PackRef;
import com.h2play.canvas_magic.data.model.TutorialProgress;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.util.DrawingCatalog;

/**
 * Created by shivam on 29/5/17.
 */
@Singleton
public class DataManager {


    public static final String SHAPE_LIST_JSON = "shape_list_json";
    public static final String FILE_INDEX = "FILE_INDEX";
    public static final String NEED_GUIDE = "need_guide";
    public static final String TRICK_COUNT = "trick_count";
    public static final String LAST_SHAPE_INDEX = "last_shape_index";
    public static final String TUTORIAL_PROGRESS = "tutorial_progress_v2";
    public static final String LAST_PACK_KIND = "last_pack_kind_v2";
    public static final String LAST_PACK_ID = "last_pack_id_v2";
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

    public TutorialProgress getTutorialProgress() {
        TutorialProgress stored = TutorialProgress.fromStored(
                preferencesHelper.getString(TUTORIAL_PROGRESS));
        if (stored != null) return stored;

        TutorialProgress migrated = TutorialProgress.migrateLegacy(
                preferencesHelper.getBoolean(NEED_GUIDE), getTrickCount());
        setTutorialProgress(migrated);
        return migrated;
    }

    public void setTutorialProgress(TutorialProgress progress) {
        TutorialProgress safe = progress == null ? TutorialProgress.NOT_STARTED : progress;
        preferencesHelper.putString(TUTORIAL_PROGRESS, safe.name());
        // Keep the old key coherent for one compatibility release.
        preferencesHelper.putBoolean(NEED_GUIDE, safe.isComplete());
    }

    public boolean needGuide() {
        return !getTutorialProgress().isComplete();
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

    /**
     * 마지막으로 공연에 사용한 도형 세트를 기억한다.
     * 세트가 삭제된 경우를 고려해 읽을 때마다 현재 목록 범위로 보정한다.
     */
    public void setLastShapeIndex(int index) {
        List<ShapeInfo> shapes = getShapeList();
        if (index >= 0 && index < shapes.size()) {
            preferencesHelper.putInt(LAST_SHAPE_INDEX, index);
            rememberPack(PackRef.custom(shapes.get(index).fileName));
        }
    }

    public int getLastShapeIndex() {
        List<ShapeInfo> shapeList = getShapeList();
        if (shapeList.isEmpty()) {
            return -1;
        }
        PackRef pack = getLastPackRef();
        int stableIndex = pack == null ? -1 : pack.findCustomIndex(shapeList);
        return stableIndex >= 0 ? stableIndex : defaultShapeIndex(shapeList);
    }

    public ShapeInfo getLastShape() {
        int index = getLastShapeIndex();
        return index < 0 ? null : getShapeList().get(index);
    }

    public ShapeInfo getShapeByFileName(String fileName) {
        if (fileName == null) return null;
        for (ShapeInfo shape : getShapeList()) {
            if (shape != null && fileName.equals(shape.fileName)) return shape;
        }
        return null;
    }

    public boolean hasPersistedPackRef() {
        return preferencesHelper.hasKey(LAST_PACK_KIND) && preferencesHelper.hasKey(LAST_PACK_ID);
    }

    public void rememberPack(PackRef pack) {
        if (pack == null || pack.id.isEmpty()) return;
        preferencesHelper.putString(LAST_PACK_KIND, pack.kind.name());
        preferencesHelper.putString(LAST_PACK_ID, pack.id);
    }

    public PackRef getLastPackRef() {
        List<ShapeInfo> shapes = getShapeList();
        PackRef stored = PackRef.restore(preferencesHelper.getString(LAST_PACK_KIND),
                preferencesHelper.getString(LAST_PACK_ID));
        if (stored != null) {
            if (stored.kind == PackRef.Kind.BUILT_IN && DrawingCatalog.index(stored.id) >= 0) {
                return stored;
            }
            if (stored.kind == PackRef.Kind.CUSTOM && stored.findCustomIndex(shapes) >= 0) {
                return stored;
            }
        }

        if (shapes.isEmpty()) return null;
        int legacyIndex = preferencesHelper.getInt(LAST_SHAPE_INDEX);
        int fallbackIndex = legacyIndex >= 0 && legacyIndex < shapes.size()
                ? legacyIndex : defaultShapeIndex(shapes);
        PackRef fallback = PackRef.custom(shapes.get(fallbackIndex).fileName);
        rememberPack(fallback);
        preferencesHelper.putInt(LAST_SHAPE_INDEX, fallbackIndex);
        return fallback;
    }

    private static int defaultShapeIndex(List<ShapeInfo> shapes) {
        for (int index = 0; index < shapes.size(); index++) {
            if (shapes.get(index) != null && "file.txt".equals(shapes.get(index).fileName)) return index;
        }
        return shapes.isEmpty() ? -1 : 0;
    }

    public void setNoMoreGuide() {
        setTutorialProgress(TutorialProgress.COMPLETED);
    }
    
    /**
     * 가이드를 다시 활성화하는 메서드
     * NEED_GUIDE 값을 false로 설정하여 needGuide() 메서드가 true를 반환하도록 함
     */
    public void enableGuide() {
        setTutorialProgress(TutorialProgress.NOT_STARTED);
    }
}
