package com.h2play.canvas_magic.features.share;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.h2play.canvas_magic.data.DataManager;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.data.model.response.ShapeOnline;
import com.h2play.canvas_magic.features.base.BasePresenter;
import com.h2play.canvas_magic.injection.ConfigPersistent;
import com.h2play.canvas_magic.util.FileUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

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
            Query query = ref.limit(14);
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

        upload(context, shape.name,shape.fileName);

    }

    public void upload(Context context, String name, String fileName) {

        String jsonText = FileUtil.getJsonFromFile(context, fileName);
        CollectionReference shapes = db.collection("shapes");

        Map<String, Object> shapeOnline = new HashMap<>();
        shapeOnline.put("name", name);
        shapeOnline.put("star", 0);
        shapeOnline.put("date", new Date());
        shapeOnline.put("json", jsonText);
        shapes.document().set(shapeOnline).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                getView().onShareComplete();
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
