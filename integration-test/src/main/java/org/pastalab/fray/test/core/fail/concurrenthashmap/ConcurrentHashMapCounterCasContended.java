package org.pastalab.fray.test.core.fail.concurrenthashmap;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code ConcurrentHashMap} only creates its counter cells after a CAS on {@code baseCount} fails.
 * That requires another thread to run between the read of {@code baseCount} and the CAS, which
 * {@code ConcurrentHashMap} performs through {@code jdk.internal.misc.Unsafe} directly.
 */
public class ConcurrentHashMapCounterCasContended {
  public static void main(String[] args) throws Exception {
    ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();
    map.put(0, 0);
    Thread t1 = new Thread(() -> map.put(1, 1));
    Thread t2 = new Thread(() -> map.put(2, 2));
    t1.start();
    t2.start();
    t1.join();
    t2.join();
    Field counterCells = ConcurrentHashMap.class.getDeclaredField("counterCells");
    counterCells.setAccessible(true);
    if (counterCells.get(map) != null) {
      throw new AssertionError("A CAS on baseCount failed and counter cells were created");
    }
  }
}
