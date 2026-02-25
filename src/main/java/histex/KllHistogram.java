package histex;

import org.apache.datasketches.kll.KllFloatsSketch;

import java.util.Arrays;

public class KllHistogram implements Histogram {

  private final KllFloatsSketch kll;

  public KllHistogram() {
    this.kll = KllFloatsSketch.newHeapInstance();
  }

  @Override
  public String getDesc() {
    return "kll(k=" + kll.getK() + ")";
  }

  @Override
  public long getN() {
    return kll.getN();
  }

  @Override
  public void addValue(float f) {
    kll.update(f);
  }

  @Override
  public double getNormalizedRank(float v) {
    return kll.getRank(v);
  }

  @Override
  public double getMinNormalizedRankDifference() {
    return kll.getNormalizedRankError(false);
  }

  @Override
  public int getMemoryUsageInBytes() {
    return kll.getSerializedSizeBytes();
  }

  public void debug() {
    System.out.println(
        Arrays.toString(kll.getSortedView().getCumulativeWeights()));
  }

  public String getInfo() {
    return "[" + kll.getMinItem() + "," + kll.getMaxItem() + "]";
  }
}
