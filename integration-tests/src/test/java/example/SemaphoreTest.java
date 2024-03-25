package example;

// java program to demonstrate
// use of semaphores Locks
import jdk.jshell.execution.Util;

import java.util.concurrent.*;

//A shared resource/class.
class Shared
{
    static int count = 0;
}

class MyThread extends Thread
{
    Semaphore sem;
    String threadName;
    public MyThread(Semaphore sem, String threadName)
    {
        super(threadName);
        this.sem = sem;
        this.threadName = threadName;
    }

    @Override
    public void run() {

        // run by thread A
        if(this.getName().equals("A"))
        {
            Utils.log("Starting " + threadName);
            try
            {
                // First, get a permit.
                Utils.log(threadName + " is waiting for a permit.");

                // acquiring the lock
                sem.acquire();

                Utils.log(threadName + " gets a permit.");

                // Now, accessing the shared resource.
                // other waiting threads will wait, until this
                // thread release the lock
                for(int i=0; i < 5; i++)
                {
                    Shared.count++;
                    Utils.log(threadName + ": " + Shared.count);

                    // Now, allowing a context switch -- if possible.
                    // for thread B to execute
                    Thread.sleep(10);
                }
            } catch (InterruptedException exc) {
                Utils.log(exc.toString());
            }

            // Release the permit.
            Utils.log(threadName + " releases the permit.");
            sem.release();
        }

        // run by thread B
        else
        {
            Utils.log("Starting " + threadName);
            try
            {
                // First, get a permit.
                Utils.log(threadName + " is waiting for a permit.");

                // acquiring the lock
                sem.acquire();

                Utils.log(threadName + " gets a permit.");

                // Now, accessing the shared resource.
                // other waiting threads will wait, until this
                // thread release the lock
                for(int i=0; i < 5; i++)
                {
                    Shared.count--;
                    Utils.log(threadName + ": " + Shared.count);

                    // Now, allowing a context switch -- if possible.
                    // for thread A to execute
                    Thread.sleep(10);
                }
            } catch (InterruptedException exc) {
                Utils.log(exc.toString());
            }
            // Release the permit.
            Utils.log(threadName + " releases the permit.");
            sem.release();
        }
    }
}

// Driver class
public class SemaphoreTest
{
    public static void testSemaphore() throws InterruptedException
    {
        // creating a Semaphore object
        // with number of permits 1
        Semaphore sem = new Semaphore(1);

        // creating two threads with name A and B
        // Note that thread A will increment the count
        // and thread B will decrement the count
        MyThread mt1 = new MyThread(sem, "A");
        MyThread mt2 = new MyThread(sem, "B");

        // stating threads A and B
        mt1.start();
        mt2.start();

        // waiting for threads A and B
        mt1.join();
        mt2.join();

        // count will always remain 0 after
        // both threads will complete their execution
        Utils.log("count: " + Shared.count);
    }
}
