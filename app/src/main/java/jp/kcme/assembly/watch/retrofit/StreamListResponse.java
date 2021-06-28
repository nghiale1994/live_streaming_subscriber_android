package jp.kcme.assembly.watch.retrofit;

import android.util.Log;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;

import jp.kcme.assembly.watch.AppUtils;
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
        Log.i(AppUtils.get().tag(), this.getClass().getSimpleName());
        AppUtils.get().printJson(this);
        return "";
    }
}
