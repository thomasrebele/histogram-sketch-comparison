package histex;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDFTest {
  @Test
  void testSample() {
    CDF cdf = CDF.fromRanks("""
        100 0
        200 0.3
        300 0.5
        800 0.5
        1000 1.0
        """);

    int n = 100;
    Random rnd = new Random(123);
    CDF.Datastream ds = cdf.makeStream(n, rnd);

    float[] result = new float[n];
    for (int i=0; i<n; i++) {
      result[i] = ds.nextValue();
    }
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ds.nextValue());
    assertTrue(ex.getMessage().contains("already been consumed"));
    Arrays.sort(result);

    System.out.println(Arrays.toString(result).replace(", ", "\n"));
  }

  @Test
  void testSample2() {
    CDF cdf = CDF.fromRanks("""
        100 0
        200 0.3
        300 0.5
        800 0.5
        1000 1.0
        """);

    int n = 100;
    Random rnd = new Random(123);
    CDF.Datastream ds = cdf.makeStream(n, rnd);
    FixedBucketHistogram h = new FixedBucketHistogram(cdf.getBuckets());

    for (int i=0; i<n; i++) {
       h.addValue(ds.nextValue());
    }

    System.out.println(h.getCumulativeCounts());
  }
}
