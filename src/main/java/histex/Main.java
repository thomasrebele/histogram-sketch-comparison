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

    DiscreteCDF uniform = CDFFactory.gaussian(500, 100, 3000);

    out.println("distribution: " + uniform.getDesc());

    Supplier<DiscreteCDF.Datastream> ds = uniform.prepareStream(1000000, ()->new Random(seed));

    List<Histogram> histograms = new ArrayList<>();

    histograms.add(new EquiWidthHistogram(100, 1000, 1200));
    histograms.add(new EquiWidthHistogram(100, 1000, 600));
    histograms.add(new EquiWidthHistogram(100, 1000, 200));
    histograms.add(new EquiWidthHistogram(100, 1000, 10));
    histograms.add(new KllHistogram());
    histograms.add(new TDigestHistogram());

    out.println("memory usage in bytes:");
    for (Histogram h : histograms) {
      h.consume(ds.get());
      out.println(h.getDesc() + ", mem size: " + h.getMemoryUsageInBytes() + ", n=" + h.getN());
    }

    GoldstandardRankEstimator goldstandard = new GoldstandardRankEstimator(ds);

    out.println("");
    out.println("compare the multiplicative accuracy of range predicate selectivity");
    out.println();
    int idx = -1;
    float rangeWidth = 0.00001f;
    while (rangeWidth < 1000) {
      idx += 1;
      out.println();
      out.println("--------------------------------------------------------------------------------");
      out.println("range width " + rangeWidth);
      final float rw = rangeWidth;
      Supplier<FloatIterator> sequence = () -> FloatIterator.sequence(100, 1000 - rw, 10000);

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