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

  public static List<Point> extractInterestingPoints(List<Point> points) {
    if (points == null || points.size() < 3) {
      return points == null ? new ArrayList<>() : new ArrayList<>(points);
    }

    double epsilon = 0.01;
    return extractInterestingPoints(points, epsilon);
  }

  public static List<Point> extractInterestingPoints(List<Point> points, double epsilon) {
    if (points == null || points.size() < 3) {
      return points == null ? new ArrayList<>() : new ArrayList<>(points);
    }

    int index = 0;
    double maxDist = 0;

    Point start = points.get(0);
    Point end = points.get(points.size() - 1);

    // Find the point with the maximum perpendicular distance from the line segment
    for (int i = 1; i < points.size() - 1; i++) {
      double dist = perpendicularDistance(points.get(i), start, end);
      if (dist > maxDist) {
        maxDist = dist;
        index = i;
      }
    }

    List<Point> result = new ArrayList<>();

    // If the max distance is greater than the tolerance, recursively simplify
    if (maxDist > epsilon) {
      List<Point> left = extractInterestingPoints(points.subList(0, index + 1), epsilon);
      List<Point> right = extractInterestingPoints(points.subList(index, points.size()), epsilon);

      result.addAll(left);
      result.remove(result.size() - 1); // Avoid duplicating the shared vertex
      result.addAll(right);
    } else {
      // Discard all points between start and end
      result.add(start);
      result.add(end);
    }

    return result;
  }

  private static double perpendicularDistance(Point pt, Point lineStart, Point lineEnd) {
    double dx = lineEnd.x() - lineStart.x();
    double dy = lineEnd.y() - lineStart.y();
    if (dx == 0 && dy == 0) {
      return Math.hypot(pt.x() - lineStart.x(), pt.y() - lineStart.y());
    }

    // Distance formula: |dy*x - dx*y + x2*y1 - y2*x1| / sqrt(dx^2 + dy^2)
    double numerator = Math.abs(dy * pt.x() - dx * pt.y() + lineEnd.x() * lineStart.y() - lineEnd.y() * lineStart.x());
    double denominator = Math.hypot(dx, dy);

    return numerator / denominator;
  }
}
