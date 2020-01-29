import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.Collections;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;

class OtsuTresholding extends UnicastRemoteObject implements RemoteOtsu {
  int[][] topMatrix = new int[0][0];
  int[][] bottomMatrix = new int[0][0];
  int[][] otsuMatrix = new int[0][0];
  int[][] fullMatrix = new int[0][0];
  long elapsedTime = 0;
  long startTime = 0;
  long stopTime = 0;
  int counter = 0;
  
  OtsuTresholding() throws RemoteException {
    super();
  }

  public long getElapsedTime() throws RemoteException {
    return elapsedTime;
  }

  public void appendMatrix(int[][] m, Boolean isParallel) throws RemoteException {
    try {
      System.out.println("appendMatrix" + m.length);
      if (topMatrix.length < 1) {
        topMatrix = m;
        System.out.println("if" + topMatrix.length);
      } else {
        bottomMatrix = m;
        fullMatrix = new int[topMatrix.length * 2][topMatrix[0].length];
        for (int i = 0; i < topMatrix.length; i++) {
          fullMatrix[i] = topMatrix[i];
        }
        for (int j = 0; j < bottomMatrix.length; j++) {
          fullMatrix[j + topMatrix.length] = bottomMatrix[j];
        }
        startTime = System.currentTimeMillis();
        if (isParallel) {
          computeParallel();
        } else {
          otsuMatrix = computeMatrix(fullMatrix, 0, fullMatrix.length);
          stopTime = System.currentTimeMillis();
          elapsedTime = stopTime - startTime;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public int[][] getOtsuMatrix () throws RemoteException {
    return otsuMatrix;
  }

  /** 
   * 
   * @param matrix The matrix with GrayValues
   * @return Matrix converted to binary with 0's and 255 values
   */
  public int[][] computeMatrix(int [][] matrix, int start, int end) throws RemoteException {
    int[] values = IntStream.range(0, 256).toArray();
    int[] counts = new int[values.length];
    ArrayList<Double> tresholdCache = new ArrayList<Double>();
    ArrayList<Double> varianceCache = new ArrayList<Double>();
 
    for (int i = start; i < end; i++) {
      for (int j = 0; j < matrix[i].length; j++) {
        int pos = matrix[i][j];
        counts[pos] += 1;
      }
    }

    for (int i = 1; i < 256; i++) {
      int treshold = values[i];
      Range rangeA = new Range(1, treshold);
      Range rangeB = new Range(treshold + 1, 256);
      OtsuValues A = getOtsuValues(counts, values, rangeA);
      OtsuValues B = getOtsuValues(counts, values, rangeB);
      Double withinClassVariance = (A.weight * A.variance) + (B.weight * B.variance);

      varianceCache.add(withinClassVariance);
      tresholdCache.add((double) treshold);
    }

    double selectedTreshold = getSelectedTreshold(varianceCache, tresholdCache);

    for (int i = start; i < end; i++) {
      for (int j = 0; j < matrix[i].length; j++) {
        int value = matrix[i][j];
        if (value > selectedTreshold) {
          matrix[i][j] = 255;
        } else {
          matrix[i][j] = 0;
        }
      }
    }

    return matrix;
  }

  /**
   * 
   * @param counts
   * @param values
   * @param range
   * @return Returns an OtsuValues Object cotaining the calculated weight, mean
   *         and variance.
   */
  private static OtsuValues getOtsuValues(int[] counts, int[] values, Range range) {
    int[] section = Arrays.copyOfRange(counts, range.start, range.limit);
    int[] sectionValues = Arrays.copyOfRange(values, range.start, range.limit);
    double weight = IntStream.of(section).sum();

    double mean = dot(doubleArray(section), doubleArray(sectionValues)) / weight;
    double[] preVariance = sub(doubleArray(sectionValues), mean);
    preVariance = multi(preVariance, preVariance);
    double variance = dot(preVariance, doubleArray(section)) / weight;
    if (Double.isNaN(variance)) {
      variance = 0.0;
    }
    return new OtsuValues(weight, mean, variance);
  }

  /**
   * 
   * @param variance
   * @param treshold
   * @return The treshold that corresponds to the minimum variance value
   */
  private static double getSelectedTreshold(ArrayList<Double> variance, ArrayList<Double> treshold) {
    double min = variance.get(0);
    int minIndex2 = 0;
    for (int i = 0; i < variance.size(); i++) {
      if (variance.get(i) < min) {
        min = variance.get(i);
        minIndex2 = i;
      }
    }
    int minIndex = variance.indexOf(Collections.min(variance));
    return treshold.get(minIndex);
  }

  /**
   * 
   * @param source
   * @return int array converted to double values
   */
  public static double[] doubleArray(int[] source) {
    double[] dest = new double[source.length];
    for (int i = 0; i < source.length; i++) {
      dest[i] = source[i];
    }
    return dest;
  }

  /**
   * 
   * @param array
   * @param value
   * @return
   */
  public static double[] sub(double[] array, double value) {
    double[] newArray = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      newArray[i] = array[i] - value;
    }
    return newArray;
  }

  /**
   * 
   * @param a
   * @param b
   * @return Dot product from a.b
   */

  public static double dot(double[] a, double[] b) {
    double sum = 0;
    for (int i = 0; i < a.length - 1; i++) {
      sum += a[i] * b[i];
    }
    return sum;
  }

  /**
   * 
   * @param a
   * @param b
   * @return axb
   */
  public static double[] multi(double[] a, double[] b) {
    double[] result = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = a[i] * b[i];
    }
    return result;
  }
  class OtsuTresholdingTask extends RecursiveAction {
    int low = 0;
    int high = 0;
    int[][] matrix;

    OtsuTresholdingTask(int[][] matrix, int low, int high) {
      this.matrix = matrix;
      this.low = low;
      this.high = high;
    }

    @Override
    protected void compute() {
      try {
        if (low < high) {
          if (high - low < (matrix.length / 5)) {
            OtsuTresholding otsuTresholding = new OtsuTresholding();
            otsuTresholding.computeMatrix(matrix, low, high);
          } else {
            int mid = (low + high) / 2;
            OtsuTresholdingTask leftTask = new OtsuTresholdingTask(matrix, low, mid);
            OtsuTresholdingTask rightTask = new OtsuTresholdingTask(matrix, mid + 1, high);
            invokeAll(leftTask, rightTask);
          }
        }
      } catch (Exception e) {
        //TODO: handle exception
      }

    }
  }
  
  public void computeParallel() throws RemoteException {
    ForkJoinPool pool = new ForkJoinPool(4);
    OtsuTresholdingTask otsuTask = new OtsuTresholdingTask(fullMatrix, 0, fullMatrix.length);
    pool.invoke(otsuTask);
    otsuMatrix = fullMatrix;
    stopTime = System.currentTimeMillis();
    elapsedTime = stopTime - startTime;
  }

  public void clearMatrix() throws RemoteException {
    topMatrix = new int[0][0];
    bottomMatrix = new int[0][0];
    fullMatrix = new int[0][0];
    otsuMatrix = new int[0][0];
    startTime = 0;
    stopTime = 0;
  }
}



interface RemoteOtsu extends Remote {
  int[][] computeMatrix(int[][] matrix, int start, int end) throws RemoteException;
  void appendMatrix(int [][] matrix, Boolean isParallel) throws RemoteException;
  int[][] getOtsuMatrix() throws RemoteException;
  long getElapsedTime() throws RemoteException;
  void clearMatrix() throws RemoteException;
}