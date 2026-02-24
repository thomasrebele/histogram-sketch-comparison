package histex;

public class EquiWidthHistogram extends CountHistogram {

  final float start;
  final float end;
  final int buckets;
  final float bucketWidth;

  public EquiWidthHistogram(float start, float end, int buckets) {
    super(buckets);
    this.start = start;
    this.end = end;
    this.buckets = buckets;
    bucketWidth = (end - start) / buckets;
  }

  @Override
  public int getBucketForValue(float v) {
    int bi = (int) ((v-start)/(end-start) * buckets);
    return bi;
  }

  @Override
  float getBucketStart(int bucketIndex) {
    return start + bucketWidth * bucketIndex;
  }

  float getBucketWidth() {
    return bucketWidth;
  }

  @Override
  public int getMemoryUsageInBytes() {
    return 2*Float.BYTES + Integer.BYTES + super.getMemoryUsageInBytes();
  }

  @Override
  public double getMinRankDifference() {
    return bucketWidth;
  }
}
