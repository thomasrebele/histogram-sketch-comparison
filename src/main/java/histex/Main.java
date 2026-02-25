package histex;

import java.io.IOException;
import java.util.Random;
import java.util.function.Supplier;

public class Main {
  public static void main(String[] args) throws IOException {

    ResultOutputCollector out = new ResultOutputCollector();

    CDF uniform = CDFFactory.gaussian(500, 100, 3000);
    Supplier<CDF.Datastream> ds = uniform.prepareStream(1000000, ()->new Random(123));

    out.println("memory usage in bytes:");

    EquiWidthHistogram h1 = new EquiWidthHistogram(100, 1000, 600);
    h1.consume(ds.get());
    out.println("equi-width histogram with " + h1.buckets + " buckets, size: " + h1.getMemoryUsageInBytes());

    EquiWidthHistogram h2 = new EquiWidthHistogram(100, 1000, 200);
    h2.consume(ds.get());
    out.println("equi-width histogram with " + h2.buckets + " buckets, size: " + h2.getMemoryUsageInBytes());

    EquiWidthHistogram h3 = new EquiWidthHistogram(100, 1000, 10);
    h3.consume(ds.get());
    out.println("equi-width histogram with " + h3.buckets + " buckets, size: " + h3.getMemoryUsageInBytes());

    KllHistogram kll = new KllHistogram();
    kll.consume(ds.get());
    out.println("KLL sketch size: " + kll.getMemoryUsageInBytes());

    TDigestHistogram tdigest = new TDigestHistogram();
    tdigest.consume(ds.get());
    out.println("t-digest sketch size: " + tdigest.getMemoryUsageInBytes());

    //out.println(Arrays.toString(h1.getRanks(h1.getBucketEnds())));
    //out.println(Arrays.toString(kll.getRanks(h1.getBucketEnds())));
    //out.println(Arrays.toString(tdigest.getRanks(h1.getBucketEnds())));

    GoldstandardRankEstimator e = new GoldstandardRankEstimator(ds);

    out.println("compare the multiplicative accuracy of range predicate selectivity");
    out.println();
    float rangeWidth = 0.00001f;
    while (rangeWidth < 1000) {
      out.println();
      out.println();
      out.println("--------------------------------------------------------------------------------");
      out.println("range width " + rangeWidth);
      final float rw = rangeWidth;
      Supplier<FloatIterator> sequence = () -> FloatIterator.sequence(100, 1000 - rw, 10000);
      out.println("equi-width histogram with " + h1.buckets);
      out.println((Measure.evaluateMultiplicativeSelectivityDifference(e, h1, sequence.get(), rangeWidth)));

      out.println();
      out.println("equi-width histogram with " + h2.buckets);
      out.println((Measure.evaluateMultiplicativeSelectivityDifference(e, h2, sequence.get(), rangeWidth)));

      out.println();
      out.println("equi-width histogram with " + h3.buckets);
      out.println((Measure.evaluateMultiplicativeSelectivityDifference(e, h3, sequence.get(), rangeWidth)));

      out.println();
      out.println("KLL sketch");
      out.println((Measure.evaluateMultiplicativeSelectivityDifference(e, kll, sequence.get(), rangeWidth)));

      out.println();
      out.println("t-digest sketch");
      out.println((Measure.evaluateMultiplicativeSelectivityDifference(e, tdigest, sequence.get(), rangeWidth)));

      rangeWidth *= 2;
    }
  }

  private static String fmt(double v) {
    return String.format("%4.4g", v);
  }
}