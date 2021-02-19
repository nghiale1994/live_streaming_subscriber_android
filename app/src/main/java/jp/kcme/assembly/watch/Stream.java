package jp.kcme.assembly.watch;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class Stream {

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
    @SerializedName("is_streaming")
    private boolean isStreaming;
    @Expose
    private HashMap<String, Thumbnail> thumbnails = new HashMap<String, Thumbnail>();

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

    @Override
    public String toString() {
        return "Stream{" +
                "id='" + id + '\'' +
                ", channelId='" + channelId + '\'' +
                ", title='" + title + '\'' +
                ", subtitle='" + subtitle + '\'' +
                ", isStreaming=" + isStreaming +
                ", thumbnails=" + thumbnails +
                '}';
    }

    static class Thumbnail {
        @Expose
        private int width;
        @Expose
        private int height;
        @Expose
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
            return "Thumbnail{" +
                    "width=" + width +
                    ", height=" + height +
                    ", createdDate='" + createdDate + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }
}
