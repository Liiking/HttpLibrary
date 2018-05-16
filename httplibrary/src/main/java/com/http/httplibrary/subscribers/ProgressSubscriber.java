package com.http.httplibrary.subscribers;

import android.content.Context;
import com.http.httplibrary.exception.ApiException;
import com.http.httplibrary.progress.ProgressCancelListener;
import com.http.httplibrary.progress.ProgressDialogHandler;

/**
 * Created by qwy on 16/3/10.
 * 用于在Http请求开始时，自动显示一个ProgressDialog
 * 在Http请求结束是，关闭ProgressDialog
 * 调用者自己对请求数据进行处理
 */
public class ProgressSubscriber<T> extends MySubscriber<T> implements ProgressCancelListener {

    private SubscriberListener mSubscriberListener;
    private ProgressDialogHandler mProgressDialogHandler;
    private boolean hideLoading = false;// 是否隐藏加载框，默认展示
    private boolean hideMsg = false;// 是否隐藏无网络提示，默认展示
    private Context context;

    public ProgressSubscriber(Context context, SubscriberListener<T> mSubscriberListener) {
        this(context, false, false, mSubscriberListener);
    }

    public ProgressSubscriber(Context context, boolean hideLoading, SubscriberListener<T> mSubscriberListener) {
        this(context, hideLoading, false, mSubscriberListener);
    }

    public ProgressSubscriber(Context context, boolean hideLoading, boolean hideMsg, SubscriberListener<T> mSubscriberListener) {
        super(context, hideMsg);
        this.hideLoading = hideLoading;
        this.mSubscriberListener = mSubscriberListener;
        this.context = context;
        if (!hideLoading) {
            mProgressDialogHandler = new ProgressDialogHandler(context, this, false);
        }
    }

    private void showProgressDialog() {
        if (mProgressDialogHandler != null && !hideLoading) {
            mProgressDialogHandler.obtainMessage(ProgressDialogHandler.SHOW_PROGRESS_DIALOG).sendToTarget();
        }
    }

    private void dismissProgressDialog() {
        if (mProgressDialogHandler != null) {
            mProgressDialogHandler.obtainMessage(ProgressDialogHandler.DISMISS_PROGRESS_DIALOG).sendToTarget();
            mProgressDialogHandler = null;
        }
    }

    /**
     * 订阅开始时调用
     * 显示ProgressDialog
     */
    @Override
    public void onRequestStart() {
        if (mSubscriberListener != null) {
            mSubscriberListener.onStart();
        }
        showProgressDialog();
    }

    /**
     * 完成，隐藏ProgressDialog
     */
    @Override
    public void onCompleted() {
        super.onCompleted();
        if (mSubscriberListener != null) {
            mSubscriberListener.onCompleted();
        }
        dismissProgressDialog();
    }

    /**
     * 对错误进行统一处理
     * 隐藏ProgressDialog
     *
     * @param e
     */
    @Override
    public void onError(ApiException e) {
        if (mSubscriberListener != null) {
            mSubscriberListener.onError(e);
        }
        dismissProgressDialog();
    }

    /**
     * 将onNext方法中的返回结果交给Activity或Fragment自己处理
     * @param t 创建Subscriber时的泛型类型
     */
    @Override
    public void onNext(T t) {
        if (mSubscriberListener != null) {
            mSubscriberListener.onNext(t);
        }
    }

    /**
     * 取消ProgressDialog的时候，取消对observable的订阅，同时也取消了http请求
     */
    @Override
    public void onCancelProgress() {
        if (!this.isUnsubscribed()) {
            this.unsubscribe();
        }
    }
}