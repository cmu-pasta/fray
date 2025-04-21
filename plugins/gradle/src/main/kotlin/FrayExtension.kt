package org.pastalab.fray.gradle

import org.pastalab.fray.plugins.base.FrayVersion

open class FrayExtension {
  var version = FrayVersion.version
  var scheduler: String? = null
  var iterations: Int? = null
  var replayDir: String? = null
  var timeout: Int? = null
  var exploreMode: Boolean = false
  var numSwitchPoints: Int? = null
}
