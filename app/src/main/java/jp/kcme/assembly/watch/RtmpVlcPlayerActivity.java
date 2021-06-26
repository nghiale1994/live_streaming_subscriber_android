package jp.kcme.assembly.watch;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class RtmpVlcPlayerActivity extends CommonActivity implements IVLCVout.Callback {
    public final static String TAG = AppUtils.get().tag();

    private String mFilePath;
    private SurfaceView mSurface;
    private int mVideoWidth;
    private int mVideoHeight;
    private MediaController controller;
    private static SurfaceHolder holder;
    private static LibVLC libvlc;
    private static MediaPlayer mMediaPlayer = null;

    /**
     * 再生中か否か
     * キー名 playing
     */
    private SharedPreferences playingPrefer;
    /**
     * 取得したURL
     * キー名 url
     */
    private SharedPreferences urlPrefer;
    /**
     * シークバーの値を一時保存するのに使用
     * シーク中に値を保存してシークバーから手を話したら値を読み込んでsetTimeするだけ
     * なお再生開始時には0を挿入している
     * キー名 progress
     */
    private static SharedPreferences progressPrefer;

    private boolean showButton;
    private boolean playButtonStatus = true;
    private static SeekBar seekBar;
    private static TextView nowTimeText;
    private static ProgressBar indicator;

    private Stream stream = new Stream();
    private static final Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.frame_rtmp_vlc_player);

        Intent intent = getIntent();
        //TODO HomeActivityからURLをintentで受け取る
        mFilePath = intent.getStringExtra("URL_STRING");

        mFilePath = "https://demo-gikai.s3.ap-northeast-1.amazonaws.com/video/80808/2021-06-22/7d30050fb44be9a8f96059e9e9ceebc3.mp4";
        //mFilePath = "rtmp://beetle.mstgikai.com/live/c38mdrj09ha7htdav1rg";

        playingPrefer = getSharedPreferences("playing", MODE_PRIVATE);
        boolean playing = playingPrefer.getBoolean("playing",false);
        Log.d(AppUtils.get().tag(), String.valueOf(playing));

        Intent intent2 = new Intent(getApplication(), StatusExeTask.class);
        startService(intent2);

        Intent intent3 = new Intent(getApplication(), CountExeTask.class);
        startService(intent3);

        Log.d(AppUtils.get().tag(), "Playing: " + mFilePath);
        mSurface = (SurfaceView) findViewById(R.id.surface);
        mSurface.setZOrderOnTop(false);
        holder = mSurface.getHolder();
        holder.setFormat(PixelFormat.TRANSLUCENT);

        controller = new MediaController(this);
        controller.setMediaPlayer(playerInterface);
        controller.setAnchorView(mSurface);

        View decorView = getWindow().getDecorView();

        Log.v(AppUtils.get().tag(),"isStreaming: " + stream.isStreaming());

        ImageView exitButton = findViewById(R.id.exit);
        exitButton.setVisibility(View.GONE);

        ImageView playButton = findViewById(R.id.play);
        playButton.setVisibility(View.GONE);

        nowTimeText = findViewById(R.id.nowTime);
        nowTimeText.setVisibility(View.GONE);

        SimpleDateFormat formatter = new SimpleDateFormat("H:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String timeFormatted = formatter.format(82000);

        Log.d(TAG,"ttimeFormatted" + timeFormatted);
        nowTimeText.setText(timeFormatted);

        TextView maxTimeText = findViewById(R.id.maxTime);
        maxTimeText.setVisibility(View.GONE);
        maxTimeText.setText(timeFormatted);

        showButton = false;

        indicator = findViewById(R.id.indicator);

        seekBar = findViewById(R.id.seekbar);
        seekBar.setProgress(0);
        //TODO 録画の時間を取得できるようにする
        seekBar.setMax(82000); //動画の時間が0:01:22の場合82000ミリ秒
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
         * SurfaceViewクリックでボタンの表示非表示切り替え
         * ただし配信のときはmaxTimeTextとplayButtonは非表示のまま
         */
        mSurface.setOnClickListener(new View.OnClickListener() {
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
                } else {
                    Log.d(TAG, String.valueOf(showButton));
                    showButton = true;
                    exitButton.setVisibility(View.VISIBLE);
                    if (!stream.isStreaming()) {
                        playButton.setVisibility(View.VISIBLE);
                        maxTimeText.setVisibility(View.VISIBLE);
                    }
                    seekBar.setVisibility(View.VISIBLE);
                    nowTimeText.setVisibility(View.VISIBLE);
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
                    playingPrefer = getSharedPreferences("playing", MODE_PRIVATE);
                    SharedPreferences.Editor editor = playingPrefer.edit();
                    editor.putBoolean("playing", false);
                    editor.apply();

                    Intent intent = new Intent(getApplication(), StatusExeTask.class);
                    stopService(intent);
                    Intent intent2 = new Intent(getApplication(), CountExeTask.class);
                    stopService(intent2);

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

    /**
     * 再生時間を取得するためのサービス
     * 現在の時間の反映とSeekBarの反映もここで実施
     */
    public static class CountExeTask extends Service {
        final int INTERVAL_PERIOD = 500;
        Timer timer = new Timer();

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onCreate() {
            super.onCreate();
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            handler.post(new Runnable() {
                                public void run() {
                                    //読み込み中が1,再生中は3,一時停止は4,5は不明（起動失敗？）,停止すると6
                                    try { //mMediaPlayer.getPlayerState()はnullの可能性あり(illegalstateexceptionまたはnullpointerexception）
                                        if (mMediaPlayer.getPlayerState() == 3) {
                                            String strTime = String.valueOf(mMediaPlayer.getTime());
                                            handler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (indicator.getVisibility() == View.VISIBLE) indicator.setVisibility(View.GONE);
                                                    SimpleDateFormat formatter = new SimpleDateFormat("H:mm:ss");
                                                    formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                                                    String timeFormatted = formatter.format(mMediaPlayer.getTime());
                                                    Log.d(TAG,"ttimeFormatted" + timeFormatted);
                                                    nowTimeText.setText(timeFormatted);
                                                }
                                            });
                                            Log.d(TAG,strTime);
                                            seekBar.setProgress((int) mMediaPlayer.getTime());
                                        } else if (mMediaPlayer.getPlayerState() == 1) {
                                            progressPrefer = getSharedPreferences("progress", MODE_PRIVATE);
                                            SharedPreferences.Editor editor = progressPrefer.edit();
                                            editor.putInt("progress",0);
                                            editor.apply();
                                        }
                                    } catch (Exception e) {
                                        Log.e(AppUtils.get().tag(), String.valueOf(e));
                                        Intent intent = new Intent(getApplication(), StatusExeTask.class);
                                        stopService(intent);
                                        Intent intent2 = new Intent(getApplication(), CountExeTask.class);
                                        stopService(intent2);
                                        restartApp();
                                    }
                                }
                            });
                        }
                    }).start();
                }
            }, 0, INTERVAL_PERIOD);
            return START_STICKY;
        }
    }

    /**
     * タイムアウト用のサービス（ずっと読込中だったら諦めて終了するとか ディレイやインターバルをCountExeTaskより遅めに設定する）
     */
    public static class StatusExeTask extends Service {
        final int INTERVAL_PERIOD = 10000;
        Timer timer = new Timer();

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onCreate() {
            super.onCreate();
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.d(AppUtils.get().tag(), "onStartCommand");

            timer.scheduleAtFixedRate(new TimerTask(){
                @Override
                public void run() {
                    new Thread(new Runnable(){
                        @Override
                        public void run() {
                            handler.post(new Runnable() {
                                public void run() {
                                    Log.d(AppUtils.get().tag(),"Service");
                                    try { //mMediaPlayer.getPlayerState()はnullの可能性あり(illegalstateexceptionまたはnullpointerexception）
                                        Log.d(AppUtils.get().tag(), "getPlayerState(): " + String.valueOf(mMediaPlayer.getPlayerState()));
                                    } catch (IllegalStateException e) {
                                        Log.e(AppUtils.get().tag(), String.valueOf(e));
                                    }
                                    //再生してるか
                                    try {
                                        Log.d(AppUtils.get().tag(), "isPlaying(): " + String.valueOf(mMediaPlayer.isPlaying()));
                                    } catch (IllegalStateException e) {
                                        Log.e(AppUtils.get().tag(), String.valueOf(e));
                                    }
                                    //読み込み中が1,再生中は3,一時停止は4,5は不明（起動失敗？）,停止すると6
                                    try {
                                        if (mMediaPlayer.getPlayerState() == 6) {
                                            Log.d(TAG,"getPlayerState():6");
                                            Intent intent = new Intent(getApplication(), StatusExeTask.class);
                                            stopService(intent);
                                            Intent intent2 = new Intent(getApplication(), CountExeTask.class);
                                            stopService(intent2);
                                            mMediaPlayer.release();
                                            restartApp();
                                        } else if (mMediaPlayer.getPlayerState() == 5) {
                                            Log.d(TAG,"getPlayerState():5");
                                            Intent intent = new Intent(getApplication(), StatusExeTask.class);
                                            stopService(intent);
                                            Intent intent2 = new Intent(getApplication(), CountExeTask.class);
                                            stopService(intent2);
                                            mMediaPlayer.release();
                                            restartApp();
                                        }  else if (mMediaPlayer.getPlayerState() == 4) {
                                            Log.d(TAG,"getPlayerState():4");
                                        }
                                        else if (mMediaPlayer.getPlayerState() == 3) {
                                            Log.d(TAG,"getPlayerState():3");
                                        }
                                        else if (mMediaPlayer.getPlayerState() == 2) {
                                            Log.d(TAG,"getPlayerState():2");
                                        } else if (mMediaPlayer.getPlayerState() == 1) {
                                            Log.d(TAG,"getPlayerState():1");
                                            Intent intent = new Intent(getApplication(), StatusExeTask.class);
                                            stopService(intent);
                                            Intent intent2 = new Intent(getApplication(), CountExeTask.class);
                                            stopService(intent2);
                                            mMediaPlayer.release();
                                            restartApp();
                                        }
                                    } catch (Exception e) {
                                        Log.e(AppUtils.get().tag(), String.valueOf(e));
                                        Intent intent = new Intent(getApplication(), StatusExeTask.class);
                                        stopService(intent);
                                        Intent intent2 = new Intent(getApplication(), CountExeTask.class);
                                        stopService(intent2);
                                        restartApp();
                                    }
                                }
                            });
                        }
                    }).start();
                }
            }, 20000, INTERVAL_PERIOD);
            return START_STICKY;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if(timer != null){
                timer.cancel();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    protected void onStart() {
        Log.v (AppUtils.get().tag(), "onStart");
        super.onStart();

        urlPrefer = getSharedPreferences("url", MODE_PRIVATE);
        String urlString = mFilePath;

        createPlayer(urlString);
    }

    @Override
    protected void onResume() {
        Log.v (AppUtils.get().tag(), "onResume");
        super.onResume();
        //finish();
    }

    @Override
    protected void onPause() {
        Log.v (AppUtils.get().tag(), "onPause");
        super.onPause();
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
        finish();
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
        //setSize(mVideoWidth, mVideoHeight);
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
        this.releasePlayer();
        restartApp();
    }

    private class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<RtmpVlcPlayerActivity> mOwner;

        public MyPlayerListener(RtmpVlcPlayerActivity owner) {
            mOwner = new WeakReference<RtmpVlcPlayerActivity>(owner);
        }

        /**
         * EndReachedは終了時に呼ばれる（ただし終了ボタン押下時にここは呼ばれないので録画を最後までみたときか配信が停止したときのみ呼ばれる）
         */
        @Override
        public void onEvent(MediaPlayer.Event event) {
            RtmpVlcPlayerActivity player = mOwner.get();

            switch (event.type) {
                case MediaPlayer.Event.EndReached:
                    Log.d(AppUtils.get().tag(), "MediaPlayerEndReached");

                    Intent intent = new Intent(getApplication(), StatusExeTask.class);
                    stopService(intent);
                    Intent intent2 = new Intent(getApplication(), CountExeTask.class);
                    stopService(intent2);

                    if (stream.isStreaming()) {
                        Context context = getApplicationContext();
                        Toast.makeText(context , R.string.end, Toast.LENGTH_LONG).show();
                    }

                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }

    /**
     * MediaControllerから取得可能な要素
     */
    private MediaController.MediaPlayerControl playerInterface = new MediaController.MediaPlayerControl() {
        //バッファパーセンテージを取得
        public int getBufferPercentage() {
            return 0;
        }
        //現在位置を取得
        public int getCurrentPosition() {
            float pos = mMediaPlayer.getPosition();
            return (int)(pos * getDuration());
        }
        //期間を取得
        public int getDuration() {
            return (int)mMediaPlayer.getLength();
        }
        //再生してるか
        public boolean isPlaying() {
            return mMediaPlayer.isPlaying();
        }
        //一時停止
        public void pause() {
            mMediaPlayer.pause();
        }
        //シーク
        public void seekTo(int pos) {
            mMediaPlayer.setPosition((float)pos / getDuration());
        }
        //開始
        public void start() {
            mMediaPlayer.play();
        }
        //一時停止してるか
        public boolean canPause() {
            return true;
        }
        //後方にシーク
        public boolean canSeekBackward() {
            return stream.isStreaming();
        }
        //前方にシーク
        public boolean canSeekForward() {
            return stream.isStreaming();
        }
        //オーディオセッションIDを取得
        @Override
        public int getAudioSessionId() {
            return 0;
        }
    };

    /**
     * 戻るキー押下時（終了ボタンと同じ動作にしている）
     */
    @Override
    public void onBackPressed(){
        Log.v (AppUtils.get().tag(), "onBackPressed");
        playingPrefer = getSharedPreferences("playing", MODE_PRIVATE);
        SharedPreferences.Editor editor = playingPrefer.edit();
        editor.putBoolean("playing",false);
        editor.apply();
        
        Intent intent = new Intent(getApplication(), StatusExeTask.class);
        stopService(intent);
        Intent intent2 = new Intent(getApplication(), CountExeTask.class);
        stopService(intent2);

        releasePlayer();
    }

    @Override
    public void onStop(){
        super.onStop();
        Log.v (AppUtils.get().tag(), "onStop");
    }
}