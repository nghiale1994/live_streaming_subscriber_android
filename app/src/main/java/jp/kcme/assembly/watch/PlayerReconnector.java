package jp.kcme.assembly.watch;

import android.util.Log;

import org.videolan.libvlc.MediaPlayer;

import java.util.Timer;
import java.util.TimerTask;

/**
 * This class only manages the reconnection of
 * 1 player at a time
 */
public class PlayerReconnector {
    private static PlayerReconnector INSTANCE;
    private MediaPlayer player;

    private Timer reconnectTimer;
    private final int RECONNECT_DELAY_IN_MS = 5000;
    private final int RECONNECT_INTERVAL_IN_MS = 5000;

    public static PlayerReconnector getInstance() {
        if (INSTANCE == null) {
            synchronized (PlayerReconnector.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PlayerReconnector();
                }
            }
        }
        return INSTANCE;
    }

    private boolean hasValidPlayer() {
        if (player == null) {
            reset();
        }
        return player == null;
    }

    private void reset() {
        player = null;
        if (reconnectTimer != null) {
            reconnectTimer.cancel();
            reconnectTimer = null;
        }
    }

    /**
     * プレイヤーの再生スケジュールを立てる
     * @param player nullの場合、何もしない
     */
    public void scheduleReconnect(MediaPlayer player) {
        Log.i(AppUtils.get().tag(), "scheduleReconnect every " + RECONNECT_INTERVAL_IN_MS + " ms for player: " + player);
        this.player = player;

        if (!hasValidPlayer()) {
            Log.w(AppUtils.get().tag(), "No player to scheduleReconnect");
            return;
        }

        if (reconnectTimer == null) {
            reconnectTimer = new Timer();
            reconnectTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    doReconnectPlayer();
                }
            }, RECONNECT_DELAY_IN_MS, RECONNECT_INTERVAL_IN_MS);
        } else {
            Log.i(AppUtils.get().tag(), "Already scheduled for player: " + player);
        }
    }

    private void doReconnectPlayer() {
        Log.d(AppUtils.get().tag(), "doReconnectPlayer: " + player);

        if (hasValidPlayer()) {
            Log.w(AppUtils.get().tag(), "No player to reconnect");
            return;
        }

        if (!player.isPlaying()) {
            Log.d(AppUtils.get().tag(), "player's state: " + player.getPlayerState());
            Log.i(AppUtils.get().tag(), "need replay: " + !player.isPlaying());
            player.stop();
            player.play();
        }
    }

    public void stopReconnect() {
        Log.d(AppUtils.get().tag(), "stopReconnect for player: " + player);
        reset();
    }
}
