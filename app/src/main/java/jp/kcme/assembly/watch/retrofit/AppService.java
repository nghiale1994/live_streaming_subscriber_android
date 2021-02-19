package jp.kcme.assembly.watch.retrofit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jp.kcme.assembly.watch.Properties;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AppService {

    private static AppService INSTANCE;

    public static AppService getInstance() {
        if (INSTANCE == null) {
            synchronized (AppService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppService();
                }
            }
        }
        return INSTANCE;
    }

    public AppRequest buildRequest() {
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .serializeNulls()
                .excludeFieldsWithoutExposeAnnotation()
                .create();

//        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
//        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
//        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
//        httpClient.addInterceptor(logging);

        return new Retrofit.Builder()
                .baseUrl(Properties.API_PREFIX)
                .addConverterFactory(GsonConverterFactory.create(gson))
//                .addCallAdapterFactory(Java7CallAdapterFactory.create())
//                .client(httpClient.build())
                .build()
                .create(AppRequest.class);
    }
}
