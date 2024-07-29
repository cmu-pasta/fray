package cmu.edu.pasta.fray.junit

import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import java.lang.reflect.Method

class MethodTestDescriptor(val testMethod: Method, parent: ClassTestDescriptor) :
    AbstractTestDescriptor(
        parent.uniqueId.append("method", testMethod.name),
        "Fray",
        MethodSource.from(testMethod),
    ) {
  init {
    setParent(parent)
  }

  override fun getType(): TestDescriptor.Type {
    return TestDescriptor.Type.TEST
  }
}
