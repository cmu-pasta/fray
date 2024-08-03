package cmu.edu.pasta.fray.junit.annotations

import cmu.pasta.fray.core.scheduler.POSScheduler
import cmu.pasta.fray.core.scheduler.Scheduler
import kotlin.reflect.KClass
import org.junit.platform.commons.annotation.Testable

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Testable
annotation class Analyze(
    val expectedException: KClass<*> = Any::class,
    val expectedLog: String = "",
    val scheduler: KClass<out Scheduler> = POSScheduler::class,
    val iteration: Int = 1,
)
