package com.h2play.canvas_magic.common.injection.component;

import javax.inject.Singleton;

import dagger.Component;
import com.h2play.canvas_magic.common.injection.module.ApplicationTestModule;
import com.h2play.canvas_magic.injection.component.AppComponent;

@Singleton
@Component(modules = ApplicationTestModule.class)
public interface TestComponent extends AppComponent {
}
