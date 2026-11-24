package com.h2play.canvas_magic.features.packs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.MvpStarterApplication;
import com.h2play.canvas_magic.data.model.PackRef;
import com.h2play.canvas_magic.features.main.MainActivity;
import com.h2play.canvas_magic.util.DrawingArtworkView;
import com.h2play.canvas_magic.util.DrawingCatalog;
import com.h2play.canvas_magic.util.DrawingExperience;

public final class DrawingPackActivity extends AppCompatActivity {
    private int pack;
    public static Intent intent(Context c,String id) {
        return new Intent(c,DrawingPackActivity.class).putExtra(DrawingExperience.EXTRA_PACK,id);
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        pack=DrawingCatalog.index(getIntent().getStringExtra(DrawingExperience.EXTRA_PACK));
        if(pack<0) { finish(); return; }
        setContentView(R.layout.activity_drawing_pack);
        ((TextView)findViewById(R.id.drawing_pack_title)).setText(
                getResources().getStringArray(R.array.drawing_pack_titles)[pack]);
        ((TextView)findViewById(R.id.drawing_pack_tip)).setText(
                getString(R.string.drawing_pack_description));
        int[] previews={R.id.drawing_pack_art_1,R.id.drawing_pack_art_2,R.id.drawing_pack_art_3};
        int[] labels={R.id.drawing_pack_label_1,R.id.drawing_pack_label_2,R.id.drawing_pack_label_3};
        String[] names=getResources().getStringArray(R.array.drawing_pack_art_names);
        String[] positions=getResources().getStringArray(R.array.drawing_pack_positions);
        for(int i=0;i<3;i++) {
            ((DrawingArtworkView)findViewById(previews[i])).show(pack,i);
            ((TextView)findViewById(labels[i])).setText(getString(R.string.drawing_pack_position_label, positions[i], names[pack*3+i]));
        }
        findViewById(R.id.drawing_pack_back).setOnClickListener(v->finish());
        findViewById(R.id.drawing_pack_replay).setOnClickListener(v-> {
            for(int id:previews) ((DrawingArtworkView)findViewById(id)).replay();
        });
        findViewById(R.id.drawing_pack_start).setOnClickListener(v-> {
            DrawingExperience.log(this,"drawing_set_start",DrawingCatalog.id(pack));
            MvpStarterApplication.get(this).getComponent().dataManager()
                    .rememberPack(PackRef.builtIn(DrawingCatalog.id(pack)));
            startActivity(MainActivity.getStartIntent(this,0)
                    .putExtra(DrawingExperience.EXTRA_PACK,DrawingCatalog.id(pack))
                    .putExtra(MainActivity.EXTRA_ENTRY_POINT, "pack_detail"));
            finish();
        });
        if(state==null) DrawingExperience.log(this,"drawing_set_open",DrawingCatalog.id(pack));
    }

}
