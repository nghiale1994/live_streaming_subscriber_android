package jp.kcme.assembly.watch.retrofit;

import jp.kcme.assembly.watch.constants.Properties;
import retrofit2.Call;
import retrofit2.http.POST;

public interface AppRequest {
    @POST(Properties.GET_STREAM_API)
    Call<StreamListResponse> fetchStreams();
}
