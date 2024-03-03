package cmu.pasta.sfuzz.runtime;

public class TargetTerminateException extends RuntimeException {
    private final int status;
    public TargetTerminateException(int stats) {
        super();
        this.status = stats;
    }

    public int getStatus() {
        return status;
    }

}
