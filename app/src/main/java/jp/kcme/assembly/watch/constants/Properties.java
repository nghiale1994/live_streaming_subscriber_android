package jp.kcme.assembly.watch.constants;

public class Properties {
    //TODO API_PREFIXを修正する

    // ? 美郷本番環境
    public static final String API_PREFIX = "http://spider01.mstgikai.com:8888";

    // ? 試験環境
    public static final String API_TEST_PREFIX = "http://spider01.mstgikai.com:7777";

    public static final String API_TEST_MP4_PREFIX = "https://demo-gikai.s3.ap-northeast-1.amazonaws.com/video/80808/2021-06-22/";

    public static final String API_TEST_RTMP_PREFIX = "rtmp://beetle.mstgikai.com/live/";

    public static final String GET_STREAM_API = "/stream/api/list";
    public static final String WATCH_STREAM = "/stream/watch";
}
