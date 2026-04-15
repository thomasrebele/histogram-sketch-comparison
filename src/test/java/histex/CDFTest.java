package histex;

import histex.sketches.FixedBucketHistogram;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDFTest {
  @Test
  void testSample() {
    DiscreteCDF cdf = DiscreteCDF.fromRanks("cdf1", """
        100 0
        200 0.3
        300 0.5
        800 0.5
        1000 1.0
        """);

    int n = 100;
    DiscreteCDF.Datastream ds = cdf.prepareStream(n, () -> new Random(123)).get();

    float[] result = new float[n];
    for (int i=0; i<n; i++) {
      result[i] = ds.nextValue();
    }
    IllegalStateException ex = assertThrows(IllegalStateException.class, ds::nextValue);
    assertTrue(ex.getMessage().contains("already been consumed"));

    assertEquals(30, countLE(result, 200f));
    assertEquals(50, countLE(result, 300f));
    assertEquals(50, countLE(result, 800f));
    assertEquals(100, countLE(result, 1000f));
  }

  private static int countLE(float[] values, float cmp) {
    var c = 0;
    for (float v : values) {
      if (v <= cmp) {
        c += 1;
      }
    }
    return c;
  }

  @Test
  void testConstruction() {
    DiscreteCDF cdf1 = DiscreteCDF.parse("cdf1", """
        100 0.3
        200 0.5
        300 0.5
        800 1.0
        1000 1.0
        """);

    DiscreteCDF cdf2 = DiscreteCDF.fromRanks("cdf2", """
        100 0
        200 0.3
        300 0.5
        800 0.5
        1000 1.0
        """);

    int n = 100;
    DiscreteCDF.Datastream ds1 = cdf1.prepareStream(n, ()->new Random(123)).get();
    DiscreteCDF.Datastream ds2 = cdf2.prepareStream(n, ()->new Random(123)).get();

    for (int i=0; i<n; i++) {
      float f1 = ds1.nextValue();
      float f2 = ds2.nextValue();
      assertEquals(f1, f2);
    }

    assertEquals(ds1.hasNext(), ds2.hasNext());
  }

  @Test
  void testDistribution() {
    DiscreteCDF cdf = DiscreteCDF.fromRanks("cdf1", """
        100 0
        200 0.3
        300 0.5
        800 0.5
        1000 1.0
        """);

    int n = 100;
    DiscreteCDF.Datastream ds = cdf.prepareStream(n, ()->new Random(123)).get();
    FixedBucketHistogram h = new FixedBucketHistogram(cdf.getBuckets(), false);

    for (int i=0; i<n; i++) {
      h.addValue(ds.nextValue());
    }

    List<Integer> cumulativeCounts = h.getCumulativeCounts();
    assertEquals(Arrays.asList(30, 50, 50, 100), cumulativeCounts);
  }
}
