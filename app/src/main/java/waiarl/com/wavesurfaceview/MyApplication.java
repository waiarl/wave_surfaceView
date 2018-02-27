package waiarl.com.wavesurfaceview;

import android.app.Application;
import android.content.Context;

/**
 * Created by waiarl on 2018/2/27.
 */

public class MyApplication extends Application {
    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }
}
