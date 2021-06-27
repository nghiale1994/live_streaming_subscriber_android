package jp.kcme.assembly.watch;

import android.util.Log;

import org.videolan.libvlc.MediaPlayer;

import java.util.Timer;
import java.util.TimerTask;

public class PlayerManager {
    private static PlayerManager INSTANCE;
    private MediaPlayer player;

    private Timer reconnectTimer;
    private final int RECONNECT_DELAY_IN_MS = 5000;
    private final int RECONNECT_INTERVAL_IN_MS = 5000;

    public static PlayerManager getInstance() {
        if (INSTANCE == null) {
            synchronized (PlayerManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PlayerManager();
                }
            }
        }
        return INSTANCE;
    }

    public void scheduleReconnect(MediaPlayer player) {
        Log.d(AppUtils.get().tag(), "scheduleReconnect every " + RECONNECT_INTERVAL_IN_MS + " ms for player: " + player);
        this.player = player;

        if( reconnectTimer == null){
            reconnectTimer = new Timer();
        }

        reconnectTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                doReconnectPlayer();
            }
        }, RECONNECT_DELAY_IN_MS, RECONNECT_INTERVAL_IN_MS);
    }

    private void doReconnectPlayer() {
        Log.d(AppUtils.get().tag(), "doReconnectPlayer: " + player);

        if (player == null) {
            Log.w(AppUtils.get().tag(), "No player to reconnect");
            return;
        }

        if (!player.isPlaying()){
            Log.d(AppUtils.get().tag(), "player's state: " + player.getPlayerState());
            Log.i(AppUtils.get().tag(), "need replay: " + !player.isPlaying());
            player.stop();
            player.play();
        }
    }

    public void stopReconnect() {
        Log.d(AppUtils.get().tag(), "stopReconnect for player: " + player);
        if (reconnectTimer != null) {
            reconnectTimer.cancel();
            reconnectTimer = null;
        }
        player = null;
    }
}
