package histex;

import java.util.Arrays;
import java.util.Random;

public class Main {
  public static void main(String[] args) {
    CDF uniform = CDFFactory.uniform(100, 1000);

    EquiWidthHistogram h = new EquiWidthHistogram(100, 1000, 2000);

    CDF.Datastream ds = uniform.makeStream(100000, new Random(123));
    h.consume(ds);

    KllHistogram kll = new KllHistogram();
    ds = uniform.makeStream(100000, new Random(123));
    kll.consume(ds);

    System.out.println(Arrays.toString(h.getRanks(h.getBucketEnds())));
    System.out.println(Arrays.toString(kll.getRanks(h.getBucketEnds())));
  }
}