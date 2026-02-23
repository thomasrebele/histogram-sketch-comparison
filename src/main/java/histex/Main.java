package histex;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;

public class Main {
  public static void main(String[] args) {
    CDF uniform = CDFFactory.uniform(100, 1000);

    EquiWidthHistogram h = new EquiWidthHistogram(100, 1000, 2000);

    Supplier<CDF.Datastream> ds = uniform.prepareStream(100000, ()->new Random(123));
    h.consume(ds.get());

    KllHistogram kll = new KllHistogram();
    kll.consume(ds.get());

    TDigestHistogram tdigest = new TDigestHistogram();
    tdigest.consume(ds.get());

    System.out.println(Arrays.toString(h.getRanks(h.getBucketEnds())));
    System.out.println(Arrays.toString(kll.getRanks(h.getBucketEnds())));
    System.out.println(Arrays.toString(tdigest.getRanks(h.getBucketEnds())));

    System.out.println();
    float rangeWidth = 0.5f;
    Supplier<FloatIterator> sequence = () -> FloatIterator.sequence(100, 1000, 10000);
    System.out.println((Measure.evaluateMultiplicativeSelectivityDifference(h, kll, sequence.get(), rangeWidth)));
    System.out.println();
    System.out.println();
    System.out.println((Measure.evaluateMultiplicativeSelectivityDifference(h, tdigest, sequence.get(), rangeWidth)));
  }

  private static String fmt(double v) {
    return String.format("%4.4g", v);
  }
}