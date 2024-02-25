module cmu.pasta.sfuzz.jdk {
    requires jdk.jlink;
    requires java.instrument;
    requires kotlin.stdlib;
    requires cmu.pasta.sfuzz.instrumentation;
    requires cmu.pasta.sfuzz.runtime;
    exports cmu.pasta.sfuzz.jdk.agent;
    exports cmu.pasta.sfuzz.jdk.jlink;
}