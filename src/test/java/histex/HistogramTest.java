package histex;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HistogramTest {

  @Test
  void test() {
    FixedBucketHistogram h = new FixedBucketHistogram(new float[]{10, 50, 500, 1000});
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
}
