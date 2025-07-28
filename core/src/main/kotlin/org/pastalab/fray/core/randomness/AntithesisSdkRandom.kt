package org.pastalab.fray.core.randomness

import com.antithesis.sdk.Random
import kotlin.math.absoluteValue

class AntithesisSdkRandom : Randomness {
  override fun nextInt(): Int {
    val randomValue = Random.getRandom()
    val value =
        if (randomValue == Long.MIN_VALUE) {
          0 // Handle Long.MIN_VALUE explicitly
        } else {
          (randomValue.absoluteValue % Int.MAX_VALUE).toInt()
        }
    return value
  }

  override fun nextDouble(): Double {
    val positiveLong = (Random.getRandom() and 0x1FFFFFFFFFFFFFL).toDouble()
    val value = positiveLong / (1L shl 53).toDouble()
    return value
  }

  override fun nextDouble(origin: Double, bound: Double): Double {
    return origin + nextDouble() * (bound - origin)
  }

  override fun done() {}
}

class AntithesisSdkRandomProvider : RandomnessProvider {
  override fun getRandomness(): Randomness {
    return AntithesisSdkRandom()
  }
}
