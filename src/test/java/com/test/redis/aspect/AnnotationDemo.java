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

    @RedisAnnotationCache(type = BusinessKeyType.GOODS_NAME, keyIndex=0, expiration=1)
    public String testValue(String key, int index) {
        logger.info("testValue entered, key={}, index={}", key, index);
        return "tvwwfs";
    }
}
