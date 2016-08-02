package com.wanghaisheng.weixingimgselector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by sheng on 2016/8/1.
 * 图片加载类 单例
 */
public class ImageLoader {

    private static final int GET_TASK = 0x110;
    private static ImageLoader instance;

    /**
     * 图片缓存核心类
     */
    private LruCache<String,Bitmap> mLruCache;

    /**
     * 线程池对象
     */
    private ExecutorService mThreadPool;
    //默认的线程池中线程数量
    public static final int DEFAULT_THREAD_COUNT = 1;
    //一个信号量，控制线程池中只有指定的threadCount个线程在执行
    private Semaphore mSemaphoreThreadPool;

    /**
     * 任务队列
     */
    private LinkedList<Runnable> mTaskQueue;

    /**
     * 后台轮询线程，它不停的在任务队列中取出任务并执行
     */
    private Thread mPoolThread;
    /**
     * 后台轮询线程Handler
     */
    private Handler mPoolThreadHandler;
    //一个信号量，防止mPoolThreadHandler还没初始化完成时，就在taskQueue中调用了mPoolThreadHandler的方法
    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);

    /**
     * UI 线程中的Handler
     */
    private Handler mUiHandler;

    //线程的高度方式，默认为后进先出
    private Type mType = Type.LIFO;
    public enum  Type {
        FIFO,LIFO;//线程的高度方式：先进先出，后进先出
    }

    private ImageLoader(int threadCount,Type type) {
        //初始化后台轮询线程
        initPoolThread();

        //初始化LruCache
        initLruCache();

        //初始化线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        //初始化任务队列
        mTaskQueue = new LinkedList<>();

        mType = type;

        mSemaphoreThreadPool = new Semaphore(threadCount);
    }

    /**
     * 双重锁机制，性能优化
     * @return
     */
    public static ImageLoader getInstance() {
        if(instance == null) {
            synchronized (ImageLoader.class) {
                if(instance == null) {
                    instance = new ImageLoader(DEFAULT_THREAD_COUNT,Type.LIFO);
                }
            }
        }

        return instance;
    }

    /**
     * 双重锁机制，性能优化
     * @return
     */
    public static ImageLoader getInstance(int threadCount,Type type) {
        if(instance == null) {
            synchronized (ImageLoader.class) {
                if(instance == null) {
                    instance = new ImageLoader(threadCount,type);
                }
            }
        }

        return instance;
    }


    /**
     * 初始化核心缓存
     */
    private void initLruCache() {
        //获取系统最大内存
        int maxmemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxmemory/8;

        /**
         * 初始化LruCache
         */
        mLruCache = new LruCache<String,Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                //内存算法
                return value.getRowBytes()*value.getHeight();
            }
        };
    }

    /**
     * 初始化后台轮询线程
     */
    private void initPoolThread() {
        mPoolThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        //从mTaskQueue中取出任务并执行
                        mThreadPool.execute(getTaskFromTaskQueue());
                        try {
                            //获取一个执行的任务之后获取一个信号量
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };

                mSemaphorePoolThreadHandler.release();

                Looper.loop();
            }
        });

        mPoolThread.start();
    }

    /**
     * 根据队列加载策略，从任务队列中获取任务
     * @return
     */
    private Runnable getTaskFromTaskQueue() {
        if(Type.FIFO == mType) {
            return mTaskQueue.removeFirst();
        } else {
            return mTaskQueue.removeLast();
        }
    }

    /**
     * 根据给定的path，为ImageView显示图片
     * @param path
     * @param imageView
     */
    public void loadImage(final String path, final ImageView imageView) {
        //ImageView设置tag，防止图片错乱
        imageView.setTag(path);

        if(mUiHandler == null) {
            mUiHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    //将获取到的bitmap显示在ImageView中
                    ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
                    String tPath = holder.path;
                    Bitmap tBitmap = holder.bitmap;
                    ImageView tImageView = holder.imageView;

                    if(tImageView.getTag().toString().equals(tPath)) {
                        tImageView.setImageBitmap(tBitmap);
                    }
                }
            };
        }

        //根据path从LruCache缓存中获取图片
        Bitmap bitmap = getBitmapFromLruCache(path);
        if(bitmap != null) {
            refreshImageView(path,bitmap,imageView);
        } else {
            //如果缓存中没有，则需要重新加载
            //向任务队列mTaskQueue中添加任务
            addTaskToTaskQueue(new Runnable(){
                @Override
                public void run() {
                    //根据ImageView获取需要显示的图片的宽和高
                    ImageInfoBean infoBean = getImageWidthAndHeight(imageView);

                    //根据图片的path和需要显示的宽和高，获取压缩后的图片
                    Bitmap bm = getSampledBitmapFromPath(path,infoBean.width,infoBean.height);
                    //将压缩后的bitmap加入LruCache
                    addBitmapToLruCache(path,bm);

                    refreshImageView(path,bm,imageView);
                    //完成一个图片的加载之后，释放一个信号量
                    mSemaphoreThreadPool.release();
                }
            });

        }
    }

    /**
     * 将压缩后的bitmap加入LruCache
     * @param path
     * @param bm
     */
    private void addBitmapToLruCache(String path, Bitmap bm) {
        if(getBitmapFromLruCache(path) == null) {
            if (bm != null) {
                mLruCache.put(path,bm);
            }
        }
    }

    private void refreshImageView(String path,Bitmap bitmap,ImageView imageView) {
        Message msg = Message.obtain();

        ImageBeanHolder holder = new ImageBeanHolder();
        holder.path = path;
        holder.bitmap = bitmap;
        holder.imageView = imageView;

        msg.obj = holder;
        mUiHandler.sendMessage(msg);
    }

    /**
     * 根据图片的path和需要显示的宽和高，获取压缩后的图片
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap getSampledBitmapFromPath(String path, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //仅仅获取图片的宽和高，不将图片加载到内存
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path,options);
        //获取图片的压缩比例
        options.inSampleSize = getSampleSize(options,width,height);

        options.inJustDecodeBounds = false;
        //使用获取到的压缩比例获取bitmap
        return BitmapFactory.decodeFile(path,options);

    }

    private int getSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;

        //如果原图的宽或高大于需要显示的图片的宽或高
        if(width > reqWidth || height > reqHeight) {
            int widthSample = Math.round(width*1.0f/reqWidth);
            int heightSample = Math.round(height*1.0f/reqHeight);

            inSampleSize = Math.max(widthSample,heightSample);
        }

        return inSampleSize;
    }

    /**
     * 根据ImageView获取需要显示的图片的宽和高
     * @param imageView
     * @return
     */
    private ImageInfoBean getImageWidthAndHeight(ImageView imageView) {
        DisplayMetrics displayMetrics  = imageView.getContext().getResources().getDisplayMetrics();
        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        int width = imageView.getWidth();
        //如果width小于0 ，则从ImageView的 LayoutParams中获取
        if(width <= 0) {
            width = layoutParams.width;
        }

        //如果width还是小于0 ，则获取ImageView的maxWidth
        if(width <= 0) {
            width = getImageViewFieldValue(imageView,"mMaxWidth");
        }

        //如果width还是小于0 ，则使用屏幕的宽和高
        if(width <= 0) {
            width = displayMetrics.widthPixels;
        }

        int height = imageView.getHeight();
        //如果height小于0 ，则从ImageView的 LayoutParams中获取
        if(height <= 0) {
            height = layoutParams.height;
        }

        //如果height还是小于0 ，则获取ImageView的maxheight
        if(height <= 0) {
            height = getImageViewFieldValue(imageView,"mMaxHeight");
        }

        //如果height还是小于0 ，则使用屏幕的宽和高
        if(height <= 0) {
            height = displayMetrics.heightPixels;
        }

        ImageInfoBean infoBean = new ImageInfoBean();
        infoBean.width = width;
        infoBean.height = height;

        return infoBean;
    }

    private int getImageViewFieldValue(ImageView imageView,String fieldName) {
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            value = field.getInt(imageView);
        } catch (Exception e) {
        }

        return value;
    }

    /**
     * 向任务队列中添加任务，并通知线程池执行任务
     * @param runnable
     */
    private synchronized void addTaskToTaskQueue(Runnable runnable) {
        mTaskQueue.add(runnable);

        try {
            if(mPoolThreadHandler == null) {
                //请求一个信号量，当mThreadPoolHandler初始化完成时，获取到信号量之后则继续执行
                mSemaphorePoolThreadHandler.acquire();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //通过后台轮询线程通知线程池获取任务并执行
        mPoolThreadHandler.sendEmptyMessage(GET_TASK);
    }

    /**
     * 根据path从LruCache缓存中获取图片
     * @param path
     * @return
     */
    private Bitmap getBitmapFromLruCache(String path) {
        return mLruCache.get(path);
    }

    /**
     * ImageBean 包装对象，用以在mUiHandler中通过Message传递对象
     */
    private class ImageBeanHolder {
        String path;
        Bitmap bitmap;
        ImageView imageView;
    }

    /**
     * ImageInfoBean 包装类，包装着ImageView的width和height
     */
    private class ImageInfoBean {
        int width;
        int height;
    }

}
