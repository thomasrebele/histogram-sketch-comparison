package histex;

import org.apache.datasketches.tdigest.TDigestDouble;

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
}
