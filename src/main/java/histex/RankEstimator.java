package histex;

public interface RankEstimator {

  double getRank(float v);

  default double getMinRankDifference() {
    return 0.0;
  }
}
