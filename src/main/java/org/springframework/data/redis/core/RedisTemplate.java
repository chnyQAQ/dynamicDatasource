package org.springframework.data.redis.core;

import java.io.Closeable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.connection.RedisTxCommands;
import org.springframework.data.redis.connection.SortParameters;
import org.springframework.data.redis.connection.RedisZSetCommands.Tuple;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.query.QueryUtils;
import org.springframework.data.redis.core.query.SortQuery;
import org.springframework.data.redis.core.script.DefaultScriptExecutor;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.script.ScriptExecutor;
import org.springframework.data.redis.core.types.RedisClientInfo;
import org.springframework.data.redis.hash.HashMapper;
import org.springframework.data.redis.hash.ObjectHashMapper;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationUtils;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

public class RedisTemplate<K, V> extends RedisAccessor implements RedisOperations<K, V>, BeanClassLoaderAware {
    private boolean enableTransactionSupport = false;
    private boolean exposeConnection = false;
    private boolean initialized = false;
    private boolean enableDefaultSerializer = true;
    @Nullable
    private RedisSerializer<?> defaultSerializer;
    @Nullable
    private ClassLoader classLoader;
    @Nullable
    private RedisSerializer keySerializer = null;
    @Nullable
    private RedisSerializer valueSerializer = null;
    @Nullable
    private RedisSerializer hashKeySerializer = null;
    @Nullable
    private RedisSerializer hashValueSerializer = null;
    private RedisSerializer<String> stringSerializer = RedisSerializer.string();
    @Nullable
    private ScriptExecutor<K> scriptExecutor;
    private final ValueOperations<K, V> valueOps = new DefaultValueOperations(this);
    private final ListOperations<K, V> listOps = new DefaultListOperations(this);
    private final SetOperations<K, V> setOps = new DefaultSetOperations(this);
    private final StreamOperations<K, ?, ?> streamOps = new DefaultStreamOperations(this, new ObjectHashMapper());
    private final ZSetOperations<K, V> zSetOps = new DefaultZSetOperations(this);
    private final GeoOperations<K, V> geoOps = new DefaultGeoOperations(this);
    private final HyperLogLogOperations<K, V> hllOps = new DefaultHyperLogLogOperations(this);
    private final ClusterOperations<K, V> clusterOps = new DefaultClusterOperations(this);

    public RedisTemplate() {
    }

    public RedisTemplate(RedisConnectionFactory connectionFactory) {
        this.setConnectionFactory(connectionFactory);
    }

    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        boolean defaultUsed = false;
        if (this.defaultSerializer == null) {
            this.defaultSerializer = new JdkSerializationRedisSerializer(this.classLoader != null ? this.classLoader : this.getClass().getClassLoader());
        }

        if (this.enableDefaultSerializer) {
            if (this.keySerializer == null) {
                this.keySerializer = this.defaultSerializer;
                defaultUsed = true;
            }

            if (this.valueSerializer == null) {
                this.valueSerializer = this.defaultSerializer;
                defaultUsed = true;
            }

            if (this.hashKeySerializer == null) {
                this.hashKeySerializer = this.defaultSerializer;
                defaultUsed = true;
            }

            if (this.hashValueSerializer == null) {
                this.hashValueSerializer = this.defaultSerializer;
                defaultUsed = true;
            }
        }

        if (this.enableDefaultSerializer && defaultUsed) {
            Assert.notNull(this.defaultSerializer, "default serializer null and not all serializers initialized");
        }

        if (this.scriptExecutor == null) {
            this.scriptExecutor = new DefaultScriptExecutor(this);
        }

        this.initialized = true;
    }

    @Nullable
    public <T> T execute(RedisCallback<T> action) {
        return this.execute(action, this.isExposeConnection());
    }

    @Nullable
    public <T> T execute(RedisCallback<T> action, boolean exposeConnection) {
        return this.execute(action, exposeConnection, false);
    }

    @Nullable
    public <T> T execute(RedisCallback<T> action, boolean exposeConnection, boolean pipeline) {
        Assert.isTrue(this.initialized, "template not initialized; call afterPropertiesSet() before using it");
        Assert.notNull(action, "Callback object must not be null");
        RedisConnectionFactory factory = this.getRequiredConnectionFactory();
        RedisConnection conn = null;

        Object var11;
        try {
            if (this.enableTransactionSupport) {
                conn = RedisConnectionUtils.bindConnection(factory, this.enableTransactionSupport);
            } else {
                conn = RedisConnectionUtils.getConnection(factory);
            }

            boolean existingConnection = TransactionSynchronizationManager.hasResource(factory);
            RedisConnection connToUse = this.preProcessConnection(conn, existingConnection);
            boolean pipelineStatus = connToUse.isPipelined();
            if (pipeline && !pipelineStatus) {
                connToUse.openPipeline();
            }

            RedisConnection connToExpose = exposeConnection ? connToUse : this.createRedisConnectionProxy(connToUse);
            T result = action.doInRedis(connToExpose);
            if (pipeline && !pipelineStatus) {
                connToUse.closePipeline();
            }

            var11 = this.postProcessResult(result, connToUse, existingConnection);
        } finally {
            RedisConnectionUtils.releaseConnection(conn, factory, this.enableTransactionSupport);
        }

        return (T) var11;
    }

    public <T> T execute(SessionCallback<T> session) {
        Assert.isTrue(this.initialized, "template not initialized; call afterPropertiesSet() before using it");
        Assert.notNull(session, "Callback object must not be null");
        RedisConnectionFactory factory = this.getRequiredConnectionFactory();
        RedisConnectionUtils.bindConnection(factory, this.enableTransactionSupport);

        Object var3;
        try {
            var3 = session.execute(this);
        } finally {
            RedisConnectionUtils.unbindConnection(factory);
        }

        return (T) var3;
    }

    public List<Object> executePipelined(SessionCallback<?> session) {
        return this.executePipelined(session, this.valueSerializer);
    }

    public List<Object> executePipelined(SessionCallback<?> session, @Nullable RedisSerializer<?> resultSerializer) {
        Assert.isTrue(this.initialized, "template not initialized; call afterPropertiesSet() before using it");
        Assert.notNull(session, "Callback object must not be null");
        RedisConnectionFactory factory = this.getRequiredConnectionFactory();
        RedisConnectionUtils.bindConnection(factory, this.enableTransactionSupport);

        List var4;
        try {
            var4 = (List)this.execute((RedisCallback<List>) (connection) -> {
                connection.openPipeline();
                boolean pipelinedClosed = false;

                List var7;
                try {
                    Object result = this.executeSession(session);
                    if (result != null) {
                        throw new InvalidDataAccessApiUsageException("Callback cannot return a non-null value as it gets overwritten by the pipeline");
                    }

                    List<Object> closePipeline = connection.closePipeline();
                    pipelinedClosed = true;
                    var7 = this.deserializeMixedResults(closePipeline, resultSerializer, this.hashKeySerializer, this.hashValueSerializer);
                } finally {
                    if (!pipelinedClosed) {
                        connection.closePipeline();
                    }

                }

                return var7;
            });
        } finally {
            RedisConnectionUtils.unbindConnection(factory);
        }

        return var4;
    }

    public List<Object> executePipelined(RedisCallback<?> action) {
        return this.executePipelined(action, this.valueSerializer);
    }

    public List<Object> executePipelined(RedisCallback<?> action, @Nullable RedisSerializer<?> resultSerializer) {
        return (List)this.execute((RedisCallback<List>) (connection) -> {
            connection.openPipeline();
            boolean pipelinedClosed = false;

            List var7;
            try {
                Object result = action.doInRedis(connection);
                if (result != null) {
                    throw new InvalidDataAccessApiUsageException("Callback cannot return a non-null value as it gets overwritten by the pipeline");
                }

                List<Object> closePipeline = connection.closePipeline();
                pipelinedClosed = true;
                var7 = this.deserializeMixedResults(closePipeline, resultSerializer, this.hashKeySerializer, this.hashValueSerializer);
            } finally {
                if (!pipelinedClosed) {
                    connection.closePipeline();
                }

            }

            return var7;
        });
    }

    public <T> T execute(RedisScript<T> script, List<K> keys, Object... args) {
        return this.scriptExecutor.execute(script, keys, args);
    }

    public <T> T execute(RedisScript<T> script, RedisSerializer<?> argsSerializer, RedisSerializer<T> resultSerializer, List<K> keys, Object... args) {
        return this.scriptExecutor.execute(script, argsSerializer, resultSerializer, keys, args);
    }

    public <T extends Closeable> T executeWithStickyConnection(RedisCallback<T> callback) {
        Assert.isTrue(this.initialized, "template not initialized; call afterPropertiesSet() before using it");
        Assert.notNull(callback, "Callback object must not be null");
        RedisConnectionFactory factory = this.getRequiredConnectionFactory();
        RedisConnection connection = this.preProcessConnection(RedisConnectionUtils.doGetConnection(factory, true, false, false), false);
        return (T) callback.doInRedis(connection);
    }

    private Object executeSession(SessionCallback<?> session) {
        return session.execute(this);
    }

    protected RedisConnection createRedisConnectionProxy(RedisConnection pm) {
        Class<?>[] ifcs = ClassUtils.getAllInterfacesForClass(pm.getClass(), this.getClass().getClassLoader());
        return (RedisConnection)Proxy.newProxyInstance(pm.getClass().getClassLoader(), ifcs, new CloseSuppressingInvocationHandler(pm));
    }

    protected RedisConnection preProcessConnection(RedisConnection connection, boolean existingConnection) {
        return connection;
    }

    @Nullable
    protected <T> T postProcessResult(@Nullable T result, RedisConnection conn, boolean existingConnection) {
        return result;
    }

    public boolean isExposeConnection() {
        return this.exposeConnection;
    }

    public void setExposeConnection(boolean exposeConnection) {
        this.exposeConnection = exposeConnection;
    }

    public boolean isEnableDefaultSerializer() {
        return this.enableDefaultSerializer;
    }

    public void setEnableDefaultSerializer(boolean enableDefaultSerializer) {
        this.enableDefaultSerializer = enableDefaultSerializer;
    }

    @Nullable
    public RedisSerializer<?> getDefaultSerializer() {
        return this.defaultSerializer;
    }

    public void setDefaultSerializer(RedisSerializer<?> serializer) {
        this.defaultSerializer = serializer;
    }

    public void setKeySerializer(RedisSerializer<?> serializer) {
        this.keySerializer = serializer;
    }

    public RedisSerializer<?> getKeySerializer() {
        return this.keySerializer;
    }

    public void setValueSerializer(RedisSerializer<?> serializer) {
        this.valueSerializer = serializer;
    }

    public RedisSerializer<?> getValueSerializer() {
        return this.valueSerializer;
    }

    public RedisSerializer<?> getHashKeySerializer() {
        return this.hashKeySerializer;
    }

    public void setHashKeySerializer(RedisSerializer<?> hashKeySerializer) {
        this.hashKeySerializer = hashKeySerializer;
    }

    public RedisSerializer<?> getHashValueSerializer() {
        return this.hashValueSerializer;
    }

    public void setHashValueSerializer(RedisSerializer<?> hashValueSerializer) {
        this.hashValueSerializer = hashValueSerializer;
    }

    public RedisSerializer<String> getStringSerializer() {
        return this.stringSerializer;
    }

    public void setStringSerializer(RedisSerializer<String> stringSerializer) {
        this.stringSerializer = stringSerializer;
    }

    public void setScriptExecutor(ScriptExecutor<K> scriptExecutor) {
        this.scriptExecutor = scriptExecutor;
    }

    private byte[] rawKey(Object key) {
        Assert.notNull(key, "non null key required");
        return this.keySerializer == null && key instanceof byte[] ? (byte[])((byte[])key) : this.keySerializer.serialize(key);
    }

    private byte[] rawString(String key) {
        return this.stringSerializer.serialize(key);
    }

    private byte[] rawValue(Object value) {
        return this.valueSerializer == null && value instanceof byte[] ? (byte[])((byte[])value) : this.valueSerializer.serialize(value);
    }

    private byte[][] rawKeys(Collection<K> keys) {
        byte[][] rawKeys = new byte[keys.size()][];
        int i = 0;

        Object key;
        for(Iterator var4 = keys.iterator(); var4.hasNext(); rawKeys[i++] = this.rawKey(key)) {
            key = var4.next();
        }

        return rawKeys;
    }

    private K deserializeKey(byte[] value) {
        return this.keySerializer != null ? (K) this.keySerializer.deserialize(value) : (K) value;
    }

    @Nullable
    private List<Object> deserializeMixedResults(@Nullable List<Object> rawValues, @Nullable RedisSerializer valueSerializer, @Nullable RedisSerializer hashKeySerializer, @Nullable RedisSerializer hashValueSerializer) {
        if (rawValues == null) {
            return null;
        } else {
            List<Object> values = new ArrayList();
            Iterator var6 = rawValues.iterator();

            while(true) {
                while(var6.hasNext()) {
                    Object rawValue = var6.next();
                    if (rawValue instanceof byte[] && valueSerializer != null) {
                        values.add(valueSerializer.deserialize((byte[])((byte[])rawValue)));
                    } else if (rawValue instanceof List) {
                        values.add(this.deserializeMixedResults((List)rawValue, valueSerializer, hashKeySerializer, hashValueSerializer));
                    } else if (rawValue instanceof Set && !((Set)rawValue).isEmpty()) {
                        values.add(this.deserializeSet((Set)rawValue, valueSerializer));
                    } else if (rawValue instanceof Map && !((Map)rawValue).isEmpty() && ((Map)rawValue).values().iterator().next() instanceof byte[]) {
                        values.add(SerializationUtils.deserialize((Map)rawValue, hashKeySerializer, hashValueSerializer));
                    } else {
                        values.add(rawValue);
                    }
                }

                return values;
            }
        }
    }

    private Set<?> deserializeSet(Set rawSet, @Nullable RedisSerializer valueSerializer) {
        if (rawSet.isEmpty()) {
            return rawSet;
        } else {
            Object setValue = rawSet.iterator().next();
            if (setValue instanceof byte[] && valueSerializer != null) {
                return SerializationUtils.deserialize(rawSet, valueSerializer);
            } else {
                return setValue instanceof Tuple ? this.convertTupleValues(rawSet, valueSerializer) : rawSet;
            }
        }
    }

    private Set<TypedTuple<V>> convertTupleValues(Set<Tuple> rawValues, @Nullable RedisSerializer valueSerializer) {
        Set<TypedTuple<V>> set = new LinkedHashSet(rawValues.size());

        Tuple rawValue;
        Object value;
        for(Iterator var4 = rawValues.iterator(); var4.hasNext(); set.add(new DefaultTypedTuple(value, rawValue.getScore()))) {
            rawValue = (Tuple)var4.next();
            value = rawValue.getValue();
            if (valueSerializer != null) {
                value = valueSerializer.deserialize(rawValue.getValue());
            }
        }

        return set;
    }

    public List<Object> exec() {
        List<Object> results = this.execRaw();
        return this.getRequiredConnectionFactory().getConvertPipelineAndTxResults() ? this.deserializeMixedResults(results, this.valueSerializer, this.hashKeySerializer, this.hashValueSerializer) : results;
    }

    public List<Object> exec(RedisSerializer<?> valueSerializer) {
        return this.deserializeMixedResults(this.execRaw(), valueSerializer, valueSerializer, valueSerializer);
    }

    protected List<Object> execRaw() {
        List<Object> raw = (List)this.execute(RedisTxCommands::exec);
        return raw == null ? Collections.emptyList() : raw;
    }

    public Boolean delete(K key) {
        byte[] rawKey = this.rawKey(key);
        Long result = (Long)this.execute((connection) -> {
            return connection.del(new byte[][]{rawKey});
        }, true);
        return result != null && result.intValue() == 1;
    }

    public Long delete(Collection<K> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return 0L;
        } else {
            byte[][] rawKeys = this.rawKeys(keys);
            return (Long)this.execute((connection) -> {
                return connection.del(rawKeys);
            }, true);
        }
    }

    public Boolean unlink(K key) {
        byte[] rawKey = this.rawKey(key);
        Long result = (Long)this.execute((connection) -> {
            return connection.unlink(new byte[][]{rawKey});
        }, true);
        return result != null && result.intValue() == 1;
    }

    public Long unlink(Collection<K> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return 0L;
        } else {
            byte[][] rawKeys = this.rawKeys(keys);
            return (Long)this.execute((connection) -> {
                return connection.unlink(rawKeys);
            }, true);
        }
    }

    public Boolean hasKey(K key) {
        byte[] rawKey = this.rawKey(key);
        return (Boolean)this.execute((connection) -> {
            return connection.exists(rawKey);
        }, true);
    }

    public Long countExistingKeys(Collection<K> keys) {
        Assert.notNull(keys, "Keys must not be null!");
        byte[][] rawKeys = this.rawKeys(keys);
        return (Long)this.execute((connection) -> {
            return connection.exists(rawKeys);
        }, true);
    }

    public Boolean expire(K key, long timeout, TimeUnit unit) {
        byte[] rawKey = this.rawKey(key);
        long rawTimeout = TimeoutUtils.toMillis(timeout, unit);
        return (Boolean)this.execute((connection) -> {
            try {
                return connection.pExpire(rawKey, rawTimeout);
            } catch (Exception var8) {
                return connection.expire(rawKey, TimeoutUtils.toSeconds(timeout, unit));
            }
        }, true);
    }

    public Boolean expireAt(K key, Date date) {
        byte[] rawKey = this.rawKey(key);
        return (Boolean)this.execute((connection) -> {
            try {
                return connection.pExpireAt(rawKey, date.getTime());
            } catch (Exception var4) {
                return connection.expireAt(rawKey, date.getTime() / 1000L);
            }
        }, true);
    }

    public void convertAndSend(String channel, Object message) {
        Assert.hasText(channel, "a non-empty channel is required");
        byte[] rawChannel = this.rawString(channel);
        byte[] rawMessage = this.rawValue(message);
        this.execute((connection) -> {
            connection.publish(rawChannel, rawMessage);
            return null;
        }, true);
    }

    public Long getExpire(K key) {
        byte[] rawKey = this.rawKey(key);
        return (Long)this.execute((connection) -> {
            return connection.ttl(rawKey);
        }, true);
    }

    public Long getExpire(K key, TimeUnit timeUnit) {
        byte[] rawKey = this.rawKey(key);
        return (Long)this.execute((connection) -> {
            try {
                return connection.pTtl(rawKey, timeUnit);
            } catch (Exception var4) {
                return connection.ttl(rawKey, timeUnit);
            }
        }, true);
    }

    public Set<K> keys(K pattern) {
        byte[] rawKey = this.rawKey(pattern);
        Set<byte[]> rawKeys = (Set)this.execute((connection) -> {
            return connection.keys(rawKey);
        }, true);
        return this.keySerializer != null ? SerializationUtils.deserialize(rawKeys, this.keySerializer) : (Set<K>) rawKeys;
    }

    public Boolean persist(K key) {
        byte[] rawKey = this.rawKey(key);
        return (Boolean)this.execute((connection) -> {
            return connection.persist(rawKey);
        }, true);
    }

    public Boolean move(K key, int dbIndex) {
        byte[] rawKey = this.rawKey(key);
        return (Boolean)this.execute((connection) -> {
            return connection.move(rawKey, dbIndex);
        }, true);
    }

    public K randomKey() {
        byte[] rawKey = (byte[])this.execute(RedisKeyCommands::randomKey, true);
        return this.deserializeKey(rawKey);
    }

    public void rename(K oldKey, K newKey) {
        byte[] rawOldKey = this.rawKey(oldKey);
        byte[] rawNewKey = this.rawKey(newKey);
        this.execute((connection) -> {
            connection.rename(rawOldKey, rawNewKey);
            return null;
        }, true);
    }

    public Boolean renameIfAbsent(K oldKey, K newKey) {
        byte[] rawOldKey = this.rawKey(oldKey);
        byte[] rawNewKey = this.rawKey(newKey);
        return (Boolean)this.execute((connection) -> {
            return connection.renameNX(rawOldKey, rawNewKey);
        }, true);
    }

    public DataType type(K key) {
        byte[] rawKey = this.rawKey(key);
        return (DataType)this.execute((connection) -> {
            return connection.type(rawKey);
        }, true);
    }

    public byte[] dump(K key) {
        byte[] rawKey = this.rawKey(key);
        return (byte[])this.execute((connection) -> {
            return connection.dump(rawKey);
        }, true);
    }

    public void restore(K key, byte[] value, long timeToLive, TimeUnit unit, boolean replace) {
        byte[] rawKey = this.rawKey(key);
        long rawTimeout = TimeoutUtils.toMillis(timeToLive, unit);
        this.execute((connection) -> {
            connection.restore(rawKey, rawTimeout, value, replace);
            return null;
        }, true);
    }

    public void multi() {
        this.execute((connection) -> {
            connection.multi();
            return null;
        }, true);
    }

    public void discard() {
        this.execute((connection) -> {
            connection.discard();
            return null;
        }, true);
    }

    public void watch(K key) {
        byte[] rawKey = this.rawKey(key);
        this.execute((connection) -> {
            connection.watch(new byte[][]{rawKey});
            return null;
        }, true);
    }

    public void watch(Collection<K> keys) {
        byte[][] rawKeys = this.rawKeys(keys);
        this.execute((connection) -> {
            connection.watch(rawKeys);
            return null;
        }, true);
    }

    public void unwatch() {
        this.execute((connection) -> {
            connection.unwatch();
            return null;
        }, true);
    }

    public List<V> sort(SortQuery<K> query) {
        return this.sort(query, this.valueSerializer);
    }

    public <T> List<T> sort(SortQuery<K> query, @Nullable RedisSerializer<T> resultSerializer) {
        byte[] rawKey = this.rawKey(query.getKey());
        SortParameters params = QueryUtils.convertQuery(query, this.stringSerializer);
        List<byte[]> vals = (List)this.execute((connection) -> {
            return connection.sort(rawKey, params);
        }, true);
        return SerializationUtils.deserialize(vals, resultSerializer);
    }

    public <T> List<T> sort(SortQuery<K> query, BulkMapper<T, V> bulkMapper) {
        return this.sort(query, bulkMapper, this.valueSerializer);
    }

    public <T, S> List<T> sort(SortQuery<K> query, BulkMapper<T, S> bulkMapper, @Nullable RedisSerializer<S> resultSerializer) {
        List<S> values = this.sort(query, resultSerializer);
        if (values != null && !values.isEmpty()) {
            int bulkSize = query.getGetPattern().size();
            List<T> result = new ArrayList(values.size() / bulkSize + 1);
            List<S> bulk = new ArrayList(bulkSize);
            Iterator var8 = values.iterator();

            while(var8.hasNext()) {
                S s = (S) var8.next();
                bulk.add(s);
                if (bulk.size() == bulkSize) {
                    result.add(bulkMapper.mapBulk(Collections.unmodifiableList(bulk)));
                    bulk = new ArrayList(bulkSize);
                }
            }

            return result;
        } else {
            return Collections.emptyList();
        }
    }

    public Long sort(SortQuery<K> query, K storeKey) {
        byte[] rawStoreKey = this.rawKey(storeKey);
        byte[] rawKey = this.rawKey(query.getKey());
        SortParameters params = QueryUtils.convertQuery(query, this.stringSerializer);
        return (Long)this.execute((connection) -> {
            return connection.sort(rawKey, params, rawStoreKey);
        }, true);
    }

    public void killClient(String host, int port) {
        this.execute((RedisCallback<Object>) (connection) -> {
            connection.killClient(host, port);
            return null;
        });
    }

    public List<RedisClientInfo> getClientList() {
        return (List)this.execute(RedisServerCommands::getClientList);
    }

    public void slaveOf(String host, int port) {
        this.execute((RedisCallback<Object>) (connection) -> {
            connection.slaveOf(host, port);
            return null;
        });
    }

    public void slaveOfNoOne() {
        this.execute((RedisCallback<Object>) (connection) -> {
            connection.slaveOfNoOne();
            return null;
        });
    }

    public ClusterOperations<K, V> opsForCluster() {
        return this.clusterOps;
    }

    public GeoOperations<K, V> opsForGeo() {
        return this.geoOps;
    }

    public BoundGeoOperations<K, V> boundGeoOps(K key) {
        return new DefaultBoundGeoOperations(key, this);
    }

    public <HK, HV> BoundHashOperations<K, HK, HV> boundHashOps(K key) {
        return new DefaultBoundHashOperations(key, this);
    }

    public <HK, HV> HashOperations<K, HK, HV> opsForHash() {
        return new DefaultHashOperations(this);
    }

    public HyperLogLogOperations<K, V> opsForHyperLogLog() {
        return this.hllOps;
    }

    public ListOperations<K, V> opsForList() {
        return this.listOps;
    }

    public BoundListOperations<K, V> boundListOps(K key) {
        return new DefaultBoundListOperations(key, this);
    }

    public BoundSetOperations<K, V> boundSetOps(K key) {
        return new DefaultBoundSetOperations(key, this);
    }

    public SetOperations<K, V> opsForSet() {
        return this.setOps;
    }

    public <HK, HV> StreamOperations<K, HK, HV> opsForStream() {
        return (StreamOperations<K, HK, HV>) this.streamOps;
    }

    public <HK, HV> StreamOperations<K, HK, HV> opsForStream(HashMapper<? super K, ? super HK, ? super HV> hashMapper) {
        return new DefaultStreamOperations(this, hashMapper);
    }

    public <HK, HV> BoundStreamOperations<K, HK, HV> boundStreamOps(K key) {
        return new DefaultBoundStreamOperations(key, this);
    }

    public BoundValueOperations<K, V> boundValueOps(K key) {
        return new DefaultBoundValueOperations(key, this);
    }

    public ValueOperations<K, V> opsForValue() {
        return this.valueOps;
    }

    public BoundZSetOperations<K, V> boundZSetOps(K key) {
        return new DefaultBoundZSetOperations(key, this);
    }

    public ZSetOperations<K, V> opsForZSet() {
        return this.zSetOps;
    }

    public void setEnableTransactionSupport(boolean enableTransactionSupport) {
        this.enableTransactionSupport = enableTransactionSupport;
    }

    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
}
