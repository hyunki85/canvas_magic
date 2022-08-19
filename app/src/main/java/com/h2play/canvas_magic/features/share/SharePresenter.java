package com.h2play.canvas_magic.features.share;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.h2play.canvas_magic.data.DataManager;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.data.model.response.ShapeOnline;
import com.h2play.canvas_magic.features.base.BasePresenter;
import com.h2play.canvas_magic.injection.ConfigPersistent;
import com.h2play.canvas_magic.util.FileUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;

@ConfigPersistent
public class SharePresenter extends BasePresenter<ShareMvpView> {

    private final DataManager dataManager;
    private final FirebaseFirestore db;
    private ArrayList<ShapeOnline> shapeOnlines = new ArrayList<>();
    public enum SORT_TYPE {
        FEATURED,
        STAR,
        RECENT
    }

    @Inject
    public SharePresenter(DataManager dataManager) {
        this.dataManager = dataManager;

        db = FirebaseFirestore.getInstance();
    }

    public void resetArray() {
        shapeOnlines.clear();
    }

    @Override
    public void attachView(ShareMvpView mvpView) {
        super.attachView(mvpView);
    }

    public void getShapeOnline(SORT_TYPE sortType, ShapeOnline lastObj) {
        checkViewAttached();
        getView().showProgress(true);

        CollectionReference ref = db.collection("shapes");
            Query query = ref.limit(9);
            if(sortType == SORT_TYPE.FEATURED) {
                query = query.whereEqualTo("featured",Boolean.valueOf(true)).orderBy("star", Query.Direction.DESCENDING);
            } else {
                if(sortType == SORT_TYPE.RECENT) {
                    query = query.orderBy("date", Query.Direction.DESCENDING);
                } else {
                    query = query.orderBy("star", Query.Direction.DESCENDING);
                }
            }
            if(lastObj != null) {
                if(sortType == SORT_TYPE.RECENT) {
                    query = query.startAfter(lastObj.date);
                } else if(sortType == SORT_TYPE.STAR) {
                    query = query.startAfter(lastObj.star);
                } else if(sortType == SORT_TYPE.FEATURED) {
                    query = query.startAfter(lastObj.star);
                }
            }
            query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Observable<ShapeOnline> shapeOnlineObservable = Observable.fromIterable(task.getResult())
                                    .map(queryDocumentSnapshot -> {
                                        ShapeOnline shape = queryDocumentSnapshot.toObject(ShapeOnline.class);
                                        shape.id = queryDocumentSnapshot.getId();
                                        if (dataManager.alreadyStarId(shape.id)) {
                                            shape.alreadyStar = true;
                                        }
                                        return shape;
                                    });

                                    shapeOnlineObservable.toList()
                                            .subscribe(queryDocumentSnapshots -> {
                                        shapeOnlines.addAll(queryDocumentSnapshots);
                                        getView().showShapes(shapeOnlines);

                            });

                        } else {

                        }
                    }
                });
    }

    public void upload(Context context, int shapeIndex) {
        ShapeInfo shape = dataManager.getShapeList().get(shapeIndex);

        upload(context, shape.name,shape.fileName,shape.count);

    }

    String getmd5hashfromstring(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(input.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

    public void upload(Context context, String name, String fileName, int count) {

        String jsonText = FileUtil.getJsonFromFile(context, fileName);

        JsonObject assetJsonObject = new Gson().fromJson(jsonText, JsonObject.class);
        JsonArray shapesJson = assetJsonObject.get("shapes").getAsJsonArray();
        Gson gson = new Gson();
        String firstShapeJson = gson.toJson(shapesJson.get(0));

        CollectionReference shapes = db.collection("shapes2");
        Map<String, Object> shapeMap = new HashMap<>();
        shapeMap.put("json", jsonText);
        shapeMap.put("count", count);
        shapeMap.put("name", name);
        shapes.add(shapeMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                CollectionReference shapesInfo = db.collection("shapesInfo");
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                Map<String, Object> shapeInfoMap = new HashMap<>();
                shapeInfoMap.put("user", currentUser.getUid());
                shapeInfoMap.put("user_name", currentUser.getUid());
                shapeInfoMap.put("shape_id", task.getResult().getId());
                shapeInfoMap.put("name", name);
                shapeInfoMap.put("star", 0);
                shapeInfoMap.put("language", Locale.getDefault().getLanguage());
                shapeInfoMap.put("date", new Date());
                shapeInfoMap.put("json", firstShapeJson);
                shapeInfoMap.put("md5", getmd5hashfromstring(jsonText));
                shapesInfo.add(shapeInfoMap).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        getView().onShareComplete();
                    }
                });
            } else {
                Log.d("upload", "upload failed");
            }
        });
    }

    public void getShapeList() {
        getView().showShapeList(dataManager.getShapeList());
    }

    public void addLike(ShapeOnline shapeOnline) {

        if(!shapeOnline.alreadyStar) {
            DocumentReference shapeRef = db.collection("shapes").document(shapeOnline.id);
            shapeRef.update("star", shapeOnline.star +1);
            dataManager.giveStarId(shapeOnline.id);
            shapeOnline.alreadyStar = true;
            shapeOnline.star += 1;
            getView().updateShape(shapeOnline);
        }

    }
}
