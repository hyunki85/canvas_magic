package com.h2play.canvas_magic;

import android.app.Application;
import android.content.Context;

import com.facebook.stetho.Stetho;
import com.google.android.gms.ads.MobileAds;
import com.h2play.canvas_magic.util.FileUtil;
import com.singhajit.sherlock.core.Sherlock;
import com.squareup.leakcanary.LeakCanary;
import com.tspoon.traceur.Traceur;

import com.h2play.canvas_magic.injection.component.AppComponent;
import com.h2play.canvas_magic.injection.component.DaggerAppComponent;
import com.h2play.canvas_magic.injection.module.AppModule;
import com.h2play.canvas_magic.injection.module.NetworkModule;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import timber.log.Timber;

public class MvpStarterApplication extends Application {

    private AppComponent appComponent;

    public static MvpStarterApplication get(Context context) {
        return (MvpStarterApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            Stetho.initializeWithDefaults(this);
            LeakCanary.install(this);
            Sherlock.init(this);
            Traceur.enableLogging();
        }

        MobileAds.initialize(this );

        if(getComponent().dataManager().getShapeList().size() <= 0) {

            String name = "Number";
            if(Locale.getDefault().getDisplayLanguage().equalsIgnoreCase(Locale.KOREAN.getDisplayLanguage())) {
                name = "숫자";
            }

            getComponent().dataManager().addFileList(name,"file.txt");
            File fl = new File(getFilesDir(), "file.txt");

            InputStream inputStream = getResources().openRawResource(R.raw.base_pattern);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            try {
                FileOutputStream outputStream = new FileOutputStream(fl);
                int i = inputStream.read();
                while (i != -1) {
                    byteArrayOutputStream.write(i);
                    outputStream.write(i);
                    i = inputStream.read();
                }

                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

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
