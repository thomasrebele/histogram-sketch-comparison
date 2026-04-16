package histex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;

// TODO tr should close the resources
public class ResultOutputCollector {

  private final Path path;
  private final OutputStream output;

  public ResultOutputCollector(String basePath) throws IOException {
    String path = basePath;
    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    String hash = ShellUtils.exec("git", "rev-parse", "--short", "HEAD").trim();
    String gitStatus = ShellUtils.exec("git", "status", "--porcelain");
    boolean isCleanRepo = Arrays.asList("", "\n").contains(gitStatus);

    String prefix = time + "-" + hash + "-";
    if(!isCleanRepo) {
      System.out.println("Warning: there are modifications in the repository:\n" + gitStatus);
      prefix += "mod-";
    }

    int max = -1;
    File resultDir = Path.of(path).toFile();
    String fprefix = prefix;
    File[] files = resultDir.listFiles((dir, name) -> name.startsWith(fprefix));
    for (File f : Objects.requireNonNull(files)) {
      String substring = f.getName().substring(prefix.length());
      int i = Integer.parseInt(substring);
      max = Math.max(i, max);
    }

    path = path+prefix+(max+1);

    this.path = Path.of(path);
    this.path.toFile().mkdirs();

    System.out.println("Storing the results of the experiment in: " + this.path.toAbsolutePath());
    System.out.println();

    File f = this.path.resolve("output.txt").toFile();
    output = new FileOutputStream(f);

    println("Result based on commit " + hash + (isCleanRepo ? " (clean)" : " (WITH MODIFICATIONS)"));
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

  public File fileAppend(String file, String string) throws IOException {
    File f = path.resolve("./" + file).toFile();
    f.toPath().getParent().toFile().mkdirs();
    try(FileOutputStream fs = new FileOutputStream(f, true)) {
      fs.write(string.getBytes(StandardCharsets.UTF_8));
    }
    return f;
  }

  public Path getPath() {
    return path;
  }
}
