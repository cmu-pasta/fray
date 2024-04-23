package cmu.pasta.fray.core.math

import kotlin.math.ceil
import kotlin.math.ln

object Utils {
  fun sampleGeometric(p: Double, rand: Double): Int {
    return ceil(ln(1 - rand) / ln(1 - p)).toInt()
  }
}
