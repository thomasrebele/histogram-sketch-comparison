package histex;

public interface Histogram {
  void addValue(float v);

  default void consume(CDF.Datastream ds) {
    while(ds.hasValue()) {
      addValue(ds.nextValue());
    }
  }

  double getRank(float v);

  default double[] getRanks(float[] values) {
    double[] result = new double[values.length];
    for (int i=0; i<values.length; i++) {
      result[i] = getRank(values[i]);
    }
    return result;
  }
}
