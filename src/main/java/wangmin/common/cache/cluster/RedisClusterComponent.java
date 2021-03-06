package wangmin.common.cache.cluster;

import wangmin.common.cache.common.RedisComponentInterface;
import wangmin.common.cache.common.MyCacheUtils;
import wangmin.common.cache.common.serializer.RedisSerializer;
import redis.clients.jedis.BinaryJedisCluster;

import java.util.*;
import java.util.Map.Entry;

/**
 * Created by wm on 2017/3/20.
 * 封装cluster redis的各种操作
 */
public class RedisClusterComponent implements RedisComponentInterface {
    private BinaryJedisCluster jedisCluster;
    public void setJedisCluster(BinaryJedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    private RedisSerializer serializer;
    public void setSerializer(RedisSerializer serializer) {
        this.serializer = serializer;
    }



    public <T> T get(final Enum type, final String key, final Class<T> c) {
        byte[] valueBytes = jedisCluster.get(MyCacheUtils.generateKeyBytes(type, key));
        return deserialize(valueBytes, c);
    }

    public void set(final Enum type, final String key, final Object value) {
        jedisCluster.set(MyCacheUtils.generateKeyBytes(type, key), serialize(value));
    }
    public void set(final Enum type, final String key, final int expireSeconds, final Object value) {
        jedisCluster.setex(MyCacheUtils.generateKeyBytes(type, key), expireSeconds, serialize(value));
    }
    public void setnxExpire(final Enum type, final String key, final long expireMilliseconds, final Object value) {
        jedisCluster.set(MyCacheUtils.generateKeyBytes(type, key), serialize(value), MyCacheUtils.NX, MyCacheUtils.PX, expireMilliseconds);
    }
    public void setxxExpire(final Enum type, final String key, final long expireMilliseconds, final Object value) {
        jedisCluster.set(MyCacheUtils.generateKeyBytes(type, key), serialize(value), MyCacheUtils.XX, MyCacheUtils.PX, expireMilliseconds);
    }

    public Long del(final Enum type, final String key) {
        return jedisCluster.del(MyCacheUtils.generateKeyBytes(type, key));
    }

    public <T> T getSet(final Enum type, final String key, final Object value, final Class<T> c) {
        byte[] valueBytes = jedisCluster.getSet(MyCacheUtils.generateKeyBytes(type, key), serialize(value));
        return deserialize(valueBytes, c);
    }

    public Boolean exists(final Enum type, final String key) {
        return jedisCluster.exists(MyCacheUtils.generateKeyBytes(type, key));
    }

    public Long incr(final Enum type, final String key) {
        return jedisCluster.incr(MyCacheUtils.generateKeyBytes(type, key));
    }
    public Long incrBy(final Enum type, final String key, final long integer) {
        return jedisCluster.incrBy(MyCacheUtils.generateKeyBytes(type, key), integer);
    }
    public Long decr(final Enum type, final String key) {
        return jedisCluster.decr(MyCacheUtils.generateKeyBytes(type, key));
    }
    public Long decrBy(final Enum type, final String key, final long integer) {
        return jedisCluster.decrBy(MyCacheUtils.generateKeyBytes(type, key), integer);
    }

    public Long expire(final Enum type, final String key, final int seconds) {
        return jedisCluster.expire(MyCacheUtils.generateKeyBytes(type, key), seconds);
    }

    @Override
    public Long rpush(final Enum type, final String key, final Object... values) {
        byte[][] valuesBytes = new byte[values.length][];
        for (int i=0; i<values.length; ++i) {
            valuesBytes[i] = serialize(values[i]);
        }
        return jedisCluster.rpush(MyCacheUtils.generateKeyBytes(type, key), valuesBytes);
    }
    @Override
    public Long lpush(final Enum type, final String key, final Object... values) {
        byte[][] valuesBytes = new byte[values.length][];
        for (int i=0; i<values.length; ++i) {
            valuesBytes[i] = serialize(values[i]);
        }
        return jedisCluster.lpush(MyCacheUtils.generateKeyBytes(type, key), valuesBytes);
    }
    @Override
    public Long llen(final Enum type, final String key) {
        return jedisCluster.llen(MyCacheUtils.generateKeyBytes(type, key));
    }
    @Override
    public <T> List<T> lrange(final Enum type, final String key, long start, long end, final Class<T> valueClass) {
        List<byte[]> bytesList = jedisCluster.lrange(MyCacheUtils.generateKeyBytes(type, key), start, end);
        List<T> result = new ArrayList<>();
        for (byte[] bytes : bytesList) {
            result.add(deserialize(bytes, valueClass));
        }
        return result;
    }
    @Override
    public String ltrim(final Enum type, final String key, long start, long end) {
        return jedisCluster.ltrim(MyCacheUtils.generateKeyBytes(type, key), start, end);
    }
    @Override
    public <T> T lindex(final Enum type, final String key, long index, final Class<T> valueClass) {
        byte[] bytes = jedisCluster.lindex(MyCacheUtils.generateKeyBytes(type, key), index);
        return deserialize(bytes, valueClass);
    }
    @Override
    public String lset(final Enum type, final String key, long index, final Object value) {
        return jedisCluster.lset(MyCacheUtils.generateKeyBytes(type, key), index, serialize(value));
    }
    @Override
    public Long lrem(final Enum type, final String key, long count, final Object value) {
        return jedisCluster.lrem(MyCacheUtils.generateKeyBytes(type, key), count, serialize(value));
    }
    @Override
    public <T> T lpop(final Enum type, final String key, final Class<T> valueClass) {
        byte[] bytes = jedisCluster.lpop(MyCacheUtils.generateKeyBytes(type, key));
        return deserialize(bytes, valueClass);
    }
    @Override
    public <T> T rpop(final Enum type, final String key, final Class<T> valueClass) {
        byte[] bytes = jedisCluster.rpop(MyCacheUtils.generateKeyBytes(type, key));
        return deserialize(bytes, valueClass);
    }


    public <T> List<T> hvals(final Enum type, final String key, final Class<T> valueClass) {
        Collection<byte[]> value = jedisCluster.hvals(MyCacheUtils.generateKeyBytes(type, key));
        List<T> list = new ArrayList<>(value.size());
        for (byte[] bs : value) {
            list.add(deserialize(bs, valueClass));
        }
        return list;
    }
    public <T> T hget(final Enum type, final String key, final Object mapKey, final Class<T> valueClass) {
        byte[] bs = jedisCluster.hget(MyCacheUtils.generateKeyBytes(type, key), serialize(mapKey));
        return deserialize(bs, valueClass);
    }
    public void hset(final Enum type, final String key, final Object mapKey, final Object mapValue) {
        jedisCluster.hset(MyCacheUtils.generateKeyBytes(type, key), serialize(mapKey), serialize(mapValue));
    }
    public void hset(final Enum type, final String key, final Object mapKey, final Object mapValue, final int expireSeconds) {
        byte[] keyBytes = MyCacheUtils.generateKeyBytes(type, key);
        jedisCluster.hset(keyBytes, serialize(mapKey), serialize(mapValue));
        jedisCluster.expire(keyBytes, expireSeconds);
    }
    public void hdel(final Enum type, final String key, final Object mapKey) {
        jedisCluster.hdel(MyCacheUtils.generateKeyBytes(type, key), serialize(mapKey));
    }
    public void hmset(final Enum type, final String key, final Map<?, ?> map) {
        if (map != null && !map.isEmpty()) {
            Map<byte[], byte[]> m = new HashMap<>(map.size());

            Iterator<?> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Entry<?, ?> next = (Entry<?, ?>) it.next();
                m.put(serialize(next.getKey()), serialize(next.getValue()));
            }

            jedisCluster.hmset(MyCacheUtils.generateKeyBytes(type, key), m);
        }
    }
    public void hmset(final Enum type, final String key, final Map<?, ?> map, final int expireSeconds) {
        if (map != null && !map.isEmpty()) {
            Map<byte[], byte[]> m = new HashMap<>(map.size());

            Iterator<?> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Entry<?, ?> next = (Entry<?, ?>) it.next();
                m.put(serialize(next.getKey()), serialize(next.getValue()));
            }

            byte[] keyBytes = MyCacheUtils.generateKeyBytes(type, key);
            jedisCluster.hmset(keyBytes, m);
            jedisCluster.expire(keyBytes, expireSeconds);
        }
    }


    public Long zadd(final Enum type, final String key, final double score, final Object member) {
        return jedisCluster.zadd(MyCacheUtils.generateKeyBytes(type, key), score, serialize(member));
    }
    public <T> List<T> zrange(final Enum type, final String key, final long start, final long end, final Class<T> clazz) {
        Collection<byte[]> value = jedisCluster.zrange(MyCacheUtils.generateKeyBytes(type, key), start, end);
        List<T> list = new ArrayList<>(value.size());
        for (byte[] b : value) {
            list.add(deserialize(b, clazz));
        }
        return list;
    }
    public <T> List<T> zrangeByScore(final Enum type, final String key, final double min, final double max, final Class<T> clazz) {
        Collection<byte[]> value = jedisCluster.zrangeByScore(MyCacheUtils.generateKeyBytes(type, key), min, max);
        List<T> list = new ArrayList<>(value.size());
        for (byte[] b : value) {
            list.add(deserialize(b, clazz));
        }
        return list;
    }
    public Long zremrangeByScore(final Enum type, final String key, final double start, final double end) {
        return jedisCluster.zremrangeByScore(MyCacheUtils.generateKeyBytes(type, key), start, end);
    }
    public Long zrem(final Enum type, final String key, final Object... members) {
        byte[][] byteMembers = new byte[members.length][];
        for (int i=0; i<members.length; ++i) {
            byteMembers[i] = serialize(members[i]);
        }
        return jedisCluster.zrem(MyCacheUtils.generateKeyBytes(type, key), byteMembers);
    }


    public Long sadd(final Enum type, final String key, final Object value) {
        return jedisCluster.sadd(MyCacheUtils.generateKeyBytes(type, key), serialize(value));
    }
    public Long srem(final Enum type, final String key, final Object value) {
        return jedisCluster.srem(MyCacheUtils.generateKeyBytes(type, key), serialize(value));
    }
    public <T> Set<T> smembers(final Enum type, final String key, final Class<T> clazz) {
        Set<byte[]> bytesSet = jedisCluster.smembers(MyCacheUtils.generateKeyBytes(type, key));
        Set<T> result = new HashSet<>();
        for (byte[] entry : bytesSet) {
            if (null != entry)
                result.add(deserialize(entry, clazz));
        }
        return result;
    }



    // private method --------------------------------------------------------
    private byte[] serialize(Object object) {
        return serializer.serializeObject(object);
    }
    private <T> T deserialize(byte[] byteArray, Class<T> c) {
        return serializer.deserializeObjectByClass(byteArray, c);
    }

}
