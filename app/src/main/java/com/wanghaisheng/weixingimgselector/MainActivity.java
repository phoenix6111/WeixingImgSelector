package com.wanghaisheng.weixingimgselector;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.apkfuns.logutils.LogUtils;
import com.wanghaisheng.weixingimgselector.bean.FolderBean;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements PopupWindowItemClickListener,OnImageSelectChangeListener{
    private static final String TAG = "MainActivity";
    public static final int SCAN_COMPLETED = 0x110;

    Toolbar mToolbar;
    GridView mGridView;
    RelativeLayout rlBottom;
    TextView tvPath;
    TextView tvCount;

    private ImageAdapter mAdapter;
    /**
     * 当前文件夹所有img的 name
     */
    private List<String> mCurrentDirImgs;
    /**
     * 当前显示文件的dir
     */
    private String mCurrentPathDir;
    /**
     * 当前显示文件dir内图片的count
     */
    private int mCurrentPathImgCount;

    ProgressDialog mWaitDialog;
    ScanFolderHandler scanFolderHandler;

    ImageDirPopupWindow dirPopupWindow;

    private List<FolderBean> mScanedFolderBeans = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
        initEvent();
    }

    private void initView() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.app_name);
        mGridView = (GridView) findViewById(R.id.gridview);
        rlBottom = (RelativeLayout) findViewById(R.id.rl_popup_container);
        tvPath = (TextView) findViewById(R.id.tv_path);
        tvCount = (TextView) findViewById(R.id.tv_count);
    }

    private void initEvent() {
        rlBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dirPopupWindow.showAsDropDown(rlBottom,0,0);
                dirPopupWindow.setCurrentPathDir(mCurrentPathDir);
                lightOff();
            }
        });

        tvCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ImageAdapter.SELECTED_IMGS.size()>0) {
                    Intent intent = new Intent(MainActivity.this,LargePicActivity.class);
                    intent.putExtra(LargePicActivity.ARG_INEX,0);
                    intent.putExtra(LargePicActivity.ARG_URLS, setToArrayList(ImageAdapter.SELECTED_IMGS));
                    startActivity(intent);
                }
            }
        });
    }

    private ArrayList<String> setToArrayList(Set<String> sets) {
        ArrayList<String> lists = new ArrayList<>();
        for (String str : sets) {
            lists.add(str);
        }

        return lists;
    }

    private void initPopupWindow() {
        dirPopupWindow = new ImageDirPopupWindow(MainActivity.this,mScanedFolderBeans);
        dirPopupWindow.setCurrentPathDir(mCurrentPathDir);
        dirPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });

        dirPopupWindow.setItemClickListener(this);
        dirPopupWindow.setAnimationStyle(R.style.dir_popupwindow_anim);
    }

    /**
     * 使屏幕区域变暗
     */
    private void lightOff() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.3f;
        getWindow().setAttributes(lp);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }

        tvCount.setText("预览("+ImageAdapter.SELECTED_IMGS.size()+")");
    }

    /**
     * 使屏幕区域变暗
     */
    private void lightOn() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 1.0f;
        getWindow().setAttributes(lp);
    }

    private void initData() {
        scanFolderHandler = new ScanFolderHandler(MainActivity.this);

        //检测是否有SD卡
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(MainActivity.this,"没有SD卡",Toast.LENGTH_LONG).show();
            return;
        }

        mWaitDialog = ProgressDialog.show(MainActivity.this,null,"正在加载中...");

        //新开一个线程扫描图片
        Thread scanImgThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver contentResolver = getContentResolver();

                Cursor cursor = contentResolver.query(uri,null,MediaStore.Images.Media.MIME_TYPE+" = ? or "+
                        MediaStore.Images.Media.MIME_TYPE+" = ? or "+MediaStore.Images.Media.MIME_TYPE+" = ?"
                        ,new String[]{"image/jpeg","image/png","image/gif"},MediaStore.Images.Media.DATE_MODIFIED);
                Set<String> scanedFolder = new HashSet<>();
                int count = cursor.getCount();
                LogUtils.d("scaned  count "+count);
                while(cursor.moveToNext()) {
                    String imgPath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    //获取图片所在的父文件夹
                    File parentFile = new File(imgPath).getParentFile();
                    if(parentFile == null) {
                        continue;
                    }

                    String imgFolder = parentFile.getAbsolutePath();
                    //如果img所在的Folder已经扫描过了，则不继续执行
                    if(scanedFolder.contains(imgFolder)) {
                        continue;
                    }

                    FolderBean folderBean = new FolderBean();
                    folderBean.setDir(imgFolder);
                    folderBean.setFirstImgPath(imgPath);
                    if(parentFile.list() == null) {
                        continue;
                    }
                    int imgCount = parentFile.list(new ImageFilter()).length;
                    folderBean.setImgCount(imgCount);

                    scanedFolder.add(imgFolder);
                    if(imgCount > mCurrentPathImgCount) {
                        mCurrentPathImgCount = imgCount;
                        mCurrentPathDir = imgFolder;
                    }

                    mScanedFolderBeans.add(folderBean);
                }

                cursor.close();
                //图片扫描完成，通知更新GridView中的内容
                scanFolderHandler.sendEmptyMessage(SCAN_COMPLETED);
            }
        });

        scanImgThread.start();
    }

    @Override
    public void onPopupWindowItemClick(FolderBean folderBean) {
        mCurrentPathDir = folderBean.getDir();
        mCurrentDirImgs = Arrays.asList(getFolderImageNames(mCurrentPathDir));
        mCurrentPathImgCount = mCurrentDirImgs.size();
        mAdapter = new ImageAdapter(MainActivity.this,mCurrentDirImgs,mCurrentPathDir);
        mAdapter.setSelectChangeListener(this);
        mGridView.setAdapter(mAdapter);

        tvPath.setText(mCurrentPathDir.substring(mCurrentPathDir.lastIndexOf("/")+1));
        tvCount.setText("预览("+ImageAdapter.SELECTED_IMGS.size()+")");
    }

    @Override
    public void onImageSelectChange() {
        tvCount.setText("预览("+ImageAdapter.SELECTED_IMGS.size()+")");
    }

    /**
     * 扫描图片完成时即调用此handler更新UI
     */
    public class ScanFolderHandler extends Handler {
        WeakReference<MainActivity> activityReference;

        public ScanFolderHandler(MainActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if(msg.what == SCAN_COMPLETED) {
                mWaitDialog.dismiss();

                //如果mCurrentPathDir 为 null则说明没扫描到图片
                if(mCurrentPathDir == null) {
                    Toast.makeText(MainActivity.this,"找不到图片",Toast.LENGTH_LONG).show();
                    return;
                }
                //显示GridView中的内容
                showGridView();

                initPopupWindow();
            }
        }
    }

    /**
     *
     */
    private void showGridView() {
        mCurrentDirImgs = Arrays.asList(getFolderImageNames(mCurrentPathDir));
//        LogUtils.d(mCurrentDirImgs);
        mAdapter = new ImageAdapter(MainActivity.this,mCurrentDirImgs,mCurrentPathDir);
        mAdapter.setSelectChangeListener(this);
        mGridView.setAdapter(mAdapter);

        String folderName = mCurrentPathDir.substring(mCurrentPathDir.lastIndexOf("/")+1);
        tvPath.setText(folderName);
        tvCount.setText("预览("+ImageAdapter.SELECTED_IMGS.size()+")");
    }

    private String[] getFolderImageNames(String folderName) {
        File folder = new File(folderName);
        if(folder.exists()) {
            return folder.list(new ImageFilter());
        }
        return null;
    }

    public class ImageFilter implements FilenameFilter {
        @Override
        public boolean accept(File file, String name) {
            return name.endsWith(".jpg")||name.endsWith(".jpeg")||name.endsWith(".png")||name.endsWith(".gif");
        }
    }


}
