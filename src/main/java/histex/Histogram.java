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

  default String debugDiffAsCsv(GoldstandardRankEstimator goldstandard) {
    float min = goldstandard.getMin();
    float max = goldstandard.getMax();

    StringBuilder sb = new StringBuilder();
    sb.append("x,y\n");
    for(int i=0; i<100; i++) {
      float v = min + (float)i/100 * (max-min);
      sb.append(v).append(",").append(getNormalizedRank(v) - goldstandard.getNormalizedRank(v)).append("\n");
    }
    return sb.toString();
  }
}
