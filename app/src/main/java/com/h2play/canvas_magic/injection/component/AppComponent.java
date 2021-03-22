package com.h2play.canvas_magic.injection.component;

import android.app.Application;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;
import com.h2play.canvas_magic.data.DataManager;
import com.h2play.canvas_magic.data.local.PreferencesHelper;
import com.h2play.canvas_magic.injection.ApplicationContext;
import com.h2play.canvas_magic.injection.module.AppModule;

@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {

    @ApplicationContext
    Context context();

    Application application();

    DataManager dataManager();

    PreferencesHelper preferenceHelper();
}
