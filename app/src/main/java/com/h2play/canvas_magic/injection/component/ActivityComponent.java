package com.h2play.canvas_magic.injection.component;

import dagger.Subcomponent;
import com.h2play.canvas_magic.features.detail.DetailActivity;
import com.h2play.canvas_magic.features.list.ShapeListActivity;
import com.h2play.canvas_magic.features.main.MainActivity;
import com.h2play.canvas_magic.features.make.MakeActivity;
import com.h2play.canvas_magic.features.menu.MenuActivity;
import com.h2play.canvas_magic.features.pincode.PinActivity;
import com.h2play.canvas_magic.features.preview.PreviewActivity;
import com.h2play.canvas_magic.features.share.ShareActivity;
import com.h2play.canvas_magic.injection.PerActivity;
import com.h2play.canvas_magic.injection.module.ActivityModule;

@PerActivity
@Subcomponent(modules = ActivityModule.class)
public interface ActivityComponent {

    void inject(MainActivity mainActivity);

    void inject(DetailActivity detailActivity);

    void inject(MenuActivity menuActivity);

    void inject(PinActivity pinActivity);

    void inject(MakeActivity makeActivity);

    void inject(ShapeListActivity listActivity);

    void inject(ShareActivity shareActivity);

    void inject(PreviewActivity previewActivity);
}
