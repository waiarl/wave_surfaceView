package waiarl.com.wavesurfaceview.imp;

import android.view.View;

/**
 * Created by waiarl on 2018/2/1.
 */

public interface WaveViewImp<T extends View> {
    T start();

    T stop();

    T destory();

    T initView(double soundDb);

    T getView();

}
