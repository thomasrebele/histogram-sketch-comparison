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

  float getMin() {
    return values.get(0);
  }

  float getMax() {
    return values.get(values.size()-1);
  }

  @Override
  public String getDesc() {
    return "goldstandard";
  }

  @Override
  public double getNormalizedRank(float value) {
    // TODO tr how to handle duplicate values?

    int i = Collections.binarySearch(values, value);
    return (double)(i < 0 ? -(i+1) : i) / values.size();
  }
}
