module org.pastalab.fray.instrumentation.jdk {
  requires jdk.jlink;
  requires java.instrument;
  requires kotlin.stdlib;
  requires org.pastalab.fray.instrumentation.base;
  requires org.pastalab.fray.runtime;
  requires org.objectweb.asm.commons;
  requires org.objectweb.asm.util;

  exports org.pastalab.fray.instrumentation.jdk.agent;
  exports org.pastalab.fray.instrumentation.jdk.jlink;
}
