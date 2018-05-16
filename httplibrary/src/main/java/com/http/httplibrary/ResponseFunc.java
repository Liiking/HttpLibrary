package com.http.httplibrary;

import com.http.httplibrary.response.BaseResponse;
import rx.functions.Func1;

/**
 * Created by qwy on 17/7/13.
 * 转换响应信息，取出data部分返回给客户端
 */
public class ResponseFunc<T> implements Func1<BaseResponse<T>, T> {

    @Override
    public T call(BaseResponse<T> httpResult) {
        return httpResult.getData();
    }
}
