package com.h2play.canvas_magic.features.share;

import android.content.Context;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.h2play.canvas_magic.data.DataManager;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.data.model.response.ShapeOnline;
import com.h2play.canvas_magic.features.base.BasePresenter;
import com.h2play.canvas_magic.injection.ConfigPersistent;
import com.h2play.canvas_magic.util.FileUtil;
import com.h2play.canvas_magic.util.ShapeSetValidator;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

@ConfigPersistent
public class SharePresenter extends BasePresenter<ShareMvpView> {

    private final DataManager dataManager;
    private final FirebaseFirestore db;
    private ArrayList<ShapeOnline> shapeOnlines = new ArrayList<>();
    private DocumentSnapshot lastDocumentSnapshot;
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
        lastDocumentSnapshot = null;
    }

    @Override
    public void attachView(ShareMvpView mvpView) {
        super.attachView(mvpView);
    }

    public void getShapeOnline(SORT_TYPE sortType) {
        if (!isViewAttached()) return;
        getView().showProgress(true);

        CollectionReference ref = db.collection("shapes");
        Query query = ref.limit(9);
        if (sortType == SORT_TYPE.FEATURED) {
            query = query.whereEqualTo("featured", Boolean.valueOf(true)).orderBy("star", Query.Direction.DESCENDING);
        } else if (sortType == SORT_TYPE.RECENT) {
            query = query.orderBy("date", Query.Direction.DESCENDING);
        } else {
            query = query.orderBy("star", Query.Direction.DESCENDING);
        }

        if (lastDocumentSnapshot != null) {
            query = query.startAfter(lastDocumentSnapshot);
        }

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (!isViewAttached()) return;
                getView().showProgress(false);

                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot == null) {
                        getView().showShapes(shapeOnlines);
                        return;
                    }
                    if (snapshot != null && !snapshot.isEmpty()) {
                        lastDocumentSnapshot = snapshot.getDocuments().get(snapshot.size() - 1);
                    }

                    ArrayList<ShapeOnline> page = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        ShapeOnline shape = document.toObject(ShapeOnline.class);
                        if (shape == null || !ShapeSetValidator.isValidCommunitySet(shape.json, shape.count)) {
                            continue;
                        }
                        shape.id = document.getId();
                        shape.alreadyStar = dataManager.alreadyStarId(shape.id);
                        page.add(shape);
                    }
                    shapeOnlines.addAll(page);
                    getView().showShapes(shapeOnlines);
                } else if (task.getException() != null) {
                    getView().showError(task.getException());
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
        if (!ShapeSetValidator.isValidCommunitySet(jsonText, count)) {
            if (isViewAttached()) getView().showError(
                    new IllegalArgumentException("Invalid drawing set"));
            return;
        }
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // 조회·좋아요·미리보기가 모두 사용하는 canonical 컬렉션에 완전한 세트를 저장한다.
        // 과거의 shapes2/shapesInfo 분리 저장은 피드(shapes)와 연결되지 않아 업로드한
        // 세트가 보이지 않았고, shapesInfo에는 첫 번째 도형만 있어 미리보기도 불가능했다.
        Map<String, Object> shapeMap = new HashMap<>();
        shapeMap.put("json", jsonText);
        shapeMap.put("count", count);
        shapeMap.put("name", name);
        shapeMap.put("star", 0);
        shapeMap.put("featured", false);
        shapeMap.put("language", Locale.getDefault().getLanguage());
        shapeMap.put("date", new Date());
        shapeMap.put("md5", getmd5hashfromstring(jsonText));
        if (currentUser != null) {
            shapeMap.put("user", currentUser.getUid());
            shapeMap.put("user_name", currentUser.getUid());
        }

        getView().showProgress(true);
        db.collection("shapes").add(shapeMap).addOnCompleteListener(task -> {
            if (!isViewAttached()) return;
            getView().showProgress(false);
            if (task.isSuccessful()) {
                getView().onShareComplete();
            } else if (task.getException() != null) {
                getView().showError(task.getException());
            }
        });
    }

    public void getShapeList() {
        getView().showShapeList(dataManager.getShapeList());
    }

    public void getShapeById(String documentId) {
        if (!isViewAttached() || documentId == null
                || !documentId.matches("[A-Za-z0-9_-]{1,128}")) return;
        db.collection("shapes").document(documentId).get().addOnSuccessListener(document -> {
            if (!isViewAttached() || !document.exists()) return;
            ShapeOnline shape = document.toObject(ShapeOnline.class);
            if (shape == null || !shape.featured
                    || !ShapeSetValidator.isValidCommunitySet(shape.json, shape.count)) return;
            shape.id = document.getId();
            shape.alreadyStar = dataManager.alreadyStarId(shape.id);
            getView().openShapePreview(shape);
        });
    }

    public void addLike(ShapeOnline shapeOnline) {
        if (!isViewAttached()) return;
        
        if(!shapeOnline.alreadyStar) {
            DocumentReference shapeRef = db.collection("shapes").document(shapeOnline.id);
            shapeOnline.alreadyStar = true;
            if (isViewAttached()) {
                getView().updateShape(shapeOnline);
            }
            shapeRef.update("star", FieldValue.increment(1))
                    .addOnSuccessListener(ignored -> {
                        dataManager.giveStarId(shapeOnline.id);
                        shapeOnline.star += 1;
                        if (isViewAttached()) {
                            getView().updateShape(shapeOnline);
                        }
                    })
                    .addOnFailureListener(error -> {
                        shapeOnline.alreadyStar = false;
                        if (isViewAttached()) {
                            getView().updateShape(shapeOnline);
                        }
                    });
        }

    }
}
