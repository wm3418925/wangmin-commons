package com.test.redis.constraint;

/**
 * Created by wm on 2017/3/22.
 * 业务中的key类型
 */
public enum BusinessKeyType {
    FOUNDATION,
    GOODS,
    ORDER,
    TRADER;

    public static final String FOUNDATION_NAME = "FOUNDATION";
    public static final String GOODS_NAME = "GOODS";
    public static final String ORDER_NAME = "ORDER";
    public static final String TRADER_NAME = "TRADER";
}
