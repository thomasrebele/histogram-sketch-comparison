package histex;

public class EquiWidthHistogram extends CountHistogram {

  final float start;
  final float end;
  final int buckets;
  final float bucketWidth;

  public EquiWidthHistogram(float start, float end, int buckets, boolean interpolate) {
    super(buckets, interpolate);
    this.start = start;
    this.end = end;
    this.buckets = buckets;
    bucketWidth = (end - start) / buckets;
  }

  @Override
  public String getDesc() {
    return "equi-width(k=" + buckets + ",interp=" + (interpolate ? "t" : "f") +")";
  }

  @Override
  public int getBucketForValue(float v) {
    int bi = (int) ((v-start)/(end-start) * buckets);
    return Math.clamp(bi, 0, this.buckets-1);
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
  public double getMinNormalizedRankDifference(float start, float end) {
    if (interpolate)
      return 0;

    int startBucket = getBucketForValue(start);
    int endBucket = getBucketForValue(end);

    int max = 0;
    for (int i=startBucket; i<endBucket; i++) {
      max = Math.max(max, count[i]);
    }
    return (double)max / n;
  }
}
