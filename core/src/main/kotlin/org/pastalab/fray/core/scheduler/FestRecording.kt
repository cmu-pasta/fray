package org.pastalab.fray.core.scheduler

import java.util.Random
import org.pastalab.fray.core.randomness.ControlledRandom

/** An immutable snapshot of the pseudo-random choices that determine one schedule */
class FestRecording(val integers: List<Int>, val doubles: List<Double>) {

  fun toRandom(): ControlledRandom =
      ControlledRandom(integers.toMutableList(), doubles.toMutableList())

  fun mutate(rng: Random, mutationCount: Int): FestRecording {
    val ints = integers.toMutableList()
    val dbls = doubles.toMutableList()
    val total = ints.size + dbls.size
    if (total == 0) {
      return this
    }
    repeat(mutationCount) {
      val pos = rng.nextInt(total)
      if (pos < ints.size) {
        ints[pos] = rng.nextInt(Int.MAX_VALUE)
      } else {
        dbls[pos - ints.size] = rng.nextDouble()
      }
    }
    return FestRecording(ints, dbls)
  }

  companion object {
    fun capture(rand: ControlledRandom): FestRecording =
        FestRecording(rand.integers.toList(), rand.doubles.toList())
  }
}
