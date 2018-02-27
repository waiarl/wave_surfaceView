package waiarl.com.wavesurfaceview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RelativeLayout;

import waiarl.com.wavesurfaceview.view.SoundSurfaceView;

public class MainActivity extends AppCompatActivity {
    private SoundSurfaceView sound_surface;
    private RelativeLayout rel;
    private View bt_stop;
    private View bt_start;
    private SoundSurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById();
    }

    private void findViewById() {
        rel= (RelativeLayout) findViewById(R.id.rel);
        surfaceView=new SoundSurfaceView(this);
        rel.addView(surfaceView, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        bt_stop=findViewById(R.id.bt_stop);
        bt_start=findViewById(R.id.bt_start);
        bt_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });
        bt_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });
    }

    private void stop() {
        surfaceView.stop();
    }

    private void start() {
        surfaceView.initView(40);
        surfaceView.start();
    }
}
