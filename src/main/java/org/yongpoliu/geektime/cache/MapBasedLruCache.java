package org.yongpoliu.geektime.cache;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;

public class MapBasedLruCache<K, V> extends LruCache<K, V> {

	private final ConcurrentMap<K, V> m = new ConcurrentHashMap<>();

	private final LinkedList<K> keyList = new LinkedList<>();

	private final BlockingQueue<Runnable> queue = new LinkedBlockingDeque<>();

	private final Worker worker = new Worker();

	// 减少 dump 对加锁的影响
	private volatile boolean isDumping = false;

	public MapBasedLruCache(int capacity, Storage<K, V> lowSpeedStorage) {
		super(capacity, lowSpeedStorage);
	}

	public void init() {
		this.worker.start();
	}

	public void destroy() {
		this.worker.interrupt();
	}

	// for testing
	List<K> dump() {

		this.isDumping = true;
		try {
			synchronized (keyList) {
				return new LinkedList<>(keyList);
			}
		} finally {
			this.isDumping = false;
		}
	}

	@Override
	public V get(K key) {

		final boolean[] cacheMissing = new boolean[]{false};
		V v = m.computeIfAbsent(key, k -> {
			cacheMissing[0] = true;
			return lowSpeedStorage.get(k);
		});

		if (!cacheMissing[0]) {
			queue.offer(() -> moveKeyFirst(key));
		} else {
			queue.offer(() -> removeLastIfFull(key));
		}
		return v;
	}

	private void moveKeyFirst(K key) {
		keyList.remove(key);
		keyList.addFirst(key);
	}

	private void removeLastIfFull(K key) {
		if (keyList.size() >= capacity) {
			K last = keyList.getLast();

			m.remove(last);

			keyList.removeLast();
		}
		keyList.addFirst(key);
	}

	private class Worker extends Thread {

		Worker() {
			super("lru-cache-thread");
		}

		@Override
		public void run() {
			while (true) {
				try {
					Runnable runnable = queue.take();
					if (isDumping) {
						synchronized (keyList) {
							runnable.run();
						}
					} else {
						runnable.run();
					}
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}
}
