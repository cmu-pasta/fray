import org.junit.jupiter.api.Test;
import org.pastalab.fray.junit.annotations.ConcurrencyTest;

public class DummyTest {
    @Test
    public void test() {
        System.out.println("1");
    }

    @ConcurrencyTest
    public void test2() {
        System.out.println("2");
    }

    @ConcurrencyTest(
            expectedException = AssertionError.class
    )
    public void test3() {
        assert(false);
    }

}
