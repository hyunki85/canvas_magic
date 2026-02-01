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
        FirebaseRemoteConfig rc = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings settings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(developerMode ? 0 : 3600)
                .build();
        rc.setConfigSettingsAsync(settings);

        try {
            // 동기 fetch는 지원되지 않아 즉시 값을 반환하면 이전 캐시값을 사용하게 됨.
            // 앱 초기 구동에서는 우선 캐시를 읽고, 비동기로 fetch하여 다음 진입 때 최신화.
            String cached = rc.getString(RC_KEY);
            if (cached != null && !cached.isEmpty()) {
                return new Gson().fromJson(cached, MenuConfig.class);
            }

            // 비동기 fetch 시도 (결과는 다음 실행에 반영)
            rc.fetchAndActivate();
        } catch (Exception ignored) {}
        return null;
    }
}
