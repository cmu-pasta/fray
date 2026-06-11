import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.FrayTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(FrayTestExtension.class)
public class SimpleTest {
    @FrayTest
    public void simpleConcurrencyTest() throws InterruptedException {
        int a = 1;
        int b = 2;
        int c = a + b;
        Thread t1 = new Thread(() -> {
            int d = c * 2;
            assertEquals(6, d);
        });

        t1.start();
        t1.join();
    }
}
