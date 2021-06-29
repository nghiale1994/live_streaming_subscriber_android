package jp.kcme.assembly.watch;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import jp.kcme.assembly.watch.state.ShowStreamState;

public class HomeActivity extends CommonActivity {
    private ArrayList<Stream> streamList;
    private StreamListAdapter streamListAdapter;

    private ShowStreamState state;

    private ImageView homeBtn;
    private ImageView refreshBtn;
    private TextView showStreamingOnlyBtn;
    private TextView showHistoryOnlyBtn;
    
    private RecyclerView streamListview;

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        state.fetchStreams();
    }
}
