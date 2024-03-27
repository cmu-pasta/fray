package cmu.pasta.sfuzz.core.scheduler

import java.util.*

class ControlledRandom(
    private val integers: MutableList<Int> = mutableListOf<Int>(),
    private val doubles: MutableList<Double> = mutableListOf<Double>()
) {

  private val random = Random()
  private var integerIndex = 0
  private var doubleIndex = 0

  fun nextInt(): Int {
    if (integerIndex >= integers.size) {
      val value = random.nextInt(0, Int.MAX_VALUE)
      integers.add(value)
      integerIndex += 1
      return value
    }
    return integers[integerIndex++]
  }

  fun nextDouble(): Double {
    if (doubleIndex >= doubles.size) {
      val value = random.nextDouble()
      doubles.add(value)
      doubleIndex += 1
      return value
    }
    return doubles[doubleIndex++]
  }
}
