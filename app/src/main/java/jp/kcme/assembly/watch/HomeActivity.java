package jp.kcme.assembly.watch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import jp.kcme.assembly.watch.state.ShowStreamState;
import jp.kcme.assembly.watch.util.NetworkUtil;

public class HomeActivity extends CommonActivity {
    private ArrayList<Stream> streamList;
    private StreamListAdapter streamListAdapter;

    private ShowStreamState state;

    private ImageView homeBtn;
    private ImageView refreshBtn;
    private TextView showStreamingOnlyBtn;
    private TextView showHistoryOnlyBtn;
    
    private RecyclerView streamListview;

    /**
     * 再生中か否か
     * キー名 playing
     */
    private SharedPreferences playingPrefer;
    /**
     * 取得したurl
     * キー名 url
     */
    private SharedPreferences urlPrefer;

    private Stream stream = new Stream();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        state = ShowStreamState.get();

        homeBtn = findViewById(R.id.home_btn);
        refreshBtn = findViewById(R.id.refresh_btn);
        showStreamingOnlyBtn = findViewById(R.id.show_streaming_btn);
        showHistoryOnlyBtn = findViewById(R.id.show_history_stream_btn);

        streamListview = findViewById(R.id.stream_list);
        streamListview.setLayoutManager(new LinearLayoutManager(this));

        streamList = new ArrayList<>();
        streamList.addAll(state.getStreamListData().getValue());
        streamListAdapter = new StreamListAdapter(this, streamList);
        streamListview.setAdapter(streamListAdapter);
        state.getStreamListData().observe(this, new Observer<ArrayList<Stream>>() {
            @Override
            public void onChanged(ArrayList<Stream> streams) {
                streamListAdapter.setData(streams);
            }
        });

        state.getShowOnlyStreaming().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                showStreamingOnlyBtn.setSelected(aBoolean);
                state.updateStreamList();
            }
        });

        state.getShowOnlyHistory().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                showHistoryOnlyBtn.setSelected(aBoolean);
                state.updateStreamList();
            }
        });

        homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                HomeActivity.this.startActivity(intent);
            }
        });

        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                state.fetchStreams();
            }
        });

        showStreamingOnlyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                state.toggleMode(ShowStreamState.SHOW_ONLY_STREAMING_MODE);
            }
        });

        showHistoryOnlyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                state.toggleMode(ShowStreamState.SHOW_ONLY_HISTORY_MODE);
            }
        });

        //TODO リストから選択した動画のURLをRtmpVlcPlayerActivityにintentで渡す intent_nameはURL_STRING
    }

    /**
     * 再生中のまま異常終了してしまったとき（かつネットが有効のとき）に復旧させる
     */
    @Override
    protected void onResume() {
        super.onResume();
        state.fetchStreams();

        playingPrefer = getSharedPreferences("playing", MODE_PRIVATE);
        boolean playing = playingPrefer.getBoolean("playing",false);

        boolean netStatus = NetworkUtil.isOnline(this);

        if (playing && netStatus) {
            urlPrefer = getSharedPreferences("url", MODE_PRIVATE);
            //TODO jsonから取得したURLを反映させるようにする
            String urlString = urlPrefer.getString("url", "https://demo-gikai.s3.ap-northeast-1.amazonaws.com/video/80808/2021-06-22/7d30050fb44be9a8f96059e9e9ceebc3.mp4");

            Log.d(TAG, urlString);

            Intent intent = new Intent(getApplication(), RtmpVlcPlayerActivity.class);
            intent.putExtra("URL_STRING", urlString);
            startActivity(intent);
        }
    }
}
