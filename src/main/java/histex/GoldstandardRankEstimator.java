package histex;

import histex.sketches.FixedBucketHistogram;

import java.util.ArrayList;
import java.util.Collections;
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
    StringBuilder sb = new StringBuilder();
    sb.append("x,y\n");

    float v = values.get(0);
    double rank = 0;
    sb.append(v).append(",").append(0).append("\n");
    int points = 3000;
    for (int i=0; i< points; i++) {
      int valueIndex = Math.clamp((long) i *values.size() / points, 0, values.size()-1);

      v = Math.nextDown(values.get(valueIndex));
      rank = getNormalizedRank(v);
      sb.append(v).append(",").append(rank).append("\n");

      v = values.get(valueIndex);
      rank = getNormalizedRank(v);
      sb.append(v).append(",").append(rank).append("\n");

      v = Math.nextUp(values.get(valueIndex));
      rank = getNormalizedRank(v);
      sb.append(v).append(",").append(rank).append("\n");

    }
    return sb.toString();
  }
}
