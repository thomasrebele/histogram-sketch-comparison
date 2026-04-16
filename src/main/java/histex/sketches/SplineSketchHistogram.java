package histex.sketches;

import histex.HistSampling;
import histex.Histogram;
import histex.Point;
import histex.sketches.splinesketch.SplineSketch;

import java.util.Arrays;
import java.util.List;

public class SplineSketchHistogram implements Histogram {

  private final SplineSketch sketch;
  private float min = Float.MAX_VALUE;
  private float max = -Float.MAX_VALUE;

  public SplineSketchHistogram(int k) {
    this.sketch = new SplineSketch(k);
  }

  @Override
  public void addValue(float v) {
    this.sketch.update(v);
    min = Math.min(min, v);
    max = Math.max(max, v);
  }

  @Override
  public int getMemoryUsageInBytes() {
    return this.sketch.serializedSketchBytesUpdatable();
  }

  @Override
  public long getN() {
    return this.sketch.getN();
  }

  @Override
  public String getDesc() {
    return "spline(" + sketch.getK() + ")";
  }

  @Override
  public double getNormalizedRank(float v) {
    List<Integer> query = this.sketch.query(Arrays.asList((double) v));
    return (double)query.getFirst() / getN();
  }

  @Override
  public String debugAsCsv() {
    List<Point> points = HistSampling.localDynamic(min, max, 12, this);
    return Point.toCsv(HistSampling.extractInterestingPoints(points, this));
  }


}
