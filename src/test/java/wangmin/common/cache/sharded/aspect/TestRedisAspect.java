package wangmin.common.cache.sharded.aspect;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:test-redis/sharded/aspect/redis-sharded-aspect.xml")
public class TestRedisAspect {
    private final static Logger logger = LoggerFactory.getLogger(TestRedisAspect.class);

    @Autowired
    private RedisAspectDemo redisAspectDemo;


    @Test
    public void doTest() {
        int indexForText = 0;
        // 第0次测试, 缓存没有值, 执行 testValue
        logger.info("result {} = {}", indexForText, redisAspectDemo.testValue("123", indexForText));  ++indexForText;
        // 第1次测试, 缓存有值, 且没有过期, 不执行 testValue
        logger.info("result {} = {}", indexForText, redisAspectDemo.testValue("123", indexForText));  ++indexForText;

        // 睡眠2秒, 缓存过期了
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 第2次测试, 缓存过期, 执行 testValue
        logger.info("result {} = {}", indexForText, redisAspectDemo.testValue("123", indexForText));  ++indexForText;


        // 第3次测试, 新的key, 缓存没有值, 执行 testValue
        logger.info("result {} = {}", indexForText, redisAspectDemo.testValue("ASG", indexForText));  ++indexForText;
    }

}
