package jp.kcme.assembly.watch;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class RtmpVlcPlayerForLiveStreamActivity extends CommonActivity implements IVLCVout.Callback {
    public final static String TAG = "LiveStreamActivity";

    private String mFilePath;
    private SurfaceView mSurface;
    private SurfaceHolder holder;
    private LibVLC libvlc;
    private static MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;

    private boolean showButton;
    private TextView nowTimeText;
    private View cloth;

    /**
     * 再生中か否か
     * キー名 playing
     * //TODO 今は状態を保存してるだけで特に使ってない
     */
    private SharedPreferences playingPrefer;
    /**
     * 取得したURL
     * キー名 url
     * //TODO 今は状態を保存してるだけで特に使ってない
     */
    private SharedPreferences urlPrefer;

    private AlertDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.frame_rtmp_vlc_player_for_livestream);

        mFilePath = Properties.API_TEST_RTMP_PREFIX + getIntent().getStringExtra("channelId");

        playingPrefer = getSharedPreferences("playing", MODE_PRIVATE);
        SharedPreferences.Editor editor = playingPrefer.edit();
        editor.putBoolean("playing",true);
        editor.apply();

        urlPrefer = getSharedPreferences("url", MODE_PRIVATE);
        SharedPreferences.Editor editor2 = urlPrefer.edit();
        editor2.putString("url",mFilePath);
        editor2.apply();

        Log.d(TAG, "Playing: " + mFilePath);
        mSurface = (SurfaceView) findViewById(R.id.live_surface);
        holder = mSurface.getHolder();

        cloth = findViewById(R.id.live_cloth);
        cloth.setAlpha(0);

        ImageView exitButton = findViewById(R.id.live_exit);
        exitButton.setVisibility(View.GONE);

        nowTimeText = findViewById(R.id.live_nowTime);
        nowTimeText.setVisibility(View.GONE);

        showButton = false;

        /**
         * Viewクリックでボタンの表示非表示切り替え
         * ただし配信のときはmaxTimeTextとseekBarとplayButtonは非表示のまま
         */
        cloth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, String.valueOf(exitButton.getVisibility()));
                if (showButton) {
                    Log.d(TAG, String.valueOf(showButton));
                    showButton = false;
                    exitButton.setVisibility(View.GONE);
                    nowTimeText.setVisibility(View.GONE);
                    cloth.setAlpha(0);
                } else {
                    Log.d(TAG, String.valueOf(showButton));
                    showButton = true;
                    exitButton.setVisibility(View.VISIBLE);
                    //nowTimeText.setVisibility(View.VISIBLE);
                    cloth.setAlpha(0.5f);
                }
            }
        });

        /**
         * 終了ボタン
         */
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (exitButton.getVisibility() == View.VISIBLE) {
                    releasePlayer();
                }
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    protected void onResume() {
        super.onResume();
        createPlayer(mFilePath);
        scheduleReconnectPLayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    /**
     * Used to set size for SurfaceView
     *
     * @param width
     * @param height
     */
    private void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        if (holder == null || mSurface == null)
            return;

        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        holder.setFixedSize(mVideoWidth, mVideoHeight);
        LayoutParams lp = mSurface.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mSurface.setLayoutParams(lp);
        mSurface.invalidate();
    }

    /**
     * Creates MediaPlayer and plays video
     *
     * @param media
     */
    private void createPlayer(String media) {
        releasePlayer();
        try {
//            if (media.length() > 0) {
//                Toast toast = Toast.makeText(this, media, Toast.LENGTH_LONG);
//                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
//                        0);
//                toast.show();
//            }

            // Create LibVLC
            // TODO: make this more robust, and sync with audio demo
            ArrayList<String> options = new ArrayList<String>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            options.add("-vvv"); // verbosity
            libvlc = new LibVLC(this, options);
            holder.setKeepScreenOn(true);

            // Creating media player
            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);

            // Seting up video output
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(mSurface);
            //vout.setSubtitlesView(mSurfaceSubtitles);
            vout.addCallback(this);
            vout.attachViews();

            Media m = new Media(libvlc, Uri.parse(media));
            mMediaPlayer.setMedia(m);
            mMediaPlayer.play();
        } catch (Exception e) {
            Toast.makeText(this, "Error in creating player!", Toast
                    .LENGTH_LONG).show();
        }
    }

    private void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        holder = null;
        libvlc.release();
        libvlc = null;

        mVideoWidth = 0;
        mVideoHeight = 0;

        playingPrefer = getSharedPreferences("playing", MODE_PRIVATE);
        SharedPreferences.Editor editor = playingPrefer.edit();
        editor.putBoolean("playing",false);
        editor.apply();

        finish();
    }

    /**
     * プレイヤーの再度再生のスケジュールを立てる
     */
    private void scheduleReconnectPLayer() {
        PlayerReconnector.getInstance().scheduleReconnect(mMediaPlayer);
        showReconnectingDialog();
    }

    private void stopReconnectPLayer() {
        PlayerReconnector.getInstance().stopReconnect();
        hideReconnectingDialog();
    }

    /**
     * プレイヤーの再度再生をリトライする度に「接続中」ダイヤログを表示する
     */
    // TODO: 6/27/2021  ダイヤログのレイアウトを変更すること
    private void showReconnectingDialog() {
        if (dialog == null) {
            final ProgressBar progressBar = new ProgressBar(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(5,5,5,5);
            lp.gravity = Gravity.START;
            progressBar.setLayoutParams(lp);
            progressBar.setPadding(5,5,5,5);

            dialog = new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.dialog_reconnecting))
                    .setView(progressBar)
                    .setCancelable(true)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            Log.d(AppUtils.get().tag(), "Hide reconnecting dialog");
                        }
                    })
                    .create();
        }

        Log.d(AppUtils.get().tag(), "Show reconnecting dialog");
        dialog.show();
    }

    private void hideReconnectingDialog() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * 主に配信側で配信が停止されたときなど異常終了時に呼ばれるkillProcessを実行するだけのメソッド
     */
    private static void restartApp() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * Registering callbacks
     */
    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

    @Override
    public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0)
            return;

        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    public void onSurfacesCreated(IVLCVout vout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {

    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {
        Log.e(TAG, "Error with hardware acceleration");
        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
        this.releasePlayer();
        restartApp();
    }

    private class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<RtmpVlcPlayerForLiveStreamActivity> mOwner;

        public MyPlayerListener(RtmpVlcPlayerForLiveStreamActivity owner) {
            mOwner = new WeakReference<RtmpVlcPlayerForLiveStreamActivity>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            RtmpVlcPlayerForLiveStreamActivity player = mOwner.get();

            Log.d(TAG, "event's type: " + Integer.toHexString(event.type));

            switch (event.type) {
                case MediaPlayer.Event.EncounteredError:
                    Log.w(TAG, "MediaPlayer.Event.EncounteredError");
                    player.scheduleReconnectPLayer();
                    break;
                case MediaPlayer.Event.EndReached:
                    Log.w(TAG, "MediaPlayer.Event.EndReached");
                    player.scheduleReconnectPLayer();
                    break;
                case MediaPlayer.Event.Playing:
                    Log.d(TAG, "MediaPlayer.Event.Playing");
                    player.stopReconnectPLayer();
                    break;
                case MediaPlayer.Event.Paused:
                    Log.d(TAG, "MediaPlayer.Event.Paused");
                    break;
                case MediaPlayer.Event.Stopped:
                    Log.d(TAG, "MediaPlayer.Event.Stopped");
                    break;
                case MediaPlayer.Event.TimeChanged:
                    Log.d(TAG, "MediaPlayer.Event.TimeChanged");
                    break;
                default:
                    break;
            }
        }
    }
}