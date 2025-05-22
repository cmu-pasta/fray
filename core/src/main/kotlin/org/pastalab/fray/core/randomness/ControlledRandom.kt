package org.pastalab.fray.core.randomness

import com.antithesis.sdk.Random
import java.util.*
import kotlin.math.absoluteValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class ControlledRandom(
    val integers: MutableList<Int> = mutableListOf(),
    val doubles: MutableList<Double> = mutableListOf(),
    @Transient private val random: Random = Random()
) {

  @Transient private var integerIndex = 0

  @Transient private var doubleIndex = 0

  fun nextInt(): Int {
    if (integerIndex >= integers.size) {
      val value = (Random.getRandom().absoluteValue % Int.MAX_VALUE).toInt()
      integers.add(value)
      integerIndex += 1
      return value
    }
    return integers[integerIndex++]
  }

  fun nextDouble(): Double {
    if (doubleIndex >= doubles.size) {
      val positiveLong = (Random.getRandom() and 0x1FFFFFFFFFFFFFL).toDouble()
      val value = positiveLong / (1L shl 53).toDouble()
      doubles.add(value)
      doubleIndex += 1
      return value
    }
    return doubles[doubleIndex++]
  }

  fun nextDouble(origin: Double, bound: Double): Double {
    if (doubleIndex >= doubles.size) {
      val positiveLong = (Random.getRandom() and 0x1FFFFFFFFFFFFFL).toDouble()
      var value = positiveLong / (1L shl 53).toDouble()
      value = origin + value * (bound - origin)
      doubles.add(value)
      doubleIndex += 1
      return value
    }
    return doubles[doubleIndex++]
  }

  fun done() {
    integers.clear()
    doubles.clear()
    integerIndex = 0
    doubleIndex = 0
  }
}
