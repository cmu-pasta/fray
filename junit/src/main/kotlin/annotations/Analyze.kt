package org.pastalab.fray.junit.annotations

import kotlin.reflect.KClass
import org.junit.platform.commons.annotation.Testable
import org.pastalab.fray.core.scheduler.POSScheduler
import org.pastalab.fray.core.scheduler.Scheduler

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Testable
annotation class Analyze(
    val expectedException: KClass<*> = Any::class,
    val expectedLog: String = "",
    val scheduler: KClass<out Scheduler> = POSScheduler::class,
    val iteration: Int = 1,
)
