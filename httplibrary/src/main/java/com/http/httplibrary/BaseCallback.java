package com.http.httplibrary;

import android.app.Activity;
import android.text.TextUtils;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public abstract class BaseCallback<T> implements Callback<T> {
    private String hookerRedirectUrl;

    public BaseCallback() {

    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        try {
            /***
             * Map路径
             */
            myOnResponse(call, response);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            /**
             * javaBean路径
             */
            onResponse(response, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        try {
            myOnFailure(call, t);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            onFailure(t);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            t.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void myOnResponse(Call<T> call, Response<T> response) {
    }


    public void myOnFailure(Call<T> call, Throwable t) {
    }


    public void onResponse(Response<T> response, Retrofit retrofit) {
    }


    public void onFailure(Throwable t) {
    }


    public static int testNum = 1;//测试数据


    /***
     * 是否是ok的请求
     * code为200或200.0等
     *
     * @param code
     * @return
     */
    public final static boolean isOkCode(String code) {
        String zc = "[0-9]+\\.0+";
        if (!TextUtils.isEmpty(code) && code.matches(zc)) {
            code = code.replaceAll("\\.0+", "");
        }
        return !TextUtils.isEmpty(code) && Utility.transformNum(code, 0) == 200;
    }

    /***
     * 是否是ok的请求
     * code为200或200.0等
     *
     * @param code
     * @return
     */
    public final static boolean isOkNewCode(String code) {
        String zc = "[0-9]+\\.0+";
        if (!TextUtils.isEmpty(code) && code.matches(zc)) {
            code = code.replaceAll("\\.0+", "");
        }
        return !TextUtils.isEmpty(code) && Utility.transformNum(code, -1) == 0;
    }

    public <T> T baseDeal(Activity context, Object ob, Class<T> tClass, boolean isShowError, boolean isVerifyCode) {
        try {
            String code = "";
            String message = "";
            if (ob instanceof Map) {
                Map row = (Map) ob;
                code = Utility.getValueFromMap(row, "code", "");
                message = Utility.getValueFromMap(row, "message", "");
            }

            if (isVerifyCode) {
                if (isOkCode(code)) {
                    return JsonParser.json2Bean(JsonParser.bean2Json(ob), tClass);
                } else {
                    if (isShowError)
                        Utility.shortToast(context, message);
                }
            } else {
                return JsonParser.json2Bean(JsonParser.bean2Json(ob), tClass);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public <T> T baseDeal(Activity context, Object ob, Class<T> tClass, boolean isShowError) {
        return baseDeal(context, ob, tClass, isShowError, true);
    }


    /**
     */
    public void dealRxResponse(T obj) {

    }
}
