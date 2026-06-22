package com.mikle.zerologic.service;

/**
 * 把当前 AppServiceImpl里的 app 生成互斥锁抽出来
 */
public interface GenerationAppLockService {

    String acquire(Long appId);

    void release(Long appId, String permitId);
}
