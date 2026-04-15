package histex.sketches;

import java.util.Arrays;

public class FixedBucketHistogram extends CountHistogram {

  float[] boundaries;

  public FixedBucketHistogram(float[] boundaries, boolean interpolate) {
    super(boundaries.length-1, interpolate);
    this.boundaries = Arrays.copyOf(boundaries, boundaries.length);
  }

  @Override
  public String getDesc() {
    return "fixed-buckets(k=" + count.length + ",interp=" + (interpolate ? "t" : "f") +")";
  }


  @Override
  public int getBucketForValue(float v) {
    int bi = Arrays.binarySearch(boundaries, v);
    if (bi < 0) {
      bi = -(bi+1);
      if (bi == 0) {throw new IllegalStateException();}
      bi -= 1;
    }
    if (bi >= count.length) {
      throw new IllegalStateException();
    }
    return bi;
  }

  @Override
  public float getBucketStart(int bucketIndex) {
    return boundaries[bucketIndex];
  }

  @Override
  public int getMemoryUsageInBytes() {
    return Float.BYTES * boundaries.length + super.getMemoryUsageInBytes();
  }
}
