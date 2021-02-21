package jp.kcme.assembly.watch.state;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import jp.kcme.assembly.watch.AppUtils;
import jp.kcme.assembly.watch.Stream;
import jp.kcme.assembly.watch.retrofit.AppRequest;
import jp.kcme.assembly.watch.retrofit.AppService;
import jp.kcme.assembly.watch.retrofit.StreamListResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShowStreamState {
    private static ShowStreamState state;
    private static ArrayList<Stream> originalList;

    private ShowStreamState() {
    }

    public static ShowStreamState get() {
        if (state == null) {
            state = new ShowStreamState();
        }

        originalList = new ArrayList<>();

        if (streamListData == null) {
            streamListData = new MutableLiveData<>(new ArrayList<>());
        }

        if (modesMap == null) {
            showOnlyStreaming = new MutableLiveData<>(false);
            showOnlyHistory = new MutableLiveData<>(false);

            modesMap = new HashMap<>();
            modesMap.put(SHOW_ONLY_STREAMING_MODE, showOnlyStreaming);
            modesMap.put(SHOW_ONLY_HISTORY_MODE, showOnlyHistory);
        }

        return state;
    }


    private static MutableLiveData<ArrayList<Stream>> streamListData;

    public MutableLiveData<ArrayList<Stream>> getStreamListData() {
        return streamListData;
    }

    public void fetchStreams() {
        AppRequest request = AppService.getInstance().buildRequest();
        Call<StreamListResponse> call = request.fetchStreams();
        call.enqueue(new Callback<StreamListResponse>() {
            @Override
            public void onResponse(Call<StreamListResponse> call, Response<StreamListResponse> response) {
                Log.i(AppUtils.get().tag(), "StreamListResponse: ");
                Log.i(AppUtils.get().tag(), response.body().toString());
                if (response.body() != null) {
                    ArrayList<Stream> fetchedStreams = response.body().getData();
                    originalList.clear();
                    originalList.addAll(sortedList(fetchedStreams));
                    streamListData.postValue(filteredListByMode(originalList));
                }
            }

            @Override
            public void onFailure(Call<StreamListResponse> call, Throwable t) {
                Log.e(AppUtils.get().tag(), "Request failed");
                t.printStackTrace();
                originalList.clear();
                streamListData.postValue(originalList);
            }
        });
    }


    public static final String SHOW_ONLY_STREAMING_MODE = "MODE_SHOW_ONLY_STREAMING";
    private static MutableLiveData<Boolean> showOnlyStreaming;

    public MutableLiveData<Boolean> getShowOnlyStreaming() {
        return showOnlyStreaming;
    }

    public static final String SHOW_ONLY_HISTORY_MODE = "MODE_SHOW_ONLY_HISTORY";
    private static MutableLiveData<Boolean> showOnlyHistory;

    public MutableLiveData<Boolean> getShowOnlyHistory() {
        return showOnlyHistory;
    }

    public static HashMap<String, MutableLiveData<Boolean>> modesMap;

    public void toggleMode(String targetMode) {
        for (String modeName : modesMap.keySet()) {
            MutableLiveData<Boolean> modeData = modesMap.get(modeName);
            if (modeData != null) {
                if (modeName.equals(targetMode)) {
                    boolean oldValue = modeData.getValue();
                    modeData.postValue(!oldValue);
                } else {
                    modeData.postValue(false);
                }
            }
        }
    }

    public String getActiveMode() {
        for (String modeName : modesMap.keySet()) {
            MutableLiveData<Boolean> modeData = modesMap.get(modeName);
            if (modeData.getValue()) {
                return modeName;
            }
        }
        return "";
    }

    /**
     * * Sort input streams, with streaming ones first and history ones after
     *
     * @param streamList the input list
     * @return ArrayList<Stream> sorted list
     */
    public ArrayList<Stream> sortedList(ArrayList<Stream> streamList) {
        ArrayList<Stream> streamingList = new ArrayList<>();
        ArrayList<Stream> historyList = new ArrayList<>();
        ArrayList<Stream> sortedList = new ArrayList<>();

        for (Stream stream : streamList) {
            if (stream.isStreaming()) {
                streamingList.add(stream);
            } else {
                historyList.add(stream);
            }
        }

        Collections.sort(streamingList);
        Collections.sort(historyList);

        sortedList.addAll(streamingList);
        sortedList.addAll(historyList);
        return sortedList;
    }

    public ArrayList<Stream> filteredListByMode(ArrayList<Stream> streamList) {
        ArrayList<Stream> filteredList = new ArrayList<>();
        String currentMode = getActiveMode();
        Log.i(AppUtils.get().tag(), "currentMode: " + currentMode);
        switch (currentMode) {
            case SHOW_ONLY_STREAMING_MODE:
                for (Stream stream : streamList) {
                    if (stream.isStreaming()) {
                        filteredList.add(stream);
                    }
                }
                break;
            case SHOW_ONLY_HISTORY_MODE:
                for (Stream stream : streamList) {
                    if (!stream.isStreaming()) {
                        filteredList.add(stream);
                    }
                }
                break;
            default:
                filteredList.addAll(streamList);
        }

        return filteredList;
    }

    public void updateStreamList() {
        streamListData.postValue(filteredListByMode(originalList));
    }
}
