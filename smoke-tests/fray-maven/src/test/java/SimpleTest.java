import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.FrayTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(FrayTestExtension.class)
public class SimpleTest {
    @FrayTest
    public void concurrencyTest() throws InterruptedException {

        Thread t = new Thread(() -> {
            assertTrue(true);
        });
        t.start();
        t.join();
    }

}
