package example;

import java.util.*;
import java.util.concurrent.Semaphore;

public class SynchronizedMap<K, V> extends AbstractMap<K, V> {
    private final Object MUTEX = "SYNCHRONIZED_MAP_MUTEX";
    private Map<K,V> m = null;
    private Set<K> keySet = null;
    private Set<Map.Entry<K,V>> entrySet = null;
    private Collection<V> values = null;
    private Semaphore trueLock = new Semaphore(1);

    public SynchronizedMap(Map<K,V> m) {
        this.m = m;
    }


    @Override
    public int size() {
        synchronized (MUTEX) {
            return m.size();
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (MUTEX) {
            return m.isEmpty();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        synchronized (MUTEX) {
            return m.containsKey(key);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        synchronized (MUTEX) {
            return m.containsValue(value);
        }
    }

    @Override
    public V get(Object key) {
        synchronized (MUTEX) {
            return m.get(key);
        }
    }

    @Override
    public V put(K key, V value) {
        synchronized (MUTEX) {
            return m.put(key, value);
        }
    }

    @Override
    public V remove(Object key) {
        synchronized (MUTEX) {
            return m.remove(key);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        synchronized (MUTEX) {
            this.m.putAll(m);
        }
    }

    @Override
    public void clear() {
        synchronized (MUTEX) {
            m.clear();
        }
    }

    @Override
    public Set<K> keySet() {
        synchronized (MUTEX) {
            if (keySet==null)
                keySet = new SynchronizedSet<>(m.keySet(), this);
            return keySet;
        }
    }

    @Override
    public Collection<V> values() {
        synchronized (MUTEX) {
            if (values==null)
                values = new SynchronizedCollection<>(m.values(), this);
            return values;
        }
    }

    @Override
    public Set<Map.Entry<K,V>> entrySet() {
        synchronized (MUTEX) {
            if (entrySet==null)
                entrySet = new SynchronizedSet<>(m.entrySet(), this);
            return entrySet;
        }
    }

    @Override
    public String toString() {
        return m.toString();
    }

    @Override
    public boolean equals(Object o) {
        synchronized (MUTEX) {
            return m.equals(o);
        }
    }

    @Override
    public int hashCode() {
        synchronized (MUTEX) {
            return m.hashCode();
        }
    }
}
