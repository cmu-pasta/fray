package org.pastalab.fray.junit.internal.atomic;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.FrayTest;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@ExtendWith(FrayTestExtension.class)
public class AtomicTests {

  @FrayTest
  public void testUpdateAndGetWithLambdaAccessingPrimitives() {

    ReentrantLock lock = new ReentrantLock();
    AtomicInteger atomicInteger = new AtomicInteger(0);

    new Thread(() -> {
      atomicInteger.updateAndGet(x -> {
        lock.lock();
        int newValue = x + 1;
        lock.unlock();
        return newValue;
      });
    }).start();
    lock.lock();
    Thread.yield();
    lock.unlock();
  }
}
