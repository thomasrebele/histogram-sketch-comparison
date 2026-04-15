package histex;

import histex.sketches.FixedBucketHistogram;
import histex.sketches.KllHistogram;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HistogramTest {

  @Test
  void testFixedBucketHistogram() {
    FixedBucketHistogram h = new FixedBucketHistogram(new float[]{10, 50, 500, 1000}, false);
    assertThrows(IllegalStateException.class, () -> {
      h.addValue(9);
    });

    h.addValue(11);
    assertEquals(Arrays.asList(1, 0, 0), h.getCounts());
    h.addValue(50);
    h.addValue(Math.nextDown(500));
    assertEquals(Arrays.asList(1, 2, 0), h.getCounts());
    h.addValue(500);
    h.addValue(Math.nextDown(1000));
    assertEquals(Arrays.asList(1, 2, 2), h.getCounts());

    assertThrows(IllegalStateException.class, () -> {
      h.addValue(1000f);
    });

    assertEquals(0.2, h.getNormalizedRank(10));
    assertEquals(0.2, h.getNormalizedRank(11));
    assertEquals(0.2, h.getNormalizedRank(40.9f));
    assertEquals(0.2, h.getNormalizedRank(49.9f));
    assertEquals(0.6, h.getNormalizedRank(50f));
  }


  @Test
  void testKllHistogram() {
    KllHistogram h = new KllHistogram();
    h.addValue(10);
    h.addValue(50);
    h.addValue(50);
    h.addValue(500);
    h.addValue(500);

    assertEquals(0.2, h.getNormalizedRank(10));
    assertEquals(0.2, h.getNormalizedRank(11));
    assertEquals(0.2, h.getNormalizedRank(40.9f));
    assertEquals(0.2, h.getNormalizedRank(49.9f));
    assertEquals(0.6, h.getNormalizedRank(50));
    assertEquals(0.6, h.getNormalizedRank(50.1f));
    assertEquals(0.6, h.getNormalizedRank(499.9f));
    assertEquals(1.0, h.getNormalizedRank(500));
    assertEquals(1.0, h.getNormalizedRank(500.1f));
  }


  @Test
  void testGoldstandard() {
    float[] values = { 10, 50, 50, 500, 500 };
    var h = new GoldstandardRankEstimator(() -> FloatIterator.of(values));

    assertEquals(0.2, h.getNormalizedRank(10));
    assertEquals(0.2, h.getNormalizedRank(11));
    assertEquals(0.2, h.getNormalizedRank(40.9f));
    assertEquals(0.2, h.getNormalizedRank(49.9f));
    assertEquals(0.6, h.getNormalizedRank(50));
    assertEquals(0.6, h.getNormalizedRank(50.1f));
    assertEquals(0.6, h.getNormalizedRank(499.9f));
    assertEquals(1.0, h.getNormalizedRank(500));
    assertEquals(1.0, h.getNormalizedRank(500.1f));
  }


  @Test
  void testEquiHeight() {
    float[] values = { 10, 11, 12, 13, 50 };
    var gs = new GoldstandardRankEstimator(() -> FloatIterator.of(values));
    var ehHist = gs.convertToEquiHeightHistogram(5, false);
    ehHist.consume(FloatIterator.of(values));

    assertEquals(0.2, ehHist.getNormalizedRank(10));
    assertEquals(0.4, ehHist.getNormalizedRank(11));
    assertEquals(0.6, ehHist.getNormalizedRank(12));
    assertEquals(0.8, ehHist.getNormalizedRank(13));
    assertEquals(1.0, ehHist.getNormalizedRank(50));
  }
}
