package histex;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HistSamplingTest {
  @Test
  void removeSuperfluousPoints1() {
    float width = 10000.0f;

    List<Point> points = new ArrayList<>();
    Point p0 = new Point(1001f, 0.0f);
    points.add(p0);
    points.add(new Point(1004f, 0.09f));
    Point p2 = new Point(1005f, 0.10f);
    points.add(p2);
    Point p3 = new Point(2001f, 0.10f);
    points.add(p3);
    points.add(new Point(2004f, 0.19f));
    Point p5 = new Point(2005f, 0.20f);
    points.add(p5);
    Point p6 = new Point(3001f, 0.20f);
    points.add(p6);
    Point p7 = new Point(3004f, 0.30f);
    points.add(p7);

    HistSampling.removeSuperfluousPoints(points, width);

    assertEquals(points, List.of(p0, p2, p3, p5, p6, p7));
  }

  @Test
  void removeSuperfluousPoints2() {
    float width = 10000.0f;

    List<Point> points = new ArrayList<>();
    Point p0 = new Point(1001f, 0.0f);
    points.add(p0);
    Point p2 = new Point(1005f, 0.10f);
    points.add(p2);
    Point p3 = new Point(2001f, 0.10f);
    points.add(p3);
    points.add(new Point(2004f, 0.19f));
    Point p5 = new Point(2005f, 0.20f);
    points.add(p5);
    Point p6 = new Point(3001f, 0.20f);
    points.add(p6);
    Point p7 = new Point(3004f, 0.30f);
    points.add(p7);

    HistSampling.removeSuperfluousPoints(points, width);

    assertEquals(points, List.of(p0, p2, p3, p5, p6, p7));
  }
}
