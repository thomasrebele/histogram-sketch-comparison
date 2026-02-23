package histex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FixedBucketHistogram implements Histogram {

  float[] buckets;
  int[] count;

  public FixedBucketHistogram(float[] buckets) {
    this.buckets = Arrays.copyOf(buckets, buckets.length);
    count = new int[buckets.length];
  }

  @Override
  public void addValue(float f) {
    int bi = Arrays.binarySearch(buckets, f);
    if (bi < 0) {
      bi = -(bi+1);
      if (bi == 0) {throw new IllegalStateException();}
      bi -= 1;
    }
    if (bi == count.length-1) {
      throw new IllegalStateException();
    }
    count[bi] += 1;
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
}
