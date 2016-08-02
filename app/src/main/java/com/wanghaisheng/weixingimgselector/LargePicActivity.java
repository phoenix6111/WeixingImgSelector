package com.wanghaisheng.weixingimgselector;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.apkfuns.logutils.LogUtils;

import java.util.ArrayList;

/**
 * Created by sheng on 2016/8/2.
 */
public class LargePicActivity extends AppCompatActivity {
    public static final String ARG_INEX = "arg_index";
    public static final String ARG_URLS = "arg_urls";

    Toolbar mToolbar;
    TextView tvTitle;
    Button btnOk;
    ViewPager mViewpager;
    ImageView ivSelect;
    RelativeLayout rlBottom;

    FragmentStatePagerAdapter pagerAdapter;
    private int index;
    private ArrayList<String> urls = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_large_pic);

        setTranslucentStatus(isApplyStatusBarTranslucency());
        setStatusBarColor(isApplyStatusBarColor());

        getDatas();

        initView();
        initData();
        initEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentUrl = urls.get(mViewpager.getCurrentItem());
        if(ImageAdapter.SELECTED_IMGS.contains(currentUrl)) {
            ivSelect.setImageResource(R.drawable.icon_checkbox_selected);
        } else {
            ivSelect.setImageResource(R.drawable.icon_checkbox_unselected);
        }
    }

    protected void initToolbar(Toolbar toolbar) {
//        toolbar.setNavigationIcon(R.drawable.tabbar_back_filter);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    public void getDatas() {
        Intent intent = getIntent();
        this.index = intent.getIntExtra(ARG_INEX,0);
        this.urls = intent.getStringArrayListExtra(ARG_URLS);
    }

    private void initView() {
        this.mToolbar = (Toolbar) findViewById(R.id.toolbar);
        initToolbar(mToolbar);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvTitle.setText(""+(index+1)+"/"+urls.size());
        ivSelect = (ImageView) findViewById(R.id.iv_select);
        this.mViewpager = (ViewPager) findViewById(R.id.view_pager);
        this.rlBottom = (RelativeLayout) findViewById(R.id.rl_bottom);
        this.btnOk = (Button) findViewById(R.id.btn_ok);
    }

    private void initEvents() {
        ivSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentUrl = urls.get(mViewpager.getCurrentItem());
                if(ImageAdapter.SELECTED_IMGS.contains(currentUrl)) {
                    ImageAdapter.SELECTED_IMGS.remove(currentUrl);
                    ivSelect.setImageResource(R.drawable.icon_checkbox_unselected);
                } else {
                    ImageAdapter.SELECTED_IMGS.add(currentUrl);
                    ivSelect.setImageResource(R.drawable.icon_checkbox_selected);
                }
            }
        });

        mViewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                tvTitle.setText(""+(position+1)+"/"+urls.size());
                String currentUrl = urls.get(mViewpager.getCurrentItem());
                if(ImageAdapter.SELECTED_IMGS.contains(currentUrl)) {
                    ivSelect.setImageResource(R.drawable.icon_checkbox_selected);
                } else {
                    ivSelect.setImageResource(R.drawable.icon_checkbox_unselected);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void initData() {
        pagerAdapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return LargePicFragment.newInstance(position,urls.get(position));
            }

            @Override
            public int getCount() {
                return urls.size();
            }
        };
        mViewpager.setAdapter(pagerAdapter);
        mViewpager.setCurrentItem(index);

    }

    /**
     * is applyStatusBarTranslucency
     *
     * @return
     */
    protected boolean isApplyStatusBarTranslucency() {
        return true;
    }

    /**
     * set status bar translucency
     *
     * @param on
     */
    protected void setTranslucentStatus(boolean on) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window win = getWindow();
            WindowManager.LayoutParams winParams = win.getAttributes();
            final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            if (on) {
                winParams.flags |= bits;
            } else {
                winParams.flags &= ~bits;
            }
            win.setAttributes(winParams);
        }
    }

    protected boolean isApplyStatusBarColor() {
        return true;
    }


    public void setStatusBarColor(boolean on) {
        if (on) {
            StatusBarUtil.setColor(this, getThemeColor(this), 0);
        }
    }

    public int getThemeColor(@NonNull Context context) {
        return getThemeAttrColor(context, R.attr.colorPrimary);
    }

    public int getThemeAttrColor(@NonNull Context context, @AttrRes int attr) {
        TypedArray a = context.obtainStyledAttributes(null, new int[]{attr});
        try {
            return a.getColor(0, 0);
        } finally {
            a.recycle();
        }
    }

    public void showOrHideToolbar() {
        float height = mToolbar.getY();
        if(height<0) {
            showToolbar();
        } else {
            hideToolbar();
        }

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);

        int winHeight = outMetrics.heightPixels;
        LogUtils.d("rl y "+rlBottom.getY()+"  winheight "+winHeight+" rl height "+rlBottom.getHeight());
        if(rlBottom.getY()>=(winHeight-rlBottom.getHeight())) {
            showBottomToolbar();
            LogUtils.d("rl y "+rlBottom.getY()+"  winheight "+winHeight);
        } else {
            hideBottomToolbar();
        }
    }

    public void hideToolbar() {
        mToolbar.animate().translationY(-mToolbar.getHeight()).setInterpolator(new AccelerateInterpolator(2));
    }

    public void showToolbar() {
        mToolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2));
    }

    public void hideBottomToolbar() {
        rlBottom.animate().translationY(rlBottom.getHeight()).setInterpolator(new AccelerateInterpolator(2));
    }

    public void showBottomToolbar() {
        rlBottom.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2));
    }


}
