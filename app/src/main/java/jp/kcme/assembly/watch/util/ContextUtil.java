package jp.kcme.assembly.watch.util;

import android.app.Application;

/**
 * Contextを渡すクラス
 */
public class ContextUtil extends Application {
    private static ContextUtil instance = null;
    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
    }

    public static ContextUtil getInstance() {
        return instance;
    }
}

