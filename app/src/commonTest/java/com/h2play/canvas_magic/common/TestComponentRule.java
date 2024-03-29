package com.h2play.canvas_magic.common;

import android.content.Context;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.h2play.canvas_magic.MvpStarterApplication;
import com.h2play.canvas_magic.common.injection.component.DaggerTestComponent;
import com.h2play.canvas_magic.common.injection.component.TestComponent;
import com.h2play.canvas_magic.common.injection.module.ApplicationTestModule;
import com.h2play.canvas_magic.data.DataManager;

/**
 * Test rule that creates and sets a Dagger TestComponent into the application overriding the
 * existing application component. Use this rule in your test case in order for the app to use mock
 * dependencies. It also exposes some of the dependencies so they can be easily accessed from the
 * tests, e.g. to stub mocks etc.
 */
public class TestComponentRule implements TestRule {

    private final TestComponent testComponent;
    private final Context context;

    public TestComponentRule(Context context) {
        this.context = context;
        MvpStarterApplication application = MvpStarterApplication.get(context);
        testComponent =
                DaggerTestComponent.builder()
                        .applicationTestModule(new ApplicationTestModule(application))
                        .build();
    }

    public TestComponent getTestComponent() {
        return testComponent;
    }

    public Context getContext() {
        return context;
    }

    public DataManager getMockApiManager() {
        return testComponent.dataManager();
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                MvpStarterApplication application = MvpStarterApplication.get(context);
                application.setComponent(testComponent);
                base.evaluate();
                application.setComponent(null);
            }
        };
    }
}
