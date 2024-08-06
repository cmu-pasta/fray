package org.pastalab.fray.junit

import java.util.function.Predicate
import org.junit.platform.commons.support.AnnotationSupport.isAnnotated
import org.junit.platform.commons.support.ReflectionSupport.findAllClassesInClasspathRoot
import org.junit.platform.commons.support.ReflectionSupport.findAllClassesInPackage
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestEngine
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.ClasspathRootSelector
import org.junit.platform.engine.discovery.MethodSelector
import org.junit.platform.engine.discovery.PackageSelector
import org.pastalab.fray.junit.annotations.FrayTest

class FrayTestEngine : TestEngine {
  override fun getId(): String {
    return "fray"
  }

  companion object {
    private val IS_FRAY_TEST_CONTAINER =
        Predicate<Class<*>> { classCandidate ->
          isAnnotated(
              classCandidate,
              FrayTest::class.java,
          )
        }
  }

  override fun discover(request: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
    val engineDescriptor = FrayEngineDescriptor(uniqueId)
    request.getSelectorsByType(ClasspathRootSelector::class.java).forEach { selector ->
      findAllClassesInClasspathRoot(selector.classpathRoot, IS_FRAY_TEST_CONTAINER) { true }
          .map { clazz -> ClassTestDescriptor(clazz, engineDescriptor) }
          .forEach { engineDescriptor.addChild(it) }
    }
    request.getSelectorsByType(PackageSelector::class.java).forEach { selector ->
      findAllClassesInPackage(
              selector.packageName,
              IS_FRAY_TEST_CONTAINER,
          ) {
            true
          }
          .stream()
          .map { clazz ->
            ClassTestDescriptor(
                clazz,
                engineDescriptor,
            )
          }
          .forEach(engineDescriptor::addChild)
    }
    request.getSelectorsByType(ClassSelector::class.java).forEach { selector ->
      val clazz = selector.javaClass
      if (IS_FRAY_TEST_CONTAINER.test(clazz)) {
        engineDescriptor.addChild(ClassTestDescriptor(clazz, engineDescriptor))
      }
    }
    return engineDescriptor
  }

  override fun execute(request: ExecutionRequest) {
    FrayTestExecutor().execute(request, request.rootTestDescriptor)
  }
}
