package histex;

interface FloatIterator extends AutoCloseable {
  static FloatIterator of(float[] values) {
    return new FloatIterator() {

      int i = 0;

      @Override
      public float nextValue() {
        return values[i++];
      }

      @Override
      public boolean hasNext() {
        return i < values.length;
      }
    };
  }

  static FloatIterator sequence(float start, float end, int n) {
    return of(new EquiWidthHistogram(start, end, n, true).getBucketEnds());
  }

  float nextValue();

  boolean hasNext();

  default void close() throws Exception {

  }
}
