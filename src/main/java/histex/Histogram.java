package histex;

public interface Histogram extends RankEstimator {
  void addValue(float v);

  int getMemoryUsageInBytes();

  /** The number of elements  */
  long getN();

  default void consume(FloatIterator it) {
    while(it.hasNext()) {
      addValue(it.nextValue());
    }
  }

  default double[] getRanks(float[] values) {
    double[] result = new double[values.length];
    for (int i=0; i<values.length; i++) {
      result[i] = getNormalizedRank(values[i]);
    }
    return result;
  }

  default String debugAsCsv() {
    return null;
  }
}
