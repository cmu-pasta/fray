# Expected Integration Test Schedules

This folder contains the orders of thread releases generated for the integration tests by a particular successful run.

It is possible that a future version of Java may produce compilation differences that change the execution indices associated with threads and thereby the meanings of the series of integers used for schedules.
To mitigate the effects of this, if the integration tests are failing in the future, the thread release order on the failing test should be compared against the corresponding text file in this folder.
If the thread release order is different, that means the threads themselves have been reindexed and the hardcoded test schedules no longer translate to the same release schedule.
This is not actually a bug, and can be fixed by adjusting the hardcoded schedules to reflect the new thread indices.

## File Naming Convention

The files in this folder are named as `TestClass_testMethod.txt`.

## Reproducing the Files

To reproduce the exact file writes that generated these files, use the `Scheduler#setFileWriteCallback` method before running the test, passing as an argument the name of the file to write to.