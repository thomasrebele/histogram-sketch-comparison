package histex;

import histex.CDF.Point;

public class CDFFactory {

  public static CDF uniform(float start, float end) {
    Point[] ps = new Point[] {
        new Point(start, 1f),
        new Point(end, 1f)
    };
    return new CDF(ps);
  }

  public static CDF gaussian(float mean, float stddev) {
    // TODO tr implement
    Point[] ps = new Point[]{};
    return new CDF(ps);
  }

}
