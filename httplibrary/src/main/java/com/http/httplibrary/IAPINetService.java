package com.http.httplibrary;

import java.util.Map;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.DELETE;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;
import rx.Observable;

public interface IAPINetService {

    @FormUrlEncoded
    @POST("{url}")
    Observable<Response<ResponseBody>> requestPost(@Path("url") String url, @FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("{url}")
    Observable<Response<ResponseBody>> requestPost(@Path("url") String url);

    @GET("{url}")
    Observable<Response<ResponseBody>> requestGet(@Path("url") String url);

    @GET("{url}")
    Observable<Response<ResponseBody>> requestGet(@Path("url") String url, @QueryMap Map<String, String> params);

    @Multipart
    @POST("upload/images")
    Observable<Response<ResponseBody>> uploadFile(@PartMap Map<String, RequestBody> files);

    // 删除 restful
    @DELETE("{path}/{id}")
    Observable<Response<ResponseBody>> delete(@Path("path") String path, @Path("id") String id);

    // 删除
    @DELETE("{path}")
    Observable<Response<ResponseBody>> delete(@Path("path") String path, @QueryMap Map<String, String> params);

    // 修改 restful
    @PUT("{path}/{id}")
    Observable<Response<ResponseBody>> put(@Path("path") String path, @Path("id") String id);

    // 修改
    @PUT("{path}")
    Observable<Response<ResponseBody>> put(@Path("path") String path, @QueryMap Map<String, String> params);

    @GET("sns/oauth2/access_token")
    Call<ResponseBody> loginByWx(@QueryMap Map<String, String> params);

    @GET("sns/userinfo")
    Call<ResponseBody> getWXUserInfo(@QueryMap Map<String, String> params);
}
