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
                log("Acquired lock.");

                try {
                    while (num > 0) {
                        log("Beginning await.");
                        empty.await();
                        log("Returned from await.");
                    }
                    num++;
                    full.signal();
                    log("Sent signal.");

                    System.out.println("produce ...." + i);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    m.unlock();
                    log("Released lock.");
                }


                i++;
          }
        });

        Thread consumer = new Thread (() -> {
            int j = 0;
            while (j < N) {
                m.lock();
                log("Acquired lock.");
                try {
                    while (num == 0) {
                        log("Beginning await.");
                        full.await();
                        log("Returned from await.");
                    }
                    total = total + j;
                    System.out.println("total ...." + total);
                    num--;
                    empty.signal();
                    log("Sent signal.");
                    System.out.println("consume ...." + j);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    m.unlock();
                    log("Released lock.");
                }

                j++;
            }
            total = total + j;
            System.out.println("total ...." + total);
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