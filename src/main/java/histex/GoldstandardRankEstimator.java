package histex;

import histex.sketches.FixedBucketHistogram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class GoldstandardRankEstimator implements RankEstimator {

  private final ArrayList<Float> values;

  public GoldstandardRankEstimator(Supplier<? extends FloatIterator> supplier) {
    FloatIterator it = supplier.get();

    values = new ArrayList<>();
    while (it.hasNext()) {
      values.add(it.nextValue());
    }
    Collections.sort(values);
  }

  float getMin() {
    return values.get(0);
  }

  float getMax() {
    return values.get(values.size()-1);
  }

  @Override
  public String getDesc() {
    return "goldstandard";
  }

  @Override
  public double getNormalizedRank(float value) {
    // TODO tr how to handle duplicate values?

    int start = 0;
    int end = values.size();

    while(start < end) {
      int mid = start + (end-start)/2;
      if (values.get(mid) > value) {
        end = mid;
      }
      else {
        start = mid+1;
      }
    }

    return (double)end / values.size();
  }

  FixedBucketHistogram convertToEquiHeightHistogram(int buckets, boolean interpolate) {
    float[] b = new float[buckets+1];
    for(int i=0; i<buckets; i++) {
      int valueIndex = Math.clamp((long) i *values.size() / buckets, 0, values.size()-1);
      b[i] = values.get(valueIndex);
    }
    b[b.length-1] = Math.nextUp(values.getLast());
    return new FixedBucketHistogram(b, interpolate);
  }

  @Override
  public String debugAsCsv() {
    List<Point> points = getInterestingPoints();
    HistSampling.removeSuperfluousPoints(points, values.getLast()-values.getFirst());
    return Point.toCsv(points);
  }

  private List<Point> getInterestingPoints() {
    List<Point> points = new ArrayList<>();
    float v = values.get(0);
    points.add(new Point(Math.nextDown(v), 0));

    float maxDiff = (values.getLast()-v) / 200;
    for (int i=1; i<values.size(); i++) {
      if ((values.get(i)-v) > maxDiff) {
        float last = values.get(i-1);
        points.add(new Point(last, (float) getNormalizedRank(last)));

        v = values.get(i);
        if (Math.nextDown(v) > last) {
          float tmp = Math.nextDown(v);
          points.add(new Point(tmp, (float) getNormalizedRank(tmp)));
        }
        points.add(new Point(v, (float) getNormalizedRank(v)));
      }
    }
    v = values.getLast();
    points.add(new Point(v, (float) getNormalizedRank(v)));
    return points;
  }



  private List<Point> sampleEquiWidthAndNeighbors() {
    List<Point> points = new ArrayList<>();
    float v = values.get(0);
    double rank = 0;
    points.add(new Point(v, 0));
    int pointCount = 3000;
    for (int i=0; i< pointCount; i++) {
      int valueIndex = Math.clamp((long) i *values.size() / pointCount, 0, values.size()-1);

      v = Math.nextDown(values.get(valueIndex));
      rank = getNormalizedRank(v);
      points.add(new Point(v, (float) rank));

      v = values.get(valueIndex);
      rank = getNormalizedRank(v);
      points.add(new Point(v, (float) rank));

      v = Math.nextUp(values.get(valueIndex));
      rank = getNormalizedRank(v);
      points.add(new Point(v, (float) rank));
    }
    return points;
  }

  public int size() {
    return this.values.size();
  }
}
