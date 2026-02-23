package histex;

interface FloatIterator {
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
    return of(new EquiWidthHistogram(start, end, n).getBucketEnds());
  }

  float nextValue();

  boolean hasNext();
}
