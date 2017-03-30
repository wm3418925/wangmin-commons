package wangmin.common.cache.cluster;

import wangmin.common.cache.BusinessKeyType;
import wangmin.common.cache.common.RedisComponentInterface;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Set;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:test-redis/cluster/jedis-cluster.xml")
public class TestJedisCluster {
    private final static Logger logger = LoggerFactory.getLogger(TestJedisCluster.class);

    @Autowired
    private RedisComponentInterface redisComponent;

    private void testGetSet() {
        redisComponent.set(BusinessKeyType.ORDER, "testKey0", "tv0");
        String value = redisComponent.get(BusinessKeyType.ORDER, "testKey0", String.class);
        logger.info("value={}", value);
    }
    private void testSets() {
        redisComponent.set(BusinessKeyType.GOODS, "testKeySet", "testValueSet");
        logger.info("testKeySet={}", redisComponent.get(BusinessKeyType.GOODS, "testKeySet", String.class));

        redisComponent.del(BusinessKeyType.GOODS, "testKey1");
        redisComponent.sadd(BusinessKeyType.GOODS, "testKey1", "tv1");
        redisComponent.sadd(BusinessKeyType.GOODS, "testKey1", "tv2----");
        redisComponent.sadd(BusinessKeyType.GOODS, "testKey1", "1");

        Set<String> smembers = redisComponent.smembers(BusinessKeyType.GOODS, "testKey1", String.class);
        logger.info("smembers={}", smembers);

        redisComponent.srem(BusinessKeyType.GOODS, "testKey1", "tv1");

        smembers = redisComponent.smembers(BusinessKeyType.GOODS, "testKey1", String.class);
        logger.info("smembers={}", smembers);
    }

    @Test
    public void doTest() {
        testSets();
    }

}
