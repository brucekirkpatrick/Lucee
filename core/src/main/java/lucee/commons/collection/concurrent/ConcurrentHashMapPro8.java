package lucee.commons.collection.concurrent;

import lucee.commons.collection.AbstractMapPro;
import lucee.commons.collection.MapPro;
import lucee.runtime.exp.PageException;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ConcurrentHashMapPro8<K, V> extends AbstractMapPro<K, V> implements Serializable {
    public static final int DEFAULT_INITIAL_CAPACITY = 32;
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;
    public static final int DEFAULT_CONCURRENCY_LEVEL = 16;
    ConcurrentHashMap<K, V> map;
    public ConcurrentHashMapPro8(int initialCapacity, float loadFactor, int concurrencyLevel) {
        map=new ConcurrentHashMap<K, V>(initialCapacity, loadFactor, concurrencyLevel);
    }
    public ConcurrentHashMapPro8(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL);
    }
    public ConcurrentHashMapPro8(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
    }
    public ConcurrentHashMapPro8() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
    }
    public ConcurrentHashMapPro8(Map<? extends K, ? extends V> m) {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1, DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
        putAll(m);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        if(value==null) return null;
        if(key==null) return value;
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return map.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        map.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        map.replaceAll(function);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return map.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return map.remove(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return map.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        return map.replace(key, value);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return map.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return map.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return map.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return map.merge(key, value, remappingFunction);
    }

    @Override
    public V g(K key) throws PageException {
        return map.get(key);
    }

    @Override
    public V g(K key, V defaultValue) {
        return map.getOrDefault(key, defaultValue);
    }

    @Override
    public V r(K key) throws PageException {
        return map.get(key);
    }

    @Override
    public V r(K key, V defaultValue) {
        return map.getOrDefault(key, defaultValue);
    }
}
