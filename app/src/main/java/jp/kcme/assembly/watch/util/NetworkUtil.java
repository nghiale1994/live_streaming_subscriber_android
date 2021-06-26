package jp.kcme.assembly.watch.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Network系
 */
public class NetworkUtil {
    public final static String TAG = "NetworkUtil";

    /**
     * Network利用を確認
     * @return trueがwifiまたはmobileの通信が可能でfalseがいずれも通信不可,機内モードはnull
     */
    public static boolean isOnline(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
}
