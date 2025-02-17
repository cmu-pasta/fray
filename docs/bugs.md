# Articles about Fray

- [Concurrency bugs in Lucene: How to fix optimistic concurrency failures](https://www.elastic.co/search-labs/blog/optimistic-concurrency-lucene-debugging)

# Bugs found by Fray

## Lucene

- [#13547 Flaky Test in TestMergeSchedulerExternal#testSubclassConcurrentMergeScheduler](https://github.com/apache/lucene/issues/13547)
- [#13552 Test TestIndexWriterWithThreads#testIOExceptionDuringWriteSegmentWithThreadsOnlyOnce Failed](https://github.com/apache/lucene/issues/13552)
- [#13571 DocumentsWriterDeleteQueue.getNextSequenceNumber assertion failure seqNo=9 vs maxSeqNo=8](https://github.com/apache/lucene/issues/13571)
- [#13593 ConcurrentMergeScheduler may spawn more merge threads than specified](https://github.com/apache/lucene/issues/13593)

## Kafka

- [#17112 StreamThread shutdown calls completeShutdown only in CREATED state](https://issues.apache.org/jira/browse/KAFKA-17112)
- [#17113 Flaky Test in GlobalStreamThreadTest#shouldThrowStreamsExceptionOnStartupIfExceptionOccurred](https://issues.apache.org/jira/browse/KAFKA-17113)
- [#17114 DefaultStateUpdater::handleRuntimeException should update isRunning before calling `addToExceptionsAndFailedTasksThenClearUpdatingAndPausedTasks`](https://issues.apache.org/jira/browse/KAFKA-17114)
- [#17162 DefaultTaskManagerTest may leak AwaitingRunnable thread](https://issues.apache.org/jira/browse/KAFKA-17162?filter=-2)
- [#17354 StreamThread::setState race condition causes java.lang.RuntimeException: State mismatch PENDING_SHUTDOWN different from STARTING](https://issues.apache.org/jira/browse/KAFKA-17354?filter=-2)
- [#17371 Flaky test in DefaultTaskExecutorTest.shouldUnassignTaskWhenRequired](https://issues.apache.org/jira/browse/KAFKA-17371?filter=-2)
- [#17379 KafkaStreams: Unexpected state transition from ERROR to PENDING_SHUTDOWN](https://issues.apache.org/jira/browse/KAFKA-17379?filter=-2)
- [#17394 Flaky test in DefaultTaskExecutorTest.shouldSetUncaughtStreamsException](https://issues.apache.org/jira/browse/KAFKA-17394?filter=-2)
- [#17402 Test failure: DefaultStateUpdaterTest.shouldGetTasksFromRestoredActiveTasks expected: <2> but was: <3>](https://issues.apache.org/jira/browse/KAFKA-17402?filter=-2)
- [#17929 `awaitProcessableTasks` is not safe in the presence of spurious wakeups.](https://issues.apache.org/jira/browse/KAFKA-17929?filter=-2)
- [#17946 Flaky test DeafultStateUpdaterTest::shouldResumeStandbyTask due to concurrency issue](https://issues.apache.org/jira/browse/KAFKA-17946?filter=-2)
- [#18418 Flaky test in KafkaStreamsTest::shouldThrowOnCleanupWhileShuttingDownStreamClosedWithCloseOptionLeaveGroupFalse](https://issues.apache.org/jira/browse/KAFKA-18418?filter=-2)


## Flink

- [#37182 PartitionedFileWriteReadTest should remove partition files after test finishes.](https://issues.apache.org/jira/browse/FLINK-37182)

## Guava

- [#7319 Lingering threads in multiple tests](https://github.com/google/guava/issues/7319)