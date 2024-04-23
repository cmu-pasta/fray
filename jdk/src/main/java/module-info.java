module cmu.pasta.fray.jdk {
    requires jdk.jlink;
    requires java.instrument;
    requires kotlin.stdlib;
    requires cmu.pasta.fray.instrumentation;
    requires cmu.pasta.fray.runtime;
    exports cmu.pasta.fray.jdk.agent;
    exports cmu.pasta.fray.jdk.jlink;
}