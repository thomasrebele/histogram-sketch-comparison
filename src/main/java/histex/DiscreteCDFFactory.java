package histex;

import histex.DiscreteCDF.Point;
import org.apache.commons.math3.distribution.NormalDistribution;

public class DiscreteCDFFactory {

  public static DiscreteCDF uniform(float start, float end) {
    Point[] ps = new Point[] {
        new Point(start, 1f),
        new Point(end, 1f)
    };
    return new DiscreteCDF("uniform", ps);
  }

  public static DiscreteCDF gaussian(float mean, float stddev, int buckets) {
    NormalDistribution normal = new NormalDistribution(mean, stddev);

    float start = mean-3*stddev;
    float end = mean+3*stddev;

    EquiWidthHistogram h = new EquiWidthHistogram(start, end, buckets, false);

    for(int i=0; i<h.buckets; i++) {
    }

    Point[] ps = new Point[h.buckets+1];
    for (int i=0; i<h.buckets; i++) {
      float v = h.getBucketStart(i);
      float e = h.getBucketEnd(i);
      double v1 = normal.cumulativeProbability(e);

      ps[i] = new Point(v, (float) v1);
    }
    ps[h.buckets-1] = new Point(end, 1.0f);
    ps[h.buckets] = new Point(end, 1.0f);

    return new DiscreteCDF("gaussian "
        + "mean="+mean + " "
        + "stddev="+stddev, ps);
  }

}
