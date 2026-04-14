package histex;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;

public class DiscreteCDF {
  private final String desc;

  /**
   * Creates a CDF from a string representation.
   * <p>
   * <code>
   *   x1 y1
   *   x2 y2
   *   ...
   * </code>
   * Point (x1,y1) specifies that bucket [x1, x2) contains y1-0 values.
   * Point (x2,y2) specifies that bucket [x2, x3) contains y2-y1 values.
   *
   * @param s a string representation
   * @return a {@link DiscreteCDF}
   */
  public static DiscreteCDF parse(String desc, String s) {
    Point[] points = s.lines().map(line -> {
      String[] split = line.split(" ");
      return new Point(Float.parseFloat(split[0]), Float.parseFloat(split[1]));
    }).toArray(Point[]::new);
    return new DiscreteCDF(desc, points);
  }

  /**
   * Creates a CDF from values with a specified rank.
   * <p>
   * <code>
   *   x1 y1
   *   x2 y2
   *   ...
   * </code>
   * Point (x1, y1) specifies that value x1 has rank y1.
   * @param s a string representation
   * @return a {@link DiscreteCDF}
   */
  public static DiscreteCDF fromRanks(String desc, String s) {
    Point[] rankPoints = s.lines().map(line -> {
      String[] split = line.split(" ");
      return new Point(Float.parseFloat(split[0]), Float.parseFloat(split[1]));
    }).toArray(Point[]::new);

    // convert to CDF points
    Point[] points = new Point[rankPoints.length];
    for(int i=0; i<points.length; i++) {
      float x = rankPoints[i].x();
      float y = rankPoints[Math.min(i+1, points.length-1)].y();
      points[i] = new Point(x, y);
    }
    return new DiscreteCDF(desc, points);
  }

  static class Datastream implements FloatIterator {
    private Point[] ps;
    private int[] remaining;
    private int remainingCount;
    public Random rnd;

    @Override
    public float nextValue() {
      if (remainingCount <= 0) {
        throw new IllegalStateException("The datastream has already been consumed");
      }
      int v = rnd.nextInt(remainingCount);
      for (int i=0; i<ps.length; i++) {
        if (v<remaining[i]) {
          remaining[i] -= 1;
          remainingCount -= 1;
          float start = ps[i].x();
          float end = ps[i+1].x();
          return rnd.nextFloat(start, Math.nextUp(end));
        }
        v -= remaining[i];
      }
      throw new IllegalStateException();
    }

    @Override
    public boolean hasNext() {
      return remainingCount > 0;
    }
  }

  // TODO tr add comment, the x-intervals are right half-open intervals, [start,end),
  //  so the start is included, but the end is not.
  // Point (x_i,y_i) specifies that P[x_i <= v < x_(i+1)] = y_i
  private final Point[] ps;

  public DiscreteCDF(String description, Point... points) {
    ps = new Point[points.length];
    System.arraycopy(points, 0, ps, 0, points.length);
    Arrays.sort(ps);

    var y = 0f;
    for (Point p : ps) {
      if (p.y() < 0 || p.y() > 1) {
        throw new IllegalArgumentException("y-values (normalized rank) must be in the interval [0,1]");
      }
      if (p.y() < y) {
        throw new IllegalArgumentException("y-values (normalized rank) must be monotonically non-decreasing");
      }
      y = p.y();
    }

    if (ps[ps.length-1].y() != 1) throw new IllegalArgumentException("there must be a point with rank 1");

    this.desc = description;
  }

  public String getDesc() {
    return "discrete CDF " + desc
        + " start=" + ps[0].x() + " end="+ps[ps.length-1].x()+" points="+ps.length;
  }

  public Supplier<Datastream> prepareStream(int n, Supplier<Random> rndSupplier) {
    int[] bucketSize = new int[ps.length-1];
    float[] remaining = new float[ps.length-1];
    float remainingTotal = 0;
    int count = 0;
    var y = 0f;
    for (int i=0; i<ps.length-1; i++) {
      var p = ps[i];
      var part = p.y() - y;
      y = p.y();
      float exact = n * part;
      bucketSize[i] = (int) Math.floor(exact);
      remaining[i] = exact - bucketSize[i];
      remainingTotal += remaining[i];
      count += bucketSize[i];
    }

    Random random = rndSupplier.get();
    int diff = n-count;
    outer: for (int i=0; i<diff; i++) {
      if (remainingTotal <= 0) {
        int bi = random.nextInt(ps.length-1);
        bucketSize[bi] += 1;
        continue;
      }

      float v = random.nextFloat(0, remainingTotal);
      for (int j=0; j<ps.length-1; j++) {
        if (v < remaining[j]) {
          bucketSize[j] += 1;
          remainingTotal -= remaining[j];
          remaining[j] = 0;
          continue outer;
        }
        v -= remaining[j];
      }
    }

    return () -> {
      Datastream ds = new Datastream();
      ds.ps = new Point[ps.length];
      System.arraycopy(ps, 0, ds.ps, 0, ps.length);
      ds.remaining = Arrays.copyOf(bucketSize, bucketSize.length);
      ds.remainingCount = n;
      ds.rnd = rndSupplier.get();
      return ds;
    };
  }


  public float[] getBuckets() {
    float[] buckets = new float[ps.length];
    for (int i=0; i<ps.length; i++) {
      buckets[i] = ps[i].x();
    }
    return buckets;
  }
}
