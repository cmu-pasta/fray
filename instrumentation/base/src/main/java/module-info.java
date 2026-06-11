module org.pastalab.fray.instrumentation.base {
  exports org.pastalab.fray.instrumentation.base;
  exports org.pastalab.fray.instrumentation.base.visitors;

  opens org.pastalab.fray.instrumentation.base;

  requires org.pastalab.fray.runtime;
  requires kotlin.reflect;
  requires org.objectweb.asm;
  requires org.objectweb.asm.tree;
  requires org.objectweb.asm.commons;
  requires java.instrument;
  requires org.objectweb.asm.util;
  requires java.logging;
}
