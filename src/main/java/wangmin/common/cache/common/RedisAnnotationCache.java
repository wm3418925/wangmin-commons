package wangmin.common.cache.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wm on 2017/3/22.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface RedisAnnotationCache {
    String type();                      // 所属业务, 当做key的前缀, 不能为空
    int keyIndex() default 0;           // 缓存key参数位置, 从0开始
    int expireSeconds() default 30;     // 缓存超时秒数, 默认30秒
    String arrayInterStr() default ","; // 如果主键是 Collection 或者 数组, 则将主键排序并拼接, 连接符为该参数
}
