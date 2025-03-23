package org.pastalab.fray.test.success.vthread;

public class VirtualThreadSimple {
    public static void main(String[] args) throws InterruptedException {
        Thread thread = Thread.ofVirtual().start(() -> System.out.println("Hello"));
        thread.join();
    }
}
