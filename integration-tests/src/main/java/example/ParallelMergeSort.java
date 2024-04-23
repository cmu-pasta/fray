package example;

import cmu.pasta.fray.core.GlobalContext;

import java.util.Arrays;


/** parallelization of MergeSort as seen in the sort-benchmarks repo
 *
 * @author Varun Upadhyay (https://github.com/varunu28)
 * @author Podshivalov Nikita (https://github.com/nikitap492)
 */
public class ParallelMergeSort {
  static final Object MSLOCK = "MERGESORTLOCK";

  private static void log (String format, Object... args) {
    GlobalContext.INSTANCE.log(format, args);
  }

  /**
   * This method implements the Generic Merge Sort
   *
   * @param unsorted the array which should be sorted
   * @param <T> Comparable class
   * @return sorted array
   */
  public <T extends Comparable<T>> T[] sort(T[] unsorted) {
    try {
      doSort(unsorted, 0, unsorted.length - 1);
      return unsorted;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * @param arr The array to be sorted
   * @param left The first index of the array
   * @param right The last index of the array Recursively sorts the array in increasing order
   */
  private static <T extends Comparable<T>> void doSort(T[] arr, int left, int right) throws InterruptedException {
    log("doSort(%s, %d, %d) called", Arrays.toString(arr), left, right);
    if (left < right) {
      int mid = left + (right - left) / 2;

      Thread t1 = new Thread(() -> {
        try {
          doSort(arr, left, mid);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      });
      Thread t2 = new Thread(() -> {
        try {
          doSort(arr, mid + 1, right);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      });
      t1.start();
      t2.start();
      t1.join();
      t2.join();
      
      log("Calling merge(%s, %d, %d, %d)", Arrays.toString(arr), left, mid, right);
      merge(arr, left, mid, right);
    }
    log("doSort(%s, %d, %d) done", Arrays.toString(arr), left, right);
  }

  /**
   * This method implements the merge step of the merge sort
   *
   * @param arr The array to be sorted
   * @param left The first index of the array
   * @param mid The middle index of the array
   * @param right The last index of the array merges two parts of an array in increasing order
   */
  private static <T extends Comparable<T>> void merge(T[] arr, int left, int mid, int right) {
    int length = right - left + 1;
    T[] temp = (T[]) new Comparable[length];
    int i = left;
    int j = mid + 1;
    int k = 0;

    while (i <= mid && j <= right) {
        if (arr[i].compareTo(arr[j]) <= 0) {
          temp[k++] = arr[i++];
        } else {
          temp[k++] = arr[j++];
        }
    }

    while (i <= mid) {
        temp[k++] = arr[i++];
    }

    while (j <= right) {
        temp[k++] = arr[j++];
    }

    log("Trying to sync on MSLOCK");
    synchronized (MSLOCK) {
      log("Syncd on MSLOCK");
      System.arraycopy(temp, 0, arr, left, length);
    }
  }
}
