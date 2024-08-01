package cmu.edu.pasta.fray.junit

import cmu.edu.pasta.fray.junit.annotations.Analyze
import java.lang.reflect.Method
import kotlin.reflect.KClass
import org.junit.platform.commons.support.AnnotationSupport.findAnnotation
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor

class MethodTestDescriptor(val testMethod: Method, val parent: ClassTestDescriptor) :
    AbstractTestDescriptor(
        parent.uniqueId.append("method", testMethod.name),
        "Fray",
        MethodSource.from(testMethod),
    ) {
  init {
    setParent(parent)
  }

  fun getExpected(): KClass<*> {
    val analyzeConfig = findAnnotation(testMethod, Analyze::class.java).get()
    return analyzeConfig.expected
  }

  override fun getType(): TestDescriptor.Type {
    return TestDescriptor.Type.TEST
  }
}
