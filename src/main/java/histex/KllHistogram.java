package histex;

import org.apache.datasketches.kll.KllFloatsSketch;

import java.util.Arrays;

public class KllHistogram implements Histogram {

  private final KllFloatsSketch kll;

  public KllHistogram() {
    this.kll = KllFloatsSketch.newHeapInstance();
  }

  @Override
  public void addValue(float f) {
    kll.update(f);
  }

  @Override
  public double getRank(float v) {
    return kll.getRank(v);
  }

  @Override
  public double getMinRankDifference() {
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
}
