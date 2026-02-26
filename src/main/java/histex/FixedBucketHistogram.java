package histex;

import java.util.Arrays;

public class FixedBucketHistogram extends CountHistogram {

  float[] buckets;

  public FixedBucketHistogram(float[] buckets, boolean interpolate) {
    super(buckets.length, interpolate);
    this.buckets = Arrays.copyOf(buckets, buckets.length);
  }

  @Override
  public String getDesc() {
    return "fixed-buckets(k=" + buckets.length + ",interp=" + (interpolate ? "t" : "f") +")";
  }


  @Override
  public int getBucketForValue(float v) {
    int bi = Arrays.binarySearch(buckets, v);
    if (bi < 0) {
      bi = -(bi+1);
      if (bi == 0) {throw new IllegalStateException();}
      bi -= 1;
    }
    if (bi == count.length-1) {
      throw new IllegalStateException();
    }
    return bi;
  }

  @Override
  float getBucketStart(int bucketIndex) {
    return buckets[bucketIndex];
  }

  @Override
  public int getMemoryUsageInBytes() {
    return Float.BYTES * buckets.length + super.getMemoryUsageInBytes();
  }
}
