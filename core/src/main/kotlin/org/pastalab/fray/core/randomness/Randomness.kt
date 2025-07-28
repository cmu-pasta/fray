package org.pastalab.fray.core.randomness

import kotlinx.serialization.Serializable

@Serializable
sealed interface Randomness {
  fun nextInt(): Int

  fun nextDouble(): Double

  fun nextDouble(origin: Double, bound: Double): Double

  fun done()
}

interface RandomnessProvider {
  fun getRandomness(): Randomness
}
