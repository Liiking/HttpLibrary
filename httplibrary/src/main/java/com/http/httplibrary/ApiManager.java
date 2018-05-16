package com.http.httplibrary;

import android.content.Context;
import android.support.annotation.NonNull;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.http.httplibrary.exception.HttpResponseFunc;
import com.http.httplibrary.exception.ServerException;
import com.http.httplibrary.response.BaseResponse;
import com.http.httplibrary.subscribers.ProgressSubscriber;
import com.http.httplibrary.subscribers.SubscriberListener;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by qwy on 2017/7/14.
 * 网络请求管理类
 */
public class ApiManager {

    private static ApiManager apiManager = null;
    private static IAPINetService apiNetService = null;

    public static long READ_TIME = 30; // 读取时间超时 秒级别
    public static long CONNECT_TIME = 10; // 连接时间超时 秒级别

    private ApiManager(Context mContext, String baseUrl) {
        apiNetService = createServiceAPI(mContext, baseUrl, RxJavaCallAdapterFactory.create(), IAPINetService.class, null, null, null);
    }

    public static void initApiManger(Context mContext, String baseUrl) {
        apiManager = new ApiManager(mContext, baseUrl);
    }

    public static ApiManager getInstance() {
        return apiManager;
    }

    public static IAPINetService getNetAPIInstance() {
        return apiNetService;
    }

    public <T> void requestPost(Context context, @NonNull String path, @NonNull Class<T> tClass, Map<String, String> params, SubscriberListener listener) {
        requestPost(context, false, path, tClass, params, listener);
    }

    public <T> void requestPost(Context context, boolean hideLoading, @NonNull String path, @NonNull Class<T> tClass, Map<String, String> params, SubscriberListener listener) {
        requestPost(context, hideLoading, false, path, tClass, params, listener);
    }

    public <T> void requestPost(Context context, boolean hideLoading, boolean hideMsg, @NonNull String path, @NonNull Class<T> tClass, Map<String, String> params, SubscriberListener listener) {
        request(context, hideLoading, hideMsg, path, false, tClass, params, listener);
    }

    public <T> void requestGet(Context context, @NonNull String path, @NonNull Class<T> tClass, Map<String, String> params, SubscriberListener listener) {
        requestGet(context, false, path, tClass, params, listener);
    }

    public <T> void requestGet(Context context, boolean hideLoading, @NonNull String path, @NonNull Class<T> tClass, Map<String, String> params, SubscriberListener listener) {
        requestGet(context, hideLoading, false, path, tClass, params, listener);
    }

    public <T> void requestGet(Context context, boolean hideLoading, boolean hideMsg, @NonNull String path, @NonNull Class<T> tClass, Map<String, String> params, SubscriberListener listener) {
        request(context, hideLoading, hideMsg, path, true, tClass, params, listener);
    }

    /**
     * 设置系统参数
     * @param context
     * @param params
     */
    public void putSystemParams(Context context, @NonNull Map<String, String> params) {
    }

    public <T> void request(final Context context, final boolean hideLoading, final String path, final boolean isGet, final Class<T> tClass, Map<String, String> params, SubscriberListener listener) {
        request(context, hideLoading, false, path, isGet, tClass, params, listener);
    }

    /**
     * 发送请求
     * @param context
     * @param hideLoading 是否隐藏加载框  false：显示  true：隐藏
     * @param hideMsg 是否隐藏无网络toast提示  false：显示  true：隐藏
     * @param path        请求路径，在UrlConfig中定义
     * @param isGet       是否是Get请求 true：get 请求
     * @param tClass      对应的数据类型
     * @param params   Get请求是，对应的url参数
     * @param listener    回调请求
     * @param <T>
     */
    public <T> void request(final Context context, final boolean hideLoading, final boolean hideMsg, final String path, final boolean isGet, final Class<T> tClass, Map<String, String> params, final SubscriberListener<T> listener) {
        Observable observable;
        if (params == null) {
            params = new HashMap<>();
        }
        putSystemParams(context, params);
        if (isGet) {
            observable = getNetAPIInstance().requestGet(path, params);
        } else {
            observable = getNetAPIInstance().requestPost(path, params);
        }
        Utility.log("params:" + new Gson().toJson(params));
        doSubscribe(context, hideLoading, hideMsg, observable, tClass, listener);
    }

    public <T> void doSubscribe(Context context, boolean hideLoading, Observable observable, final Class<T> tClass, final SubscriberListener<T> listener) {
        doSubscribe(context, hideLoading, false, observable, tClass, listener);
    }
    public <T> void doSubscribe(Context context, boolean hideLoading, boolean hideMsg, Observable observable, final Class<T> tClass, final SubscriberListener<T> listener) {
        ProgressSubscriber<T> subscriber = new ProgressSubscriber<T>(context, hideLoading, hideMsg, listener);
        observable.subscribeOn(Schedulers.io())
                .map(new ServerResponseFunc<T>())
                .map(new Func1<BaseResponse<T>, T>() {
                    @Override
                    public T call(BaseResponse<T> response) {
                        try {
                            Utility.LogTooLongE("doSubscribe:", "response:" + Utility.formatJson(response.getData().toString()));
                            // 服务器请求数据成功，返回里面的数据实体
                            if (tClass != null && response.getData() != null) {
                                try {
                                    Utility.log("======response :" + new Gson().toJson(response));
                                    if (response.getData() instanceof Map) {
                                        T data = JsonParser.getBeanFromMap((Map<String, Object>) response.getData(), tClass);
                                        if (listener != null) {
                                            listener.onNext(response.getCode(), data);
                                        }
                                        return data;
                                    } else {
                                        Gson gson = new Gson();
                                        String json = gson.toJson(response.getData());
                                        T data = gson.fromJson(json, tClass);
                                        if (listener != null) {
                                            listener.onNext(response.getCode(), data);
                                        }
                                        return data;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                })
                .onErrorResumeNext(new HttpResponseFunc<T>())
                .unsubscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(subscriber);
    }

    // 拦截响应体，判断里面的状态码，返回固定格式的公共数据类型Response<T>
    private class ServerResponseFunc<T> implements Func1<Response<ResponseBody>, BaseResponse<T>> {
        @Override
        public BaseResponse<T> call(Response<ResponseBody> response) {
            String result = "";
            BaseResponse<T> baseResponse = null;
            try {
                // 对返回码进行判断，如果不是200，则证明服务器端返回错误信息了，便根据跟服务器约定好的错误码去解析异常
                if (!response.isSuccessful()) {
                    ResponseBody body = response.errorBody();
                    if (body != null) {
                        result = body.string();
                        baseResponse = new Gson().fromJson(result, BaseResponse.class);
                    }
                    if (baseResponse != null) {
                        // 如果服务器端有错误信息返回，那么抛出异常，让下面的方法去捕获异常做统一处理
                        throw new ServerException(response.code(), baseResponse.getMessage());
                    }
                } else {
                    ResponseBody body = response.body();
                    if (body != null) {
                        result = body.string();
                        baseResponse = new Gson().fromJson(result, BaseResponse.class);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 服务器请求数据成功，返回里面的数据实体
            return baseResponse;
        }
    }

    /**
     * 修改
     * @param context
     * @param hideLoading
     * @param listener
     * @param <T>
     */
    public <T> void put(final Context context, final boolean hideLoading, String path, String id, final Class<T> tClass, SubscriberListener<T> listener) {
        Map<String, String> params = new HashMap<>();
        params.put("id", id);
        put(context, hideLoading, path, params, tClass, listener);
    }

    /**
     * 修改
     * @param context
     * @param hideLoading
     * @param listener
     * @param <T>
     */
    public <T> void put(final Context context, final boolean hideLoading, String path, Map<String, String> params, final Class<T> tClass, SubscriberListener<T> listener) {
        Observable observable;
        observable = getNetAPIInstance().put(path, params);
        doSubscribe(context, hideLoading, observable, tClass, listener);
    }

    /**
     * 删除
     * @param context
     * @param hideLoading
     * @param listener
     * @param <T>
     */
    public <T> void deleteRestful(final Context context, final boolean hideLoading, String path, String id, final Class<T> tClass, SubscriberListener<T> listener) {
        Observable observable;
        observable = getNetAPIInstance().delete(path, id);
        doSubscribe(context, hideLoading, observable, tClass, listener);
    }

    /**
     * 删除
     * @param context
     * @param hideLoading
     * @param listener
     * @param <T>
     */
    public <T> void delete(final Context context, final boolean hideLoading, String path, Map<String, String> params, final Class<T> tClass, SubscriberListener<T> listener) {
        Observable observable;
        observable = getNetAPIInstance().delete(path, params);
        doSubscribe(context, hideLoading, observable, tClass, listener);
    }

    /**
     * 上传文件
     * @param context
     * @param hideLoading
     * @param params
     * @param listener
     * @param <T>
     */
    public <T> void uploadFile(final Context context, final boolean hideLoading, final Class<T> tClass, final Map<String, RequestBody> params, SubscriberListener<T> listener) {
        Observable observable;
        observable = getNetAPIInstance().uploadFile(params);
        doSubscribe(context, hideLoading, observable, tClass, listener);
    }

    public static <T> T createServiceAPI(Context context, String baseUrl, CallAdapter.Factory factory, Class<T> serviceClass) {
        return createServiceAPI(context, baseUrl, factory, serviceClass, null, null, null);
    }

    /***
     * 创建ServiceApi对象
     * @param context
     */
    public static <T> T createServiceAPI(final Context context, String baseUrl, CallAdapter.Factory factory, Class<T> serviceClass, Interceptor applicationInterceptor, Interceptor[] netWorkInterceptor, int... rawResources) {
        try {
            Interceptor temApplicationInterceptor = applicationInterceptor;
            if (temApplicationInterceptor == null) {
                temApplicationInterceptor = new DefaultInterceptorApplication();
            }
            Interceptor[] temNetsInterceptor = netWorkInterceptor;
            if (temNetsInterceptor == null || temNetsInterceptor.length <= 0) {
                temNetsInterceptor = new Interceptor[1];
                temNetsInterceptor[0] = new DefaultInterceptorNetwork();
            }

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .readTimeout(READ_TIME, TimeUnit.SECONDS)
                    .connectTimeout(CONNECT_TIME, TimeUnit.SECONDS)
                    .addInterceptor(temApplicationInterceptor);
//            for (Interceptor tem : temNetsInterceptor) {
//                builder.addNetworkInterceptor(tem);
//            }
            builder.retryOnConnectionFailure(true);
            try {
                initHttpsConfig(context, builder, rawResources);
            } catch (Exception e) {
                e.printStackTrace();
            }
            OkHttpClient client = builder.addInterceptor(new Interceptor() {
                @Override
                public okhttp3.Response intercept(Chain chain) throws IOException {
                    // 添加公共头信息
                    Request request = chain.request()
                            .newBuilder()
//                            .addHeader("Content-Type", "application/json")//  x-www-form-urlencoded
//                            .addHeader("User-Agent", "Mozilla/5.0")
                            .build();
                    Utility.log(request.headers().toString());
                    return chain.proceed(request);
                }
            }).build();
            Retrofit.Builder builder1 = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create(getGson()));
            if (factory != null) {
                builder1.addCallAdapterFactory(factory);
            }
            Retrofit retrofit = builder1.callFactory(client)
                    .build();
            return retrofit.create(serviceClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /***
     * https证书
     */
    public static void initHttpsConfig(Context context, OkHttpClient.Builder builder, int... rawResources) {
        try {
            if (context != null && rawResources != null && rawResources.length > 0) {
                final String KEY_STORE_TYPE_P12 = "PKCS12";//证书类型
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE_P12);
                keyStore.load(null);
                for (int index = 0; index < rawResources.length; index++) {
                    try {
                        InputStream is = context.getResources().openRawResource(rawResources[index]);
                        keyStore.setCertificateEntry("" + index, certificateFactory.generateCertificate(is));
                        if (is != null) {
                            is.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
                if (sslContext != null) {
                    builder.sslSocketFactory(sslContext.getSocketFactory());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Gson getGson() {
        Gson gson = new Gson();
        try {
            GsonBuilder builder = new GsonBuilder();
            gson = builder.create();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gson;
    }

}
