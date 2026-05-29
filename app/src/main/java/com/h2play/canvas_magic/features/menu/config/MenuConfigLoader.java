package com.h2play.canvas_magic.features.menu.config;

import android.content.Context;

import com.google.gson.Gson;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 메뉴 설정을 로드하는 유틸 (현재는 assets/menu_config.json 에서 로드)
 * 추후 Firebase Remote Config 또는 Firestore로 대체 가능
 */
public class MenuConfigLoader {
    private static final String ASSET_PATH = "menu_config.json";
    private static final String RC_KEY = "menu_config_json";

    public interface ConfigUpdateListener {
        void onConfigLoaded(MenuConfig config);
    }

    public static MenuConfig loadFromAssets(Context context) {
        try (InputStream is = context.getAssets().open(ASSET_PATH);
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return new Gson().fromJson(sb.toString(), MenuConfig.class);
        } catch (IOException e) {
            return null; // 실패 시 null 반환하여 폴백 UI 사용
        }
    }

    /**
     * Remote Config에서 menu_config_json 키를 가져와 파싱. 실패 시 null
     */
    public static MenuConfig loadFromRemoteConfig(Context context, boolean developerMode) {
        return loadFromRemoteConfig(context, developerMode, null);
    }

    /**
     * Remote Config에서 캐시값을 즉시 반환하고, 최신값 fetch 완료 시 listener로 다시 전달합니다.
     */
    public static MenuConfig loadFromRemoteConfig(Context context, boolean developerMode, ConfigUpdateListener listener) {
        FirebaseRemoteConfig rc = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings settings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(developerMode ? 0 : 300)
                .build();
        rc.setConfigSettingsAsync(settings);

        fetchLatest(rc, listener);

        try {
            String cached = rc.getString(RC_KEY);
            if (cached != null && !cached.isEmpty()) {
                return new Gson().fromJson(cached, MenuConfig.class);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void fetchLatest(FirebaseRemoteConfig rc, ConfigUpdateListener listener) {
        rc.fetchAndActivate().addOnSuccessListener(updated -> {
            if (listener == null) {
                return;
            }
            try {
                String latest = rc.getString(RC_KEY);
                if (latest != null && !latest.isEmpty()) {
                    listener.onConfigLoaded(new Gson().fromJson(latest, MenuConfig.class));
                }
            } catch (Exception ignored) {}
        });
    }
}
