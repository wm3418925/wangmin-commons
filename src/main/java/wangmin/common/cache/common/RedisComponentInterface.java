package wangmin.common.cache.common;

import java.util.*;

/**
 * Created by wm on 2017/3/30.
 * 封装redis 的各种操作
 */
public interface RedisComponentInterface {
    <T> T get(final Enum type, final String key, final Class<T> c);

    void set(final Enum type, final String key, final Object value);

    void set(final Enum type, final String key, final long expire, final Object value);

    Long del(final Enum type, final String key);

    <T> T getSet(final Enum type, final String key, final Object value, final Class<T> c);

    Boolean exists(final Enum type, final String key);

    Long incr(final Enum type, final String key);
    Long incrBy(final Enum type, final String key, final long integer);
    Long decr(final Enum type, final String key);
    Long decrBy(final Enum type, final String key, final long integer);

    Long expire(final Enum type, final String key, final int seconds);


    Long rpush(final Enum type, final String key, final Object... values);
    Long lpush(final Enum type, final String key, final Object... values);
    Long llen(final Enum type, final String key);
    <T> List<T> lrange(final Enum type, final String key, long start, long end, final Class<T> valueClass);
    String ltrim(final Enum type, final String key, long start, long end);
    <T> T lindex(final Enum type, final String key, long index, final Class<T> valueClass);
    String lset(final Enum type, final String key, long index, final Object value);
    Long lrem(final Enum type, final String key, long count, final Object value);
    <T> T lpop(final Enum type, final String key, final Class<T> valueClass);
    <T> T rpop(final Enum type, final String key, final Class<T> valueClass);

    
    <T> List<T> hvals(final Enum type, final String key, final Class<T> valueClass);
    <T> T hget(final Enum type, final String key, final Object mapKey, final Class<T> c);
    void hset(final Enum type, final String key, final Object mapKey, final Object mapValue);
    void hset(final Enum type, final String key, final Object mapKey, final Object mapValue, final int second);
    void hdel(final Enum type, final String key, final Object mapKey);
    void hmset(final Enum type, final String key, final Map<?, ?> map);
    void hmset(final Enum type, final String key, final Map<?, ?> map, final long expire);


    Long zadd(final Enum type, final String key, final double score, final Object member);
    <T> List<T> zrange(final Enum type, final String key, final long start, final long end, final Class<T> clazz);
    <T> List<T> zrangeByScore(final Enum type, final String key, final double min, final double max, final Class<T> clazz);
    Long zremrangeByScore(final Enum type, final String key, final double start, final double end);


    Long sadd(final Enum type, final String key, final Object value);
    Long srem(final Enum type, final String key, final Object value);
    <T> Set<T> smembers(final Enum type, final String key, final Class<T> clazz);

}
