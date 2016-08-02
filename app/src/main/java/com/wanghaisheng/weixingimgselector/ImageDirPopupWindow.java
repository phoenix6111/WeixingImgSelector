package com.wanghaisheng.weixingimgselector;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.wanghaisheng.weixingimgselector.bean.FolderBean;

import java.util.List;

/**
 * Created by sheng on 2016/8/1.
 */
public class ImageDirPopupWindow extends PopupWindow {
    //PoupupWindow的宽和高
    private int mWidth;
    private int mHeight;

    //ListView中的数据
    private List<FolderBean> mDatas;
    //当前选中的图片文件夹path
    private String mCurrentPathDir;
    private Context mContext;
    View convertView;
    ListView mListView;
    ListDirAdapter mAdapter;

    private PopupWindowItemClickListener itemClickListener;

    public void setItemClickListener(PopupWindowItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public void setCurrentPathDir(String currentPathDir) {
        this.mCurrentPathDir = currentPathDir;
        mAdapter.setCurrentPathDir(mCurrentPathDir);
        mAdapter.notifyDataSetChanged();
    }

    public ImageDirPopupWindow(Context context, List<FolderBean> datas) {
        this.mContext = context;
        this.mDatas = datas;
        //设置宽和高
        calculateWidthAndHeight();
        convertView = LayoutInflater.from(mContext).inflate(R.layout.popup_window_layout,null);
        setContentView(convertView);
        //设置宽度和高度
        setWidth(mWidth);
        setHeight(mHeight);

        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        initView();
        initEvent();

    }


    private void initEvent() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(itemClickListener != null) {
                    itemClickListener.onPopupWindowItemClick(mDatas.get(i));
                    dismiss();
                }
            }
        });
    }

    private void initView() {
        mListView = (ListView) convertView.findViewById(R.id.popup_listview);
        mAdapter = new ListDirAdapter(mContext,mDatas);
        mAdapter.setItemClickListener(itemClickListener);
        mListView.setAdapter(mAdapter);
    }

    //设置宽和高，宽为屏幕的宽度，高为屏幕高度的0.7倍
    private void calculateWidthAndHeight() {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);

        mWidth = outMetrics.widthPixels;
        mHeight = (int) (outMetrics.heightPixels*0.75f);
//        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
    }

}
