package histex;

import org.apache.datasketches.tdigest.TDigestDouble;

import java.util.List;

public class TDigestHistogram implements Histogram {

  private final TDigestDouble tdigest;
  private int n = 0;

  public TDigestHistogram() {
    this.tdigest = new TDigestDouble((short)120);
  }

  @Override
  public long getN() {
    return n;
  }

  @Override
  public String getDesc() {
    return "tdigest(k=" + tdigest.getK() + ")";
  }

  @Override
  public void addValue(float v) {
    tdigest.update(v);
    n+=1;
  }

  @Override
  public double getNormalizedRank(float v) {
    return tdigest.getRank(v);
  }

  @Override
  public int getMemoryUsageInBytes() {
    return tdigest.toByteArray().length;
  }

  @Override
  public String debugAsCsv() {

    StringBuilder sb = new StringBuilder();
    sb.append("x,y\n");
    List<Point> points = HistSampling.localDynamic((float) tdigest.getMinValue(), (float) tdigest.getMaxValue(), 10, this);
    for (Point p : points) {
      sb.append(p.x()).append(",").append(p.y()).append("\n");
    }
    return sb.toString();
  }

  public String getInfo() {
    return "[" + tdigest.getMinValue() + "," + tdigest.getMaxValue() + "]";
  }

  public double getMin() {
    return tdigest.getMinValue();
  }

  public double getMax() {
    return tdigest.getMaxValue();
  }
}
