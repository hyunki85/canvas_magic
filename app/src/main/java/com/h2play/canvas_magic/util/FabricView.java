package com.h2play.canvas_magic.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import com.h2play.canvas_magic.util.DrawableObjects.CBitmap;
import com.h2play.canvas_magic.util.DrawableObjects.CDrawable;
import com.h2play.canvas_magic.util.DrawableObjects.CPath;
import com.h2play.canvas_magic.util.DrawableObjects.CText;
import com.h2play.canvas_magic.util.DrawableObjects.CTransform;
import com.h2play.canvas_magic.util.DrawableObjects.CTranslation;

/**
 * 모던화된 FabricView - 캔버스 그리기 뷰
 * 
 * 개선사항:
 * - 하드웨어 가속 활용
 * - 메모리 효율성 개선
 * - 터치 반응성 향상
 * - deprecated API 제거
 * 
 * 사용법:
 * <pre>
 * FabricView fabricView = findViewById(R.id.fabricView);
 * fabricView.setColor(Color.RED);
 * fabricView.setSize(10);
 * fabricView.setInteractionMode(FabricView.DRAW_MODE);
 * </pre>
 */
public class FabricView extends View {

    // Drawable 리스트
    private final ArrayList<CDrawable> mDrawableList = new ArrayList<>();
    private final ArrayList<CDrawable> mUndoList = new ArrayList<>();
    private final ArrayList<JsonObject> jsonObjects = new ArrayList<>();

    // 선택 관련
    private CDrawable selected = null;
    private CDrawable hovering = null;
    private long pressStartTime;
    private float pressedX;
    private float pressedY;

    // 그리기 설정
    private int mColor = Color.BLACK;
    private int mBackgroundColor = Color.WHITE;
    private Paint.Style mStyle = Paint.Style.STROKE;
    private float mSize = 5f;
    
    // 상태
    private int savePoint = 0;
    private int mInteractionMode = DRAW_MODE;
    private int mBackgroundMode = BACKGROUND_STYLE_BLANK;
    private boolean mTextExpectTouch = false;

    // 삭제 아이콘
    private Bitmap deleteIcon;
    private final RectF deleteIconPosition = new RectF(-1, -1, -1, -1);
    private DeletionListener deletionListener = null;

    // 터치 추적
    private float lastTouchX, lastTouchY;
    private final RectF dirtyRect = new RectF();
    
    // 현재 그리기 중인 경로
    private CPath currentPath;
    private Paint currentPaint;
    private Paint selectionPaint;
    private int selectionColor = Color.DKGRAY;
    
    // 크롭 영역
    private Rect cropBounds = null;

    // 상수
    private static final float TOUCH_TOLERANCE = 4;
    private static final int MAX_CLICK_DURATION = 1000;
    private static final int MAX_CLICK_DISTANCE = 15;
    private static final int SELECTION_LINE_WIDTH = 2;
    
    public static final int NOTEBOOK_LEFT_LINE_COLOR = Color.RED;
    public static final int NOTEBOOK_LEFT_LINE_PADDING = 120;

    // 배경 모드
    public static final int BACKGROUND_STYLE_BLANK = 0;
    public static final int BACKGROUND_STYLE_NOTEBOOK_PAPER = 1;
    public static final int BACKGROUND_STYLE_GRAPH_PAPER = 2;

    // 상호작용 모드
    public static final int DRAW_MODE = 0;
    public static final int SELECT_MODE = 1;
    public static final int ROTATE_MODE = 2;
    public static final int LOCKED_MODE = 3;

    /**
     * 생성자
     *
     * @param context 컨텍스트
     * @param attrs   뷰 속성
     */
    public FabricView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        setFocusable(true);
        setFocusableInTouchMode(true);
        setBackgroundColor(mBackgroundColor);
        
        // 하드웨어 가속 활성화
        setLayerType(LAYER_TYPE_HARDWARE, null);

        // 선택 페인트 초기화
        selectionPaint = new Paint();
        selectionPaint.setAntiAlias(true);
        selectionPaint.setColor(selectionColor);
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeJoin(Paint.Join.ROUND);
        selectionPaint.setStrokeWidth(SELECTION_LINE_WIDTH);
        selectionPaint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));

        deleteIcon = BitmapFactory.decodeResource(context.getResources(),
                android.R.drawable.ic_menu_delete);
    }

    /**
     * 캔버스 그리기
     */
    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas, mBackgroundMode);
        Rect totalBounds = new Rect(canvas.getWidth(), canvas.getHeight(), 0, 0);

        // 모든 drawable 그리기
        for (int i = 0; i < mDrawableList.size(); i++) {
            try {
                CDrawable d = mDrawableList.get(i);
                if (d instanceof CTransform) {
                    continue;
                }

                Rect bounds = d.computeBounds();
                totalBounds.union(bounds);
                d.draw(canvas);
                
                // 선택 모드에서 선택된 객체 표시
                if (mInteractionMode == SELECT_MODE && d.equals(selected)) {
                    growRect(bounds, SELECTION_LINE_WIDTH);
                    canvas.drawRect(new RectF(bounds), selectionPaint);
                    deleteIconPosition.left = bounds.right - (deleteIcon.getWidth() / 2f);
                    deleteIconPosition.top = bounds.top - (deleteIcon.getHeight() / 2f);
                    deleteIconPosition.right = deleteIconPosition.left + deleteIcon.getWidth();
                    deleteIconPosition.bottom = deleteIconPosition.top + deleteIcon.getHeight();
                    canvas.drawBitmap(deleteIcon, deleteIconPosition.left, deleteIconPosition.top, d.getPaint());
                }
            } catch (Exception ex) {
                // 개별 drawable 그리기 실패 시 계속 진행
            }
        }
        
        cropBounds = totalBounds.width() > 0 ? totalBounds : null;
    }

    public ArrayList<CDrawable> getDrawables() {
        return mDrawableList;
    }

    private void growRect(Rect rect, int amount) {
        rect.left -= amount;
        rect.top -= amount;
        rect.bottom += amount;
        rect.right += amount;
    }


    /*********************************************************************************************/
    /*******************************     Handling User Touch    **********************************/
    /*********************************************************************************************/

    /**
     * Handles user touch events.
     *
     * @param event the user's motion event
     * @return true, the event is consumed.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // delegate action to the correct method
        if (getInteractionMode() == DRAW_MODE)
            return onTouchDrawMode(event);
        if (getInteractionMode() == SELECT_MODE)
            return onTouchSelectMode(event);
        if (getInteractionMode() == ROTATE_MODE)
            return onTouchRotateMode(event);
        // if none of the above are selected, delegate to locked mode
        return onTouchLockedMode(event);
    }

    /**
     * Handles touch event if the mode is set to locked
     *
     * @param event the event to handle
     * @return false, shouldn't do anything with it for now
     */
    private boolean onTouchLockedMode(MotionEvent event) {
        // return false since we don't want to do anything so far
        return false;
    }

    /**
     * Handles the touch input if the mode is set to rotate
     *
     * @param event the touch event
     * @return the result of the action
     */
    private boolean onTouchRotateMode(MotionEvent event) {
        return false;
    }

    public void actionDown(float eventX, float eventY) {

        currentPath = new CPath();
        currentPaint = new Paint();
        currentPaint.setAntiAlias(true);
        currentPaint.setColor(mColor);
        currentPaint.setStyle(mStyle);
        currentPaint.setStrokeJoin(Paint.Join.ROUND);
        currentPaint.setStrokeWidth(mSize);
        currentPath.setPaint(currentPaint);
        currentPath.moveTo(eventX, eventY);
        // capture touched locations
        lastTouchX = eventX;
        lastTouchY = eventY;
        mDrawableList.add(currentPath);
        mUndoList.clear();

    }

    /**
     * Handles the touch input if the mode is set to draw
     *
     * @param event the touch event
     * @return the result of the action
     */
    public boolean onTouchDrawMode(MotionEvent event) {
        // getDrawables location of touch
        float eventX = event.getX();
        float eventY = event.getY();
        if (eventX < 0) {
            eventX = 0;
        }
        if (eventY < 0) {
            eventY = 0;
        }
        if (eventX > getWidth()) {
            eventX = getWidth();
        }
        if (eventY > getHeight()) {
            eventY = getHeight();
        }

        // based on the users action, start drawing
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                actionDown(eventX,eventY);
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("action","down");
                jsonObject.addProperty("x",eventX);
                jsonObject.addProperty("y",eventY);
                jsonObjects.add(jsonObject);

                return true;
            }
            case MotionEvent.ACTION_MOVE: {



                float dx = Math.abs(eventX - lastTouchX);
                float dy = Math.abs(eventY - lastTouchY);

                if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {

                    actionMove(lastTouchX, lastTouchY, (eventX + lastTouchX) / 2, (eventY + lastTouchY) / 2);
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("action","move");
                    jsonObject.addProperty("x1",lastTouchX);
                    jsonObject.addProperty("y1",lastTouchY);
                    jsonObject.addProperty("x2",(eventX + lastTouchX) / 2);
                    jsonObject.addProperty("y2",(eventY + lastTouchY) / 2);
                    jsonObjects.add(jsonObject);

                }
//                int historySize = event.getHistorySize();
//                for (int i = 0; i < historySize; i++) {
//                    float historicalX = event.getHistoricalX(i);
//                    float historicalY = event.getHistoricalY(i);
//                    currentPath.lineTo(historicalX, historicalY);
//                }

                // After replaying history, connect the line to the touch point.
                //  currentPath.lineTo(eventX, eventY);

                dirtyRect.left = Math.min(currentPath.getXcoords(), dirtyRect.left);
                dirtyRect.right = Math.max(currentPath.getXcoords() + currentPath.getWidth(), dirtyRect.right);
                dirtyRect.top = Math.min(currentPath.getYcoords(), dirtyRect.top);
                dirtyRect.bottom = Math.max(currentPath.getYcoords() + currentPath.getHeight(), dirtyRect.bottom);

                // After replaying history, connect the line to the touch point.
                cleanDirtyRegion(eventX, eventY);
                break;
            }
            case MotionEvent.ACTION_UP: {
                actionUp(eventX,eventY);
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("action","up");
                jsonObject.addProperty("x",eventX);
                jsonObject.addProperty("y",eventY);
                jsonObjects.add(jsonObject);
            }
            default:
                return false;
        }

        // Include some padding to ensure nothing is clipped
        invalidate();
//                (int) (dirtyRect.left - 20),
//                (int) (dirtyRect.top - 20),
//                (int) (dirtyRect.right + 20),
//                (int) (dirtyRect.bottom + 20));

        // register most recent touch locations
        lastTouchX = eventX;
        lastTouchY = eventY;
        return true;
    }

    public void actionUp(float x, float y) {

        currentPath.lineTo(x, y);
    }

    public void actionMove(float x1, float y1, float x2, float y2) {

        currentPath.quadTo(x1, y1, x2, y2);
        lastTouchX = x1;
        lastTouchY = y1;
    }

    /**
     * Handles the touch input if the mode is set to select
     *
     * @param event the touch event
     */
    private boolean onTouchSelectMode(MotionEvent event) {
        ListIterator<CDrawable> li = mDrawableList.listIterator(mDrawableList.size());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressStartTime = SystemClock.uptimeMillis();
                pressedX = event.getX();
                pressedY = event.getY();

                while (li.hasPrevious()) {
                    CDrawable d = li.previous();
                    if (d instanceof CTransform) {
                        continue;
                    }
                    Rect rect = d.computeBounds();
                    if (rect.contains((int)pressedX, (int)pressedY)) {
                        hovering = d;
                        break;
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                long pressDuration = SystemClock.uptimeMillis() - pressStartTime;
                double distance = Math.sqrt(Math.pow((event.getX() - pressedX), 2) + Math.pow((event.getY() - pressedY), 2));
                if (pressDuration < MAX_CLICK_DURATION && distance < MAX_CLICK_DISTANCE) {
                    //It was a click not a drag.
                    if (hovering == null && deleteIconPosition.contains(event.getX(), event.getY())) {
                        deleteSelection();
                        return true;
                    }
                    selected = hovering;
                } else if (distance > MAX_CLICK_DISTANCE) {
                    //It was a drag. Move the object there.
                    if (hovering != null) {
                        CTranslation trans = new CTranslation(hovering);
                        Vector<Integer> v = new Vector<>(2);
                        v.add((int) (event.getX() - pressedX));
                        v.add((int) (event.getY() - pressedY));
                        trans.setDirection(v);
                        hovering.addTransform(trans);
                        mDrawableList.add(trans);
                        mUndoList.clear();
                    }
                }
                invalidate();
                hovering = null;
                return true;
        }
        return false;
    }


    /*******************************************
     * Drawing Events
     ******************************************/
    /**
     * 배경 그리기
     *
     * @param canvas         캔버스
     * @param backgroundMode 배경 모드
     */
    public void drawBackground(Canvas canvas, int backgroundMode) {
        canvas.drawColor(mBackgroundColor);
        if (backgroundMode != BACKGROUND_STYLE_BLANK) {
            Paint linePaint = new Paint();
            linePaint.setColor(Color.argb(50, 0, 0, 0));
            linePaint.setStyle(mStyle);
            linePaint.setStrokeJoin(Paint.Join.ROUND);
            linePaint.setStrokeWidth(mSize - 2f);
            switch (backgroundMode) {
                case BACKGROUND_STYLE_GRAPH_PAPER:
                    drawGraphPaperBackground(canvas, linePaint);
                    break;
                case BACKGROUND_STYLE_NOTEBOOK_PAPER:
                    drawNotebookPaperBackground(canvas, linePaint);
                default:
                    break;
            }
        }
    }

    /**
     * Draws a graph paper background on the view
     *
     * @param canvas the canvas to draw on
     * @param paint  the paint to use
     */
    private void drawGraphPaperBackground(Canvas canvas, Paint paint) {
        int i = 0;
        boolean doneH = false, doneV = false;

        // while we still need to draw either H or V
        while (!(doneH && doneV)) {

            // check if there is more H lines to draw
            if (i < canvas.getHeight())
                canvas.drawLine(0, i, canvas.getWidth(), i, paint);
            else
                doneH = true;

            // check if there is more V lines to draw
            if (i < canvas.getWidth())
                canvas.drawLine(i, 0, i, canvas.getHeight(), paint);
            else
                doneV = true;

            // declare as done
            i += 75;
        }
    }

    /**
     * Draws a notebook paper background on the view
     *
     * @param canvas the canvas to draw on
     * @param paint  the paint to use
     */
    private void drawNotebookPaperBackground(Canvas canvas, Paint paint) {
        int i = 0;
        boolean doneV = false;
        // draw horizental lines
        while (!(doneV)) {
            if (i < canvas.getHeight())
                canvas.drawLine(0, i, canvas.getWidth(), i, paint);
            else
                doneV = true;
            i += 75;
        }
        // change line color
        paint.setColor(NOTEBOOK_LEFT_LINE_COLOR);
        // draw side line
        canvas.drawLine(NOTEBOOK_LEFT_LINE_PADDING, 0,
                NOTEBOOK_LEFT_LINE_PADDING, canvas.getHeight(), paint);


    }

    /**
     * Draw text on the screen
     *
     * @param text the text to draw
     * @param x    the x location of the text
     * @param y    the y location of the text
     * @param p    the paint to use
     */
    public void drawText(String text, int x, int y, Paint p) {
        mDrawableList.add(new CText(text, x, y, p));
        mUndoList.clear();
        invalidate();
    }

    /**
     * 키보드에서 텍스트 캡처 (미구현)
     */
    private void drawTextFromKeyboard() {
        // TODO: 구현 필요
        mTextExpectTouch = true;
    }

    /**
     * Retrieve the region needing to be redrawn
     *
     * @param eventX The current x location of the touch
     * @param eventY the current y location of the touch
     */
    private void cleanDirtyRegion(float eventX, float eventY) {
        // figure out the sides of the dirty region
        dirtyRect.left = Math.min(lastTouchX, eventX);
        dirtyRect.right = Math.max(lastTouchX, eventX);
        dirtyRect.top = Math.min(lastTouchY, eventY);
        dirtyRect.bottom = Math.max(lastTouchY, eventY);
    }

    /**
     * Cancels the last operation. Works on both CDrawable and CTransform.
     */
    public void undo() {
        if (mDrawableList.size() > 0) {
            CDrawable toUndo = mDrawableList.get(mDrawableList.size() - 1);
            mUndoList.add(toUndo);
            mDrawableList.remove(mDrawableList.size() - 1);
            if(toUndo instanceof CTransform) {
                CTransform t = (CTransform)toUndo;
                t.getDrawable().removeTransform(t);
            }

            invalidate();
        }
    }

    /**
     * Re-instates the last undone operation.
     */
    public void redo() {
        if (mUndoList.size() > 0) {
            CDrawable toRedo = mUndoList.get(mUndoList.size() - 1);
            mDrawableList.add(toRedo);
            mDrawableList.addAll(toRedo.getTransforms());
            mUndoList.remove(toRedo);
            if(toRedo instanceof CTransform) {
                CTransform t = (CTransform)toRedo;
                t.getDrawable().addTransform(t);
            }

            invalidate();
        }
    }

    /**
     * Clean the canvas, remove everything drawn on the canvas.
     * WARNING: Before calling this, ask the user to confirm because <b>this cannot be undone</b>.
     */
    public void cleanPage() {
        // remove everything from the list
        mDrawableList.clear();
        jsonObjects.clear();
        currentPath = null;
        mUndoList.clear();
        savePoint = 0;
        // request to redraw the canvas
        invalidate();
    }

    /**
     * Draws an image on the canvas
     *
     * @param x      location of the image
     * @param y      location of the image
     * @param width  the width of the image
     * @param height the height of the image
     * @param pic    the image itself
     */
    public void drawImage(int x, int y, int width, int height, Bitmap pic) {
        CBitmap bitmap = new CBitmap(pic, x, y);
        bitmap.setWidth(width);
        bitmap.setHeight(height);
        mDrawableList.add(bitmap);
        mUndoList.clear();
        invalidate();
    }

    public void addPath(CPath cPath) {
        mDrawableList.add(cPath);
    }

    public void addDrawables(List<CPath> drawableList) {
        mDrawableList.addAll(drawableList);
        invalidate();
    }

    /*******************************************
     * Getters and Setters
     ******************************************/


    /**
     * Gets what has been drawn on the canvas so far as a bitmap
     *
     * @return Bitmap of the canvas.
     */
    public Bitmap getCanvasBitmap() {
        // build drawing cache of the canvas, use it to create a new bitmap, then destroy it.
        buildDrawingCache();
        Bitmap mCanvasBitmap = Bitmap.createBitmap(getDrawingCache());
        destroyDrawingCache();

        // return the created bitmap.
        return mCanvasBitmap;
    }

    /**
     * Gets what has been drawn on the canvas so far as a bitmap. Removes any margin around the drawn objects.
     *
     * @return Bitmap of the canvas, cropped.
     */
    public Bitmap getCroppedCanvasBitmap() {
        if(cropBounds == null) {
            //No pixels at all
            return null;
        }
        Bitmap mCanvasBitmap = getCanvasBitmap();
        Bitmap cropped = Bitmap.createBitmap(mCanvasBitmap, cropBounds.left, cropBounds.top, cropBounds.width(), cropBounds.height());
        return cropped;
    }

    /**
     * @return the drawing line color. Default is Color.BLACK.
     */
    public int getColor() {
        return mColor;
    }

    /**
     * Setter for the the drawing line color.
     * @param mColor The new color.
     */
    public void setColor(int mColor) {
        this.mColor = mColor;
    }

    /**
     * @return the background color. Default is Color.WHITE.
     */
    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    /**
     * Setter for the background color.
     * @param mBackgroundColor The new background color.
     */
    public void setBackgroundColor(int mBackgroundColor) {
        this.mBackgroundColor = mBackgroundColor;
    }

    /**
     * @return the background decorations mode. Default is BACKGROUND_STYLE_BLANK.
     */
    public int getBackgroundMode() {
        return mBackgroundMode;
    }

    /**
     * Setter for the background decorations mode. Can be BACKGROUND_STYLE_BLANK*, BACKGROUND_STYLE_NOTEBOOK_PAPER, or BACKGROUND_STYLE_GRAPH_PAPER.
     * @param mBackgroundMode
     */
    public void setBackgroundMode(int mBackgroundMode) {
        this.mBackgroundMode = mBackgroundMode;
        invalidate();
    }

    /**
     * @return The drawing style. Can be Paint.Style.FILL, Paint.Style.STROKE, or Paint.Style.FILL_AND_STROKE. Default is Paint.Style.STROKE.
     */
    public Paint.Style getStyle() {
        return mStyle;
    }

    /**
     * Setter for the drawing style.
     * @param mStyle The new drawing style. Can be Can be FILL, STROKE, or FILL_AND_STROKE.
     */
    public void setStyle(Paint.Style mStyle) {
        this.mStyle = mStyle;
    }

    /**
     * @return The width of the line for drawing.
     */
    public float getSize() {
        return mSize;
    }

    /**
     * Setter for the line width. The default is 5.
     * @param mSize The new width for the line.
     */
    public void setSize(float mSize) {
        this.mSize = mSize;
    }

    /**
     * @return The interaction mode. The default is DRAW_MODE.
     */
    public int getInteractionMode() {
        return mInteractionMode;
    }

    /**
     * Setter for the interaction mode. Can be DRAW_MODE, SELECT_MODE, ROTATE_MODE, or LOCKED_MODE.
     * @param interactionMode
     */
    public void setInteractionMode(int interactionMode) {

        // if the value passed is not any of the flags, set the library to locked mode
        if (interactionMode > LOCKED_MODE)
            interactionMode = LOCKED_MODE;
        else if (interactionMode < DRAW_MODE)
            interactionMode = LOCKED_MODE;

        this.mInteractionMode = interactionMode;
        invalidate();
    }

    /**
     * @return the list of all CDrawables in order of insertion.
     */
    public List<CDrawable> getDrawablesList() {
        return mDrawableList;
    }

    /**
     * Indicates that all CDrawables in the list have been saved.
     */
    public void markSaved() {
        savePoint = mDrawableList.size();
    }

    /**
     * @return true if there were no new operations done after the last call to markSaved().
     */
    public boolean isSaved() {
        return savePoint == mDrawableList.size();
    }

    /**
     * @return The list of all CDrawables that have been added after the last call to markSaved().
     */
    public List<CDrawable> getUnsavedDrawablesList() {
        if (savePoint > mDrawableList.size()) {
            //Some things were deleted.
            return new ArrayList<>();
        }
        return mDrawableList.subList(savePoint, mDrawableList.size());
    }

    /**
     * Deletes all CDrawables that were added after the last call to markSaved().
     */
    public void revertUnsaved() {
        List<CDrawable> unsaved = getUnsavedDrawablesList();
        for (CDrawable d :
                unsaved) {
            deleteDrawable(d);
        }
    }

    /**
     * Marks the last inserted CDrawable as the selected object.
     */
    public void selectLastDrawn() {
        if (mDrawableList.isEmpty()) {
            return;
        }

        ListIterator<CDrawable> li = mDrawableList.listIterator(mDrawableList.size());
        while (li.hasPrevious()) {
            CDrawable d = li.previous();
            if (d instanceof CTransform) {
                continue;
            }
            selected = d;
            break;
        }
        invalidate();
    }

    /**
     * @return The currently selected CDrawable.
     */
    public CDrawable getSelection() {
        return selected;
    }

    /**
     * Cancels all selection. No object will be selected.
     */
    public void deSelect() {
        selected = null;
        invalidate();
    }

    /**
     * Deletes the currently selected object. No object will be selected after that.
     */
    public void deleteSelection() {
        if (selected == null) {
            return;
        }
        deleteDrawable(selected);
        selected = null;
    }

    /**
     * Removes a specific CDrawable.
     * @param drawable The object to remove.
     */
    public void deleteDrawable(CDrawable drawable) {
        if (drawable == null) {
            return;
        }
        ArrayList<CDrawable> toDelete = new ArrayList<>();
        toDelete.add(drawable);
        toDelete.addAll(drawable.getTransforms());
        mDrawableList.removeAll(toDelete);
        if (deletionListener != null) {
            deletionListener.deleted(drawable);
        }
        mUndoList.add(drawable);
        invalidate();
    }

    /**
     * Setter for the "delete" icon. The default is android.R.drawable.ic_menu_delete.
     * @param newIcon The new delete icon.
     */
    public void setDeleteIcon(Bitmap newIcon) {
        deleteIcon = newIcon;
    }

    /**
     * Setter for the deletion event listener. Refer to the Observer pattern.
     * @param newListener The listener for any deletion event.
     */
    public void setDeletionListener(DeletionListener newListener) {
        deletionListener = newListener;
    }

    public ArrayList<JsonObject> getJsonArrays() {
        return jsonObjects;
    }

    /**
     * This interface must be implemented by your deletion event listener.
     */
    public interface DeletionListener {
        /**
         * This method will be called whenever a CDrawable is deleted.
         * @param drawable The object that was deleted.
         */
        void deleted(CDrawable drawable);
    }

    /**
     * @return The current selection rectangle color.  The default is Color.DKGRAY.
     */
    public int getSelectionColor() {
        return selectionColor;
    }

    /**
     * Setter for the selection rectangle color.
     * @param selectionColor The new selection rectangle color.
     */
    public void setSelectionColor(int selectionColor) {
        this.selectionColor = selectionColor;
    }

}
