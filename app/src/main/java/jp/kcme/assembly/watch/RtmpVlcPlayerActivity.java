package jp.kcme.assembly.watch;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RtmpVlcPlayerActivity extends CommonActivity implements IVLCVout.Callback {
    public final static String TAG = "RtmpVlcPlayerActivity";

    private String mFilePath;
    private SurfaceView mSurface;
    private int mVideoWidth;
    private int mVideoHeight;
    private static SurfaceHolder holder;
    private static LibVLC libvlc;
    private static MediaPlayer mMediaPlayer = null;

    /**
     * 動画が再生中か否か
     * キー名 playing
     * デフォルト値 false
     */
    private SharedPreferences playingPrefer;
    /**
     * 取得したURL
     * キー名 url
     * デフォルト値 ""
     * //TODO 今はURLを保存してるだけで特に使ってない
     */
    private SharedPreferences urlPrefer;
    /**
     * SeekBarの値を一時保存するのに使用
     * シーク中に値を保存してシークバーから手を話したら値を読み込んでsetTimeするだけ
     * なお再生開始時には0を挿入している
     * キー名 progress
     * デフォルト値 0
     */
    private SharedPreferences progressPrefer;
    /**
     * アプリ離脱復帰の判断に使用
     * キー名 pause
     * デフォルト値 false
     */
    private SharedPreferences pausePrefer;

    private boolean showButton;
    private boolean playButtonStatus = true;
    private SeekBar seekBar;
    private TextView nowTimeText;
    private TextView slashText;
    private View cloth;

    private AlertDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.frame_rtmp_vlc_player);

        //TODO URLを修正する
        mFilePath = Properties.API_TEST_MP4_PREFIX + "7d30050fb44be9a8f96059e9e9ceebc3.mp4";

        urlPrefer = getSharedPreferences("url", MODE_PRIVATE);
        SharedPreferences.Editor editor = urlPrefer.edit();
        editor.putString("url",mFilePath);
        editor.apply();

        Log.d(AppUtils.get().tag(), "Playing: " + mFilePath);
        mSurface = (SurfaceView) findViewById(R.id.surface);
        mSurface.setZOrderOnTop(false);
        holder = mSurface.getHolder();
        holder.setFormat(PixelFormat.TRANSLUCENT);

        cloth = findViewById(R.id.cloth);
        cloth.setAlpha(0);

        ImageView exitButton = findViewById(R.id.exit);
        exitButton.setVisibility(View.GONE);

        ImageView playButton = findViewById(R.id.play);
        playButton.setVisibility(View.GONE);

        nowTimeText = findViewById(R.id.nowTime);
        nowTimeText.setVisibility(View.GONE);

        slashText = findViewById(R.id.slash);
        slashText.setVisibility(View.GONE);

        TextView maxTimeText = findViewById(R.id.maxTime);

        seekBar = findViewById(R.id.seekbar);

        String duration = getIntent().getStringExtra("duration");

        if (duration.length() > 6) {
            String regex = "(\\d{2}):(\\d{2}):(\\d{2})";
            Pattern p = Pattern.compile(regex);

            Matcher m = p.matcher(duration);
            if (m.find()){
                int hh = Integer.parseInt(m.group(1));
                int mm = Integer.parseInt(m.group(2));
                int ss = Integer.parseInt(m.group(3));

                int a = hh * 3600000;
                int b = mm * 60000;
                int c = ss * 1000;

                seekBar.setMax(a + b + c);
                Log.d("SeekBarMax: ", String.valueOf(a + b + c));
                maxTimeText.setText(duration);
            }
        } else {
            String regex = "(\\d{2}):(\\d{2})";
            Pattern p = Pattern.compile(regex);

            Matcher m = p.matcher(duration);
            if (m.find()){
                int mm = Integer.parseInt(m.group(1));
                int ss = Integer.parseInt(m.group(2));

                int a = mm * 60000;
                int b = ss * 1000;

                seekBar.setMax(a + b);
                Log.d("SeekBarMax: ", String.valueOf(a + b));
                maxTimeText.setText("00:" + duration);
            }
        }

        maxTimeText.setVisibility(View.GONE);
        showButton = false;

        progressPrefer = getSharedPreferences("progress", MODE_PRIVATE);
        int progress = progressPrefer.getInt("progress",0);

        seekBar.setProgress(progress);
        seekBar.setVisibility(View.GONE);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressPrefer = getSharedPreferences("progress", MODE_PRIVATE);
                SharedPreferences.Editor editor = progressPrefer.edit();
                editor.putInt("progress",progress);
                editor.apply();
                Log.d(TAG, String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mMediaPlayer.pause();
                playButtonStatus = false;
                Drawable drawable = getResources().getDrawable(R.drawable.outline_play_circle_black_48);
                playButton.setImageDrawable(drawable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                progressPrefer = getSharedPreferences("progress", MODE_PRIVATE);
                int progress = progressPrefer.getInt("progress",0);
                mMediaPlayer.setTime(progress);
                mMediaPlayer.play();
                playButtonStatus = true;
                Drawable drawable = getResources().getDrawable(R.drawable.outline_pause_circle_black_48);
                playButton.setImageDrawable(drawable);
            }
        });

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
                    playButton.setVisibility(View.GONE);
                    seekBar.setVisibility(View.GONE);
                    nowTimeText.setVisibility(View.GONE);
                    maxTimeText.setVisibility(View.GONE);
                    cloth.setAlpha(0);
                    slashText.setVisibility(View.GONE);
                } else {
                    Log.d(TAG, String.valueOf(showButton));
                    showButton = true;
                    exitButton.setVisibility(View.VISIBLE);
                    playButton.setVisibility(View.VISIBLE);
                    maxTimeText.setVisibility(View.VISIBLE);
                    seekBar.setVisibility(View.VISIBLE);
                    nowTimeText.setVisibility(View.VISIBLE);
                    slashText.setVisibility(View.VISIBLE);
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

        /**
         * 一時停止再開ボタン
         * ただし配信のときは常に非表示かつ無効
         */
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playButton.getVisibility() == View.VISIBLE) {
                    if (playButtonStatus) {
                        mMediaPlayer.pause();
                        playButtonStatus = false;
                        Drawable drawable = getResources().getDrawable(R.drawable.outline_play_circle_black_48);
                        playButton.setImageDrawable(drawable);
                    } else {
                        mMediaPlayer.play();
                        playButtonStatus = true;
                        Drawable drawable = getResources().getDrawable(R.drawable.outline_pause_circle_black_48);
                        playButton.setImageDrawable(drawable);
                    }
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
        Log.v (AppUtils.get().tag(), "onResume");
        super.onResume();

        pausePrefer = getSharedPreferences("pause", MODE_PRIVATE);
        boolean isPause = pausePrefer.getBoolean("pause",false);

        if (isPause) {
            try {
                createPlayer(mFilePath);
            } catch (NullPointerException e) {
                restartApp();
            }
        } else {
            createPlayer(mFilePath);
            scheduleReconnectPLayer();
        }
    }

    @Override
    protected void onPause() {
        Log.v (AppUtils.get().tag(), "onPause");
        super.onPause();

        pausePrefer = getSharedPreferences("pause", MODE_PRIVATE);
        boolean isPause = pausePrefer.getBoolean("pause",false);

        if (!isPause) {
            mMediaPlayer.pause();
            SharedPreferences.Editor editor = pausePrefer.edit();
            editor.putBoolean("pause",true);
            editor.apply();
        } else {
        }
    }

    @Override
    protected void onDestroy() {
        Log.v (AppUtils.get().tag(), "onDestroy");
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
            releasePlayer();
            Toast.makeText(this, "Error in creating player!", Toast
                    .LENGTH_LONG).show();
            restartApp();
        }
    }

    private void releasePlayer() {
        if (libvlc == null) {
            return;
        }
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

        pausePrefer = getSharedPreferences("pause", MODE_PRIVATE);
        boolean isPause = pausePrefer.getBoolean("pause",false);

        if (!isPause) {
            progressPrefer = getSharedPreferences("progress", MODE_PRIVATE);
            SharedPreferences.Editor editor2 = progressPrefer.edit();
            editor2.putInt("progress", 0);
            editor2.apply();
        }
        restartApp();
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
            //Dialogレイアウト取得用のView
            View inputView;

            //Dialog用レイアウトの読み込み
            LayoutInflater factory = LayoutInflater.from(this);
            inputView = factory.inflate(R.layout.dialog_layout, null);

            dialog = new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.dialog_reconnecting))
                    .setView(inputView)
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
     * killProcessを実行する
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
        Log.e(AppUtils.get().tag(), "Error with hardware acceleration");
        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
        this.releasePlayer();
        restartApp();
    }

    @Override
    public void onBackPressed(){
        Log.v (AppUtils.get().tag(), "onBackPressed");
    }

    private class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<RtmpVlcPlayerActivity> mOwner;

        public MyPlayerListener(RtmpVlcPlayerActivity owner) {
            mOwner = new WeakReference<RtmpVlcPlayerActivity>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            RtmpVlcPlayerActivity player = mOwner.get();

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

                    pausePrefer = getSharedPreferences("pause", MODE_PRIVATE);
                    boolean isPause = pausePrefer.getBoolean("pause",false);
                    progressPrefer = getSharedPreferences("progress", MODE_PRIVATE);
                    int progress = progressPrefer.getInt("progress",0);

                    //TODO 動画再生→アプリ強制終了→アプリ起動→別の動画を再生時でも同一の時間で復元してしまうので修正する
                    if (isPause) {
                        mMediaPlayer.setTime(progress);
                        SharedPreferences.Editor editor2 = pausePrefer.edit();
                        editor2.putBoolean("pause",false);
                        editor2.apply();
                    }

                    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String timeFormatted = formatter.format(mMediaPlayer.getTime());
                    Log.d(TAG, String.valueOf(mMediaPlayer.getTime()));
                    Log.d(TAG,"ttimeFormatted: " + timeFormatted);
                    nowTimeText.setText(timeFormatted);
                    seekBar.setProgress((int) mMediaPlayer.getTime());
                    progressPrefer = getSharedPreferences("progress", MODE_PRIVATE);
                    SharedPreferences.Editor editor = progressPrefer.edit();
                    editor.putInt("progress",(int) mMediaPlayer.getTime());
                    editor.apply();
                    break;
                default:
                    break;
            }
        }
    }
}