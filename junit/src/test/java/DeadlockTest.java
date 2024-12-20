import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.FrayTestExtension;
import org.pastalab.fray.junit.annotations.ConcurrencyTest;

@ExtendWith(FrayTestExtension.class)
public class DeadlockTest {

    @ConcurrencyTest(iterations = 100)
    public void deadlockInMainThread() throws InterruptedException {
        Object o = new Object();
        synchronized (o) {
            o.wait();
        }
    }

    @ConcurrencyTest(iterations = 100)
    public void deadlockInChildThread() throws InterruptedException {
        Thread t = new Thread(() -> {
            Object o = new Object();
            synchronized (o) {
                try {
                    o.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t.start();
    }
}
