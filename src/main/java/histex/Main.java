package histex;

import histex.sketches.EquiWidthHistogram;
import histex.sketches.KllHistogram;
import histex.sketches.SplineSketchHistogram;
import histex.sketches.TDigestHistogram;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
  public static void main(String[] args) throws IOException {
    String rootPath = "draft-results/";

    //if (true) {
    //  Main.createLandingPages(Path.of(rootPath), Path.of(rootPath), null, null);
    //  return;
    //}

    ResultOutputCollector out = new ResultOutputCollector(rootPath);
    StringBuilder sbDescription = new StringBuilder();

    Path.of("draft-results/latest").toFile().delete();
    ShellUtils.exec("ln", "-s", out.getPath().toAbsolutePath().toString(), "draft-results/latest");


    final long seed = new Random(System.nanoTime()).nextLong();
    out.println("seed: " + seed);

    Supplier<FloatIterator> it = //
        //loadTpcdsColumn("src/main/resources/tpcds-column-dumps/sf100/item.i_brand_id.zstd", sbDescription)
        //loadTpcdsColumn("src/main/resources/tpcds-column-dumps/sf100/item.i_category_id.zstd", sbDescription)
        //loadTpcdsColumn("src/main/resources/tpcds-column-dumps/sf100/item.i_current_price.zstd", sbDescription)
        loadTpcdsColumn("src/main/resources/tpcds-column-dumps/sf100/item.i_wholesale_cost.zstd", sbDescription)
        ;

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

    out.fileAppend("desc.txt", sbDescription.toString());

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


      String rangeDir = "/" + String.format("%02d", idx) + "-range-" + rangeWidth;
      for (RankEstimator candidate : estimators) {
        Measure.Result result = Measure.evaluateMultiplicativeSelectivityDifference(
            goldstandard, candidate, sequence.get(), rangeWidth);
        String desc = String.format("%30s", candidate.getDesc());
        out.println(desc + ": " + result);

        String samples = Arrays.stream(result.samples()).map(Object::toString).collect(Collectors.joining("\n"));
        out.fileAppend(rangeDir + "/samples-" + candidate.getDesc(), samples);
      }


      rangeWidth *= 2;
    }

    // visualizations
    String vizDir = "viz/";
    StringBuilder toc = new StringBuilder();
    toc.append("file\tdesc\n");
    StringBuilder tocDiff = new StringBuilder();
    tocDiff.append("file\tdesc\n");
    for (RankEstimator candidate : estimators) {
      String csv = candidate.debugAsCsv();
      if(csv != null) {
        File f = out.fileAppend(vizDir + "/histogram-" + candidate.getDesc() + ".csv", csv);
        toc.append(f.getName()).append("\t").append(candidate.getDesc()).append("\n");
      }
      String diffCsv = candidate.debugDiffAsCsv(goldstandard);
      if(diffCsv != null) {
        File f = out.fileAppend(vizDir + "/histogram-" + candidate.getDesc() + "-diff.csv", diffCsv);
        tocDiff.append(f.getName()).append("\t").append(candidate.getDesc()).append("\n");
      }
    }

    out.fileAppend(vizDir + "/toc.csv", toc.toString());
    out.fileAppend(vizDir + "/toc-diff.csv", tocDiff.toString());


    Main.createLandingPages(Path.of(rootPath), Path.of(rootPath), null, null);
  }

  private static Supplier<FloatIterator> loadTpcdsColumn(String path, StringBuilder sbDescription) {
    String desc = path.replaceAll(".*sf", "sf").replace(".zstd", "");
    sbDescription.append("data: ").append(desc);
    return FloatIteratorFactory.readFile(path);
  }


  private interface TOCLink {
    String getHref(Path root);
    String getText(Path base);

    static String relativize(Path p, Path root) {
      return root.relativize(p).toString().replace("\\", "/");
    }
  }

  private record VizLink(Path viz, Path toc) implements TOCLink {

    @Override
    public String getHref(Path root) {
      String vizPath = root.relativize(viz).toString();
      return "/" + vizPath + "?toc=" + TOCLink.relativize(toc, root);
    }

    @Override
    public String getText(Path base) {
      return TOCLink.relativize(toc, base);
    }
  }

  private record DirLink(Path dir, String desc) implements TOCLink {
    @Override
    public String getHref(Path root) {
      return TOCLink.relativize(dir, root);
    }

    @Override
    public String getText(Path base) {
      return desc + " (" + dir.getFileName() + ")";
    }
  }


  private static List<TOCLink> createLandingPages(Path root, Path base, Path parentViz, String outerContext) throws IOException {
    List<TOCLink> tocs = new ArrayList<>();


    Path otherViz = base.resolve("viz.html");
    Path viz = Files.isRegularFile(otherViz) ? otherViz : parentViz;

    Path desc = base.resolve("desc.txt");

    String myContext = outerContext;
    if (Files.isRegularFile(desc)) {
      myContext = Files.readString(desc);
      myContext = outerContext == null ? myContext : outerContext + " / " + myContext;
    }
    String context = myContext;

    try(Stream<Path> walk = Files.walk(base, 1, FileVisitOption.FOLLOW_LINKS)) {
      walk.forEach(p -> {
        if (Objects.equals(p, base)) return;

        try {
          if (Files.isDirectory(p)) {
            tocs.addAll(createLandingPages(root, p, viz, context));
            return;
          }

          if (!Files.isRegularFile(p)) return;
          if (!p.getFileName().toString().startsWith("toc")) return;
          tocs.add(new VizLink(viz, p));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }

    tocs.sort(Comparator.comparing(t -> t.getHref(root)));
    createLandingPage(root, base, context, tocs);

    if (Files.isRegularFile(desc)) {
      return Collections.singletonList(new DirLink(base, Files.readString(desc)));
    }

    return tocs;
  }

  private static void createLandingPage(Path root, Path base, String context, List<TOCLink> tocs) throws IOException {
    // generate html TOC of all diagrams
    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html><!-- file generated, do not edit manually! --><html>");
    html.append("<style>a{text-decoration:none;}</style>");
    html.append("<body>\n<h1>").append(context).append("</h1><h2>").append(root.relativize(base)).append("</h2>\n<ul>\n");

    for (TOCLink toc : tocs) {
      // get path relative to base for clean links
      html.append("  <li><a href=\"").append(toc.getHref(root))
          .append("\">")
          .append(toc.getText(base))
          .append("</a></li>\n");
    }

    html.append("</ul>\n</body></html>");
    Files.write(base.resolve("index.html"), html.toString().getBytes());
  }

  private static String fmt(double v) {
    return String.format("%4.4g", v);
  }
}