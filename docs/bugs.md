# Bugs found by Fray

## JDK

- [# 8358601 Race condition causes FutureTask returned by ScheduledThreadPoolExecutor hangs](https://bugs.openjdk.org/browse/JDK-8358601)

## Lucene

- [#13547 Flaky Test in TestMergeSchedulerExternal#testSubclassConcurrentMergeScheduler](https://github.com/apache/lucene/issues/13547)
- [#13552 Test TestIndexWriterWithThreads#testIOExceptionDuringWriteSegmentWithThreadsOnlyOnce Failed](https://github.com/apache/lucene/issues/13552)
- [#13571 DocumentsWriterDeleteQueue.getNextSequenceNumber assertion failure seqNo=9 vs maxSeqNo=8](https://github.com/apache/lucene/issues/13571)
- [#13593 ConcurrentMergeScheduler may spawn more merge threads than specified](https://github.com/apache/lucene/issues/13593)

## Kafka

- [#17112 StreamThread shutdown calls completeShutdown only in CREATED state](https://issues.apache.org/jira/browse/KAFKA-17112)
- [#17113 Flaky Test in GlobalStreamThreadTest#shouldThrowStreamsExceptionOnStartupIfExceptionOccurred](https://issues.apache.org/jira/browse/KAFKA-17113)
- [#17114 DefaultStateUpdater::handleRuntimeException should update isRunning before calling `addToExceptionsAndFailedTasksThenClearUpdatingAndPausedTasks`](https://issues.apache.org/jira/browse/KAFKA-17114)
- [#17162 DefaultTaskManagerTest may leak AwaitingRunnable thread](https://issues.apache.org/jira/browse/KAFKA-17162)
- [#17354 StreamThread::setState race condition causes java.lang.RuntimeException: State mismatch PENDING_SHUTDOWN different from STARTING](https://issues.apache.org/jira/browse/KAFKA-17354)
- [#17371 Flaky test in DefaultTaskExecutorTest.shouldUnassignTaskWhenRequired](https://issues.apache.org/jira/browse/KAFKA-17371)
- [#17379 KafkaStreams: Unexpected state transition from ERROR to PENDING_SHUTDOWN](https://issues.apache.org/jira/browse/KAFKA-17379)
- [#17394 Flaky test in DefaultTaskExecutorTest.shouldSetUncaughtStreamsException](https://issues.apache.org/jira/browse/KAFKA-17394)
- [#17402 Test failure: DefaultStateUpdaterTest.shouldGetTasksFromRestoredActiveTasks expected: <2> but was: <3>](https://issues.apache.org/jira/browse/KAFKA-17402)
- [#17929 `awaitProcessableTasks` is not safe in the presence of spurious wakeups.](https://issues.apache.org/jira/browse/KAFKA-17929)
- [#17946 Flaky test DeafultStateUpdaterTest::shouldResumeStandbyTask due to concurrency issue](https://issues.apache.org/jira/browse/KAFKA-17946)
- [#18418 Flaky test in KafkaStreamsTest::shouldThrowOnCleanupWhileShuttingDownStreamClosedWithCloseOptionLeaveGroupFalse](https://issues.apache.org/jira/browse/KAFKA-18418)


## Flink

- [#37182 PartitionedFileWriteReadTest should remove partition files after test finishes.](https://issues.apache.org/jira/browse/FLINK-37182)

## Guava

- [#7319 Lingering threads in multiple tests](https://github.com/google/guava/issues/7319)
