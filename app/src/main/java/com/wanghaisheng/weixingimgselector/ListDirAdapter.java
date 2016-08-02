package com.wanghaisheng.weixingimgselector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wanghaisheng.weixingimgselector.bean.FolderBean;

import java.util.List;

/**
 * Created by sheng on 2016/8/2.
 */
public class ListDirAdapter extends ArrayAdapter<FolderBean> {
    private Context mContext;
    private List<FolderBean> mDatas;
    private String mCurrentPathDir;
    private LayoutInflater mLayoutInflater;

    private PopupWindowItemClickListener itemClickListener;

    public void setItemClickListener(PopupWindowItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public void setCurrentPathDir(String currentPathDir) {
        this.mCurrentPathDir = currentPathDir;
    }

    public ListDirAdapter(Context context, List<FolderBean> datas) {
        super(context, 0, datas);
        mContext = context;
        mDatas = datas;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if(convertView == null) {
            viewHolder = new ViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.item_popup_window,parent,false);
            ImageView ivThumb = (ImageView) convertView.findViewById(R.id.iv_thumb);
            TextView tvDir = (TextView) convertView.findViewById(R.id.tv_dir);
            TextView tvCount = (TextView) convertView.findViewById(R.id.tv_count);
            ImageView ivCurrentPathIndicator = (ImageView) convertView.findViewById(R.id.iv_current_folder_indicator);

            viewHolder.ivThumb = ivThumb;
            viewHolder.tvDir = tvDir;
            viewHolder.tvCount = tvCount;
            viewHolder.ivCurrentPathIndicator = ivCurrentPathIndicator;
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        final FolderBean folderBean = getItem(position);
        ImageView ivThumb = viewHolder.ivThumb;
        //重置ImageView
        ivThumb.setImageResource(R.drawable.pictures_no);
        ImageLoader.getInstance().loadImage(folderBean.getFirstImgPath(),ivThumb);
        TextView tvDir = viewHolder.tvDir;
        String dirName = folderBean.getDir();
        dirName = dirName.substring(dirName.lastIndexOf("/")+1);
        tvDir.setText(dirName);
        TextView tvCount = viewHolder.tvCount;
        tvCount.setText(folderBean.getImgCount()+"张");
        ImageView indicator = viewHolder.ivCurrentPathIndicator;
        String foldPath = folderBean.getDir();
//        LogUtils.d("foldpath  "+foldPath+"  mcurrentpath dir "+mCurrentPathDir);
        if(foldPath.equals(mCurrentPathDir)) {
            indicator.setVisibility(View.VISIBLE);
            indicator.setImageResource(R.drawable.icon_folder_selected);
        } else {
            indicator.setVisibility(View.GONE);
        }

        return convertView;
    }

    class ViewHolder {
        ImageView ivThumb;
        TextView tvDir;
        TextView tvCount;
        ImageView ivCurrentPathIndicator;
    }
}
