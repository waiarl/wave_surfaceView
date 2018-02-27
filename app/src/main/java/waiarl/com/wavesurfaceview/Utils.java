package waiarl.com.wavesurfaceview;

import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

/**
 * Created by waiarl on 2018/2/27.
 */

public class Utils {
    public static int getColor(@NonNull int res) {
        return ContextCompat.getColor(MyApplication.getContext(), res);
    }

}
