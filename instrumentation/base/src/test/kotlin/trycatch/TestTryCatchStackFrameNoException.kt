package trycatch

import kotlin.test.assertNotEquals
import kotlin.test.assertSame
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
    val newBuffer = appTransformer.transform(null, "", null, null, ba)
    assertNotEquals(newBuffer, ba)
  }

  @Test
  fun configuredPrefixesBypassTransformation() {
    val property = ApplicationCodeTransformer.EXCLUDE_PREFIXES_PROPERTY
    val original = System.getProperty(property)
    val ba =
        this.javaClass
            .getResource("/classfiles/trycatch/org.springframework.core.\$Proxy11.class")
            .openStream()
            .readBytes()

    try {
      System.setProperty(property, " unused.prefix , org.springframework.core ")
      val transformer = ApplicationCodeTransformer()
      val transformed =
          transformer.transform(null, "org/springframework/core/\$Proxy11", null, null, ba)

      assertSame(ba, transformed)
    } finally {
      if (original == null) {
        System.clearProperty(property)
      } else {
        System.setProperty(property, original)
      }
    }
  }
}
