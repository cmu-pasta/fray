package example;

import java.util.List;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class MultithreadedHashMap {
    // uses ConcurrentMap
    public static ConcurrentHashMap<Integer, Integer> customLogic (List<Integer> keys, List<Integer> values) throws InterruptedException {
        ConcurrentHashMap<Integer, Integer> concurrentMap = new ConcurrentHashMap<Integer, Integer>();
        int mid = keys.size()/2;
        Thread thread1 = new Thread(() -> {
            try {
              addToConcurrentMap(concurrentMap, keys, values, 0, mid);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
              addToConcurrentMap(concurrentMap, keys, values, mid+1, keys.size()-1);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        return concurrentMap;
    }

    private static void addToConcurrentMap(ConcurrentHashMap<Integer, Integer> concurrentMap, List<Integer> keys, List<Integer> values, int start, int end) {
        for (int i = start; i <= end; i++) {
            if (concurrentMap.contains(keys.get(i))) {
                concurrentMap.put(keys.get(i), 0);
            }
            else {
                concurrentMap.put(keys.get(i), values.get(i));
            }
        }
    }

    // uses SynchronizedMap
    public static SynchronizedMap<Integer, Integer> customLogic2 (List<Integer> keys, List<Integer> values) throws InterruptedException {
          SynchronizedMap<Integer, Integer> concurrentMap = new SynchronizedMap<>(new HashMap<>());
          int mid = keys.size()/2;

           Thread thread1 = new Thread(() -> {
               try {
                 addToConcurrentMap2(concurrentMap, keys, values, 0, mid);
               } catch (Exception e) {
                 throw new RuntimeException(e);
               }
           });

           Thread thread2 = new Thread(() -> {
               try {
                 addToConcurrentMap2(concurrentMap, keys, values, mid+1, keys.size()-1);
               } catch (Exception e) {
                 throw new RuntimeException(e);
               }
           });

           thread1.start();
           thread2.start();

           thread1.join();
           thread2.join();

          return concurrentMap;
    }

    private static void addToConcurrentMap2(SynchronizedMap<Integer, Integer> concurrentMap, List<Integer> keys, List<Integer> values, int start, int end) {
        for (int i = start; i <= end; i++) {
            if (concurrentMap.containsKey(keys.get(i))) {
                concurrentMap.put(keys.get(i), 0);
            }
            else {
                concurrentMap.put(keys.get(i), values.get(i));
            }
        }
    }
}
