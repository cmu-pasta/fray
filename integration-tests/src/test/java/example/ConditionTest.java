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

    private void log (String format, Object... args) {
        GlobalContext.INSTANCE.log(format, args);
    }

    public static void main(String[] args) {
        ConditionTest t = new ConditionTest();
        t.testConditionAwait();
    }

    public void testConditionAwait() {

        Thread producer = new Thread (() -> {
            int i = 0;
            while (i < N) {
                m.lock();
                log("Acquired lock.\n");

                try {
                    while (num > 0) {
                        log("Beginning empty await.\n");
                        empty.await();
                        log("Returned from empty await.\n");
                    }
                    num++;
                    full.signal();
                    log("Sent full signal.\n");

                } catch (InterruptedException e) {
                } finally {
                    m.unlock();
                    log("Released lock.\n");
                }


                i++;
          }
        });

        Thread consumer = new Thread (() -> {
            int j = 0;
            while (j < N) {
                m.lock();
                log("Acquired lock.\n");
                try {
                    while (num == 0) {
                        log("Beginning full await.\n");
                        full.await();
                        log("Returned from full await.\n");
                    }
                    total = total + j;
                    num--;
                    empty.signal();
                    log("Sent empty signal.\n");
                } catch (InterruptedException e) {
                } finally {
                    m.unlock();
                    log("Released lock.\n");
                }

                j++;
            }
            total = total + j;
            flag = true;
        });

        producer.start();
        consumer.start();

        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assert total == ((N * (N + 1)) / 2);
    }
}