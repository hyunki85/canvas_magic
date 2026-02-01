package com.h2play.canvas_magic.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.TimeUnit;

public class TopicManager {

    private static final String TOPIC_ALL = "mymind";
    private static final String TOPIC_NEW = "mymind_new";
    private static final String TOPIC_DORMANT = "mymind_dormant";
    private static final String TOPIC_ACTIVE = "mymind_active";

    private static final long NEW_USER_THRESHOLD_MS = TimeUnit.DAYS.toMillis(7);
    private static final long DORMANT_THRESHOLD_MS = TimeUnit.DAYS.toMillis(3);

    public static void updateTopics(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                ReminderWorker.PREF_NAME, Context.MODE_PRIVATE);

        long installTime = prefs.getLong(ReminderWorker.KEY_INSTALL_TIME, 0);
        long now = System.currentTimeMillis();

        FirebaseMessaging fm = FirebaseMessaging.getInstance();

        // 전체 토픽은 항상 구독
        fm.subscribeToTopic(TOPIC_ALL);

        // 신규 사용자 토픽
        if (installTime > 0 && (now - installTime) < NEW_USER_THRESHOLD_MS) {
            fm.subscribeToTopic(TOPIC_NEW);
        } else {
            fm.unsubscribeFromTopic(TOPIC_NEW);
        }

        // 활성/휴면 토픽: 앱을 연 시점이므로 항상 활성으로 전환
        fm.subscribeToTopic(TOPIC_ACTIVE);
        fm.unsubscribeFromTopic(TOPIC_DORMANT);
    }
}
