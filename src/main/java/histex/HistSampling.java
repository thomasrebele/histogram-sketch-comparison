package histex;

import java.util.ArrayList;
import java.util.List;

public class HistSampling {
  static List<Point> equiWidth(float min, float max, int n, Histogram hist) {
    List<Point> points = new ArrayList<>();
    for(int i=0; i<n; i++) {
      float range = (float) (max - min);
      float v = (float) (min+(i*range/200));
      float normalizedRank = (float) hist.getNormalizedRank(v);
      points.add(new Point(v, normalizedRank));
    }
    return points;
  }

  public static List<Point> localDynamic(float min, float max, int depthLimit, Histogram hist) {
    List<Point> points = new ArrayList<>();
    enter(points, Math.nextDown(min), 0, Math.nextUp(max), 1, 12, hist);
    points.add(new Point(max, (float) hist.getNormalizedRank(max)));
    return points;
  }

  private static void enter(List<Point> points, float a, float aRank, float b, float bRank, int remainingDepth,
      Histogram hist) {
    // terminate recursion
    if (remainingDepth == 0 || a == b || bRank-aRank < 0.005) {
      boolean sameHeight = !points.isEmpty() && points.getLast().y() == aRank
          && points.size() > 1 && points.getLast().y() == points.get(points.size()-2).y();
      if (sameHeight) {
        // update last point
        points.set(points.size() - 1, new Point(a, aRank));
      }
      else {
        // add new point
        points.add(new Point(a, aRank));
      }
      return;
    }

    float mid = a+(b-a)/2;
    float midRank = (float) hist.getNormalizedRank(mid);

    enter(points, a, aRank, mid, midRank, remainingDepth-1, hist);
    enter(points, mid, midRank, b, bRank, remainingDepth-1, hist);
  }
}
