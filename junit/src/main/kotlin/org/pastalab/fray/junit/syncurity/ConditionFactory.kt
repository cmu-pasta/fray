package org.pastalab.fray.junit.syncurity

import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier
import org.hamcrest.Matcher
import org.pastalab.fray.junit.syncurity.conditions.CallableHamcrestCondition

fun await(): ConditionFactory {
  return ConditionFactory()
}

class ConditionFactory() {
  fun <T> until(supplier: Supplier<T>, matcher: Matcher<T>) {
    CallableHamcrestCondition(supplier, matcher).await()
  }

  fun untilAtomic(atomic: AtomicInteger, matcher: Matcher<Int>) {
    until({ atomic.get() }, matcher)
  }
}
