package org.pastalab.fray.runtime;

public class TargetTerminateException extends RuntimeException {
  public TargetTerminateException() {}

  public TargetTerminateException(String message) {
    super(message);
  }

  public TargetTerminateException(String message, Throwable cause) {
    super(message, cause);
  }

  public TargetTerminateException(Throwable cause) {
    super(cause);
  }

  public TargetTerminateException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
