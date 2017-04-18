package wangmin.common.cache.sharded;

import wangmin.common.cache.common.BinaryCacheRepositoryInterface;
import wangmin.common.cache.common.NoCacheException;
import wangmin.common.utils.BinaryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

/**
 * <p>User: Wang Min
 * <p>Date: 2017-3-20
 * <p>Version: 1.0
 */
public class BinaryCacheShardedRepository implements BinaryCacheRepositoryInterface {
    private static final Logger logger = LoggerFactory.getLogger(BinaryCacheShardedRepository.class);

    private ShardedJedisPool pool;
    public void setPool(ShardedJedisPool pool) {
        this.pool = pool;
    }

    /**
     * 抛出NoCacheException, 表示没有从cache找到值
     * 返回值: byte数组长度为0代表cache保存的是空值, 其他代表cache保存的是正确值
     * */
    @Override
    public byte[] get(byte[] keyBytes, boolean setExpire, int expireSeconds) throws NoCacheException {
        try {
            byte[] valueBytes = getInternal(keyBytes);
            if (null == valueBytes)
                throw new NoCacheException();

            if (setExpire) {
                if (0 == valueBytes.length) {
                    // null 值 过期时间需要更短
                    expireSeconds >>>= 2;
                    if (expireSeconds <= 0) expireSeconds = 1;
                }
                expireInternal(keyBytes, expireSeconds);
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
    public void set(byte[] keyBytes, byte[] valueBytes) throws Exception {
        try {
            if (null == valueBytes || 0 == valueBytes.length) {
                valueBytes = BinaryUtils.emptyByteArray;
            }

            setInternal(keyBytes, valueBytes);
        } catch (Exception e) {
            logger.info("set error, keyBytes={}, valueBytes={}", keyBytes, valueBytes);
            throw e;
        }
    }

    /**
     * valueBytes 为空或者长度为0, 代表添加一个空的cache
     * */
    @Override
    public void set(byte[] keyBytes, byte[] valueBytes, int expireSeconds) throws Exception {
        try {
            if (null == valueBytes || 0 == valueBytes.length) {
                valueBytes = BinaryUtils.emptyByteArray;

                // null 值 过期时间需要更短
                expireSeconds >>>= 2;
                if (expireSeconds <= 0) expireSeconds = 1;
            }

            setInternal(keyBytes, expireSeconds, valueBytes);
        } catch (Exception e) {
            logger.info("set error, keyBytes={}, valueBytes={}, expireSeconds={}", keyBytes, valueBytes, expireSeconds);
            throw e;
        }
    }

    @Override
    public void del(byte[] keyBytes) throws Exception {
        try {
            delInternal(keyBytes);
        } catch (Exception e) {
            logger.info("del error, keyBytes={}", keyBytes);
            throw e;
        }
    }




    /**
     * 操作内部实现
     * **/
    private byte[] getInternal(final byte[] keyBytes) {
        return this.execute(new JedisAction<byte[]>() {
            @Override
            public byte[] action(ShardedJedis jedis) {
                return jedis.get(keyBytes);
            }
        });
    }
    private void setInternal(final byte[] keyBytes, final byte[] valueBytes) {
        this.execute(new JedisActionNoResult() {
            @Override
            public void action(ShardedJedis jedis) {
                jedis.set(keyBytes, valueBytes);
            }
        });
    }
    private void setInternal(final byte[] keyBytes, final int expireSeconds, final byte[] valueBytes) {
        this.execute(new JedisActionNoResult() {
            @Override
            public void action(ShardedJedis jedis) {
                jedis.setex(keyBytes, expireSeconds, valueBytes);
            }
        });
    }
    private Long delInternal(final byte[] keyBytes) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.del(keyBytes);
            }
        });
    }
    private Long expireInternal(final byte[] keyBytes, final int seconds) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.expire(keyBytes, seconds);
            }
        });
    }

    /**
     * 无返回结果的回调接口定义。
     */
    private interface JedisActionNoResult {
        void action(ShardedJedis jedis);
    }
    /**
     * 有返回结果的回调接口定义。
     */
    private interface JedisAction<T> {
        T action(ShardedJedis jedis);
    }
    private void returnResource(ShardedJedis jedis) {
        // 返还到连接池
        if (jedis != null) {
            try {
                pool.returnResource(jedis);
            } catch (Throwable e) {
                returnBrokenResource(jedis);
            }
        }
    }
    private void returnBrokenResource(ShardedJedis jedis) {
        if (jedis != null) {
            try {
                pool.returnBrokenResource(jedis);
            } catch (Throwable e) {
                logger.error("", e);
            }
        }
    }
    /**
     * 执行无返回结果的action。
     */
    private void execute(JedisActionNoResult jedisAction) {
        ShardedJedis jedis = null;
        try {
            jedis = pool.getResource();
            jedisAction.action(jedis);
        } finally {
            returnResource(jedis);
        }
    }
    /**
     * 执行有返回结果的action。
     */
    private <T> T execute(JedisAction<T> jedisAction) {
        ShardedJedis jedis = null;

        long threadId = Thread.currentThread().getId();

        try {
            jedis = pool.getResource();
            return jedisAction.action(jedis);
        } catch (Exception e) {
            StringBuffer sb = new StringBuffer("redis连接池[");
            sb.append(pool.getNumActive()).append(",");
            sb.append(pool.getNumIdle()).append(",");
            sb.append(pool.getNumWaiters()).append("]");
            logger.error(sb.toString(), e);
            throw new RuntimeException(e);
        } finally {
            returnResource(jedis);
        }
    }
}

