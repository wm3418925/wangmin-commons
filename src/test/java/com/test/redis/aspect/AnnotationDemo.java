package com.test.redis.aspect;

import com.test.redis.constraint.BusinessKeyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import wangmin.common.cache.annotation.RedisAnnotationCache;

/**
 * Created by wm on 2017/3/22.
 */
@Service
public class AnnotationDemo {
    private final static Logger logger = LoggerFactory.getLogger(AnnotationDemo.class);

    /**
     * 这个函数 被aop的环绕拦截
     * 会将方法返回值放入redis缓存, 如果缓存中存在, 则不执行函数, 直接使用缓存的值
     *
     * 注解各个属性的意义:
     * type : 缓存前缀, 不同业务使用不同的
     * keyIndex : 缓存的key在参数中的位置, 从0开始
     * expiration : 缓存过期时间 (单位秒)
     * **/
    @RedisAnnotationCache(type = BusinessKeyType.GOODS_NAME, keyIndex=0, expiration=1)
    public String testValue(String key, int index) {
        logger.info("testValue entered, key={}, index={}", key, index);
        return "tvwwfs";
    }
}
