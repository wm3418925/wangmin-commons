package wangmin.common.cache.cluster;

import redis.clients.jedis.Protocol;
import wangmin.common.cache.common.BinaryCacheRepositoryInterface;
import wangmin.common.cache.common.NoCacheException;
import wangmin.common.utils.BinaryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.BinaryJedisCluster;

/**
 * <p>User: Wang Min
 * <p>Date: 2017-3-20
 * <p>Version: 1.0
 */
public class BinaryCacheClusterRepository implements BinaryCacheRepositoryInterface {
    private static final Logger logger = LoggerFactory.getLogger(BinaryCacheClusterRepository.class);

    protected BinaryJedisCluster jedisCluster;
    public void setJedisCluster(BinaryJedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    /**
     * 抛出NoCacheException, 表示没有从cache找到值
     * 返回值: byte数组长度为0代表cache保存的是空值, 其他代表cache保存的是正确值
     * */
    @Override
    public byte[] get(byte[] keyBytes, boolean setExpire, int expiration) throws NoCacheException {
        try {
            byte[] valueBytes = jedisCluster.get(keyBytes);
            if (null == valueBytes)
                throw new NoCacheException();

            if (setExpire) {
                if (0 == valueBytes.length) {
                    // null 值 过期时间需要更短
                    expiration >>>= 2;
                    if (expiration <= 0) expiration = 1;
                }
                jedisCluster.expire(keyBytes, expiration);
            }

            return valueBytes;
        } catch (Exception e) {
            if (!(e instanceof NoCacheException))
                logger.info("get error, keyBytes={}", keyBytes);
            throw e;
        }
    }

    /**
     * valueBytes 为空或者长度为0, 代表添加一个空的cache
     * */
    @Override
    public void set(byte[] keyBytes, byte[] valueBytes, int expirationSeconds) throws Exception {
        try {
            if (null == valueBytes || 0 == valueBytes.length) {
                valueBytes = BinaryUtils.emptyByteArray;

                // null 值 过期时间需要更短
                expirationSeconds >>>= 2;
                if (expirationSeconds <= 0) expirationSeconds = 1;
            }

            jedisCluster.set(keyBytes, valueBytes, null, Protocol.toByteArray(expirationSeconds), 0);
        } catch (Exception e) {
            logger.info("set error, keyBytes={}, valueBytes={}", keyBytes, valueBytes);
            throw e;
        }
    }

    @Override
    public void del(byte[] keyBytes) throws Exception {
        try {
            jedisCluster.del(keyBytes);
        } catch (Exception e) {
            logger.info("del error, keyBytes={}", keyBytes);
            throw e;
        }
    }
}

