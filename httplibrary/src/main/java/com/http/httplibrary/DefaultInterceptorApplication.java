package com.http.httplibrary;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 应用层拦截器
 * Created by RP on 2015/12/15.
 */
public class DefaultInterceptorApplication extends DefaultInterceptorNetwork {
    private final Handler mHander = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
        }
    };

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        try {
            // 网络检查
            Response response = chain.proceed(chain.request());
            String content = response.body().string();
            response = createResponse(request, response, content);
            String newContent = content;
            try {
                newContent = response.body().string();
            } catch (Exception e) {
                e.printStackTrace();
            }
            response = response.newBuilder().body(ResponseBody.create(response.body().contentType(), newContent)).build();
            try {
                if (Utility.showLog) {
//                    Utility.log("intercept-req", request.toString() + ";\n" + newContent);
                    Utility.LogTooLongE("intercept", request.toString() + "; \n" + Utility.formatJson(newContent));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        } catch (Exception e) {
            Utility.log("intercept", "intercept:request=" + request.toString() + "; \nresponse:Exception" + e.toString());
            e.printStackTrace();
        }
        return getNullResponse(request);

    }

    /***
     * 解密数据
     *
     * @param response
     * @return
     */
    private Response createResponse(Request request, Response response, String content) {
        Response.Builder builder = response.newBuilder();
        // 其他处理
        return builder.body(ResponseBody.create(response.body().contentType(), content)).build();
    }

    private Response getNullResponse(Request request) {
        Response.Builder builder = new Response.Builder();
        builder.request(request);
        builder.protocol(Protocol.HTTP_1_1);
        builder.code(500);
        return builder.build();
    }


}
