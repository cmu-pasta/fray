package org.pastalab.fray.junit

import java.lang.reflect.Method
import org.junit.platform.commons.util.AnnotationUtils
import org.junit.platform.commons.util.ReflectionUtils
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource
import org.pastalab.fray.junit.annotations.ConcurrencyTest

class ClassTestDescriptor(val testClass: Class<*>, parent: TestDescriptor) :
    AbstractTestDescriptor(
        parent.uniqueId.append("class", testClass.name),
        testClass.simpleName,
        ClassSource.from(testClass),
    ) {
  init {
    setParent(parent)
    addAllChildren()
  }

  private fun addAllChildren() {
    val isTestMethod = { field: Method ->
      AnnotationUtils.isAnnotated(
          field,
          ConcurrencyTest::class.java,
      )
    }

    ReflectionUtils.findMethods(
            testClass, isTestMethod, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
        .stream()
        .map { method ->
          MethodTestDescriptor(
              method,
              this,
          )
        }
        .forEach { child ->
          this.addChild(
              child,
          )
        }
  }

  override fun getType(): TestDescriptor.Type {
    return TestDescriptor.Type.CONTAINER
  }
}
