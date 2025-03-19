package org.anonlab.fray.junit.syncurity.conditions

import java.util.function.Supplier
import org.anonlab.fray.runtime.SyncurityCondition
import org.hamcrest.Matcher

class CallableHamcrestCondition<T>(val supplier: Supplier<T>, val matcher: Matcher<T>) :
    SyncurityCondition() {
  override fun satisfied(): Boolean {
    try {
      val value = supplier.get()
      return matcher.matches(value)
    } catch (e: Throwable) {
      return false
    }
  }
}
