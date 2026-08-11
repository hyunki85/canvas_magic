package com.h2play.canvas_magic;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.facebook.stetho.Stetho;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.h2play.canvas_magic.features.menu.MenuActivity;
import com.h2play.canvas_magic.util.Analytics;
import com.h2play.canvas_magic.util.AppOpenAdManager;
import com.h2play.canvas_magic.util.FileUtil;
import com.h2play.canvas_magic.util.ReminderScheduler;
import com.singhajit.sherlock.core.Sherlock;
//import com.tspoon.traceur.Traceur;

import com.h2play.canvas_magic.injection.component.AppComponent;
import com.h2play.canvas_magic.injection.component.DaggerAppComponent;
import com.h2play.canvas_magic.injection.module.AppModule;
import com.h2play.canvas_magic.injection.module.NetworkModule;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import timber.log.Timber;

public class MvpStarterApplication extends Application {

    private AppComponent appComponent;

    public static MvpStarterApplication get(Context context) {
        return (MvpStarterApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase first - making sure this happens before anything else
        com.google.firebase.FirebaseApp.initializeApp(getApplicationContext());

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            Stetho.initializeWithDefaults(this);
            //LeakCanary.install(this);
            Sherlock.init(this);
        }

        // Rest of the initialization code
        MobileAds.initialize(this);
        RequestConfiguration aa = new RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("73431CF5ECC2ECB14BBF5CFE4DD450C0")).build();
        MobileAds.setRequestConfiguration(aa);

        // Schedule reminder notifications
        ReminderScheduler.schedule(this);

        // Moving this after Firebase is initialized
        if(getComponent().dataManager().getShapeList().size() <= 0) {
            initializeDefaultShapePacks();
        }

        // 계측 초기화 + 세션 시작 시점의 사용자 상태를 속성으로 고정.
        // 이 두 속성이 있어야 GA4에서 "튜토리얼 완료자 vs 미완료자"의
        // 재방문율을 나눠 볼 수 있다.
        Analytics.init(this);
        Analytics.setTutorialDone(this, !getComponent().dataManager().needGuide());
        Analytics.setTrickCount(this, getComponent().dataManager().getTrickCount());
    }
    
    /**
     * 기본 도형 팩을 초기화하는 메서드
     * 숫자 도형 팩을 앱 최초 실행 시 추가합니다.
     */
    private void initializeDefaultShapePacks() {
        // 숫자 도형 팩
        String numberName = getString(R.string.default_shape_set_name);
        getComponent().dataManager().addFileList(numberName, "file.txt", 9);
        copyRawToFile(R.raw.base_pattern, "file.txt");
    }
    
    /**
     * raw 리소스를 내부 저장소 파일로 복사하는 헬퍼 메서드
     * @param rawResourceId raw 리소스 ID
     * @param fileName 저장할 파일 이름
     */
    private void copyRawToFile(int rawResourceId, String fileName) {
        File file = new File(getFilesDir(), fileName);
        try (InputStream inputStream = getResources().openRawResource(rawResourceId);
             FileOutputStream outputStream = new FileOutputStream(file)) {
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } catch (IOException e) {
            Timber.e(e, "Failed to copy raw resource to file: %s", fileName);
        }
    }

    public AppComponent getComponent() {
        if (appComponent == null) {
            appComponent = DaggerAppComponent.builder()
                    .networkModule(new NetworkModule(this))
                    .appModule(new AppModule(this))
                    .build();
        }
        return appComponent;
    }

    // Needed to replace the component with a test specific one
    public void setComponent(AppComponent appComponent) {
        this.appComponent = appComponent;
    }
}
