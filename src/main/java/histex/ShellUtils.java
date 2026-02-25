package histex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ShellUtils {
  public static String exec(String... cmd) throws IOException {
      Process process = Runtime.getRuntime().exec(cmd);
      var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }
      return sb.toString();
  }
}
