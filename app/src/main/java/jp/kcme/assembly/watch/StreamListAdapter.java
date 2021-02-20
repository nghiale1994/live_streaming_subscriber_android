package jp.kcme.assembly.watch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

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
        layoutParams.height = (int) (parent.getHeight() * 0.25);
        itemView.setLayoutParams(layoutParams);

        return new ViewHolder(itemView);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        final Stream stream = streamList.get(position);

        if (stream.isStreaming()) {
            viewHolder.getStreamType().setBackground(context.getDrawable(R.drawable.streaming_type_background));
            viewHolder.getStreamType().setText(R.string.streaming);
        } else {
            viewHolder.getStreamType().setBackground(context.getDrawable(R.drawable.video_type_background));
            viewHolder.getStreamType().setText(R.string.video);
        }

        viewHolder.getTitle().setText(stream.getTitle());
        viewHolder.getSubtitle().setText(stream.getSubtitle());

        Stream.Thumbnail thumbnail = stream.getThumbnails().get("mobile");
        if(thumbnail != null) {
            Picasso.get().load(thumbnail.getUrl()).into(viewHolder.getThumbnail());
        }

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "http://spider01.mstgikai.com:8888/stream/watch";
                if (!stream.getChannelId().equals("")) {
                    url += "?channel=" + stream.getChannelId();
                }
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                CustomTabsIntent customTabsIntent = builder.build();

                customTabsIntent.intent.setPackage("com.android.chrome");
                customTabsIntent.launchUrl(context, Uri.parse(url));
            }
        });
    }

    @Override
    public int getItemCount() {
        return streamList.size();
    }
}
