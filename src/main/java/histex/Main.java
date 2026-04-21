package histex;

import histex.sketches.EquiWidthHistogram;
import histex.sketches.KllHistogram;
import histex.sketches.SplineSketchHistogram;
import histex.sketches.TDigestHistogram;
import org.apache.commons.lang3.tuple.Pair;

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
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;

public class Main {

  static List<String> TPCDS_DUMPS_OTHERS = Arrays.asList(
      "sf100000/item.i_brand_id.zstd",
      "sf100000/item.i_category_id.zstd",
      "sf100000/item.i_class_id.zstd",
      "sf100000/item.i_item_sk.zstd",
      "sf100000/item.i_manager_id.zstd",
      "sf100000/item.i_rec_end_date.zstd",
      "sf100000/item.i_rec_start_date.zstd",
      "sf100000/item.i_wholesale_cost.zstd",
      "sf10/store_sales.ss_store_sk.zstd",
      "sf10/store_sales.ss_ticket_number.zstd",
      "sf1/store_sales.ss_addr_sk.zstd",
      "sf1/store_sales.ss_cdemo_sk.zstd",
      "sf1/store_sales.ss_customer_sk.zstd",
      "sf1/store_sales.ss_ext_discount_amt.zstd",
      "sf1/store_sales.ss_ext_list_price.zstd",
      "sf1/store_sales.ss_ext_sales_price.zstd",
      "sf1/store_sales.ss_ext_tax.zstd",
      "sf1/store_sales.ss_ext_wholesale_cost.zstd",
      "sf1/store_sales.ss_hdemo_sk.zstd",
      "sf1/store_sales.ss_item_sk.zstd",
      "sf1/store_sales.ss_net_paid_inc_tax.zstd",
      "sf1/store_sales.ss_promo_sk.zstd",
      "sf1/store_sales.ss_sold_time_sk.zstd"
  );


  static List<String> TPCDS_DUMPS_TARGET = Arrays.asList(
      "sf100/customer_address.ca_zip.zstd",
      "sf10/catalog_returns.cr_return_amount.zstd",
      "sf1/catalog_sales.cs_net_paid.zstd",
      "sf1/catalog_sales.cs_net_profit.zstd",
      "sf1/catalog_sales.cs_quantity.zstd",
      "tiny/date_dim.d_date.zstd",
      "tiny/date_dim.d_dom.zstd",
      "tiny/date_dim.d_month_seq.zstd",
      "tiny/date_dim.d_moy.zstd",
      "tiny/date_dim.d_qoy.zstd",
      "tiny/date_dim.d_year.zstd",
      "tiny/household_demographics.hd_vehicle_count.zstd",
      "tiny/income_band.ib_lower_bound.zstd",
      "tiny/income_band.ib_upper_bound.zstd",
      "sf100000/item.i_current_price.zstd",
      "sf100000/item.i_manufact_id.zstd",
      "sf1/inventory.inv_quantity_on_hand.zstd",
      "sf100000/store.s_number_employees.zstd",
      "sf10/store_returns.sr_return_amt.zstd",
      "sf1/store_sales.ss_coupon_amt.zstd",
      "sf1/store_sales.ss_list_price.zstd",
      "sf1/store_sales.ss_net_paid.zstd",
      "sf1/store_sales.ss_net_profit.zstd",
      "sf1/store_sales.ss_quantity.zstd",
      "sf1/store_sales.ss_sales_price.zstd",
      "sf1/store_sales.ss_wholesale_cost.zstd",
      "tiny/time_dim.t_hour.zstd",
      "tiny/time_dim.t_minute.zstd",
      "tiny/time_dim.t_time.zstd",
      "sf100000/web_page.wp_char_count.zstd",
      "sf10/web_returns.wr_return_amt.zstd",
      "sf1/web_sales.ws_net_paid.zstd",
      "sf1/web_sales.ws_net_profit.zstd",
      "sf10/web_sales.ws_quantity.zstd"
  );

  static List<String> TPCDS_DUMPS_TEST = Arrays.asList(
      "sf1/store_sales.ss_quantity.zstd"
  );

  static List<String> TPCDS_DUMPS = TPCDS_DUMPS_TARGET;

  private static final String TPCDS_DUMP_PREFIX = "src/main/resources/tpcds-column-dumps/";

  private static List<Histogram> createHistogramList(ExperimentRunner.MapArgument map) {
    var ds = (Dataset) map.get("dataset");
    var goldstandard = (GoldstandardRankEstimator) map.get("goldstandard");
    float min = goldstandard.getMin();
    float max = goldstandard.getMax();

    List<Histogram> histograms = new ArrayList<>();
    histograms.add(new EquiWidthHistogram(min, max, 1200, true));
    histograms.add(new EquiWidthHistogram(min, max, 600, true));
    histograms.add(new EquiWidthHistogram(min, max, 200, true));
    //histograms.add(new EquiWidthHistogram(min, max, 11, true));
    histograms.add(new EquiWidthHistogram(min, max, 1200, false));
    //histograms.add(new EquiWidthHistogram(min, max, 11, false));
    histograms.add(goldstandard.convertToEquiHeightHistogram(200, true));
    histograms.add(goldstandard.convertToEquiHeightHistogram(200, false));

    histograms.add(new KllHistogram());
    histograms.add(new SplineSketchHistogram(200));
    histograms.add(new TDigestHistogram());

    for (Histogram h : histograms) {
      h.consume(ds.data().get());
    }
    return histograms;
  }

  private static List<RankEstimator> createRankEstimatorList(ExperimentRunner.MapArgument map) {
    List<RankEstimator> estimators = new ArrayList<>();
    estimators.addAll(createHistogramList(map));

    var goldstandard = (GoldstandardRankEstimator) map.get("goldstandard");
    estimators.add(goldstandard);

    return estimators;
  }


  private static record Dataset(String desc, Supplier<FloatIterator> data) {
    @Override
    public String toString() {
      return desc;
    }
  }

  public static void main(String[] args) throws IOException, CloneNotSupportedException {
    String rootPath = "draft-results/";

    //if (true) {
    //  Main.createLandingPages(Path.of(rootPath), Path.of(rootPath), null, null);
    //  return;
    //}

    ResultOutputCollector out = ResultOutputCollector.of(rootPath);
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

    ExperimentRunner<ExperimentRunner.MapArgument> er = new ExperimentRunner<>();

    List<Dataset> datasets = TPCDS_DUMPS.stream().map(Main::loadTpcdsColumn).toList();

    out.fileAppend("desc.txt", datasets.size() + "_datasets");

    er.factor("dataset", datasets);
    er.factor("goldstandard", map -> {
      var ds = (Dataset) map.get("dataset");
      var gs = new GoldstandardRankEstimator(ds.data());
      if (gs.size() == 0) {
        System.out.println("could not load " + ds.desc);
        return List.of();
      }
      return List.of(gs);
    }).dependsOn("dataset");
    er.factor("histogram", Main::createHistogramList).dependsOn("goldstandard");
    er.factor("rankEstimator", Main::createRankEstimatorList).dependsOn("goldstandard");

    er.factor("rangeWidth", map -> {
      var goldstandard = (GoldstandardRankEstimator) map.get("goldstandard");
      float min = goldstandard.getMin();
      float max = goldstandard.getMax();

      List<Pair<Float, Integer>> rangeWidths = new ArrayList<>();
      int idx = -1;
      float totalWidth = max-min;
      float rangeWidth = (float) (totalWidth*1e-7);
      while (rangeWidth < totalWidth) {
        idx += 1;
        rangeWidths.add(Pair.of(rangeWidth, idx));
        rangeWidth *= 2;
      }
      return rangeWidths;
    }).dependsOn("goldstandard");


    // create visualizations
    er.prepare().factors(List.of("dataset", "goldstandard", "rankEstimator")).call(map -> {
      try {
        var ds = (Dataset)map.get("dataset");
        var myout = out.sub(ds.desc.replace("/", "_"));
        var goldstandard = (GoldstandardRankEstimator) map.get("goldstandard");

        // visualizations
        String vizDir = "viz/";
        var candidate = (RankEstimator) map.get("rankEstimator");

        String tocEntry = null;
        String tocDiffEntry = null;

        String csv = candidate.debugAsCsv();
        if (csv != null) {
          File f = myout.fileAppend(vizDir + "/histogram-" + candidate.getDesc() + ".csv", csv);
          tocEntry = f.getName() + "\t" + candidate.getDesc() + "\n";
        }
        String diffCsv = candidate.debugDiffAsCsv(goldstandard);
        if (diffCsv != null) {
          File f = myout.fileAppend(vizDir + "/histogram-" + candidate.getDesc() + "-diff.csv", diffCsv);
          tocDiffEntry = f.getName() + "\t" + candidate.getDesc() + "\n";
        }
        return map.with("toc", tocEntry).with("tocDiff", tocDiffEntry);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }, new ExperimentRunner.MapArgument()).aggregators(Map.ofEntries(entry("rankEstimator", (ExperimentRunner.Aggregator<ExperimentRunner.MapArgument>) (map, result) -> {
      var ds = (Dataset)map.get("dataset");
      try {
        var myout = out.sub(ds.desc.replace("/", "_"));
        myout.fileAppend("desc.txt", ds.desc);

        String vizDir = "viz/";

        StringBuilder toc = new StringBuilder();
        toc.append("file\tdesc\n");
        StringBuilder tocDiff = new StringBuilder();
        tocDiff.append("file\tdesc\n");

        for (ExperimentRunner.MapArgument r : result) {
          Object tocEntry = r.get("toc");
          if (tocEntry != null) toc.append(tocEntry);
          Object tocDiffEntry = r.get("tocDiff");
          if (tocDiffEntry != null) tocDiff.append(tocDiffEntry);
        }

        myout.fileAppend(vizDir + "/toc.csv", toc.toString());
        myout.fileAppend(vizDir + "/toc-diff.csv", tocDiff.toString());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return map;
    })))
    //.run()
    ;

    out.println("");
    out.println("compare the multiplicative accuracy of range predicate selectivity");
    out.println();
    er.prepare().factors(List.of("dataset", "goldstandard", "rankEstimator", "rangeWidth")).call(map -> {
      var ds = (Dataset)map.get("dataset");
      var goldstandard = (GoldstandardRankEstimator) map.get("goldstandard");
      float min = goldstandard.getMin();
      float max = goldstandard.getMax();
      Pair<Float, Integer> rangeWidth = (Pair<Float, Integer>)map.get("rangeWidth");
      float rw = rangeWidth.getLeft();
      int idx = rangeWidth.getRight();

      Supplier<FloatIterator> sequence = () -> FloatIterator.sequence(min, max - rw, 10000);

      String rangeDir = "/" + String.format("%02d", idx) + "-range-" + rangeWidth;
      RankEstimator candidate = (RankEstimator) map.get("rankEstimator");

      Measure.Result result = Measure.evaluateMultiplicativeSelectivityDifference(
          goldstandard, candidate, sequence.get(), rw);
      try {
        var myout = out.sub(ds.desc.replace("/", "_"));
        String desc = String.format("%30s", candidate.getDesc());
        String samples = Arrays.stream(result.samples()).map(Object::toString).collect(Collectors.joining("\n"));
        myout.println(desc + ": " + result);
        myout.fileAppend(rangeDir + "/samples-" + candidate.getDesc(), samples);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      return map.with("result", result);
    }, new ExperimentRunner.MapArgument()).aggregators(Map.ofEntries(
        entry("rangeWidth",(ExperimentRunner.Aggregator<ExperimentRunner.MapArgument>) (map, result) -> {

      var candidate = (RankEstimator) map.get("rankEstimator");
      String vizDir = "viz/";
      String file =  candidate.getDesc() + ".csv";

      try {
        var sb = new StringBuilder();
        sb.append("x,y\n");

        var ds = (Dataset)map.get("dataset");
        var myout = out.sub(ds.desc.replace("/", "_"));
        for (ExperimentRunner.MapArgument r : result) {
          Measure.Result mr = (Measure.Result) r.get("result");
          float rw = ((Pair<Float, Integer>)r.get("rangeWidth")).getLeft();
          if (Double.isFinite(mr.scaleDiff())) {
            sb.append(rw).append(",").append(mr.scaleDiff()).append("\n");
          }
        }

        myout.fileAppend(vizDir + file, sb.toString());

      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      String tocEntry = file + "\t" + candidate.getDesc() + "\n";
      return map.with("toc", tocEntry);
    }),
      entry("rankEstimator", (ExperimentRunner.Aggregator<ExperimentRunner.MapArgument>) (map, result) -> {

        String vizDir = "viz/";
        var ds = (Dataset)map.get("dataset");
        try {
          var myout = out.sub(ds.desc.replace("/", "_"));
          StringBuilder toc = new StringBuilder();
          toc.append("file\tdesc\n");

          for(ExperimentRunner.MapArgument r : result) {
            Object tocEntry = r.get("toc");
            if (tocEntry != null) {
              toc.append(tocEntry);
            }
          }

          myout.fileAppend(vizDir + "/toc.csv", toc.toString());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        return map;
          }
      ))).run();

    Main.createLandingPages(Path.of(rootPath), Path.of(rootPath), null, null);

    //er.call(List.of("dataset", "goldstandard", "histogram", "rangeWidth"), map -> {

    //  System.out.println(map);

    //  return map;
    //}, new ExperimentRunner.MapArgument());

    if (true) {
      return;
    }

    out.println("distribution: TODO" );

    Supplier<? extends FloatIterator> ds = it;
    GoldstandardRankEstimator goldstandard = new GoldstandardRankEstimator(ds);

    float min = goldstandard.getMin();
    float max = goldstandard.getMax();

    List<Histogram> histograms = new ArrayList<>();
    histograms.add(new EquiWidthHistogram(min, max, 1200, true));
    histograms.add(new EquiWidthHistogram(min, max, 600, true));
    histograms.add(new EquiWidthHistogram(min, max, 200, true));
    //histograms.add(new EquiWidthHistogram(min, max, 11, true));
    histograms.add(new EquiWidthHistogram(min, max, 1200, false));
    //histograms.add(new EquiWidthHistogram(min, max, 11, false));
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




  }

  private static Dataset loadTpcdsColumn(String path) {
    String desc = path.replaceAll(".*sf", "sf").replace(".zstd", "");
    return new Dataset(desc, FloatIteratorFactory.readFile(TPCDS_DUMP_PREFIX + path));
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
      return "/" + TOCLink.relativize(dir, root);
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
    String headline = context == null ? "TOC" : context;
    html.append("<body>\n<h1>").append(headline).append("</h1><h2>").append(root.relativize(base)).append("</h2>\n<ul>\n");

    for (TOCLink toc : tocs) {
      // get path relative to base for clean links
      String href = toc.getHref(root);
      if(href.contains("latest/latest")) {
        System.out.println("here");
      }
      html.append("  <li><a href=\"").append(href)
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