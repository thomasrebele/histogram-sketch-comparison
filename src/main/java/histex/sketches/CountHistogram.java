package histex.sketches;

import histex.HistSampling;
import histex.Histogram;
import histex.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CountHistogram implements Histogram {

  final boolean interpolate;
  int n;
  protected int[] count;

  public CountHistogram(int buckets, boolean interpolate) {
    count = new int[buckets];
    this.interpolate = interpolate;
  }

  @Override
  public long getN() {
    return n;
  }

  public List<Integer> getCounts() {
    return Arrays.stream(count).limit(count.length).boxed().toList();
  }

  public List<Integer> getCumulativeCounts() {
    List<Integer> result = new ArrayList<>();
    var sum = 0;
    for (int i=0; i<count.length; i++) {
      sum += count[i];
      result.add(sum);
    }
    return result;
  }

  @Override
  public double getNormalizedRank(float v) {
    long c = 0;
    int i;
    for (i=0; i<count.length; i++) {
      float bucketEnd = getBucketEnd(i);
      if (v <= bucketEnd) {break;}
      c += count[i];
    }

    double interp = 0;

    if (interpolate) {
      if (i < count.length) {
        float bucketStart = getBucketStart(i);
        float bucketEnd = getBucketEnd(i);
        interp = (v - bucketStart) / (bucketEnd - bucketStart) * count[i];
        interp = Math.clamp(interp, 0, count[i]);
      }
    }
    else {
      float bucketEnd = getBucketStart(i);
      for (; i<count.length; i++) {
        if (v < bucketEnd) {
          break;
        }
        c += count[i];
        bucketEnd = getBucketEnd(i);
      }
    }
    double base = c;
    return (base + interp)/n;
  }

  @Override
  public void addValue(float v) {
    int bi = getBucketForValue(v);
    n += 1;
    count[bi] += 1;
  }

  abstract int getBucketForValue(float v);

  public abstract float getBucketStart(int bucketIndex);

  public float getBucketEnd(int bucketIndex) {
    return getBucketStart(bucketIndex+1);
  }

  public float[] getBucketEnds() {
    float[] result = new float[count.length];
    for(int i=0; i<count.length; i++) {
      result[i] = getBucketEnd(i);
    }
    return result;
  }

  @Override
  public int getMemoryUsageInBytes() {
    return Integer.BYTES * count.length;
  }

  @Override
  public String debugAsCsv() {
    List<Point> points = new ArrayList<>();
    float v = getBucketStart(0);
    points.add(new Point(v, 0));

    if (!interpolate) {
      v = Math.nextUp(v);
      points.add(new Point(v, 0));
    }

    for(int i=0; i<count.length; i++) {
      if (!interpolate) {
        v = Math.nextDown(getBucketEnd(i));
        addPoint(points, v);
      }

      v = getBucketEnd(i);
      addPoint(points, v);

      if (!interpolate) {
        v = Math.nextUp(getBucketEnd(i));
        addPoint(points, v);
      }
    }
    return Point.toCsv(HistSampling.extractInterestingPoints(points) );
  }

  private void addPoint(List<Point> points, float v) {
    double rank = getNormalizedRank(v);
    if (Double.isNaN(rank)) {
      return;
    }
    points.add(new Point(v, (float) rank));
  }
}
