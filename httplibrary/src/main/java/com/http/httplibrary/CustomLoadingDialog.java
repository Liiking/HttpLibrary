package com.http.httplibrary;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

/**
 * Created by qwy on 17/8/8.
 * 自定义加载框
 */
public class CustomLoadingDialog extends Dialog {

    public CustomLoadingDialog(Context context){
            super(context, R.style.CustomDialogStyle);
        }

    /**
     * 自定义加载框主题
     * @param context
     * @param theme
     */
    public CustomLoadingDialog(Context context, int theme){
            super(context, theme);
        }

        protected void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            setContentView(R.layout.dialog_custom_loading);
        }

}