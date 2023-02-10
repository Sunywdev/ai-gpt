package com.sunyw.xyz.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 名称: XX定义
 * 功能: <功能详细描述>
 * 方法: <方法简述-方法描述>
 * 版本: 1.0
 * 作者: sunyw
 * 说明: 基于Guava Cache构建的本地缓存
 * 时间: 2022/9/28 9:44
 */
@Component
@Slf4j
public class LocalCacheUtils<T> {



    /**
     * 缓存项最大数量
     */
    private static final long GUAVA_CACHE_SIZE=100000;
    /**
     * 缓存时间：毫秒
     */
    private static final long GUAVA_CACHE_TIME=500000;
    /**
     * 并发级别，同时写缓存的线程个数
     */
    public static final int CONCURRENCY_LEVEL=8;
    /**
     * 容器的初始值大小
     */
    public static final int INITIAL_CAPACITY=10;

    /**
     * 缓存操作对象
     */
    private static LoadingCache<String,Object> GLOBAL_CACHE;

    private final static Map<String,LoadingCache<String,Object>> ALL_GLOBAL_CACHE=Maps.newHashMap();

    static {
        GLOBAL_CACHE=buildLoadingCache(GUAVA_CACHE_SIZE,GUAVA_CACHE_TIME);
    }

    private static <K,V> LoadingCache<K,V> buildLoadingCache(Long maximumSize,Long expireAfterAccess) {
        try{
            return (LoadingCache<K,V>) loadCache(new CacheLoader<String,Object>() {
                @Override
                public Object load(String key) throws Exception {
                    /**
                     * 该方法主要是处理缓存键不存在缓存值时的处理逻辑
                     */
                    if (log.isDebugEnabled()) {
                        log.debug("Guava Cache缓存值不存在，初始化空值，键名：{}",key);
                    }
                    return ObjectUtils.NULL;
                }
            },maximumSize,expireAfterAccess);
        } catch (Exception e) {
            log.error("初始化Guava Cache出错",e);
        }
        return null;
    }

    /**
     * 全局缓存设置
     * <ul>
     * <li>缓存项最大数量：100000</li>
     * <li>缓存有效时间（分钟）：10</li>
     * </ul>
     *
     * @param cacheLoader
     * @return
     * @throws Exception
     */
    private static <K,V> LoadingCache<K,V> loadCache(CacheLoader<K,V> cacheLoader,Long maximumSize,Long expireAfterAccess) throws Exception {
        /*
         * maximumSize 缓存池大小，在缓存项接近该大小时， Guava开始回收旧的缓存项 expireAfterAccess 表示最后一次使用该缓存项多长时间后失效 removalListener 移除缓存项时执行的逻辑方法 recordStats 开启Guava Cache的统计功能
         */
        return CacheBuilder.newBuilder().initialCapacity(INITIAL_CAPACITY).maximumSize(maximumSize).expireAfterAccess(expireAfterAccess,TimeUnit.MILLISECONDS)
                .removalListener(removalNotification -> {
                    if (log.isDebugEnabled()) {
                        log.debug("Guava Cache缓存回收成功，键：{}, 值：{}",removalNotification.getKey(),removalNotification.getValue());
                    }
                }).recordStats().concurrencyLevel(CONCURRENCY_LEVEL).build(cacheLoader);
    }

    /**
     * 设置缓存值
     *
     * @param key
     * @param value
     */
    public static void put(String key,Object value) {
        try{
            GLOBAL_CACHE.put(key,value);
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(),getAverageLoadPenalty());
            }
        } catch (Exception e) {
            log.error("设置缓存值出错",e);
        }
    }

    private static LoadingCache<String,Object> getLoadCache(String key,Long expireTime) {
        LoadingCache<String,Object> cacheLoadCache=ALL_GLOBAL_CACHE.get(key);
        if (cacheLoadCache == null) {
            cacheLoadCache=buildLoadingCache(GUAVA_CACHE_SIZE,expireTime);
            ALL_GLOBAL_CACHE.put(key,cacheLoadCache);
        }
        return cacheLoadCache;
    }


    /**
     * 失效时间秒不为默认时间10分钟的
     *
     * @param key        大KEY，一般为模块信息
     * @param hashKey    查询的KEY
     * @param value      值
     * @param expireTime 失效时间(单位秒)
     */
    public static void put(String key,String hashKey,Object value,Long expireTime) {
        try{
            getLoadCache(key,expireTime).put(hashKey,value);
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(key),getAverageLoadPenalty(key));
            }
        } catch (Exception e) {
            log.error("设置缓存值出错",e);
        }
    }

    /**
     * 批量设置缓存值
     *
     * @param map
     */
    public static void putAll(Map<? extends String,? extends Object> map) {
        try{
            GLOBAL_CACHE.putAll(map);
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(),getAverageLoadPenalty());
            }
        } catch (Exception e) {
            log.error("批量设置缓存值出错",e);
        }
    }

    /**
     * 批量设置缓存值
     *
     * @param map
     */
    public static void putAll(String key,Map<? extends String,? extends Object> map,Long expireTime) {
        try{
            getLoadCache(key,expireTime).putAll(map);
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(key),getAverageLoadPenalty(key));
            }
        } catch (Exception e) {
            log.error("批量设置缓存值出错",e);
        }
    }

    /**
     * 获取缓存值
     * <p>注：如果键不存在值，将调用CacheLoader的load方法加载新值到该键中</p>
     *
     * @param key
     * @return
     */
    public static Object getIfNotPresentLoad(String key) {
        Object obj=null;
        try{
            obj=GLOBAL_CACHE.get(key);
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(),getAverageLoadPenalty());
            }
        } catch (Exception e) {
            log.error("获取缓存值出错",e);
        }
        return obj;
    }

    public static <T> T getIfNotPresentLoad(String key,Class<T> clazz) {
        Object obj=getIfNotPresentLoad(key);
        return obj == null ? null : (T) obj;
    }

    public static Object getIfNotPresentLoad(String key,String hashKey) {
        Object obj=null;
        try{
            if (ALL_GLOBAL_CACHE.get(key) == null) {
                return obj;
            }
            obj=ALL_GLOBAL_CACHE.get(key).get(hashKey);
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(key),getAverageLoadPenalty(key));
            }
        } catch (Exception e) {
            log.error("获取缓存值出错",e);
        }
        return obj;
    }

    public static <T> T getIfNotPresentLoad(String key,String hashKey,Class<T> clazz) {
        Object obj=getIfNotPresentLoad(key,hashKey);
        return obj == null ? null : (T) obj;
    }

    /**
     * 获取缓存值
     * <p>注：如果键不存在值，将直接返回 NULL</p>
     *
     * @param key
     * @return
     */
    public static Object get(String key) {
        Object obj=null;
        try{
            obj=GLOBAL_CACHE.getIfPresent(key);
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(),getAverageLoadPenalty());
            }
        } catch (Exception e) {
            log.error("获取缓存值出错",e);
        }
        return obj;
    }

    public static <T> T get(String key,Class<T> clazz) {
        Object obj=get(key);
        return obj == null ? null : (T) obj;
    }

    public static Object get(String key,String hashKey) {
        Object obj=null;
        try{
            if (ALL_GLOBAL_CACHE.get(key) == null) {
                return null;
            }
            obj=ALL_GLOBAL_CACHE.get(key).getIfPresent(hashKey);
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(key),getAverageLoadPenalty(key));
            }
        } catch (Exception e) {
            log.error("获取缓存值出错",e);
        }
        return obj;
    }

    public static <T> T get(String key,String hashKey,Class<T> clazz) {
        Object obj=get(key,hashKey);
        return obj == null ? null : (T) obj;
    }


    /**
     * 移除缓存
     *
     * @param key
     */
    public static void remove(String key) {
        try{
            GLOBAL_CACHE.invalidate(key);
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(),getAverageLoadPenalty());
            }
        } catch (Exception e) {
            log.error("移除缓存出错",e);
        }
    }

    public static void remove(String key,String hashKey) {
        try{
            if (ALL_GLOBAL_CACHE.get(key) == null) {
                return;
            }
            ALL_GLOBAL_CACHE.get(key).invalidate(hashKey);
            if (ALL_GLOBAL_CACHE.get(key).size() == 0) {
                ALL_GLOBAL_CACHE.remove(key);
            }
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(key),getAverageLoadPenalty(key));
            }
        } catch (Exception e) {
            log.error("移除缓存出错",e);
        }
    }

    /**
     * 批量移除缓存
     *
     * @param keys
     */
    public static void removeAll(Iterable<String> keys) {
        try{
            GLOBAL_CACHE.invalidateAll(keys);
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(),getAverageLoadPenalty());
            }
        } catch (Exception e) {
            log.error("批量移除缓存出错",e);
        }
    }

    public static void removeAll(String key,Iterable<String> keys) {
        try{
            if (ALL_GLOBAL_CACHE.get(key) == null) {
                return;
            }
            ALL_GLOBAL_CACHE.get(key).invalidateAll(keys);
            if (ALL_GLOBAL_CACHE.get(key).size() == 0) {
                ALL_GLOBAL_CACHE.remove(key);
            }
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(key),getAverageLoadPenalty(key));
            }
        } catch (Exception e) {
            log.error("批量移除缓存出错",e);
        }
    }

    /**
     * 清空所有缓存
     */
    public static void removeAll() {
        try{
            GLOBAL_CACHE.invalidateAll();
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(),getAverageLoadPenalty());
            }
        } catch (Exception e) {
            log.error("清空所有缓存出错",e);
        }
    }

    public static void removeAll(String key) {
        try{
            if (ALL_GLOBAL_CACHE.get(key) == null) {
                return;
            }
            ALL_GLOBAL_CACHE.get(key).invalidateAll();
            ALL_GLOBAL_CACHE.remove(key);
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(key),getAverageLoadPenalty(key));
            }
        } catch (Exception e) {
            log.error("清空所有缓存出错",e);
        }
    }

    /**
     * 获取缓存项数量
     *
     * @return
     */
    public static long size() {
        long size=0;
        try{
            size=GLOBAL_CACHE.size();
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(),getAverageLoadPenalty());
            }
        } catch (Exception e) {
            log.error("获取缓存项数量出错",e);
        }
        return size;
    }

    public static long size(String key) {
        long size=0;
        try{
            if (ALL_GLOBAL_CACHE.get(key) == null) {
                return size;
            }
            size=ALL_GLOBAL_CACHE.get(key).size();
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(key),getAverageLoadPenalty(key));
            }
        } catch (Exception e) {
            log.error("获取缓存项数量出错",e);
        }
        return size;
    }

    /**
     * 把缓存数据转换为Map对象
     *
     * @return
     */
    public static Map<String,Object> valueToMap() {
        ConcurrentMap<String,Object> valueMap=Maps.newConcurrentMap();
        try{
            valueMap=GLOBAL_CACHE.asMap();
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(),getAverageLoadPenalty());
            }
        } catch (Exception e) {
            log.error("获取所有缓存项的键出错",e);
        }
        return valueMap;
    }

    /**
     * 把缓存数据转换为Map对象
     *
     * @return
     */
    public static Map<String,Object> valueToMap(String key) {
        ConcurrentMap<String,Object> valueMap=Maps.newConcurrentMap();
        try{
            if (ALL_GLOBAL_CACHE.get(key) == null) {
                return valueMap;
            }
            valueMap=ALL_GLOBAL_CACHE.get(key).asMap();
            if (log.isDebugEnabled()) {
                log.debug("缓存命中率：{}，新值平均加载时间：{}",getHitRate(key),getAverageLoadPenalty(key));
            }
        } catch (Exception e) {
            log.error("获取所有缓存项的键出错",e);
        }
        return valueMap;
    }

    /**
     * 缓存命中率
     *
     * @return
     */
    public static double getHitRate() {
        return GLOBAL_CACHE.stats().hitRate();
    }

    public static double getHitRate(String key) {
        return ALL_GLOBAL_CACHE.get(key) == null ? 0D : ALL_GLOBAL_CACHE.get(key).stats().hitRate();
    }

    /**
     * 加载新值的平均时间，单位为纳秒
     *
     * @return
     */
    public static double getAverageLoadPenalty() {
        return GLOBAL_CACHE.stats().averageLoadPenalty();
    }

    public static double getAverageLoadPenalty(String key) {
        return ALL_GLOBAL_CACHE.get(key) == null ? 0D : ALL_GLOBAL_CACHE.get(key).stats().averageLoadPenalty();
    }

    /**
     * 缓存项被回收的总数，不包括显式清除
     *
     * @return
     */
    public static long getEvictionCount() {
        return GLOBAL_CACHE.stats().evictionCount();
    }

    public static long getEvictionCount(String key) {
        return ALL_GLOBAL_CACHE.get(key) == null ? 0L : ALL_GLOBAL_CACHE.get(key).stats().evictionCount();
    }

}
