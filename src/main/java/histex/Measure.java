package histex;

public class Measure {

  public static double evaluateMultiplicativeDifference(Histogram h1, Histogram h2, float[] values) {
    double diff = 0;

    for (float v : values) {
      double r1 = h1.getRank(v);
      double r2 = h2.getRank(v);

      if (r2 > r1) {
        double tmp = r2;
        r2 = r1;
        r1 = tmp;
      }

      System.out.println("ratio: " + (r1/r2));
      diff += Math.log(r1) - Math.log(r2);
    }
    diff /= Math.log(10);
    return diff / values.length;
  }

}
