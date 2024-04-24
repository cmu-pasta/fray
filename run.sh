while true
do
  ./gradlew cleanTest :integration-tests:test --tests "cmu.pasta.fray.it.lincheck.LogicalOrderingAVLTest.testConcurrentInsertRemove"
done
