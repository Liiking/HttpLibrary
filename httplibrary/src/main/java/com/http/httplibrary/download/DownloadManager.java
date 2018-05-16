package com.http.httplibrary.download;

import android.content.Context;
import android.text.TextUtils;
import com.http.httplibrary.Utility;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by qwy on 2017/2/2.
 * 下载管理器,支持断点续传
 */
public class DownloadManager {

    private static final AtomicReference<DownloadManager> INSTANCE = new AtomicReference<>();
    private HashMap<String, Call> downCalls;// 用来存放各个下载的请求
    private OkHttpClient mClient;// OKHttpClient;
    private static String ROOT_DOWNLOAD_DIR;

    // 获得一个单例类
    public static DownloadManager getInstance() {
        for (; ; ) {
            DownloadManager current = INSTANCE.get();
            if (current != null) {
                return current;
            }
            current = new DownloadManager();
            if (INSTANCE.compareAndSet(null, current)) {
                return current;
            }
        }
    }

    public DownloadManager rootDownloadDir(String dir) {
        ROOT_DOWNLOAD_DIR = dir;
        return getInstance();
    }

    public static String getRootDownloadDir() {
        return ROOT_DOWNLOAD_DIR;
    }

    private DownloadManager() {
        downCalls = new HashMap<>();
        mClient = new OkHttpClient.Builder().build();
    }

    /**
     * 开始下载
     * @param url              下载请求的网址
     * @param downLoadObserver 用来回调的接口
     */
    public void download(Context context, String url, DownLoadObserver downLoadObserver) {
        Observable.just(url)
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return !downCalls.containsKey(s);
                    }
                })
                .flatMap(new Func1<String, Observable<DownloadInfo>>() {
                    @Override
                    public Observable<DownloadInfo> call(String s) {
                        return Observable.just(createDownInfo(s));
                    }
                }).filter(new Func1<DownloadInfo, Boolean>() {
                    @Override
                    public Boolean call(DownloadInfo s) {
                        return s != null;
                    }
                })
                .flatMap(new Func1<DownloadInfo, Observable<DownloadInfo>>() {
                    @Override
                    public Observable<DownloadInfo> call(DownloadInfo info) {
                        return Observable.create(new DownloadSubscribe(info));
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())// 在主线程回调
//                .sample(800, TimeUnit.MILLISECONDS)// 限制生产者发送数据的速度，防止MissingBackpressureException
                .subscribeOn(Schedulers.io())// 在子线程执行
                .subscribe(downLoadObserver);// 添加观察者
    }

    /**
     * 根据下载地址获取下载文件（下载成功后调用）
     * @param url
     * @return
     */
    public static File getDownloadFile(String url) {
        String fileName = getFileName(url);
        if(TextUtils.isEmpty(fileName)){
            return null;
        }
        return new File(getRootDownloadDir(), fileName);
    }

    /**
     * 从URL里截取文件名
     * @param url
     * @return
     */
    private static String getFileName(String url) {
        if(TextUtils.isEmpty(url)){
            return "";
        }
        return url.substring(url.lastIndexOf("/"));
    }

    /**
     * 创建DownInfo
     * @param url 请求网址
     * @return DownInfo
     */
    private DownloadInfo createDownInfo(String url) {
        DownloadInfo downloadInfo = new DownloadInfo(url);
        long contentLength = getContentLength(url);// 获得文件大小
        downloadInfo.setTotal(contentLength);
        if (contentLength <= 0) {
            return null;
        }
        String fileName = getFileName(url);
        downloadInfo.setFileName(fileName);
        File file = new File(getRootDownloadDir(), fileName);
        Utility.chmod(file.getAbsolutePath());
        // 设置改变过的文件名/大小
        downloadInfo.setProgress(0);
        return downloadInfo;
    }

    private class DownloadSubscribe implements Observable.OnSubscribe<DownloadInfo> {
        private DownloadInfo downloadInfo;

        public DownloadSubscribe(DownloadInfo downloadInfo) {
            this.downloadInfo = downloadInfo;
        }

        @Override
        public void call(Subscriber<? super DownloadInfo> e) {
            try {
                String url = downloadInfo.getUrl();
                long downloadLength = downloadInfo.getProgress();// 已经下载好的长度
                long contentLength = downloadInfo.getTotal();// 文件的总长度
                // 初始进度信息
                e.onNext(downloadInfo);

                Request request = new Request.Builder()
                        // 确定下载的范围,添加此头,则服务器就可以跳过已经下载好的部分
                        .addHeader("RANGE", "bytes=" + downloadLength + "-" + contentLength)
                        .url(url)
                        .build();
                Call call = mClient.newCall(request);
                downCalls.put(url, call);// 把这个添加到call里,方便取消
                Response response = call.execute();

                File file = new File(getRootDownloadDir(), downloadInfo.getFileName());
                InputStream is = null;
                FileOutputStream fileOutputStream = null;
                try {
                    is = response.body().byteStream();
                    fileOutputStream = new FileOutputStream(file, true);
                    byte[] buffer = new byte[2048];// 缓冲数组2kB
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, len);
                        downloadLength += len;
                        downloadInfo.setProgress(downloadLength);
                        // 对下载进度做个过滤，防止多次调用
                        if (downloadLength * 1000 / downloadInfo.getTotal() % 10 == 0) {
                            e.onNext(downloadInfo);
                        }
                    }
                    fileOutputStream.flush();
                    downCalls.remove(url);
                } finally {
                    // 关闭IO流
                    closeAll(is, fileOutputStream);
                }
                e.onCompleted();// 完成
            }catch (Exception e1){
                e.onError(e1);
            }
        }
    }

    public static void closeAll(Closeable... closeables){
        if(closeables == null){
            return;
        }
        for (Closeable closeable : closeables) {
            if(closeable!=null){
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取下载长度
     * @param downloadUrl
     * @return
     */
    public long getContentLength(String downloadUrl) {
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        try {
            Response response = mClient.newCall(request).execute();
            if (response != null && response.isSuccessful()) {
                long contentLength = 0;
                ResponseBody body = response.body();
                if(body != null) {
                    contentLength = body.contentLength();
                }
                return contentLength == 0 ? DownloadInfo.TOTAL_ERROR : contentLength;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DownloadInfo.TOTAL_ERROR;
    }


}
