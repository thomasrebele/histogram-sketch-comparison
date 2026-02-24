package histex;

public class Measure {

  public static double evaluateMultiplicativeDifference(RankEstimator h1, RankEstimator h2, float[] values) {
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

  public record Result(double scaleDiff, int ranges, int zeroH1, int zeroH2, int zeroBoth) {
    @Override
    public String toString() {
      return "Result{"
          + "scaleDiff=" + String.format("%4.4f", scaleDiff)
          + ", ranges=" + ranges
          + ", zeroH1=" + zeroH1
          + ", zeroH2=" + zeroH2
          + ", zeroBoth=" + zeroBoth + '}';
    }
  }

  public static Result evaluateMultiplicativeSelectivityDifference(RankEstimator h1, RankEstimator h2, FloatIterator values, float rangeWidth) {
    double diff = 0;

    int zeroH1 = 0;
    int zeroH2 = 0;
    int zeroBoth = 0;
    int valid = 0;

    int count = 0;
    while (values.hasNext()) {
      float v = values.nextValue();
      count += 1;

      double r1 = h1.getRank(v+rangeWidth) - h1.getRank(v);
      double r2 = h2.getRank(v+rangeWidth) - h2.getRank(v);
      r1 = Math.max(r1, h1.getMinRankDifference());
      r2 = Math.max(r2, h2.getMinRankDifference());
      if (r1 == 0 && r2 == 0) {
        zeroBoth += 1;
        continue;
      }
      else if (r1 == 0) {
        zeroH1 += 1;
        continue;
      }
      else if (r2 == 0) {
        zeroH2 += 1;
        continue;
      }

      if (r2 > r1) {
        double tmp = r2;
        r2 = r1;
        r1 = tmp;
      }

      //System.out.println("ratio: " + (r1/r2));
      diff += Math.log(r1) - Math.log(r2);
      valid++;
    }
    diff /= Math.log(10);
    return new Result(diff / valid, count, zeroH1, zeroH2, zeroBoth);
  }

}
