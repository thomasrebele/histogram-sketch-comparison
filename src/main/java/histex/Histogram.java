package histex;

public interface Histogram {
  void addValue(float v);

  double getRank(float v);

  default void consume(CDF.Datastream ds) {
    while(ds.hasNext()) {
      addValue(ds.nextValue());
    }
  }

  default double getMinRankDifference() {
    return 0.0;
  }

  default double[] getRanks(float[] values) {
    double[] result = new double[values.length];
    for (int i=0; i<values.length; i++) {
      result[i] = getRank(values[i]);
    }
    return result;
  }
}
