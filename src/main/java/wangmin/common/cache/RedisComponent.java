package wangmin.common.cache;

import wangmin.common.cache.serializer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.util.SafeEncoder;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;

/**
 * Created by wm on 2017/3/21.
 * 封装redis的各种操作
 * srem 这类方法可能有bug
 */
public class RedisComponent {
    private final static Logger logger = LoggerFactory.getLogger(RedisComponent.class);

    private ShardedJedisPool pool;

    private RedisSerializer serializer;

    private final static Map<Long, Integer> poolRefCounter = new ConcurrentHashMap<>();

    private boolean debug = false;

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public static String buildRedisKeyByObjects(Object... keyObjects) {
        StringBuffer sb = new StringBuffer();

        for (Object keyObject : keyObjects) {
            sb.append(keyObject).append('_');
        }

        if (keyObjects.length >= 1) {
            sb.delete(0, 1);
        }

        return sb.toString();
    }

    private static String generateKey(Enum type, String key) {
        if (null == type) {
            throw new RuntimeException("empty type for key:"+key);   //return key;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(type.name());
            sb.append('~');
            sb.append(key);
            return sb.toString();
        }
    }
    private static byte[] generateKeyBytes(Enum type, String key) {
        return SafeEncoder.encode(generateKey(type, key));
    }

    /****************** 常用方法 *******************/

    /**
     * 获取 key-value 的 value
     */
    public <T> T get(final Enum type, final String key, final Class<T> c) {
        return this.execute(new JedisAction<T>() {
            @Override
            public T action(ShardedJedis jedis) {
                byte[] bs = jedis.get(generateKeyBytes(type, key));
                return deserialize(bs, c);
            }
        });
    }

    public <T> T getSet(final Enum type, final String key, final Object value, final Class<T> c) {
        return this.execute(new JedisAction<T>() {
            @Override
            public T action(ShardedJedis jedis) {
                byte[] bs = jedis.getSet(generateKeyBytes(type, key), serialize(value));
                return deserialize(bs, c);
            }
        });
    }

    public boolean exists(final Enum type, final String key) {
        return this.execute(new JedisAction<Boolean>() {
            @Override
            public Boolean action(ShardedJedis jedis) {
                return jedis.exists(generateKey(type, key));
            }
        });
    }

    /**
     * 获取 key-value 的 value. <br>
     * 如果 value 是一个 list, 请使用此方法.
     */
    public <T> List<T> getList(final Enum type, final String key, final Class<T> c) {
        return this.executeForList(new JedisActionForList<T>() {
            @Override
            public List<T> action(ShardedJedis jedis) {
                byte[] bs = jedis.get(generateKeyBytes(type, key));
                return deserializeForList(bs, c);
            }
        });
    }

    /**
     * redis设值
     *
     * @param key
     * @param value
     */
    public void set(final Enum type, final String key, final Object value) {
        this.execute(new JedisActionNoResult() {
            @Override
            public void action(ShardedJedis jedis) {
                jedis.set(generateKeyBytes(type, key), serialize(value));
            }
        });
    }

    /**
     * redis设值
     *
     * @param key
     * @param value
     * @param expire 过期时间，单位秒
     */
    public void set(final Enum type, final String key, final Object value, final long expire) {
        this.execute(new JedisActionNoResult() {
            @Override
            public void action(ShardedJedis jedis) {
                jedis.setex(generateKeyBytes(type, key), (int) expire, serialize(value));
            }
        });
    }

    /**
     * 获取 key mapKey mapValue 中的 mapValue 列表.
     */
    public <T> List<T> hvals(final Enum type, final String key, final Class<T> c) {
        return this.executeForList(new JedisActionForList<T>() {
            @Override
            public List<T> action(ShardedJedis jedis) {
                Collection<byte[]> value = jedis.hvals(generateKeyBytes(type, key));
                List<T> list = new ArrayList<T>(value.size());
                for (byte[] bs : value) {
                    list.add(deserialize(bs, c));
                }
                return list;
            }
        });
    }

    /**
     * 获取 key mapKey mapValue 中指定的 mapValue.
     */
    public <T> T hget(final Enum type, final String key, final Object mapKey, final Class<T> c) {
        return this.execute(new JedisAction<T>() {
            @Override
            public T action(ShardedJedis jedis) {
                byte[] bs = jedis.hget(generateKeyBytes(type, key), serialize(mapKey));
                return deserialize(bs, c);
            }
        });
    }

    /**
     * 获取 key mapKey mapValue 中指定的 mapValue.<br>
     * 如果 mapValue 是一个 list, 请使用此方法.
     */
    public <T> List<T> hgetList(final Enum type, final String key, final Object mapKey, final Class<T> c) {
        return this.executeForList(new JedisActionForList<T>() {
            @Override
            public List<T> action(ShardedJedis jedis) {
                byte[] value = jedis.hget(generateKeyBytes(type, key), serialize(mapKey));
                return deserializeForList(value, c);
            }
        });
    }

    /**
     * 缓存 key mapKey mapValue.
     */
    public void hset(final Enum type, final String key, final Object mapKey, final Object mapValue) {
        this.execute(new JedisActionNoResult() {
            @Override
            public void action(ShardedJedis jedis) {
                jedis.hset(generateKeyBytes(type, key), serialize(mapKey), serialize(mapValue));
            }
        });
    }

    public void hset(final Enum type, final String key, final Object mapKey, final Object mapValue, final int second) {
        this.execute(new JedisActionNoResult() {
            @Override
            public void action(ShardedJedis jedis) {
                jedis.hset(generateKeyBytes(type, key), serialize(mapKey), serialize(mapValue));
                jedis.expire(generateKey(type, key), second);
            }
        });
    }

    /**
     * 删除集合中对应的key/value
     */
    public void hdel(final Enum type, final String key, final Object mapKey) {
        this.execute(new JedisActionNoResult() {
            @Override
            public void action(ShardedJedis jedis) {
                jedis.hdel(generateKeyBytes(type, key), serialize(mapKey));
            }
        });
    }

    /**
     * 缓存 key map<mapKey,mapValue>.
     */
    public void hmset(final Enum type, final String key, final Map<?, ?> map) {
        this.execute(new JedisActionNoResult() {
            @Override
            public void action(ShardedJedis jedis) {
                if (map != null && !map.isEmpty()) {
                    Map<byte[], byte[]> m = new HashMap<byte[], byte[]>(map.size());

                    Iterator<?> it = map.entrySet().iterator();
                    while (it.hasNext()) {
                        Entry<?, ?> next = (Entry<?, ?>) it.next();
                        m.put(serialize(next.getKey()), serialize(next.getValue()));
                    }
                    jedis.hmset(generateKeyBytes(type, key), m);
                }
            }
        });
    }

    /**
     * @param key
     * @param map
     * @param expire 过期时间，单位秒
     */
    public void hmset(final Enum type, final String key, final Map<?, ?> map, final long expire) {
        this.execute(new JedisActionNoResult() {
            @Override
            public void action(ShardedJedis jedis) {
                if (map != null && !map.isEmpty()) {
                    Map<byte[], byte[]> m = new HashMap<byte[], byte[]>(map.size());

                    Iterator<?> it = map.entrySet().iterator();
                    while (it.hasNext()) {
                        Entry<?, ?> next = (Entry<?, ?>) it.next();
                        m.put(serialize(next.getKey()), serialize(next.getValue()));
                    }
                    jedis.hmset(generateKeyBytes(type, key), m);
                    jedis.expire(generateKey(type, key), (int) expire);
                }
            }
        });
    }

    /**
     * 删除一个 Key.
     */
    public Long del(final Enum type, final String key) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.del(generateKey(type, key));
            }
        });
    }

    /**
     * redis zadd command.
     */
    public Long zadd(final Enum type, final String key, final double score, final Object member) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.zadd(generateKeyBytes(type, key), score, serialize(member));
            }
        });
    }

    /**
     * redis zrange command.
     */
    public <T> List<T> zrange(final Enum type, final String key, final long start, final long end, final Class<T> clazz) {
        return this.executeForList(new JedisActionForList<T>() {
            @Override
            public List<T> action(ShardedJedis jedis) {
                Collection<byte[]> value = jedis.zrange(generateKeyBytes(type, key), start, end);
                List<T> list = new ArrayList<T>(value.size());
                for (byte[] b : value) {
                    list.add(deserialize(b, clazz));
                }
                return list;
            }
        });
    }

    /**
     * redis zrangeByScore command.
     */
    public <T> List<T> zrangeByScore(final Enum type, final String key, final double min, final double max, final Class<T> clazz) {
        return this.executeForList(new JedisActionForList<T>() {
            @Override
            public List<T> action(ShardedJedis jedis) {
                Collection<byte[]> value = jedis.zrangeByScore(generateKeyBytes(type, key), min, max);
                List<T> list = new ArrayList<T>(value.size());
                for (byte[] b : value) {
                    list.add(deserialize(b, clazz));
                }
                return list;
            }
        });
    }

    /**
     * redis zremrangeByScore command.
     */
    public Long zremrangeByScore(final Enum type, final String key, final double start, final double end) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.zremrangeByScore(generateKey(type, key), start, end);
            }
        });
    }

    public Long zrem(final Enum type, final String key, final String... members) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.zrem(generateKey(type, key), members);
            }
        });
    }

    /**
     * redis incr command.
     */
    public Long incr(final Enum type, final String key) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.incr(generateKey(type, key));
            }
        });
    }

    /**
     * redis incrby command.
     */
    public Long incrBy(final Enum type, final String key, final long integer) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.incrBy(generateKey(type, key), integer);
            }
        });
    }

    /**
     * redis decr command.
     */
    public Long decr(final Enum type, final String key) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.decr(generateKey(type, key));
            }
        });
    }

    /**
     * redis decrby command.
     */
    public Long decrBy(final Enum type, final String key, final long integer) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.decrBy(generateKey(type, key), integer);
            }
        });
    }

    /**
     * redis expire command.
     */
    public Long expire(final Enum type, final String key, final int seconds) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.expire(generateKey(type, key), seconds);
            }
        });
    }

    public Long sadd(final Enum type, final String key, final Object value) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.sadd(generateKeyBytes(type, key), serialize(value));
            }
        });
    }

    public Long srem(final Enum type, final String key, final Object value) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.srem(generateKeyBytes(type, key), serialize(value));
            }
        });
    }

    public Set<String> smembers(final Enum type, final String key) {
        return this.execute(new JedisAction<Set<String>>() {
            @Override
            public Set<String> action(ShardedJedis jedis) {
                return jedis.smembers(generateKey(type, key));
            }
        });
    }

    // internal method
    // -----------------------------------------------------------------------

    /**
     * 执行有返回结果的action。
     */
    private <T> T execute(JedisAction<T> jedisAction) {
        ShardedJedis jedis = null;

        long threadId = Thread.currentThread().getId();

        try {

            if (debug) {
                try {
                    if (!poolRefCounter.containsKey(threadId)) {
                        poolRefCounter.put(threadId, 0);
                    }

                    if (poolRefCounter.get(threadId) > 0) {
                        logger.error("线程[" + threadId + "]redis连接泄露");
                    }

                    poolRefCounter.put(threadId, poolRefCounter.get(threadId) + 1);
                } catch (Exception e) {

                }
            }
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
            if (debug) {
                try {
                    poolRefCounter.put(threadId, poolRefCounter.get(threadId) - 1);
                } catch (Exception e) {

                }
            }
        }
    }

    /**
     * 执行有返回结果,并且返回结果是List的action。
     */
    private <T> List<T> executeForList(JedisActionForList<T> jedisAction) {
        ShardedJedis jedis = null;
        try {
            jedis = pool.getResource();
            return jedisAction.action(jedis);
        } finally {
            returnResource(jedis);
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
     * 有返回结果的回调接口定义。
     */
    private interface JedisAction<T> {
        T action(ShardedJedis jedis);
    }

    /**
     * 有返回结果的回调接口定义。
     */
    private interface JedisActionForList<T> {
        List<T> action(ShardedJedis jedis);
    }

    /**
     * 无返回结果的回调接口定义。
     */
    private interface JedisActionNoResult {
        void action(ShardedJedis jedis);
    }

    // private method
    // -----------------------------------------------------------------------
    private byte[] serialize(Object object) {
        return getSerializer().serialize(object);
    }

    private <T> T deserialize(byte[] byteArray, Class<T> c) {
        return getSerializer().deserialize(byteArray, c);
    }

    private <E> List<E> deserializeForList(byte[] byteArray, Class<E> elementC) {
        return getSerializer().deserializeForList(byteArray, elementC);
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

    @PreDestroy
    public void destroy() {
        try {
            pool.destroy();
        } catch (Throwable e) {
            logger.error("redis释放失败", e);
        }
    }

    public ShardedJedisPool getPool() {
        return pool;
    }

    public void setPool(ShardedJedisPool pool) {
        this.pool = pool;
    }

    public RedisSerializer getSerializer() {

        if (this.serializer == null) {
            synchronized (this) {
                if (this.serializer == null) {
                    // 为了向下兼容默认,如果没有提供序列化器,默认使用,json序列化
                    serializer = new JsonRedisSerializer();
                    logger.info("RedisComponent [" + this.toString() + "] is done! serializer:" + serializer.toString());
                }
            }
        }

        return serializer;
    }

    public void setSerializer(RedisSerializer serializer) {
        this.serializer = serializer;
    }

    public static interface SyncLockCallback<T> {
        public T callback();
    }

    private final static String SYNC_LOCK_SUFFIX = "_SYNC";

    /**
     * 同步保护
     *
     * @param lock
     * @param expire
     * @param callback
     * @return
     */
    public <T> T sync(final Enum type, final String lock, final long expire, SyncLockCallback<T> callback) {

        if (callback == null) {
            throw new IllegalArgumentException();
        }

        if (acquire(type, lock + SYNC_LOCK_SUFFIX, expire)) {
            try {
                return callback.callback();
            } finally {
                release(type, lock + SYNC_LOCK_SUFFIX);
            }
        } else {
            return null;
        }
    }

    /**
     * 本地同步保护
     *
     * @param lock
     * @param timeout
     * @param timeoutUnit
     * @param callback
     * @return
     */
    public <T> T syncWithLock(Lock lock, long timeout, TimeUnit timeoutUnit, SyncLockCallback<T> callback) {

        if (lock == null || timeoutUnit == null || callback == null) {
            throw new IllegalArgumentException();
        }

        try {
            if (lock.tryLock(timeout, timeoutUnit)) {
                try {
                    return callback.callback();
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            logger.error("", e);
        }

        return null;
    }

    /**
     * 通过SETNX试图获取一个锁
     *
     * @param key
     * @param expire 存活时间(秒)
     * @return
     */
    private boolean acquire(final Enum type, final String key, final long expire) {
        final String redisKey = generateKey(type, key);

        return this.execute(new JedisAction<Boolean>() {
            @Override
            public Boolean action(ShardedJedis jedis) {
                boolean success = false;

                try {

                    long value = System.currentTimeMillis() + expire * 1000 + 1;

                    // 通过setnx获取一个lock
                    Long acquired = jedis.setnx(redisKey, String.valueOf(value));
                    // setnx成功，则成功获取一个锁
                    if (acquired != null && acquired > 0) {

                        if (expire > 0) {
                            try {
                                jedis.expire(redisKey, (int) expire);
                            } catch (Throwable e) {
                                logger.error("", e);
                            }
                        }

                        success = true;
                    }
                    // setnx失败，说明锁仍然被其他对象保持，检查其是否已经超时
                    else {
                        // 当前锁过期时间
                        long oldValue = Long.valueOf(jedis.get(redisKey));
                        // 超时
                        if (oldValue < System.currentTimeMillis()) {
                            // 查看是否有并发
                            String oldValueAgain = jedis.getSet(redisKey, String.valueOf(value));
                            // 获取锁成功
                            if (Long.valueOf(oldValueAgain) == oldValue) {

                                if (expire > 0) {
                                    try {
                                        jedis.expire(redisKey, (int) expire);
                                    } catch (Throwable e) {
                                        logger.error("", e);
                                    }
                                }

                                success = true;
                            }
                            // 已被其他进程捷足先登了
                            else {
                                success = false;
                            }
                        } else {
                            // 未超时，则直接返回失败
                            success = false;
                        }
                    }
                } catch (Throwable e) {
                    logger.error("", e);
                }
                return success;
            }
        });
    }

    private final static Pattern LCK_TIME = Pattern.compile("\\d+");

    /**
     * 释放锁
     *
     * @param key
     */
    private void release(final Enum type, final String key) {
        final String redisKey = generateKey(type, key);

        this.execute(new JedisAction<Void>() {
            @Override
            public Void action(ShardedJedis jedis) {
                try {

                    String lckUUID = jedis.get(redisKey);
                    if (lckUUID == null || !LCK_TIME.matcher(lckUUID).find()) {
                        return null;
                    }
                    Long getValue = Long.parseLong(lckUUID);
                    // 避免删除非自己获取得到的锁
                    if (System.currentTimeMillis() < getValue.longValue()) {
                        jedis.del(redisKey);
                    }
                } catch (Exception e) {
                    logger.error("", e);
                }

                return null;
            }
        });

    }

}
