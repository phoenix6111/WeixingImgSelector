package com.wanghaisheng.weixingimgselector;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by sheng on 2016/8/1.
 */
public class ImageAdapter extends BaseAdapter {

    private Context mContext;
    private List<String> fileNames;
    private String foldPath;
    private LayoutInflater mLayoutInflater;

    public static Set<String> SELECTED_IMGS = new HashSet<>();
    private OnImageSelectChangeListener selectChangeListener;

    public void setSelectChangeListener(OnImageSelectChangeListener selectChangeListener) {
        this.selectChangeListener = selectChangeListener;
    }

    public ImageAdapter(Context context, List<String> fileNames, String foldPath) {
        this.mContext = context;
        this.fileNames = fileNames;
        this.foldPath = foldPath;
        this.mLayoutInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return fileNames.size();
    }

    @Override
    public Object getItem(int i) {
        return fileNames.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup viewGroup) {
        ViewHolder viewHolder = null;
        if(convertView == null) {
            viewHolder = new ViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.item_gridview,viewGroup,false);
            ImageView ivImage = (ImageView) convertView.findViewById(R.id.iv_img);
            ImageView ivSelect = (ImageView) convertView.findViewById(R.id.iv_select);
            viewHolder.ivImage = ivImage;
            viewHolder.ivSelect = ivSelect;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        //重置状态
        final ImageView ivImage = viewHolder.ivImage;
        ivImage.setImageDrawable(new BitmapDrawable());
        final ImageView ivSelect = viewHolder.ivSelect;
        ivSelect.setImageResource(R.drawable.icon_checkbox_unselected);
        final String imgPath = foldPath+"/"+fileNames.get(position);
//        LogUtils.d(imgPath);
        ImageLoader.getInstance().loadImage(imgPath,ivImage);
//        LogUtils.d(SELECTED_IMGS);
        ivSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(SELECTED_IMGS.contains(imgPath)) {
                    SELECTED_IMGS.remove(imgPath);
                    ivSelect.setImageResource(R.drawable.icon_checkbox_unselected);
                    ivImage.setColorFilter(null);
                } else {
                    SELECTED_IMGS.add(imgPath);
                    ivSelect.setImageResource(R.drawable.icon_checkbox_selected);
                    ivImage.setColorFilter(Color.parseColor("#77000000"));
                }

                if(selectChangeListener != null) {
                    selectChangeListener.onImageSelectChange();
                }
            }
        });

        ivImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext,LargePicActivity.class);
                intent.putExtra(LargePicActivity.ARG_INEX,position);
                intent.putExtra(LargePicActivity.ARG_URLS,parseUrls());
                mContext.startActivity(intent);
            }
        });

        if(SELECTED_IMGS.contains(imgPath)) {
            ivSelect.setImageResource(R.drawable.icon_checkbox_selected);
            ivImage.setColorFilter(Color.parseColor("#77000000"));
        } else {
            ivSelect.setImageResource(R.drawable.icon_checkbox_unselected);
            ivImage.setColorFilter(null);
        }

        return convertView;
    }

    private ArrayList<String> parseUrls() {
        ArrayList<String> paths = new ArrayList<>();
        for (String str : fileNames) {
            paths.add(foldPath+"/"+str);
        }

        return paths;
    }

    public class ViewHolder {
        ImageView ivImage;
        ImageView ivSelect;
    }
}
