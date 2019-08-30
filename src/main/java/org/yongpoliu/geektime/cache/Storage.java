package org.yongpoliu.geektime.cache;

public interface Storage<K, V> {

	V get(K key);
}
