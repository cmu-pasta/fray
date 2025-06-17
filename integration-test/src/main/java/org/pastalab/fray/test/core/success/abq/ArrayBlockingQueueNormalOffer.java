package org.pastalab.fray.test.core.success.abq;

import java.util.concurrent.ArrayBlockingQueue;

public class ArrayBlockingQueueNormalOffer {
    public static void main(String[] args) {
        ArrayBlockingQueue<Integer> abq = new ArrayBlockingQueue<>(10);

        abq.offer(10);
    }
}
