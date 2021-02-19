package jp.kcme.assembly.watch.retrofit;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;

import jp.kcme.assembly.watch.Stream;

public class StreamListResponse {
    @Expose
    String status;
    @Expose
    ArrayList<Stream> data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ArrayList<Stream> getData() {
        return data;
    }

    public void setData(ArrayList<Stream> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "StreamListResponse{" +
                "status='" + status + '\'' +
                ", data=" + data +
                '}';
    }
}
