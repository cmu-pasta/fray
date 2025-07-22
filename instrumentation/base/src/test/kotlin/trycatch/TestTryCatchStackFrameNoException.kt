package trycatch

import kotlin.test.assertNotEquals
import org.junit.jupiter.api.Test
import org.pastalab.fray.instrumentation.base.ApplicationCodeTransformer

class TestTryCatchStackFrameNoException {

  @Test
  fun testTryCatchStackFrameNoException() {
    val ba =
        this.javaClass
            .getResource("/classfiles/trycatch/org.springframework.core.\$Proxy11.class")
            .openStream()
            .readBytes()
    val appTransformer = ApplicationCodeTransformer()
    var newBuffer = appTransformer.transform(null, "", null, null, ba)
    assertNotEquals(newBuffer, ba)
  }
}
