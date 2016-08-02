package com.wanghaisheng.weixingimgselector;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by sheng on 2016/8/2.
 */
public class LargePicFragment extends Fragment {
    public static final String ARG_INDEX = "arg_index";
    public static final String ARG_URL = "arg_url";

    private String url;
    private ImageView image;

    public static LargePicFragment newInstance(int index, String url) {
        LargePicFragment fragment = new LargePicFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_INDEX,index);
        bundle.putString(ARG_URL,url);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.frgt_large_pic,container, false);
        this.image = (ImageView) parentView.findViewById(R.id.image);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LargePicActivity activity = (LargePicActivity) getActivity();
                activity.showOrHideToolbar();
            }
        });

        return parentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.url = getArguments().getString(ARG_URL);
        ImageLoader.getInstance().loadImage(url,image);
    }
}
