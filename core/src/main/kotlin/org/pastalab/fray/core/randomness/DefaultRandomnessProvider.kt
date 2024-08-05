package org.pastalab.fray.core.randomness

class DefaultRandomnessProvider : RandomnessProvider {
  val random = java.util.Random()
  val integers = mutableListOf<Int>()
  val doubles = mutableListOf<Double>()
  val longs = mutableListOf<Long>()
  val bytes = mutableListOf<Byte>()

  override fun nextInt(): Int {
    val next = random.nextInt()
    integers.add(next)
    return next
  }

  override fun nextInt(bound: Int): Int {
    val next = random.nextInt(bound)
    integers.add(next)
    return next
  }

  override fun nextLong(): Long {
    val next = random.nextLong()
    longs.add(next)
    return next
  }

  override fun nextBoolean(): Boolean {
    val next = random.nextInt()
    integers.add(next)
    return next % 2 == 0
  }

  override fun nextDouble(): Double {
    val next = random.nextDouble()
    doubles.add(next)
    return next
  }

  override fun nextGaussian(): Double {
    val next = random.nextGaussian()
    doubles.add(next)
    return next
  }
}
