package wangmin.common.cache.normal;

import wangmin.common.cache.common.BinaryCacheRepositoryInterface;
import wangmin.common.cache.common.NoCacheException;
import wangmin.common.utils.BinaryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.types.Expiration;

/**
 * <p>User: Wang Min
 * <p>Date: 2017-3-20
 * <p>Version: 1.0
 */
public class BinaryCacheNormalRepository implements BinaryCacheRepositoryInterface {
    private static final Logger logger = LoggerFactory.getLogger(BinaryCacheNormalRepository.class);

    // redis 连接工厂
    private JedisConnectionFactory connFactory;
    public void setConnFactory(JedisConnectionFactory connFactory) {
        this.connFactory = connFactory;
    }

    /**
     * 抛出NoCacheException, 表示没有从cache找到值
     * 返回值: byte数组长度为0代表cache保存的是空值, 其他代表cache保存的是正确值
     * */
    @Override
    public byte[] get(byte[] keyBytes, boolean setExpire, int expirationSeconds) throws NoCacheException {
        RedisConnection conn = null;
        try {
            conn = connFactory.getConnection();

            byte[] valueBytes = conn.get(keyBytes);
            if (null == valueBytes)
                throw new NoCacheException();

            if (setExpire) {
                if (0 == valueBytes.length) {
                    // null 值 过期时间需要更短
                    expirationSeconds >>>= 2;
                    if (expirationSeconds <= 0) expirationSeconds = 1;
                }
                conn.expire(keyBytes, expirationSeconds);
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

    /**
     * valueBytes 为空或者长度为0, 代表添加一个空的cache
     * */
    @Override
    public void set(byte[] keyBytes, byte[] valueBytes, int expirationSeconds) throws Exception {
        RedisConnection conn = null;
        try {
            conn = connFactory.getConnection();

            if (null == valueBytes || 0 == valueBytes.length) {
                valueBytes = BinaryUtils.emptyByteArray;

                // null 值 过期时间需要更短
                expirationSeconds >>>= 2;
                if (expirationSeconds <= 0) expirationSeconds = 1;
            }

            conn.set(keyBytes, valueBytes, Expiration.seconds(expirationSeconds), RedisStringCommands.SetOption.UPSERT);
        } catch (Exception e) {
            logger.info("set error, keyBytes={}, valueBytes={}", keyBytes, valueBytes);
            throw e;
        } finally {
            if (conn != null)
                conn.close();
        }
    }

    @Override
    public void del(byte[] keyBytes) throws Exception {
        RedisConnection conn = null;
        try {
            conn = connFactory.getConnection();

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

