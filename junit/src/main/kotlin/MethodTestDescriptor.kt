package org.pastalab.fray.junit

import java.io.File
import java.lang.reflect.Method
import kotlinx.serialization.json.Json
import org.junit.platform.commons.support.AnnotationSupport.findAnnotation
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.MethodSource
import org.pastalab.fray.core.randomness.ControlledRandom
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

  fun getScheduler(): Pair<Scheduler, ControlledRandom> {
    if (!analyzeConfig.replay.isEmpty()) {
      val path = analyzeConfig.replay
      val randomPath = "${path}/random.json"
      val schedulerPath = "${path}/schedule.json"
      if (path.startsWith("classpath")) {
        val randomnessProvider = Json.decodeFromString<ControlledRandom>(javaClass.getResource
        (randomPath.split(":")[1]).readText())
        val scheduler = Json.decodeFromString<Scheduler>(javaClass.getResource(schedulerPath
            .split(":")[1])
            .readText())
        return Pair(scheduler, randomnessProvider)
      } else {
        val randomnessProvider = Json.decodeFromString<ControlledRandom>(File(randomPath).readText())
        val scheduler = Json.decodeFromString<Scheduler>(File(schedulerPath).readText())
        return Pair(scheduler, randomnessProvider)
      }
    }
    return Pair(
        analyzeConfig.scheduler.java.getDeclaredConstructor().newInstance(), ControlledRandom())
  }

  override fun getType(): TestDescriptor.Type {
    return TestDescriptor.Type.TEST
  }
}
