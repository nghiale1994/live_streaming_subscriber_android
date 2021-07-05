package jp.kcme.assembly.watch.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import jp.kcme.assembly.watch.R;
import jp.kcme.assembly.watch.activity.RtmpVlcPlayerActivity;
import jp.kcme.assembly.watch.activity.RtmpVlcPlayerForLiveStreamActivity;
import jp.kcme.assembly.watch.model.Stream;
import jp.kcme.assembly.watch.util.AppUtils;
import jp.kcme.assembly.watch.util.NetworkUtil;

public class StreamListAdapter extends RecyclerView.Adapter<StreamListAdapter.ViewHolder> {

    private ArrayList<Stream> streamList;
    private Context context;

    public StreamListAdapter(Context context, ArrayList<Stream> streamList) {
        this.context = context;
        this.streamList = streamList;
    }

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView streamType;
        private final ImageView thumbnail;
        private final TextView title;
        private final TextView subtitle;

        public ViewHolder(View view) {
            super(view);
            streamType = view.findViewById(R.id.stream_type);
            thumbnail = view.findViewById(R.id.thumbnail);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
        }

        public TextView getStreamType() {
            return streamType;
        }

        public TextView getTitle() {
            return title;
        }

        public TextView getSubtitle() {
            return subtitle;
        }

        public ImageView getThumbnail() { return thumbnail;}
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_stream_item, parent, false);

        ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
        layoutParams.height = (int) (parent.getHeight() * 0.3);
        itemView.setLayoutParams(layoutParams);

        return new ViewHolder(itemView);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        final Stream stream = streamList.get(viewHolder.getAdapterPosition());

        switch (stream.getType()) {
            case Stream.Type.Streaming:
                viewHolder.getStreamType().setBackground(context.getDrawable(R.drawable.streaming_type_background));
                viewHolder.getStreamType().setText(R.string.streaming);
                break;
            case Stream.Type.History:
                viewHolder.getStreamType().setBackground(context.getDrawable(R.drawable.video_type_background));
                viewHolder.getStreamType().setText(R.string.video);
                break;
            default:
                break;
        }

        viewHolder.getTitle().setText(stream.getTitle());
        viewHolder.getSubtitle().setText(stream.getSubtitle());

        Stream.Thumbnail thumbnail = stream.getThumbnails().get("mobile");
        if(thumbnail != null) {
            Log.i(AppUtils.get().tag(), "Thumbnail's url: " + thumbnail.getUrl());
            Picasso.get().load(thumbnail.getUrl()).into(viewHolder.getThumbnail());
        } else {
            viewHolder.getThumbnail().setImageResource(0);
        }

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                String url = Properties.API_PREFIX + Properties.WATCH_STREAM;
//                if (!stream.getChannelId().equals("")) {
//                    url += "?channel=" + stream.getChannelId();
//                }
////                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
////                CustomTabsIntent customTabsIntent = builder.build();
////
////                customTabsIntent.intent.setPackage("com.android.chrome");
////                customTabsIntent.launchUrl(context, Uri.parse(url));
//
//                TrustedWebActivityIntentBuilder builder = new TrustedWebActivityIntentBuilder(Uri.parse(url))
//                        .setScreenOrientation(ScreenOrientation.LANDSCAPE);
//                TwaLauncher launcher = new TwaLauncher(context);
//                launcher.launch(builder, null, null, null);
//
////                Intent intent = new Intent(context, com.google.androidbrowserhelper.trusted.LauncherActivity.class);
////                intent.setData(Uri.parse(url));
//////                intent.setData(Uri.parse("https://google.com"));
////                intent.setAction(Intent.ACTION_VIEW);
////                context.startActivity(intent);

//                Intent intent = new Intent(context, RTMPPlayerActivity.class);
                if (NetworkUtil.isOnline(context)) {
                    Intent intent;
                    if (stream.isStreaming()) {
                        intent = new Intent(context, RtmpVlcPlayerForLiveStreamActivity.class);
                        if (!stream.getChannelId().equals("")) {
                            intent.putExtra("channelId", stream.getChannelId());
                            intent.putExtra("createdDate",stream.getCreatedDate());
                        }
                    } else {
                        intent = new Intent(context, RtmpVlcPlayerActivity.class);
                        intent.putExtra("duration",stream.getVideo().getDuration());
                    }
                    context.startActivity(intent);
                } else {
                    String netErrorStr = context.getString(R.string.network_error);
                    Toast.makeText(context, netErrorStr, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void setData(ArrayList<Stream> newStreams) {
        streamList.clear();
        streamList.addAll(newStreams);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return streamList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}
