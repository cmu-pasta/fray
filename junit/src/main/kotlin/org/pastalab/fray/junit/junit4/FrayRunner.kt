package org.pastalab.fray.junit.junit4

import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod

class FrayRunner(clazz: Class<Any>) : BlockJUnit4ClassRunner(clazz) {
  override fun runChild(method: FrameworkMethod?, notifier: RunNotifier?) {}
}
