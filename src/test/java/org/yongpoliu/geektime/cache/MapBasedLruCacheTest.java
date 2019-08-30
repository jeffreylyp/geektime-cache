package org.yongpoliu.geektime.cache;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MapBasedLruCacheTest {

	private static MapBasedLruCache<String, Long> cache = new MapBasedLruCache<>(5, Long::parseLong);

	@BeforeClass
	public static void init() {
		cache.init();
	}

	@Test
	public void keyListShouldEqual() throws InterruptedException {
		for (int i = 0; i < 10; i++) {
			cache.get(i + "");
		}

		// 因为有异步操作，所以做个临时的等待
		TimeUnit.MILLISECONDS.sleep(10);

		String[] keyArray = cache.dump().toArray(new String[0]);

		String[] expectedArray = {"9", "8", "7", "6", "5"};
		assertArrayEquals(expectedArray, keyArray);
	}

	@AfterClass
	public static void destroy() {
		cache.destroy();
	}
}
