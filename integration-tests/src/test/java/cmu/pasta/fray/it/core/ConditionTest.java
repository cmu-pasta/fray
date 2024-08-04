package cmu.pasta.fray.it.core;

import cmu.edu.pasta.fray.junit.annotations.Analyze;
import cmu.edu.pasta.fray.junit.annotations.FrayTest;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static cmu.pasta.fray.it.Utils.log;


@FrayTest
public class ConditionTest {


    static int num;
    static long total;
    static boolean flag;

    @Analyze
    public void conditionAwait() {

        Lock m = new ReentrantLock();
        int N = 3;
        Condition empty = m.newCondition();
        Condition full = m.newCondition();

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

                } catch (InterruptedException ignored) {
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
    }
}
