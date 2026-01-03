package org.pastalab.fray.test.core.success.classconstructor;

import java.security.cert.X509CertSelector;

public class X509CertSelectorConstructorNoDeadlock {
  public static void main(String[] args) {
    new Thread(
            () -> {
              new X509CertSelector();
            })
        .start();
    new Thread(
            () -> {
              new X509CertSelector();
            })
        .start();
  }
}
