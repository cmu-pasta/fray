package org.pastalab.fray.core.randomness

import java.io.File
import java.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

@Serializable
class ControlledRandom(
    val integers: MutableList<Int> = mutableListOf(),
    val doubles: MutableList<Double> = mutableListOf(),
    @Transient private val random: Random = Random()
) : Randomness {

  @Transient private var integerIndex = 0

  @Transient private var doubleIndex = 0

  override fun nextInt(): Int {
    if (integerIndex >= integers.size) {
      val value = random.nextInt(Int.MAX_VALUE)
      integers.add(value)
      integerIndex += 1
      return value
    }
    return integers[integerIndex++]
  }

  override fun nextDouble(): Double {
    if (doubleIndex >= doubles.size) {
      val value = random.nextDouble()
      doubles.add(value)
      doubleIndex += 1
      return value
    }
    return doubles[doubleIndex++]
  }

  override fun nextDouble(origin: Double, bound: Double): Double {
    if (doubleIndex >= doubles.size) {
      val value = origin + random.nextDouble() * (bound - origin)
      doubles.add(value)
      doubleIndex += 1
      return value
    }
    return doubles[doubleIndex++]
  }

  override fun done() {
    integers.clear()
    doubles.clear()
    integerIndex = 0
    doubleIndex = 0
  }
}

class ControlledRandomProvider : RandomnessProvider {
  override fun getRandomness(): Randomness {
    return ControlledRandom()
  }
}

class RecordedRandomProvider(val recordingPath: String) : RandomnessProvider {
  override fun getRandomness(): Randomness {
    return Json.decodeFromString<Randomness>(File(recordingPath).readText())
  }
}
