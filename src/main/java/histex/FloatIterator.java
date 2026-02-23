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

  float nextValue();

  boolean hasNext();
}
