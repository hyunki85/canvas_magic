package com.h2play.canvas_magic.util;

import android.content.Context;

import com.google.firebase.messaging.FirebaseMessaging;

public class TopicManager {

    public static final String TOPIC_NEW_DRAWING_SET = "canvas_new_drawing_set";
    private static final String TOPIC_ALL = "mymind";
    private static final String TOPIC_NEW = "mymind_new";
    private static final String TOPIC_DORMANT = "mymind_dormant";
    private static final String TOPIC_ACTIVE = "mymind_active";

    public static void updateTopics(Context context) {
        FirebaseMessaging fm = FirebaseMessaging.getInstance();
        // 1.2.11 retires broad behavioral messaging. Only explicit new-drawing opt-in remains.
        fm.unsubscribeFromTopic(TOPIC_ALL);
        fm.unsubscribeFromTopic(TOPIC_NEW);
        fm.unsubscribeFromTopic(TOPIC_DORMANT);
        fm.unsubscribeFromTopic(TOPIC_ACTIVE);
        if (NotificationPermissionManager.isOptedIn(context)) {
            fm.subscribeToTopic(TOPIC_NEW_DRAWING_SET);
        } else {
            fm.unsubscribeFromTopic(TOPIC_NEW_DRAWING_SET);
        }
    }

    /** WorkManager에서 3일 이상 미방문 상태를 확인했을 때 호출한다. */
    public static void markDormant() {
        FirebaseMessaging fm = FirebaseMessaging.getInstance();
        fm.unsubscribeFromTopic(TOPIC_DORMANT);
        fm.unsubscribeFromTopic(TOPIC_ACTIVE);
    }
}
