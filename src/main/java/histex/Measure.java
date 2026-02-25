package histex;

import org.apache.datasketches.sampling.ReservoirItemsSketch;

public class Measure {

  public static double evaluateMultiplicativeDifference(RankEstimator h1, RankEstimator h2, float[] values) {
    double diff = 0;

    for (float v : values) {
      double r1 = h1.getNormalizedRank(v);
      double r2 = h2.getNormalizedRank(v);

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

  public record Result(double scaleDiff, int ranges, int zeroH1, int zeroH2, int zeroBoth, RatioResult[] samples) {
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

  public record RatioResult(double r1, double r2, double r1s, double r1e, double r2s, double r2e) {
    @Override
    public String toString() {
      return "r1 " + r1 + "  r2 " + r2 + "   r1: [" + r1s + "," + r1e  + "], r2: [" + r2s + "," + r2e + "]";
    }
  }

  public static Result evaluateMultiplicativeSelectivityDifference(RankEstimator h1, RankEstimator h2, FloatIterator values, float rangeWidth) {
    ReservoirItemsSketch<RatioResult> sample = ReservoirItemsSketch.newInstance(20);

    double diff = 0;

    int zeroH1 = 0;
    int zeroH2 = 0;
    int zeroBoth = 0;
    int valid = 0;

    int count = 0;
    while (values.hasNext()) {
      float v = values.nextValue();
      count += 1;

      double r1e = h1.getNormalizedRank(v + rangeWidth);
      double r1s = h1.getNormalizedRank(v);
      double r1 = r1e - r1s;
      double r2e = h2.getNormalizedRank(v + rangeWidth);
      double r2s = h2.getNormalizedRank(v);
      double r2 = r2e - r2s;
      sample.update(new RatioResult(r1, r2, r1s, r1e, r2s, r2e));

      r1 = Math.max(r1, h1.getMinNormalizedRankDifference());
      r2 = Math.max(r2, h2.getMinNormalizedRankDifference());
      //System.out.println("ratio: " + (r1/r2) + "      r1 " + r1 + "  r2 " + r2 + "   r1es " + r1e + " " + r1s + "  r2es " + r2e + " " + r2s);
      if (r1 == 0 && r2 == 0) {
        zeroBoth += 1;
      }
      if (r1 == 0) {
        zeroH1 += 1;
      }
      if (r2 == 0) {
        zeroH2 += 1;
      }
      if (r1 == 0 || r2 == 0) {
        continue;
      }

      if (r2 > r1) {
        double tmp = r2;
        r2 = r1;
        r1 = tmp;
      }

      diff += Math.log(r1) - Math.log(r2);
      valid++;
    }
    diff /= Math.log(10);

    return new Result(diff / valid, count, zeroH1, zeroH2, zeroBoth, sample.getSamples());
  }

}
