package com.rdc.bms.os_experiment;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class PCQueue<E> {

    private LinkedList<E> mBuffer = new LinkedList<>();

    private Semaphore mEmptySemp;//空缓冲区的数量

    private Semaphore mFullSemp;//满缓冲区的数量

    private Semaphore mMutexSemp;//访问互斥信号量

    private Semaphore mPRequestSemp;//放入产品过程互斥量,主要用于实现动画

    private Semaphore mCRequestSemp;

    private int count;//缓冲区内产品数量

    public PCQueue(int size){
        count=0;
        mEmptySemp = new Semaphore(size);
        mFullSemp = new Semaphore(0);
        mMutexSemp = new Semaphore(1);
        mPRequestSemp = new Semaphore(1);
        mCRequestSemp = new Semaphore(1);
    }

    public void PAnimateStart(){
        try {
            mPRequestSemp.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void PAnimateEnd(){
        mPRequestSemp.release();
    }

    public void CAnimateStart(){
        try {
            mCRequestSemp.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void CAnimateEnd(){
        mCRequestSemp.release();
    }



    /**
     * 缓冲区内产品数量
     * @return
     */
    public String getCount(){
        return String.valueOf(count);
    }

    public boolean put(E value){
        try {
            //生产者访问时，先获取一个空缓冲区，再获取互斥信号量
            mEmptySemp.acquire();
            mMutexSemp.acquire();
            mBuffer.push(value);
            count++;
            //释放信号量时，先释放互斥量，再增加一个满缓冲区
            mMutexSemp.release();
            mFullSemp.release();
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public E get(){
        E e = null;
        try {
            //消费者访问时，先获取一个满缓冲区，再获取互斥信号量
            mFullSemp.acquire();
            mMutexSemp.acquire();
            e = mBuffer.poll();
            count--;
            //释放信号量时，先释放互斥量，再增加一个空缓冲区
            mMutexSemp.release();
            mEmptySemp.release();
            return e;
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return e;
        }
    }

    public void clear(){
        mBuffer.clear();
        count=0;
    }
}
