package jp.kcme.assembly.watch;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSourceFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

public class RTMPPlayerActivity extends CommonActivity {
    SimpleExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.i(AppUtils.get().tag(), "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtmp_player);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(AppUtils.get().tag(), "onResume");

        try {
            // Create Simple ExoPlayer
            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
            TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);

            PlayerView playerView = findViewById(R.id.simple_player);
            playerView.setPlayer(player);

            // Create RTMP Data Source
            RtmpDataSourceFactory rtmpDataSourceFactory = new RtmpDataSourceFactory();
            MediaSource videoSource = new ExtractorMediaSource
                    .Factory(rtmpDataSourceFactory)
                    .createMediaSource(Uri.parse("rtmp://beetle.mstgikai.com/live/" + getIntent().getStringExtra("channelId")));

            player.prepare(videoSource);
            player.setPlayWhenReady(true);

        } catch (Exception e) {
            Log.i(AppUtils.get().tag(), "Player Exception e: " + e);
            Log.i(AppUtils.get().tag(), "Player Exception e's message: " + e.getMessage());
            Log.i(AppUtils.get().tag(), "Player Exception e's cause: " + e.getCause());
        }
    }

    @Override
    protected void onStop() {
        Log.i(AppUtils.get().tag(), "onStop");
        super.onStop();

        try {
            player.release();

        } catch (Exception e) {
            Log.i(AppUtils.get().tag(), "Player Exception e: " + e);
            Log.i(AppUtils.get().tag(), "Player Exception e's message: " + e.getMessage());
            Log.i(AppUtils.get().tag(), "Player Exception e's cause: " + e.getCause());
        }
    }
}
