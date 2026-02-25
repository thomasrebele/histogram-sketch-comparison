package histex;

import org.apache.datasketches.tdigest.TDigestDouble;

public class TDigestHistogram implements Histogram {

  private final TDigestDouble tdigest;

  public TDigestHistogram() {
    this.tdigest = new TDigestDouble((short)120);

  }

  @Override
  public String getDesc() {
    return "tdigest(k=" + tdigest.getK() + ")";
  }

  @Override
  public void addValue(float v) {
    tdigest.update(v);
  }

  @Override
  public double getNormalizedRank(float v) {
    return tdigest.getRank(v);
  }

  @Override
  public int getMemoryUsageInBytes() {
    return tdigest.toByteArray().length;
  }
}
