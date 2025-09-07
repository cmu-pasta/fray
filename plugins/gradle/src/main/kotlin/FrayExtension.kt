package org.pastalab.fray.gradle

import org.pastalab.fray.plugins.base.FrayVersion

open class FrayExtension {
  var version = FrayVersion.version
  var jdkPath: String? = null
}
