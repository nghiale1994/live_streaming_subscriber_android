package jp.kcme.assembly.watch;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class Stream implements Comparable {

    @Expose
    private String id = "";
    @Expose
    @SerializedName("channel_id")
    private String channelId = "";
    @Expose
    private String title = "";
    @Expose
    private String subtitle = "";
    @Expose
    @SerializedName("created_date")
    private String createdDate = "";
    @Expose
    @SerializedName("is_streaming")
    private boolean isStreaming;
    @Expose
    private HashMap<String, Thumbnail> thumbnails = new HashMap<String, Thumbnail>();
    @Expose
    private Video video = new Video();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isStreaming() {
        return isStreaming;
    }

    public void setStreaming(boolean streaming) {
        isStreaming = streaming;
    }

    public HashMap<String, Thumbnail> getThumbnails() {
        return thumbnails;
    }

    public void setThumbnails(HashMap<String, Thumbnail> thumbnails) {
        this.thumbnails = thumbnails;
    }

    public Video getVideo() {
        return video;
    }

    public void setVideo(Video video) {
        this.video = video;
    }

    @Override
    public String toString() {
        Log.i(AppUtils.get().tag(), this.getClass().getSimpleName());
        AppUtils.get().printJson(this);
        return "";
    }

    public static Date parseDate(String dateString) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        Date value;
        try {
            value = df1.parse(dateString);
        } catch (ParseException e1) {
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            try {
                value = df2.parse(dateString);
            } catch (ParseException e2) {
                return null;
            }
        }
        return value;
    }

    @Override
    public int compareTo(Object o) {
        Date thisDate = parseDate(this.getCreatedDate());
        Date thatDate = parseDate(((Stream) o).getCreatedDate());
        try {
            if (thatDate.after(thisDate)) {
                return 1;
            } else if (thatDate.before(thisDate)) {
                return -1;
            }
        } catch (Exception e) {
            Log.e(AppUtils.get().tag(), "Exception: " + e);
        }
        return 0;
    }

    static class Thumbnail {
        @Expose
        private int width;
        @Expose
        private int height;
        @Expose
        @SerializedName("created_date")
        private String createdDate = "";
        @Expose
        private String url = "";

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public String getCreatedDate() {
            return createdDate;
        }

        public void setCreatedDate(String createdDate) {
            this.createdDate = createdDate;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @Override
        public String toString() {
            Log.i(AppUtils.get().tag(), this.getClass().getSimpleName());
            AppUtils.get().printJson(this);
            return "";
        }
    }

    static class Video {
        @Expose
        private String url = "";
        @Expose
        private String duration = "";
        @Expose
        @SerializedName("created_date")
        private String createdDate = "";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }

        public String getCreatedDate() {
            return createdDate;
        }

        public void setCreatedDate(String createdDate) {
            this.createdDate = createdDate;
        }

        @Override
        public String toString() {
            Log.i(AppUtils.get().tag(), this.getClass().getSimpleName());
            AppUtils.get().printJson(this);
            return "";
        }
    }
}
