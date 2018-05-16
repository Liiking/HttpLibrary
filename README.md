# HttpLibrary
## rxjava1+retrofit2的一个初级封装。包括统一异常处理，加载框，取消订阅，拦截器（日志、加密、请求头），文件上传下载模块（支持断点续传）等。不断优化中。

本项目为使用demo。
使用方法：
### 1.clone后引入httplibrary为module。
### 2.在自定义Application调用ApiManager.initApiManger(this, BASE_URL);初始化。
### 3.get请求示例：
###     ApiManager.getInstance()
        .requestGet(MainActivity.this, API_REQUEST_PATH, Object.class, p, new SubscriberListener<Object>() {
            @Override
            public void onNext(Object obj) {
                textView.setText(new Gson().toJson(obj));
            }
        });

#### 可根据接口约定修改BaseResponse类，如返回格式为 code message data，此处onNext直接操作data。
