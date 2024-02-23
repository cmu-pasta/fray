module cmu.pasta.sfuzz.jdk {
    requires jdk.jlink;
    requires java.instrument;
    requires kotlin.stdlib;
    requires org.objectweb.asm;
    requires cmu.pasta.sfuzz.runtime;
}