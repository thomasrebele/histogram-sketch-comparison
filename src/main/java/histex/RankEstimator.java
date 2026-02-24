package histex;

public interface RankEstimator {

  /**
   * Get the normalized rank of the value v.
   * @return a value between 0 and 1 (both inclusive)
   */
  double getNormalizedRank(float v);

  default double getMinNormalizedRankDifference() {
    return 0.0;
  }
}
