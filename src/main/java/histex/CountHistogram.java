package histex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CountHistogram implements Histogram {

  int n;
  protected int[] count;

  public CountHistogram(int buckets) {
    count = new int[buckets];
  }

  public List<Integer> getCounts() {
    return Arrays.stream(count).limit(count.length-1).boxed().toList();
  }

  public List<Integer> getCumulativeCounts() {
    List<Integer> result = new ArrayList<>();
    var sum = 0;
    for (int i=0; i<count.length-1; i++) {
      sum += count[i];
      result.add(sum);
    }
    return result;
  }

  @Override
  public double getRank(float v) {
    long c = 0;
    for (int i=0; i<count.length; i++) {
      float bucketStart = getBucketStart(i);
      if (v <= bucketStart) {break;}
      c += count[i];
    }

    return (double)c / n;
  }

  @Override
  public void addValue(float v) {
    int bi = getBucketForValue(v);
    n += 1;
    count[bi] += 1;
  }

  abstract int getBucketForValue(float v);

  abstract float getBucketStart(int bucketIndex);

  float getBucketEnd(int bucketIndex) {
    return getBucketStart(bucketIndex+1);
  }

  public float[] getBucketEnds() {
    float[] result = new float[count.length];
    for(int i=0; i<count.length; i++) {
      result[i] = getBucketEnd(i);
    }
    return result;
  }

}
