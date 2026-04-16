package histex;

import histex.sketches.EquiWidthHistogram;
import histex.sketches.KllHistogram;
import histex.sketches.SplineSketchHistogram;
import histex.sketches.TDigestHistogram;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
  public static void main(String[] args) throws IOException {
    String basePath = "draft-results/";
    ResultOutputCollector out = new ResultOutputCollector(basePath);

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
    histograms.add(new EquiWidthHistogram(min, max, 1200, false));
    EquiWidthHistogram e = new EquiWidthHistogram(min, max, 11, false);
    histograms.add(e);
    histograms.add(goldstandard.convertToEquiHeightHistogram(200, true));
    histograms.add(goldstandard.convertToEquiHeightHistogram(200, false));

    histograms.add(new KllHistogram());
    histograms.add(new SplineSketchHistogram(200));
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

    Main.createLandingPage(basePath);
  }

  private static void createLandingPage(String basePath) throws IOException {
    Path root = Paths.get(basePath);

    // look for toc files
    List<Path> tocFiles;
    try (Stream<Path> walk = Files.walk(root)) {
      tocFiles = walk.filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().startsWith("toc"))
          .collect(Collectors.toCollection(ArrayList::new));
    }
    Collections.sort(tocFiles);

    // generate html TOC of all diagrams
    StringBuilder html = new StringBuilder();
    html.append("<!-- file generated, do not edit manually! --><html><body>\n<h1>TOC Index</h1>\n<ul>\n");

    for (Path file : tocFiles) {
      // get path relative to base for clean links
      String relativePath = root.relativize(file).toString().replace("\\", "/");
      html.append("  <li><a href=\"viz.html?toc=")
          .append(relativePath)
          .append("\">")
          .append(relativePath)
          .append("</a></li>\n");
    }

    html.append("</ul>\n</body></html>");
    Files.write(root.resolve("index.html"), html.toString().getBytes());
  }

  private static String fmt(double v) {
    return String.format("%4.4g", v);
  }
}