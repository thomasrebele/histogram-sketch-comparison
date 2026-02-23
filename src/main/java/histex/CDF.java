package histex;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;

public class CDF {
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
   * @return a {@link CDF}
   */
  public static CDF parse(String s) {
    Point[] points = s.lines().map(line -> {
      String[] split = line.split(" ");
      return new Point(Float.parseFloat(split[0]), Float.parseFloat(split[1]));
    }).toArray(Point[]::new);
    return new CDF(points);
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
   * @return a {@link CDF}
   */
  public static CDF fromRanks(String s) {
    Point[] rankPoints = s.lines().map(line -> {
      String[] split = line.split(" ");
      return new Point(Float.parseFloat(split[0]), Float.parseFloat(split[1]));
    }).toArray(Point[]::new);

    // convert to CDF points
    Point[] points = new Point[rankPoints.length];
    for(int i=0; i<points.length; i++) {
      float x = rankPoints[i].x;
      float y = rankPoints[Math.min(i+1, points.length-1)].y;
      points[i] = new Point(x, y);
    }
    return new CDF(points);
  }

  static class Datastream {
    private Point[] ps;
    private int[] remaining;
    private int remainingCount;
    public Random rnd;

    public float nextValue() {
      if (remainingCount == 0) {
        throw new IllegalStateException("The datastream has already been consumed");
      }
      int v = rnd.nextInt(remainingCount);
      for (int i=0; i<ps.length; i++) {
        if (v<remaining[i]) {
          remaining[i] -= 1;
          remainingCount -= 1;
          float start = ps[i].x;
          float end = ps[i+1].x;
          return rnd.nextFloat(start, Math.nextUp(end));
        }
        v -= remaining[i];
      }
      throw new IllegalStateException();
    }

    public boolean hasValue() {
      return remainingCount > 0;
    }
  }

  record Point(float x, float y) implements Comparable<Point> {
    @Override
    public int compareTo(Point o) {
      return Float.compare(this.x, o.x);
    }
  }

  // TODO tr add comment, the x-intervals are right half-open intervals, [start,end),
  //  so the start is included, but the end is not.
  // Point (x_i,y_i) specifies that P[x_i <= v < x_(i+1)] = y_i
  private final Point[] ps;

  public CDF(Point... points) {
    ps = new Point[points.length];
    System.arraycopy(points, 0, ps, 0, points.length);
    Arrays.sort(ps);

    var y = 0f;
    for (Point p : ps) {
      if (p.y < 0 || p.y > 1) {
        throw new IllegalArgumentException("y-values (normalized rank) must be in the interval [0,1]");
      }
      if (p.y < y) {
        throw new IllegalArgumentException("y-values (normalized rank) must be monotonically non-decreasing");
      }
      y = p.y;
    }

    if (ps[ps.length-1].y != 1) throw new IllegalArgumentException("there must be a minimal point with rank 1");
  }

  public Supplier<Datastream> prepareStream(int n, Supplier<Random> rndSupplier) {
    int[] remaining = new int[ps.length+1];
    var max = 0;
    var maxi = 0;
    int count = 0;
    var y = 0f;
    for (int i=0; i<ps.length; i++) {
      var p = ps[i];
      var part = p.y - y;
      y = p.y;

      remaining[i] = Math.round(n * part);
      if (remaining[i] > max) {
        max = remaining[i];
        maxi = i;
      }

      count += remaining[i];
    }

    int diff = n-count;
    System.out.println("difference: " + diff);
    remaining[maxi] -= diff;

    return () -> {
      Datastream ds = new Datastream();
      ds.ps = new Point[ps.length];
      System.arraycopy(ps, 0, ds.ps, 0, ps.length);
      ds.remaining = Arrays.copyOf(remaining, remaining.length);
      ds.remainingCount = n;
      ds.rnd = rndSupplier.get();
      return ds;
    };
  }


  public float[] getBuckets() {
    float[] buckets = new float[ps.length];
    for (int i=0; i<ps.length; i++) {
      buckets[i] = ps[i].x;
    }
    return buckets;
  }
}
