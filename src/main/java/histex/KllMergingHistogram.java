package histex;

import org.apache.datasketches.kll.KllFloatsSketch;
import org.apache.datasketches.quantilescommon.FloatsSketchSortedView;

import java.util.Arrays;

public class KllMergingHistogram implements Histogram {

  private final KllFloatsSketch kll;

  public KllMergingHistogram() {
    this.kll = KllFloatsSketch.newHeapInstance();
  }

  @Override
  public String getDesc() {
    return "kll_merging(k=" + kll.getK() + ")";
  }

  @Override
  public long getN() {
    return kll.getN();
  }

  @Override
  public void addValue(float f) {
    KllFloatsSketch kll2 = KllFloatsSketch.newHeapInstance();
    kll2.update(f);
    this.kll.merge(kll2);
  }

  @Override
  public double getNormalizedRank(float v) {
    return kll.getRank(v);
  }

  @Override
  public double getMinNormalizedRankDifference(float start, float end) {
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

  @Override
  public String debugAsCsv() {
    FloatsSketchSortedView sv = kll.getSortedView();
    StringBuilder sb = new StringBuilder();
    sb.append("x,y\n");
    for(int i=0; i<sv.getQuantiles().length; i++) {
      float v = sv.getQuantiles()[i];
      double normalizedRank = getNormalizedRank(v);
      sb.append(v).append(",").append(normalizedRank).append("\n");
    }
    return sb.toString();
  }
}
