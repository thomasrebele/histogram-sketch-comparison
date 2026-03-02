package histex;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HistogramTest {

  @Test
  void test() {
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
  }

  @Test
  void testEquiHeight() {
    float[] values = { 10, 11, 12, 13, 50 };
    var gs = new GoldstandardRankEstimator(() -> FloatIterator.of(values));
    var ehHist = gs.convertToEquiHeightHistogram(5, false);
    ehHist.consume(FloatIterator.of(values));

    assertEquals(0.0, ehHist.getNormalizedRank(10));
    assertEquals(0.2, ehHist.getNormalizedRank(11));
    assertEquals(0.4, ehHist.getNormalizedRank(12));
    assertEquals(0.6, ehHist.getNormalizedRank(13));
    assertEquals(0.8, ehHist.getNormalizedRank(50));
  }
}
