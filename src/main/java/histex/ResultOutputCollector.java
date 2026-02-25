package histex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ResultOutputCollector {

  private final Path path;
  private final OutputStream output;

  public ResultOutputCollector() throws IOException {
    LocalDateTime now = LocalDateTime.now();
    String path = "results/";
    String prefix = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
        + "-" + ShellUtils.exec("git", "rev-parse", "--short", "HEAD") + "-";

    int max = -1;
    File resultDir = Path.of(path).toFile();
    File[] files = resultDir.listFiles((dir, name) -> name.startsWith(prefix));
    for (File f : Objects.requireNonNull(files)) {
      String substring = f.getName().substring(prefix.length());
      int i = Integer.parseInt(substring);
      max = Math.max(i, max);
    }

    path = path+prefix+(max+1);

    this.path = Path.of(path);
    this.path.toFile().mkdirs();

    System.out.println("Storing the results of the experiment in: " + this.path.toAbsolutePath());

    File f = this.path.resolve("output.txt").toFile();
    output = new FileOutputStream(f);
  }

  public void println(Object... objs) throws IOException {
    for (Object s : objs) {
      output.write(Objects.toString(s).getBytes(StandardCharsets.UTF_8));
      System.out.print(s);
    }
    output.write('\n');
    System.out.println();
    output.flush();
  }
}
