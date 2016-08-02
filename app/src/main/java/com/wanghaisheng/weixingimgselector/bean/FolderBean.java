package com.wanghaisheng.weixingimgselector.bean;

/**
 * Created by sheng on 2016/8/1.
 */
public class FolderBean {

    //文件路径
    private String dir;
    private String name;
    //图片数
    private int imgCount;
    //第一张图片的path
    private String firstImgPath;

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
        int lastIndex = dir.lastIndexOf("/");
        this.name = dir.substring(lastIndex+1);
    }

    public String getName() {
        return name;
    }

    public int getImgCount() {
        return imgCount;
    }

    public void setImgCount(int imgCount) {
        this.imgCount = imgCount;
    }

    public String getFirstImgPath() {
        return firstImgPath;
    }

    public void setFirstImgPath(String firstImgPath) {
        this.firstImgPath = firstImgPath;
    }
}
