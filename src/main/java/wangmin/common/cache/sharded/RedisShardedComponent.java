package wangmin.common.cache.sharded;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import wangmin.common.cache.common.MyCacheUtils;
import wangmin.common.cache.common.RedisComponentInterface;
import wangmin.common.cache.common.serializer.RedisSerializer;

/**
 * Created by wm on 2017/3/21.
 * 封装sharded redis的各种操作
 */
public class RedisShardedComponent implements RedisComponentInterface {
    private final static Logger logger = LoggerFactory.getLogger(RedisShardedComponent.class);

    private ShardedJedisPool pool;
    public void setPool(ShardedJedisPool pool) {
        this.pool = pool;
    }

    private RedisSerializer serializer;
    public void setSerializer(RedisSerializer serializer) {
        this.serializer = serializer;
    }



    public <T> T get(final Enum type, final String key, final Class<T> c) {
        return this.execute(new JedisAction<T>() {
            @Override
            public T action(ShardedJedis jedis) {
                byte[] bs = jedis.get(MyCacheUtils.generateKeyBytes(type, key));
                return deserialize(bs, c);
            }
        });
    }

    public void set(final Enum type, final String key, final Object value) {
        this.execute(new JedisActionNoResult() {
            @Override
            public void action(ShardedJedis jedis) {
                jedis.set(MyCacheUtils.generateKeyBytes(type, key), serialize(value));
            }
        });
    }

    public void set(final Enum type, final String key, final long expire, final Object value) {
        this.execute(new JedisActionNoResult() {
            @Override
            public void action(ShardedJedis jedis) {
                jedis.setex(MyCacheUtils.generateKeyBytes(type, key), (int) expire, serialize(value));
            }
        });
    }

    public Long del(final Enum type, final String key) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.del(MyCacheUtils.generateKeyBytes(type, key));
            }
        });
    }

    public <T> T getSet(final Enum type, final String key, final Object value, final Class<T> c) {
        return this.execute(new JedisAction<T>() {
            @Override
            public T action(ShardedJedis jedis) {
                byte[] bs = jedis.getSet(MyCacheUtils.generateKeyBytes(type, key), serialize(value));
                return deserialize(bs, c);
            }
        });
    }

    public Boolean exists(final Enum type, final String key) {
        return this.execute(new JedisAction<Boolean>() {
            @Override
            public Boolean action(ShardedJedis jedis) {
                return jedis.exists(MyCacheUtils.generateKeyBytes(type, key));
            }
        });
    }

    public Long incr(final Enum type, final String key) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.incr(MyCacheUtils.generateKeyBytes(type, key));
            }
        });
    }
    public Long incrBy(final Enum type, final String key, final long integer) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.incrBy(MyCacheUtils.generateKeyBytes(type, key), integer);
            }
        });
    }
    public Long decr(final Enum type, final String key) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.decr(MyCacheUtils.generateKeyBytes(type, key));
            }
        });
    }
    public Long decrBy(final Enum type, final String key, final long integer) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.decrBy(MyCacheUtils.generateKeyBytes(type, key), integer);
            }
        });
    }

    public Long expire(final Enum type, final String key, final int seconds) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.expire(MyCacheUtils.generateKeyBytes(type, key), seconds);
            }
        });
    }

    @Override
    public Long rpush(final Enum type, final String key, final Object... values) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                byte[][] valuesBytes = new byte[values.length][];
                for (int i = 0; i < values.length; ++i) {
                    valuesBytes[i] = serialize(values[i]);
                }
                return jedis.rpush(MyCacheUtils.generateKeyBytes(type, key), valuesBytes);
            }
        });
    }
    @Override
    public Long lpush(final Enum type, final String key, final Object... values) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                byte[][] valuesBytes = new byte[values.length][];
                for (int i = 0; i < values.length; ++i) {
                    valuesBytes[i] = serialize(values[i]);
                }
                return jedis.lpush(MyCacheUtils.generateKeyBytes(type, key), valuesBytes);
            }
        });
    }
    @Override
    public Long llen(final Enum type, final String key) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.llen(MyCacheUtils.generateKeyBytes(type, key));
            }
        });
    }
    @Override
    public <T> List<T> lrange(final Enum type, final String key, final long start, final long end, final Class<T> valueClass) {
        return this.execute(new JedisAction<List<T>>() {
            @Override
            public List<T> action(ShardedJedis jedis) {
                List<byte[]> bytesList = jedis.lrange(MyCacheUtils.generateKeyBytes(type, key), start, end);
                List<T> result = new ArrayList<>();
                for (byte[] bytes : bytesList) {
                    result.add(deserialize(bytes, valueClass));
                }
                return result;
            }
        });
    }
    @Override
    public String ltrim(final Enum type, final String key, final long start, final long end) {
        return this.execute(new JedisAction<String>() {
            @Override
            public String action(ShardedJedis jedis) {
                return jedis.ltrim(MyCacheUtils.generateKeyBytes(type, key), start, end);
            }
        });
    }
    @Override
    public <T> T lindex(final Enum type, final String key, final long index, final Class<T> valueClass) {
        return this.execute(new JedisAction<T>() {
            @Override
            public T action(ShardedJedis jedis) {
                byte[] bytes = jedis.lindex(MyCacheUtils.generateKeyBytes(type, key), index);
                return deserialize(bytes, valueClass);
            }
        });
    }
    @Override
    public String lset(final Enum type, final String key, final long index, final Object value) {
        return this.execute(new JedisAction<String>() {
            @Override
            public String action(ShardedJedis jedis) {
                return jedis.lset(MyCacheUtils.generateKeyBytes(type, key), index, serialize(value));
            }
        });
    }
    @Override
    public Long lrem(final Enum type, final String key, final long count, final Object value) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.lrem(MyCacheUtils.generateKeyBytes(type, key), count, serialize(value));
            }
        });
    }
    @Override
    public <T> T lpop(final Enum type, final String key, final Class<T> valueClass) {
        return this.execute(new JedisAction<T>() {
            @Override
            public T action(ShardedJedis jedis) {
                byte[] bytes = jedis.lpop(MyCacheUtils.generateKeyBytes(type, key));
                return deserialize(bytes, valueClass);
            }
        });
    }
    @Override
    public <T> T rpop(final Enum type, final String key, final Class<T> valueClass) {
        return this.execute(new JedisAction<T>() {
            @Override
            public T action(ShardedJedis jedis) {
                byte[] bytes = jedis.rpop(MyCacheUtils.generateKeyBytes(type, key));
                return deserialize(bytes, valueClass);
            }
        });
    }


    public <T> List<T> hvals(final Enum type, final String key, final Class<T> valueClass) {
        return this.executeForList(new JedisActionForList<T>() {
            @Override
            public List<T> action(ShardedJedis jedis) {
                Collection<byte[]> value = jedis.hvals(MyCacheUtils.generateKeyBytes(type, key));
                List<T> list = new ArrayList<>(value.size());
                for (byte[] bs : value) {
                    list.add(deserialize(bs, valueClass));
                }
                return list;
            }
        });
    }
    public <T> T hget(final Enum type, final String key, final Object mapKey, final Class<T> c) {
        return this.execute(new JedisAction<T>() {
            @Override
            public T action(ShardedJedis jedis) {
                byte[] bs = jedis.hget(MyCacheUtils.generateKeyBytes(type, key), serialize(mapKey));
                return deserialize(bs, c);
            }
        });
    }
    public void hset(final Enum type, final String key, final Object mapKey, final Object mapValue) {
        this.execute(new JedisActionNoResult() {
            @Override
            public void action(ShardedJedis jedis) {
                jedis.hset(MyCacheUtils.generateKeyBytes(type, key), serialize(mapKey), serialize(mapValue));
            }
        });
    }
    public void hset(final Enum type, final String key, final Object mapKey, final Object mapValue, final int second) {
        this.execute(new JedisActionNoResult() {
            @Override
            public void action(ShardedJedis jedis) {
                jedis.hset(MyCacheUtils.generateKeyBytes(type, key), serialize(mapKey), serialize(mapValue));
                jedis.expire(MyCacheUtils.generateKeyBytes(type, key), second);
            }
        });
    }
    public void hdel(final Enum type, final String key, final Object mapKey) {
        this.execute(new JedisActionNoResult() {
            @Override
            public void action(ShardedJedis jedis) {
                jedis.hdel(MyCacheUtils.generateKeyBytes(type, key), serialize(mapKey));
            }
        });
    }
    public void hmset(final Enum type, final String key, final Map<?, ?> map) {
        this.execute(new JedisActionNoResult() {
            @Override
            public void action(ShardedJedis jedis) {
                if (map != null && !map.isEmpty()) {
                    Map<byte[], byte[]> m = new HashMap<>(map.size());

                    Iterator<?> it = map.entrySet().iterator();
                    while (it.hasNext()) {
                        Entry<?, ?> next = (Entry<?, ?>) it.next();
                        m.put(serialize(next.getKey()), serialize(next.getValue()));
                    }

                    jedis.hmset(MyCacheUtils.generateKeyBytes(type, key), m);
                }
            }
        });
    }
    public void hmset(final Enum type, final String key, final Map<?, ?> map, final long expire) {
        this.execute(new JedisActionNoResult() {
            @Override
            public void action(ShardedJedis jedis) {
                if (map != null && !map.isEmpty()) {
                    Map<byte[], byte[]> m = new HashMap<>(map.size());

                    Iterator<?> it = map.entrySet().iterator();
                    while (it.hasNext()) {
                        Entry<?, ?> next = (Entry<?, ?>) it.next();
                        m.put(serialize(next.getKey()), serialize(next.getValue()));
                    }

                    byte[] keyBytes = MyCacheUtils.generateKeyBytes(type, key);
                    jedis.hmset(keyBytes, m);
                    jedis.expire(keyBytes, (int) expire);
                }
            }
        });
    }


    public Long zadd(final Enum type, final String key, final double score, final Object member) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.zadd(MyCacheUtils.generateKeyBytes(type, key), score, serialize(member));
            }
        });
    }
    public <T> List<T> zrange(final Enum type, final String key, final long start, final long end, final Class<T> clazz) {
        return this.executeForList(new JedisActionForList<T>() {
            @Override
            public List<T> action(ShardedJedis jedis) {
                Collection<byte[]> value = jedis.zrange(MyCacheUtils.generateKeyBytes(type, key), start, end);
                List<T> list = new ArrayList<>(value.size());
                for (byte[] b : value) {
                    list.add(deserialize(b, clazz));
                }
                return list;
            }
        });
    }
    public <T> List<T> zrangeByScore(final Enum type, final String key, final double min, final double max, final Class<T> clazz) {
        return this.executeForList(new JedisActionForList<T>() {
            @Override
            public List<T> action(ShardedJedis jedis) {
                Collection<byte[]> value = jedis.zrangeByScore(MyCacheUtils.generateKeyBytes(type, key), min, max);
                List<T> list = new ArrayList<>(value.size());
                for (byte[] b : value) {
                    list.add(deserialize(b, clazz));
                }
                return list;
            }
        });
    }
    public Long zremrangeByScore(final Enum type, final String key, final double start, final double end) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.zremrangeByScore(MyCacheUtils.generateKeyBytes(type, key), start, end);
            }
        });
    }
    public Long zrem(final Enum type, final String key, final Object... members) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                byte[][] bytes = new byte[members.length][];
                for (int i=0; i<members.length; ++i) {
                    bytes[i] = serialize(members[i]);
                }
                return jedis.zrem(MyCacheUtils.generateKeyBytes(type, key), bytes);
            }
        });
    }


    public Long sadd(final Enum type, final String key, final Object value) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.sadd(MyCacheUtils.generateKeyBytes(type, key), serialize(value));
            }
        });
    }
    public Long srem(final Enum type, final String key, final Object value) {
        return this.execute(new JedisAction<Long>() {
            @Override
            public Long action(ShardedJedis jedis) {
                return jedis.srem(MyCacheUtils.generateKeyBytes(type, key), serialize(value));
            }
        });
    }
    public <T> Set<T> smembers(final Enum type, final String key, final Class<T> clazz) {
        return this.execute(new JedisAction<Set<T>>() {
            @Override
            public Set<T> action(ShardedJedis jedis) {
                Set<byte[]> bytesSet = jedis.smembers(MyCacheUtils.generateKeyBytes(type, key));
                Set<T> result = new HashSet<>();
                for (byte[] bytes : bytesSet) {
                    result.add(deserialize(bytes, clazz));
                }
                return result;
            }
        });
    }



    // private member --------------------------------------------------------
    private final static Map<Long, Integer> poolRefCounter = new ConcurrentHashMap<>();
    private boolean debug = false;


    // private method --------------------------------------------------------
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

    private byte[] serialize(Object object) {
        return serializer.serializeObject(object);
    }

    private <T> T deserialize(byte[] byteArray, Class<T> c) {
        return serializer.deserializeObjectByClass(byteArray, c);
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




    // public method --------------------------------------------------------
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    @PreDestroy
    public void destroy() {
        try {
            pool.destroy();
        } catch (Throwable e) {
            logger.error("redis释放失败", e);
        }
    }

}
