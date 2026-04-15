package histex;

import java.util.List;

public record Point(float x, float y) implements Comparable<Point> {
  @Override
  public int compareTo(Point o) {
    return Float.compare(this.x, o.x);
  }

  public static String toCsv(List<Point> points) {
    StringBuilder sb = new StringBuilder();
    sb.append("x,y\n");
    for (Point p : points) {
      if (Float.isNaN(p.x) || Float.isNaN(p.y)) {
        continue;
      }
      sb.append(p.x()).append(",").append(p.y()).append("\n");
    }
    return sb.toString();
  }

  public static double dist(Point p1, Point p2) {
    return Math.hypot(p1.x-p2.x, p1.y-p2.y);
  }

  public static double angle(Point p1, Point p2) {
    double dy = p2.y-p1.y;
    double dx = p2.x-p1.x;

    return Math.atan2(dy, dx);
  }
}
