package com.http.httplibrary.subscribers;

import android.content.Context;
import com.http.httplibrary.R;
import com.http.httplibrary.Utility;
import com.http.httplibrary.exception.ApiException;
import com.http.httplibrary.exception.ERROR;
import rx.Subscriber;

/**
 * Created by qwy on 2017/4/19.
 * Subscriber基类,可以在这里处理client网络连接状况
 * （比如没有wifi，没有4g，没有联网等）
 */
public abstract class MySubscriber<T> extends Subscriber<T> {

    private Context context;
    private boolean hideMsg;// 是否隐藏网络异常toast

    public MySubscriber(Context context) {
        this(context, false);
    }

    public MySubscriber(Context context, boolean hideMsg) {
        this.context = context;
        this.hideMsg = hideMsg;
    }

    @Override
    public void onStart() {
        super.onStart();
        // 接下来可以检查网络连接等操作
        if (!Utility.hasNet(context)) {
            if (!hideMsg) {
                Utility.shortToast(context, R.string.no_net_error);
            }
            if (context != null) {
                onError(new ApiException(new Throwable(context.getResources().getString(R.string.no_net_error)), ERROR.NETWORK_ERROR));
            }
            // 取消本次Subscriber订阅
            if (!isUnsubscribed()) {
                unsubscribe();
            }
        } else {
            onRequestStart();
        }
    }

    public abstract void onRequestStart();

    @Override
    public void onError(Throwable e) {
        if (e instanceof ApiException){
            // 访问获得对应的Exception
            ApiException ae = (ApiException) e;
            if (ae.code == ERROR.SHOW_MESSAGE_ERROR) {
                // 参数错误，展示错误信息
                Utility.shortToastInMainThread(context, ae.message);
            } else if (ae.code == ERROR.TOKEN_NEED_REFRESH) {
                // TODO token过期,清除本地用户信息，跳登录等
            }
            onError(ae);
        } else {
            // 将Throwable 和 未知错误的status code返回
            onError(new ApiException(e, ERROR.UNKNOWN));
        }
    }

    public abstract void onError(ApiException e);

    @Override
    public void onCompleted() {
    }
}