import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.FrayTestExtension;
import org.pastalab.fray.junit.annotations.ConcurrencyTest;

@ExtendWith(FrayTestExtension.class)
public class DummyTest {

    public DummyTest() {
        System.out.println("Constructor");
    }

    @Test
    public void test() {
        System.out.println("1");
    }

    @ConcurrencyTest(iterations = 100)
    public void test2() {
        System.out.println("2");
    }

    @ConcurrencyTest(
            iterations = 100
    )
    public void test3() {
        assert(false);
    }

}
