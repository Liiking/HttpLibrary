package com.http.httplibrary.exception;

import rx.Observable;
import rx.functions.Func1;

/**
 * Created by qwy on 17/8/29.
 * 全局异常处理
 */
public class HttpResponseFunc<T> implements Func1<Throwable, Observable<T>> {
    @Override
    public Observable<T> call(Throwable throwable) {
        // ExceptionEngine为处理异常的驱动器
        ApiException ae = ExceptionEngine.handleException(throwable);
        return Observable.error(ae);
    }
}