package histex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Main {
  public static void main(String[] args) throws IOException {

    ResultOutputCollector out = new ResultOutputCollector();
    final long seed = new Random(System.nanoTime()).nextLong();
    out.println("seed: " + seed);

    Supplier<FloatIterator> it =
        FloatIteratorFactory.readFile("src/main/resources/tpcds-column-dumps/sf1/store_sales.ss_ext_list_price.zstd");

    out.println("distribution: TODO" );

    Supplier<? extends FloatIterator> ds = it;

    TDigestHistogram tdigest = new TDigestHistogram();
    tdigest.consume(ds.get());

    float min = (float)tdigest.getMin();
    float max = (float)tdigest.getMax();

    List<Histogram> histograms = new ArrayList<>();
    histograms.add(new EquiWidthHistogram(min, max, 1200));
    histograms.add(new EquiWidthHistogram(min, max, 600));
    histograms.add(new EquiWidthHistogram(min, max, 200));
    histograms.add(new EquiWidthHistogram(min, max, 10));
    histograms.add(new KllHistogram());

    for (Histogram h : histograms) {
      h.consume(ds.get());
    }

    histograms.add(tdigest);

    out.println("memory usage in bytes:");
    for (Histogram h : histograms) {
      out.println(h.getDesc() + ", mem size: " + h.getMemoryUsageInBytes() + ", n=" + h.getN());
    }

    out.println("range of values: " + tdigest.getInfo());

    GoldstandardRankEstimator goldstandard = new GoldstandardRankEstimator(ds);

    out.println("");
    out.println("compare the multiplicative accuracy of range predicate selectivity");
    out.println();
    int idx = -1;
    float totalWidth = max-min;
    float rangeWidth = (float) (totalWidth*1e-7);
    while (rangeWidth < totalWidth) {
      idx += 1;
      out.println();
      out.println("--------------------------------------------------------------------------------");
      out.println("range width " + rangeWidth);
      final float rw = rangeWidth;
      Supplier<FloatIterator> sequence = () -> FloatIterator.sequence(min, max - rw, 10000);

      for (Histogram candidate : histograms) {
        Measure.Result result = Measure.evaluateMultiplicativeSelectivityDifference(
            goldstandard, candidate, sequence.get(), rangeWidth);
        String desc = String.format("%20s", candidate.getDesc());
        out.println(desc + ": " + result);

        String samples = Arrays.stream(result.samples()).map(Object::toString).collect(Collectors.joining("\n"));
        out.fileAppend("/" + idx + "-range-" + rangeWidth + "/" + candidate.getDesc(), samples);
      }

      rangeWidth *= 2;
    }
  }

  private static String fmt(double v) {
    return String.format("%4.4g", v);
  }
}