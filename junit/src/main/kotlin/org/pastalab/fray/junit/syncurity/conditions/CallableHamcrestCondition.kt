package org.pastalab.fray.junit.syncurity.conditions

import java.util.function.Supplier
import org.hamcrest.Matcher
import org.pastalab.fray.runtime.SyncurityCondition

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
