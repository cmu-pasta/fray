package example;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class UnsafeCompareAndSwapLock {
    private volatile int state = 0;
    private static final Unsafe unsafe;
    private static final long stateOffset;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            stateOffset = unsafe.objectFieldOffset(UnsafeCompareAndSwapLock.class.getDeclaredField("state"));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public void lock() {
        while (!unsafe.compareAndSwapInt(this, stateOffset, 0, 1));
    }

    public void unlock() {
        state = 0;
    }
}