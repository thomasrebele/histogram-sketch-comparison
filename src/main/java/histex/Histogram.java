package histex;

public interface Histogram {
  void addValue(float v);

  default void consume(CDF.Datastream ds) {
    while(ds.hasValue()) {
      addValue(ds.nextValue());
    }
  }

  float getRank(float v);

  default float[] getRanks(float[] values) {
    float[] result = new float[values.length];
    for (int i=0; i<values.length; i++) {
      result[i] = getRank(values[i]);
    }
    return result;
  }
}
