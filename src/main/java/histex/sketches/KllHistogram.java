package histex.sketches;

import histex.HistSampling;
import histex.Histogram;
import histex.Point;
import org.apache.datasketches.kll.KllFloatsSketch;
import org.apache.datasketches.quantilescommon.FloatsSketchSortedView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    List<Point> points = new ArrayList<>();
    for(int i=0; i<sv.getQuantiles().length; i++) {
      float v = sv.getQuantiles()[i];
      if (!points.isEmpty() && points.getLast().x() == v) {
        continue;
      }

      double normalizedRank = getNormalizedRank(v);
      points.add(new Point(v, (float) normalizedRank));
    }
    return Point.toCsv(HistSampling.extractInterestingPoints(points));
  }
}
