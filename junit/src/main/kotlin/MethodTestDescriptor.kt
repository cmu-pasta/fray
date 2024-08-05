package org.pastalab.fray.junit

import java.lang.reflect.Method
import org.junit.platform.commons.support.AnnotationSupport.findAnnotation
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.pastalab.fray.core.scheduler.Scheduler
import org.pastalab.fray.junit.annotations.Analyze

class MethodTestDescriptor(val testMethod: Method, val parent: ClassTestDescriptor) :
    AbstractTestDescriptor(
        parent.uniqueId.append("method", testMethod.name),
        testMethod.name,
        MethodSource.from(testMethod),
    ) {
  init {
    setParent(parent)
  }

  val analyzeConfig = findAnnotation(testMethod, Analyze::class.java).get()

  fun getScheduler(): Scheduler {
    return analyzeConfig.scheduler.java.getDeclaredConstructor().newInstance()
  }

  override fun getType(): TestDescriptor.Type {
    return TestDescriptor.Type.TEST
  }
}
