package example;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ArithmeticProgBad {
    private static final int N = 3;

    private static int num;
    private static long total;
    private static boolean flag;

    private static Lock m = new ReentrantLock();
    private static Condition empty = m.newCondition();
    private static Condition full = m.newCondition();

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

                    System.out.println("produce ...." + i);
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
                    System.out.println("total ...." + total);
                    num--;
                    empty.signal();
                    System.out.println("consume ...." + j);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    m.unlock();
                }

                j++;
            }
            total = total + j;
            System.out.println("total ...." + total);
            flag = true;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        num = 0;
        total = 0;

        Thread t1 = new Thread(new Thread1());
        Thread t2 = new Thread(new Thread2());

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        if (flag)
            assert total != ((N * (N + 1)) / 2);
    }
}
