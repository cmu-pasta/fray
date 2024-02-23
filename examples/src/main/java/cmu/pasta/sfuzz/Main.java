package cmu.pasta.sfuzz;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Hello world!");
            }
        });
        t.start();
        t.join();
    }
}