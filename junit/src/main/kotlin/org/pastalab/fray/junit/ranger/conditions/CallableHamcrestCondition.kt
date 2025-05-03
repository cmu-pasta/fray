package org.pastalab.fray.junit.ranger.conditions

import java.util.function.Supplier
import org.hamcrest.Matcher
import org.pastalab.fray.runtime.RangerCondition

class CallableHamcrestCondition<T>(val supplier: Supplier<T>, val matcher: Matcher<T>) :
    RangerCondition() {
  override fun satisfied(): Boolean {
    try {
      val value = supplier.get()
      return matcher.matches(value)
    } catch (e: Throwable) {
      return false
    }
  }
}
