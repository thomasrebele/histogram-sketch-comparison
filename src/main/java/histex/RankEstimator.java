package histex;

public interface RankEstimator {

  String getDesc();

  /**
   * Get the normalized rank of the value v.
   * @return a value between 0 and 1 (both inclusive)
   */
  double getNormalizedRank(float v);

  default double getMinNormalizedRankDifference(float start, float end) {
    return 0.0;
  }

  default String debugAsCsv() {
    return null;
  }

  default String debugDiffAsCsv(GoldstandardRankEstimator goldstandard) {
    float min = goldstandard.getMin();
    float max = goldstandard.getMax();

    StringBuilder sb = new StringBuilder();
    sb.append("x,y\n");
    for(int i=0; i<100; i++) {
      float v = min + (float)i/100 * (max-min);
      sb.append(v).append(",").append(getNormalizedRank(v) - goldstandard.getNormalizedRank(v)).append("\n");
    }
    return sb.toString();
  }
}
