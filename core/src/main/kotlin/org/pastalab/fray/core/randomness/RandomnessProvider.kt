package org.pastalab.fray.core.randomness

interface RandomnessProvider {
  fun nextInt(): Int

  fun nextInt(bound: Int): Int

  fun nextLong(): Long

  fun nextBoolean(): Boolean

  fun nextDouble(): Double

  fun nextGaussian(): Double
}
