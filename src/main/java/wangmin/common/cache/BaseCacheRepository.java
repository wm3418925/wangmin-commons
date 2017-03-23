package wangmin.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.types.Expiration;
import wangmin.common.utils.BinaryUtils;

import java.util.List;

/**
 * <p>User: Wang Min
 * <p>Date: 2017-3-20
 * <p>Version: 1.0
 */
public abstract class BaseCacheRepository {
    private static final Logger logger = LoggerFactory.getLogger(BaseCacheRepository.class);

    public static class NoCacheException extends RuntimeException {}

    protected abstract JedisConnectionFactory getConnFactory();

    /**
     * 抛出NoCacheException, 表示没有从cache找到值
     * 返回值: byte数组长度为0代表cache保存的是空值, 其他代表cache保存的是正确值
     * */
    protected byte[] get(byte[] keyBytes, boolean setExpire, int expiration) throws NoCacheException {
        RedisConnection conn = null;
        try {
            conn = getConnFactory().getConnection();

            byte[] valueBytes = conn.get(keyBytes);
            if (null == valueBytes)
                throw new NoCacheException();

            if (setExpire) {
                if (0 == valueBytes.length) {
                    // null 值 过期时间需要更短
                    expiration >>>= 2;
                    if (expiration <= 0) expiration = 1;
                }
                conn.expire(keyBytes, expiration);
            }

            return valueBytes;
        } catch (Exception e) {
            if (!(e instanceof NoCacheException))
                logger.info("get error, keyBytes={}", keyBytes);
            throw e;
        } finally {
            if (conn != null)
                conn.close();
        }
    }
    protected List<byte[]> mGet(byte[]... keys) {
        RedisConnection conn = null;
        try {
            conn = getConnFactory().getConnection();

            return conn.mGet(keys);
        } finally {
            if (conn != null)
                conn.close();
        }
    }

    /**
     * valueBytes 为空或者长度为0, 代表添加一个空的cache
     * */
    protected void set(byte[] keyBytes, byte[] valueBytes, int expiration) throws Exception {
        RedisConnection conn = null;
        try {
            conn = getConnFactory().getConnection();

            if (null == valueBytes || 0 == valueBytes.length) {
                valueBytes = BinaryUtils.emptyByteArray;

                // null 值 过期时间需要更短
                expiration >>>= 2;
                if (expiration <= 0) expiration = 1;
            }

            conn.set(keyBytes, valueBytes, Expiration.seconds(expiration), RedisStringCommands.SetOption.UPSERT);
        } catch (Exception e) {
            logger.info("set error, keyBytes={}, valueBytes={}", keyBytes, valueBytes);
            throw e;
        } finally {
            if (conn != null)
                conn.close();
        }
    }

    protected void del(byte[] keyBytes) throws Exception {
        RedisConnection conn = null;
        try {
            conn = getConnFactory().getConnection();

            conn.del(keyBytes);
        } catch (Exception e) {
            logger.info("del error, keyBytes={}", keyBytes);
            throw e;
        } finally {
            if (conn != null)
                conn.close();
        }
    }
}

