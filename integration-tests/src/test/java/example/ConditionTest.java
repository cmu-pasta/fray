package example;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import cmu.pasta.sfuzz.core.GlobalContext;

public class ConditionTest {

    private static int num;
    private static long total;
    private static boolean flag;

    private static Lock m = new ReentrantLock();
    private static int N = 3;
    private static Condition empty = m.newCondition();
    private static Condition full = m.newCondition();

    private static void log(String format, Object... args) {
        GlobalContext.INSTANCE.log(format, args);
    }

    private static class Thread1 implements Runnable {
        public void run() {
            int i = 0;
            while (i < N) {
                m.lock();
                try {
                    while (num > 0) {
                        empty.await();
                    }
                    num++;
                    full.signal();

                    log("produce ...." + i);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    m.unlock();
                }


                i++;
            }
        }
    }

    private static class Thread2 implements Runnable {
        public void run() {
            int j = 0;
            while (j < N) {
                m.lock();
                try {
                    while (num == 0) {
                        full.await();
                    }
                    total = total + j;
                    log("total ...." + total);
                    num--;
                    empty.signal();
                    log("consume ...." + j);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    m.unlock();
                }

                j++;
            }
            total = total + j;
            log("total ...." + total);
            flag = true;
        }
    }

    public static void main(String[] args) {
        num = 0;
        total = 0;

        Thread t1 = new Thread(new Thread1());
        Thread t2 = new Thread(new Thread2());

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
        }

        if (flag)
            assert total == ((N * (N + 1)) / 2);
    }

}