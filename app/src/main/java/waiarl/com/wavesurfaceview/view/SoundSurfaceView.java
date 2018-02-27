package waiarl.com.wavesurfaceview.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

import waiarl.com.wavesurfaceview.R;
import waiarl.com.wavesurfaceview.Utils;
import waiarl.com.wavesurfaceview.imp.WaveViewImp;

/**
 * Created by waiarl on 2018/1/30.
 */

public class SoundSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable, WaveViewImp<SoundSurfaceView> {
    public static final String TAG = SoundSurfaceView.class.getSimpleName();
    private final Context mContext;
    private SurfaceHolder mHolder;
    private Paint mPaint;
    private int mWidth;
    private int mheight;
    private boolean isOnCreate;
    private int mRate;
    private boolean shouldPlaying;
    private int mStartX;
    private int mEndX;
    private int mCenterY;
    private int mMaxHeight;
    private int mTopY;
    private int mBottomY;
    private int mcenterX;
    private Canvas mCanvas;
    private int[] mColors;
    private float mMaxWaveWidth;
    private long startTime;
    private float mWaveP;
    private double mSoundDb;
    private int mMaxDb;
    private int mMinDb;
    private int mDvDb;
    private float mTransDuration;
    private Rect mDrawRect;
    private boolean isPlaying;
    private Thread mWorkThread;
    private float mWaveOffset;

    public SoundSurfaceView(Context context) {
        this(context, null);
    }

    public SoundSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SoundSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isOnCreate = true;
        mWidth = getWidth();
        mheight = getHeight();
        setPoint(mWidth, mheight);
        if (shouldPlaying) {
            start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mWidth = width;
        mheight = height;
        setPoint(mWidth, mheight);

    }

    private void setPoint(int mWidth, int mheight) {
        mStartX = 0;//绘制的起点x
        mEndX = mWidth;//绘制区域的终点x
        mcenterX = (mStartX + mEndX) / 2;//绘制的中心点X
        mCenterY = mheight / 3;//绘制的中心点Y
        mMaxHeight = mheight / 7;//绘制的波的理论最大高度，由于是贝塞尔曲线，实际最高值比这个值低很多，三点的仅为1/2
        mTopY = mCenterY - mMaxHeight;//绘制的理论到波峰的时候的Y点坐标
        mBottomY = mCenterY + mMaxHeight;//绘制的理论到波谷的时候的Y点坐标

        mMaxWaveWidth = mWidth / mWaveP;//一条 完整 的波的宽度
        final float mWaveMaxTopHeight = 0.5f * mMaxHeight + 10;//根据贝塞尔曲线原理，得到波的最大高度，另外加10像素的误差区间
        final float mWaveMaxTopY = mCenterY - mWaveMaxTopHeight;//实际上的绘制的Y的最高点坐标，10像素的误差容错
        mDrawRect = new Rect(0, (int) mWaveMaxTopY, mWidth, (int) (mWaveMaxTopHeight * 2));//绘制的矩形区域，

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isOnCreate = false;
        isPlaying = false;
    }

    private void init() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        setZOrderOnTop(true);
        mHolder.setFormat(PixelFormat.TRANSPARENT);

        mRate = 1000 / 25;//绘制时间间隔，25位帧率，即为一秒绘制25次
        mColors = new int[]{Utils.getColor(R.color.sound_line_color_1),
                Utils.getColor(R.color.sound_line_color_2),
                Utils.getColor(R.color.sound_line_color_3),
                Utils.getColor(R.color.sound_line_color_4),
                Utils.getColor(R.color.sound_line_color_5),
                Utils.getColor(R.color.sound_line_color_6),
                Utils.getColor(R.color.sound_line_color_7)
        };
        mPaint.setStrokeWidth(4);//绘制线条高度
        mTransDuration = 800f;//从屏幕左端滑到右端所需要时间
        mWaveP = 2.f;//屏幕最多有mWaveP/2个 【完整长度波】, 1代表为一个波峰 或 一个波谷,为方便计算，这个数据最好为2的整数倍
        mWaveOffset = mWaveP;//为了波平滑，用来计算x流动偏移值的 为不小于mWaveP的最小的2的倍数，即为里面有整数个完整波（波谷与波峰）
        mMaxDb = 100;//声音达到70以上分呗的时候画最大曲线
        mMinDb = 0;//声音达到mMinDb以下分呗时画最小曲线
        mDvDb = mMaxDb - mMinDb;//声音的分呗的变化区间
    }


    public synchronized SoundSurfaceView start() {
        shouldPlaying = true;
        if (isOnCreate && !isPlaying) {
            mWorkThread = new Thread(this);
            startTime = System.currentTimeMillis();
            mWorkThread.start();
        }
        return this;
    }

    public SoundSurfaceView stop() {
        shouldPlaying = false;
        isPlaying = false;
        mSoundDb = 0;
        startTime = System.currentTimeMillis();
        clearCanvas();
        return this;
    }


    /**
     * 声音分贝
     *
     * @param soundDb
     */
    public SoundSurfaceView initView(double soundDb) {
        this.mSoundDb = soundDb;
        return this;
    }

    @Override
    public SoundSurfaceView getView() {
        return this;
    }

    @Override
    public void run() {
        synchronized (mHolder) {
            while (isOnCreate && shouldPlaying) {
                isPlaying = true;
                final long timeOffset = drawView();
                long rate = mRate - timeOffset;
                if (rate <= 0) {//1秒的容错误差
                    rate = 1;
                }
                try {
                    Thread.sleep(rate);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private long drawView() {
        final long startTime = System.currentTimeMillis();
        if (mHolder != null) {
            mCanvas = mHolder.lockCanvas(mDrawRect);
            if (mCanvas == null) {
                final long endTime = System.currentTimeMillis();
                final long timeOffet = endTime - startTime;
                return timeOffet;
            }
        }
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        final float xOffset = getXOffset();
        final float waveHeight = getWaveHeight();
        final List<PointF> points = getPoints(xOffset);
        final int size = points.size();
        final int lineSize = mColors.length;
        for (int k = 0; k < lineSize; k++) {
            mPaint.setColor(mColors[k]);
            final float lineWaveHeight = waveHeight * (1f - k / (float) (lineSize - 2.5));//由于最后一条线不动，所以被除数要-1
            for (int i = 0; i < size - 1; i++) {
                final float x1 = points.get(i).x;
                final float y1 = mCenterY;
                final float x3 = points.get(i + 1).x;
                final float y3 = mCenterY;
                final float x2 = (x1 + x3) / 2;
                final float y2 = ((x3 - x1) / mMaxWaveWidth) * lineWaveHeight * (points.get(i + 1).y) + mCenterY;
                final Path p = new Path();
                p.moveTo(x1, y1);
                p.cubicTo(x1, y1, x2, y2, x3, y3);
                mCanvas.drawPath(p, mPaint);
            }
        }
        if (mCanvas != null && mHolder != null) {
            mHolder.unlockCanvasAndPost(mCanvas);
        }
        final long endTime = System.currentTimeMillis();
        final long timeOffet = endTime - startTime;
        Log.i(TAG, "timeOffet=" + timeOffet);
        return timeOffet;
    }

    /**
     * 获取当前波的最大高度
     *
     * @return
     */
    private float getWaveHeight() {
        if (mSoundDb > mMaxDb) {
            mSoundDb = mMaxDb;
        }
        if (mSoundDb < mMinDb) {
            mSoundDb = mMinDb;
        }
        final float waveHeight = (float) ((mSoundDb - mMinDb) / mDvDb * mMaxHeight);

        return waveHeight;
    }

    private float getXOffset() {
        final float d = mWaveP % 2;
        if (d != 0) {
            mWaveOffset = ((int) (mWaveP / 2) + 1) * 2;
        } else {
            mWaveOffset = mWaveP;
        }
        final float mul = mWaveOffset / mWaveP;
        final float durationOffset = mTransDuration * mul;
        final float widthOffset = mWidth * mul;
        final float xOffset = ((System.currentTimeMillis() - startTime) % durationOffset) / durationOffset * widthOffset;
        return xOffset;
    }

    private List<PointF> getPoints(float xOffset) {
        final List<PointF> points = new ArrayList<>();
        final int min = (int) (Math.ceil(-mWaveOffset) - 1);
        final int max = (int) Math.ceil(mWaveOffset);

        for (int i = min; i <= max; i++) {
            final float re = xOffset + i * mMaxWaveWidth;
            if (re > 0 && re < mWidth) {
                final float y = (float) Math.pow(-1, i + 1);
                points.add(new PointF(re, y));
            }
        }
        final int length = points.size();
        if (points.size() > 0) {
            points.add(new PointF(mWidth, points.get(length - 1).y * (-1)));
            points.add(0, new PointF(0, points.get(0).y * (-1)));
        }

        return points;
    }


    public SoundSurfaceView destory() {
        stop();
        return this;
    }

    public void clearCanvas() {
        if (mHolder != null) {
            mCanvas = mHolder.lockCanvas();
            if (mCanvas != null) {
                try {
                    mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    mHolder.unlockCanvasAndPost(mCanvas);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
