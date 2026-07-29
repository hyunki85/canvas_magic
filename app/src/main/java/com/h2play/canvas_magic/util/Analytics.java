package com.h2play.canvas_magic.util;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * 리텐션 퍼널 계측 헬퍼.
 *
 * 이 앱의 활성화 지표는 "튜토리얼 완료"가 아니라 "실제로 마술을 1회 수행"이다.
 * 따라서 계측의 목적은 두 가지다.
 *   1) 첫 수행(TRICK_PERFORMED)까지 어느 단계에서 이탈하는가
 *   2) 첫 수행 이후 재수행이 일어나는가
 *
 * 이벤트/파라미터 이름은 GA4 제약을 따른다 (snake_case, 이벤트 40자·파라미터 40자 이내).
 */
public final class Analytics {

    // ---- 튜토리얼 퍼널 ----
    public static final String TUTORIAL_START = "tutorial_start";
    public static final String TUTORIAL_STEP = "tutorial_step";
    public static final String TUTORIAL_EXIT = "tutorial_exit";
    public static final String TUTORIAL_QUIZ = "tutorial_quiz";
    public static final String TUTORIAL_GUIDE_SHOWN = "tutorial_guide_shown";
    public static final String TUTORIAL_LONGPRESS_DONE = "tutorial_longpress_done";
    public static final String TUTORIAL_PRACTICE = "tutorial_practice";
    public static final String TUTORIAL_COMPLETE = "tutorial_complete";

    // ---- 핵심 활성화 / 리텐션 ----
    public static final String TRICK_PERFORMED = "trick_performed";
    public static final String PACK_OPENED = "pack_opened";

    // ---- 깊이 있는 사용 (콘텐츠 제작 / 커뮤니티) ----
    public static final String SHAPE_CREATED = "shape_created";
    public static final String COMMUNITY_OPEN = "community_open";
    public static final String COMMUNITY_UPLOAD = "community_upload";
    public static final String MENU_ACTION = "menu_action";

    // ---- 사용자 속성 ----
    private static final String PROP_TRICK_COUNT = "trick_count_bucket";
    private static final String PROP_TUTORIAL_DONE = "tutorial_done";

    private static FirebaseAnalytics fa;

    private Analytics() {
    }

    public static void init(Context context) {
        if (fa == null) {
            fa = FirebaseAnalytics.getInstance(context.getApplicationContext());
        }
    }

    private static FirebaseAnalytics get(Context context) {
        init(context);
        return fa;
    }

    public static void log(Context context, String event) {
        log(context, event, null);
    }

    public static void log(Context context, String event, Bundle params) {
        if (context == null) return;
        try {
            get(context).logEvent(event, params == null ? new Bundle() : params);
        } catch (Exception e) {
            // 계측 실패가 기능을 막아서는 안 된다
        }
    }

    /** 파라미터 1개짜리 이벤트 단축 */
    public static void log(Context context, String event, String key, String value) {
        Bundle b = new Bundle();
        b.putString(key, value);
        log(context, event, b);
    }

    public static void log(Context context, String event, String key, long value) {
        Bundle b = new Bundle();
        b.putLong(key, value);
        log(context, event, b);
    }

    public static void setUserProperty(Context context, String name, String value) {
        if (context == null) return;
        try {
            get(context).setUserProperty(name, value);
        } catch (Exception e) {
            // ignore
        }
    }

    public static void setTutorialDone(Context context, boolean done) {
        setUserProperty(context, PROP_TUTORIAL_DONE, String.valueOf(done));
    }

    /**
     * 누적 수행 횟수를 코호트로 묶어 사용자 속성에 기록한다.
     * 원시 숫자를 그대로 넣으면 GA4에서 세그먼트를 만들기 어렵다.
     */
    public static void setTrickCount(Context context, int count) {
        setUserProperty(context, PROP_TRICK_COUNT, bucket(count));
    }

    private static String bucket(int count) {
        if (count <= 0) return "0";
        if (count == 1) return "1";
        if (count <= 5) return "2_5";
        if (count <= 20) return "6_20";
        return "21_plus";
    }
}
