package org.pastalab.fray.test.core.success.varhandle;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class ArrayVarHandleVolatileOpsNoInfLoop {
  private static final Object[] data = new Object[32];
  private static final VarHandle DATA = MethodHandles.arrayElementVarHandle(Object[].class);

  public static void main(String[] args) throws InterruptedException {
    DATA.setVolatile(data, 0, 0);
    var t1 =
        new Thread(
            () -> {
              DATA.setVolatile(data, 0, 1);
            });
    var t2 =
        new Thread(
            () -> {
              while (DATA.getVolatile(data, 0).equals(0)) {
                // Do nothing. getVolatile should trigger a reschedule.
              }
            });
    t1.start();
    t2.start();
    t1.join();
    t2.join();
  }
}
