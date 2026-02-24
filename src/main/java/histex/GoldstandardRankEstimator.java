package histex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Supplier;

public class GoldstandardRankEstimator implements RankEstimator {

  private final ArrayList<Float> values;

  public GoldstandardRankEstimator(Supplier<? extends FloatIterator> supplier) {
    FloatIterator it = supplier.get();

    values = new ArrayList<>();
    while (it.hasNext()) {
      values.add(it.nextValue());
    }
    Collections.sort(values);
  }

  @Override
  public double getRank(float value) {
    // TODO tr how to handle duplicate values?

    int i = Collections.binarySearch(values, value);
    return i < 0 ? -(i+1) : i;
  }
}
