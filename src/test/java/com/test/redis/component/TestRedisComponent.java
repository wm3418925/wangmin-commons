package com.test.redis.component;

import com.test.redis.constraint.BusinessKeyType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import wangmin.common.cache.ShardedJedisComponent;

import java.util.Set;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:spring/test-redis/component/test-redis-component.xml")
public class TestRedisComponent {
    private final static Logger logger = LoggerFactory.getLogger(TestRedisComponent.class);

    @Autowired
    private ShardedJedisComponent redisComponent;

    @Before
    public void before() {
    }

    private void testGetSet() {
        redisComponent.set(BusinessKeyType.ORDER, "testKey0", "tv0");
        String value = redisComponent.get(BusinessKeyType.ORDER, "testKey0", String.class);
        logger.info("value={}", value);
    }
    private void testSets() {
        redisComponent.sadd(BusinessKeyType.GOODS, "testKey1", "tv1");
        redisComponent.sadd(BusinessKeyType.GOODS, "testKey1", "tv2----");
        redisComponent.sadd(BusinessKeyType.GOODS, "testKey1", 1);

        Set<String> smembers = redisComponent.smembers(BusinessKeyType.GOODS, "testKey1");
        logger.info("smembers={}",smembers);

        redisComponent.srem(BusinessKeyType.GOODS, "testKey1", "tv1");

        smembers = redisComponent.smembers(BusinessKeyType.GOODS, "testKey1");
        logger.info("smembers={}",smembers);
    }

    @Test
    public void doTest() {
        testGetSet();
        testSets();
    }

}
