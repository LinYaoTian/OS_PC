package com.rdc.bms.os_experiment;

import android.animation.Animator;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LYT";
    @BindView(R.id.p_1)
    View mP1view;
    @BindView(R.id.p_2)
    View mP2view;
    @BindView(R.id.p_3)
    View mP3view;
    @BindView(R.id.p_4)
    View mP4view;
    @BindView(R.id.p_5)
    View mP5view;
    @BindView(R.id.c_1)
    View mC1view;
    @BindView(R.id.c_2)
    View mC2view;
    @BindView(R.id.c_3)
    View mC3view;
    @BindView(R.id.c_4)
    View mC4view;
    @BindView(R.id.c_5)
    View mC5view;
    @BindView(R.id.et_p_num)
    EditText mEtPNum;
    @BindView(R.id.et_c_num)
    EditText mEtCNum;
    @BindView(R.id.et_buffer_num)
    EditText mEtBufferNum;
    @BindView(R.id.tv_current)
    TextView mTvCurrent;
    @BindView(R.id.btn)
    Button mBtnStart;
    @BindView(R.id.layout_root)
    ConstraintLayout mRootLayout;
    @BindView(R.id.buffer)
    View mBufferView;

    private View[] mPList;
    private View[] mCList;
    private View mTopProduct;
    private View mBottomProduct;
    private int mMaxBuffer;//缓冲区最大值
    private int mCNum;//消费者数量
    private int mPNum;//生产者数量
    private PCQueue<Integer> mBufferQueue;
    private float[] mTopLocation;
    private float[] mBottomLocation;
    private ExecutorService mExecutor;

    private final int P_REQUEST_START = 0;//生产者开始交付产品
    private final int P_REQUEST_END = 1;//生产者交付产品完成
    private final int C_REQUEST_START = 2;//消费者开始获取产品
    private final int C_REQUEST_END = 3;//消费者获取产品完成

    private final int C_ANIMATE = 0;
    private final int P_ANIMATE = 1;
    private boolean isStart;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case P_REQUEST_START:
                    mPList[msg.arg1].setBackgroundResource(R.drawable.circle_green);
                    break;
                case P_REQUEST_END:
                    mTvCurrent.setText(mBufferQueue.getCount());
                    float[] origin = new float[]{
                            mPList[msg.arg1].getX()+Util.dip2px(MainActivity.this,30)/2-25,
                            mPList[msg.arg1].getY()+Util.dip2px(MainActivity.this,30)/2-25};
                    startAnimate(mTopLocation,origin,mTopProduct,P_ANIMATE,msg.arg1);
                    break;
                case C_REQUEST_START:
                    mCList[msg.arg1].setBackgroundResource(R.drawable.circle_green);
                    break;
                case C_REQUEST_END:
                    mTvCurrent.setText(mBufferQueue.getCount());
                    float[]target = new float[]{
                            mCList[msg.arg1].getX()+Util.dip2px(MainActivity.this,30)/2-25,
                            mCList[msg.arg1].getY()+Util.dip2px(MainActivity.this,30)/2-25};
                    startAnimate(target,mBottomLocation,mBottomProduct,C_ANIMATE,msg.arg1);
                    break;
            }
            return false;
        }
    });

    /**
     * 产品从缓存取出或存入动画
     * @param target 目标坐标
     * @param origin 起点坐标
     * @param view 移动的View
     * @param type 放入或取出
     * @param i 生产者或消费者编号
     */
    private void startAnimate(float[] target, float[] origin, final View view, final int type, final int i) {
        view.setX(origin[0]);
        view.setY(origin[1]);
        view.animate().x(target[0]).y(target[1]).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.INVISIBLE);
                if (type == P_ANIMATE){
                    mBufferQueue.PAnimateEnd();
                    mPList[i].setBackgroundResource(R.drawable.circle_yellow);
                }else if (type == C_ANIMATE){
                    mBufferQueue.CAnimateEnd();
                    mCList[i].setBackgroundResource(R.drawable.circle_yellow);
                }


            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        }).start();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initData();
        initViews();
        initListeners();
        mBufferView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTopLocation = new float[2];
                mBottomLocation = new float[2];
                int[] location = new int[2];
                mBufferView.getLocationOnScreen(location);
                mTopLocation[0] = mBufferView.getWidth() / 2 + location[0] - 25;
                mTopLocation[1] = location[1];
                mBottomLocation[0] = mTopLocation[0];
                mBottomLocation[1] = location[0] + mBufferView.getHeight() + 50;
            }
        });
    }

    private void initListeners() {
        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isStart){
                    mMaxBuffer = Integer.valueOf(mEtBufferNum.getText().toString());
                    mCNum = Integer.valueOf(mEtCNum.getText().toString());
                    mPNum = Integer.valueOf(mEtPNum.getText().toString());
                    mEtBufferNum.setEnabled(false);
                    mEtCNum.setEnabled(false);
                    mEtPNum.setEnabled(false);
                    mBtnStart.setText("停止");
                    mBufferQueue = new PCQueue<>(mMaxBuffer);
                    mTvCurrent.setText(mBufferQueue.getCount());
                    setC();
                    setP();
                    startPC();
                    isStart = true;
                }else {
                    mExecutor.shutdownNow();
                    mEtBufferNum.setEnabled(true);
                    mEtCNum.setEnabled(true);
                    mEtPNum.setEnabled(true);
                    mBtnStart.setText("开始");
                    isStart = false;
                }
            }

            /**
             * 设置生产者
             */
            private void setP() {
                for (int i = 0; i < 5; i++) {
                    if (i < mPNum){
                        mPList[i].setBackgroundResource(R.drawable.circle_yellow);
                    }else {
                        mPList[i].setBackgroundResource(R.drawable.circle_grey);
                    }
                }
            }

            /**
             * 设置消费者
             */
            private void setC() {
                for (int i = 0; i < 5; i++) {
                    if (i < mCNum){
                        mCList[i].setBackgroundResource(R.drawable.circle_yellow);
                    }else {
                        mCList[i].setBackgroundResource(R.drawable.circle_grey);
                    }
                }
            }

            private void sendMessage(int what,int arg1){
                Message msg = mHandler.obtainMessage();
                msg.what = what;
                msg.arg1 = arg1;
                mHandler.sendMessage(msg);
            }

            /**
             * 开始执行生产者-消费者模型
             */
            private void startPC() {
                final Random random = new Random();
                mExecutor = Executors.newFixedThreadPool(10);
                //生产者生产完成
                for (int i = 0; i < mPNum; i++) {
                    final int finalI = i;
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            while (true){
                                try {
                                    sendMessage(P_REQUEST_START,finalI);
                                    int value = random.nextInt(10);
                                    if (!mBufferQueue.put(value)){
                                        break;
                                    }
                                    //动画
                                    Log.d(TAG, "run: 生产者--->"+(finalI +1)+"   value="+value);
                                    mBufferQueue.PAnimateStart();
                                    sendMessage(P_REQUEST_END,finalI);
                                    //频率为 2000-4000 ms 完成一次
                                    Thread.sleep(2000+random.nextInt(2000));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    break;
                                }
                            }
                        }
                    });
                }

                //消费者请求消费
                for (int i = 0; i < mCNum; i++) {
                    final int finalI = i;
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            while (true){
                                try {
                                    //频率为 500-1000 ms 请求一次
                                    Thread.sleep(2000+random.nextInt(2000));
                                    sendMessage(C_REQUEST_START,finalI);
                                    Integer value = mBufferQueue.get();
                                    if (value == null){
                                        break;
                                    }
                                    //动画
                                    Log.d(TAG, "run: 消费者->"+(finalI +1)+"   value="+value);
                                    mBufferQueue.CAnimateStart();
                                    sendMessage(C_REQUEST_END,finalI);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    break;
                                }
                            }
                        }
                    });
                }

            }
        });
    }

    private void initData() {
        mCList = new View[]{mC1view, mC2view, mC3view, mC4view, mC5view};
        mPList = new View[]{mP1view, mP2view, mP3view, mP4view, mP5view};
        isStart = false;
    }

    private void initViews() {
        mTopProduct = new View(MainActivity.this);
        mTopProduct.setVisibility(View.INVISIBLE);
        mTopProduct.setBackgroundResource(R.drawable.circle_red);
        ConstraintLayout.LayoutParams params1 =
                new ConstraintLayout.LayoutParams(50,50);
        params1.bottomToTop = R.id.buffer;
        params1.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        params1.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        mTopProduct.setLayoutParams(params1);
        mRootLayout.addView(mTopProduct);

        mBottomProduct = new View(MainActivity.this);
        mBottomProduct.setVisibility(View.INVISIBLE);
        mBottomProduct.setBackgroundResource(R.drawable.circle_red);
        ConstraintLayout.LayoutParams params2 =
                new ConstraintLayout.LayoutParams(50,50);
        params2.topToBottom = R.id.buffer;
        params2.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        params2.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        mBottomProduct.setLayoutParams(params2);
        mRootLayout.addView(mBottomProduct);
    }
}
