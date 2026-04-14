package histex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Main {
  public static void main(String[] args) throws IOException {

    ResultOutputCollector out = new ResultOutputCollector();

    Path.of("draft-results/latest").toFile().delete();
    ShellUtils.exec("ln", "-s", out.getPath().toAbsolutePath().toString(), "draft-results/latest");


    final long seed = new Random(System.nanoTime()).nextLong();
    out.println("seed: " + seed);

    Supplier<FloatIterator> it =
        FloatIteratorFactory.readFile("src/main/resources/tpcds-column-dumps/sf100/item.i_brand_id.zstd");

    out.println("distribution: TODO" );

    Supplier<? extends FloatIterator> ds = it;
    GoldstandardRankEstimator goldstandard = new GoldstandardRankEstimator(ds);

    float min = goldstandard.getMin();
    float max = goldstandard.getMax();

    List<Histogram> histograms = new ArrayList<>();
    histograms.add(new EquiWidthHistogram(min, max, 1200, true));
    histograms.add(new EquiWidthHistogram(min, max, 600, true));
    histograms.add(new EquiWidthHistogram(min, max, 200, true));
    histograms.add(new EquiWidthHistogram(min, max, 11, true));
    //histograms.add(new EquiWidthHistogram(min, max, 1200, false));
    EquiWidthHistogram e = new EquiWidthHistogram(min, max, 11, false);
    histograms.add(e);
    histograms.add(goldstandard.convertToEquiHeightHistogram(200, true));
    histograms.add(goldstandard.convertToEquiHeightHistogram(200, false));

    histograms.add(new KllHistogram());
    histograms.add(new TDigestHistogram());

    for (Histogram h : histograms) {
      h.consume(ds.get());
    }

    out.println("memory usage in bytes:");
    for (Histogram h : histograms) {
      out.println(h.getDesc() + ", mem size: " + h.getMemoryUsageInBytes() + ", n=" + h.getN());
    }

    out.println("range of values: [" + min + "," + max + "]");

    List<RankEstimator> estimators = new ArrayList<>();
    estimators.addAll(histograms);
    estimators.add(goldstandard);

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

      StringBuilder toc = new StringBuilder();
      toc.append("file\tdesc\n");
      StringBuilder tocDiff = new StringBuilder();
      tocDiff.append("file\tdesc\n");
      String rangeDir = "/" + idx + "-range-" + rangeWidth;
      for (RankEstimator candidate : estimators) {
        Measure.Result result = Measure.evaluateMultiplicativeSelectivityDifference(
            goldstandard, candidate, sequence.get(), rangeWidth);
        String desc = String.format("%30s", candidate.getDesc());
        out.println(desc + ": " + result);

        String samples = Arrays.stream(result.samples()).map(Object::toString).collect(Collectors.joining("\n"));
        out.fileAppend(rangeDir + "/samples-" + candidate.getDesc(), samples);
        String csv = candidate.debugAsCsv();
        if(csv != null) {
          File f = out.fileAppend(rangeDir + "/histogram-" + candidate.getDesc() + ".csv", csv);
          toc.append(f.getName()).append("\t").append(candidate.getDesc()).append("\n");
        }
        String diffCsv = candidate.debugDiffAsCsv(goldstandard);
        if(diffCsv != null) {
          File f = out.fileAppend(rangeDir + "/histogram-" + candidate.getDesc() + "-diff.csv", diffCsv);
          tocDiff.append(f.getName()).append("\t").append(candidate.getDesc()).append("\n");
        }
      }
      out.fileAppend(rangeDir + "/toc.csv", toc.toString());
      out.fileAppend(rangeDir + "/toc-diff.csv", tocDiff.toString());

      rangeWidth *= 2;
    }
  }

  private static String fmt(double v) {
    return String.format("%4.4g", v);
  }
}