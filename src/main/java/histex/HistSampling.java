package histex;

import java.util.ArrayList;
import java.util.Collections;
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

  public static List<Point> extractInterestingPoints(List<Point> points, Histogram hist) {
    if (points == null || points.size() < 3) {
      return points == null ? new ArrayList<>() : new ArrayList<>(points);
    }
    Collections.sort(points);

    double width = points.getLast().x()-points.getFirst().x();

    //List<Point> result = new ArrayList<>();
    //double epsilon = 0.001;
    //extractInterestingPoints(points, epsilon, width, hist, result);
    //result.add(points.getLast());

    List<Point> result = new ArrayList<>(points);
    //removeSuperfluousPoints(result, width);
    return result;
  }

  private static void extractInterestingPoints(List<Point> points, double epsilon, double width, Histogram hist, List<Point> result) {
    int index = 0;
    double maxDist = 0;

    Point start = points.get(0);
    Point end = points.get(points.size() - 1);

    // Find the point with the maximum perpendicular distance from the line segment
    for (int i = 1; i < points.size() - 1; i++) {
      double dist = perpendicularDistance(points.get(i), start, end, width);
      if (dist > maxDist) {
        maxDist = dist;
        index = i;
      }
    }

    // If the max distance is greater than the tolerance, recursively simplify
    if (maxDist > epsilon) {
      extractInterestingPoints(points.subList(0, index + 1), epsilon, width, hist, result);
      extractInterestingPoints(points.subList(index, points.size()), epsilon, width, hist, result);
    } else {
      Point p = points.getFirst();
      float prev = Math.nextDown(p.x());
      float rank = (float) hist.getNormalizedRank(prev);
      if (Math.abs(rank-p.y()) >= 0.005) {
        Point p1 = new Point(prev, rank);
        result.add(p1);
      }
      result.add(p);
    }
  }

  private static double perpendicularDistance(Point pt, Point lineStart, Point lineEnd, double width) {
    double dx = lineEnd.x() - lineStart.x();
    double dy = lineEnd.y() - lineStart.y();
    if (dx == 0 && dy == 0) {
      return Math.hypot((pt.x() - lineStart.x()) / width, pt.y() - lineStart.y());
    }

    // Distance formula: |dy*x - dx*y + x2*y1 - y2*x1| / sqrt(dx^2 + dy^2)
    double numerator = Math.abs(dy * pt.x() - dx * pt.y() + lineEnd.x() * lineStart.y() - lineEnd.y() * lineStart.x())/width;
    double denominator = Math.hypot(dx/width, dy);

    return numerator / denominator;
  }

  static void removeSuperfluousPoints(List<Point> result, double width) {
    int end = result.size()-1;
    int current = result.size()-2;
    while (current-->0) {
      // no intermediate points
      if (current+1 == end) {
        continue;
      }
      // collect the variance of the angles
      Point p1 = result.get(current);
      Point p2 = result.get(end);
      double angle = Point.normangle(p1, p2, width);
      double angle1 = 0;
      double angle2 = 0;
      for (int i = current + 1; i < end; i++) {
        double a = Point.normangle(p1, result.get(i), width);
        double adiff = angle-a;
        angle1 = Math.min(angle1, adiff);
        angle2 = Math.max(angle2, adiff);
      }

      // check if all angles are similar
      double threshold = Math.PI * 2 / 360 * 2;
      if (-angle1 < threshold && angle2 < threshold) {
        continue;
      }

      // current does not belong to the "line"
      int start = current+1;
      // remove intermediate points
      result.subList(start+1, end).clear();
      // the start of the line becomes the new end
      end = start;
    }

    if (1 < end) {
      result.subList(1, end).clear();
    }
  }
}
